package com.kk.fileupload;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kk.utils.LogUtil;
import com.kk.utils.security.Md5;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;

import rx.Observable;

/**
 * Created by zhangkai on 2017/5/24.
 */

public class SocketBigFileUpload extends IBigFileUpload {
    private String ip;
    private int port;
    private Socket socket;
    private String uuid;
    private String uid;

    private int timeout;

    public SocketBigFileUpload(String ip, int port, String uuid, String uid, Builder builder) {
        super(builder);
        this.ip = ip;
        this.port = port;
        this.uuid = uuid;
        this.uid = uid;
    }

    public static class Builder extends IBigFileUpload.Builder {
        private int timeout = 10 * 1000;

        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setFile(File bigFile) {
            this.bigFile = bigFile;
            return this;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setListener(BigFileUploadListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setReadSize(int readSize) {
            this.readSize = readSize;
            return this;
        }

        public SocketBigFileUpload build(String ip, int port, String uuid, String uid) {
            return new SocketBigFileUpload(ip, port, uuid, uid, this);
        }
    }

    @Override
    public void upload() {
        try {
            socket = new Socket(ip, port);
            socket.setSoTimeout(timeout);
            OutputStream outStream = socket.getOutputStream();
            outStream.write(toJson().getBytes());
            ResultInfo resultInfo = getResultInfo();

            if (resultInfo == null) {
                listener.failure(toResponseInfo(resultInfo));
                return;
            }

            if (resultInfo.code == 1) {
                listener.success(toResponseInfo(resultInfo));
                return;
            }

            if (resultInfo.code == 0) {
                int read;
                long readLength = 0;
                long filesize = bigFile.length();
                try {
                    int offset = (int) resultInfo.package_index;
                    randomAccessFile.seek(offset);
                    readLength = offset;
                    byte[] buffer = new byte[readSize];
                    while (readLength <= filesize - readSize) {
                        read = randomAccessFile.read(buffer, 0, this.readSize);
                        if (read == -1) break;
                        readLength += read;
                        outStream.write(buffer, 0, read);
                        listener.process(readLength / (float) filesize);
                    }
                    if (readLength < filesize) {
                        read = randomAccessFile.read(buffer, 0, (int) (filesize - readLength
                        ));
                        if (read != -1) {
                            outStream.write(buffer, 0, read);
                            readLength += read;
                        }
                    }

                    listener.process(readLength / (float) filesize);

                    resultInfo = getResultInfo();
                    if (resultInfo == null || resultInfo.code != 1) {
                        listener.failure(toResponseInfo(resultInfo));
                        return;
                    }

                    listener.success(toResponseInfo(resultInfo));

                } catch (Exception e) {
                    e.printStackTrace();
                    listener.failure(toResponseInfo(resultInfo));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            listener.failure(null);
        } finally {
            close();
        }
    }

    private ResultInfo getResultInfo() {
        ResultInfo resultInfo = null;
        try {
            PushbackInputStream inputStream = new PushbackInputStream(socket.getInputStream());
            byte[] buffer = new byte[1024];
            int len = inputStream.read(buffer, 0, buffer.length);
            if (len <= 0) {
                return resultInfo;
            }
            resultInfo = JSON.parseObject(new String(buffer, 0, len), ResultInfo.class);
        } catch (Exception e) {

        }
        return resultInfo;
    }


    private void close() {
        if (socket != null) {
            try {
                socket.getOutputStream().close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private String toJson() {
        return "{\"file_name\":\"" + bigFile.getName() + "\", \"file_size\":" + bigFile.length() + ", \"source_id\": " +
                "\"" +
                md5Check +
                "\",\"imeil\":\"" + uuid + "\", \"user_id\":\"" + uid + "\"}";
    }


}
