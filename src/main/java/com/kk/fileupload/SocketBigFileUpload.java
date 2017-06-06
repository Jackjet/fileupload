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

/**
 * Created by zhangkai on 2017/5/24.
 */

public class SocketBigFileUpload extends IBigFileUpload {
    private String ip;
    private int port;
    private Socket socket;

    public SocketBigFileUpload(String ip, int port, Builder builder) {
        super(builder);
        this.ip = ip;
        this.port = port;

        setRandomAccessFile();
    }

    public static class Builder extends IBigFileUpload.Builder {
        public Builder setFile(File bigFile) {
            this.bigFile = bigFile;
            return this;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setListener(HttpBigFileUpload.BigFileUploadListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setReadSize(int readSize) {
            this.readSize = readSize;
            return this;
        }


        public SocketBigFileUpload build(String ip, int port) {
            return new SocketBigFileUpload(ip, port, this);
        }
    }

    @Override
    public void upload() {
        try {
            socket = new Socket(ip, port);
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
                    byte[] buffer = new byte[readSize];
                    while (readLength + offset <= filesize - readSize) {
                        read = randomAccessFile.read(buffer, 0, this.readSize);
                        if (read == -1) break;
                        readLength += read;
                        outStream.write(buffer, 0, read);
                    }
                    if (readLength + offset < filesize) {
                        read = randomAccessFile.read(buffer, 0, (int) (filesize - (readLength +
                                offset)));
                        if (read != -1) outStream.write(buffer, 0, read);
                    }
                    resultInfo = getResultInfo();

                    if (resultInfo == null) {
                        listener.failure(toResponseInfo(resultInfo));
                        return;
                    }

                    if (resultInfo.code == 1) {

                    }
                } catch (Exception e) {
                    listener.failure(toResponseInfo(resultInfo));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private ResultInfo getResultInfo() {
        ResultInfo resultInfo = null;
        PushbackInputStream inputStream = null;
        try {
            inputStream = new PushbackInputStream(socket.getInputStream());
            byte[] buffer = new byte[512];
            int len = inputStream.read(buffer, 0, buffer.length);
            if (len <= 0) {
                return resultInfo;
            }
            resultInfo = JSON.parseObject(new String(buffer, 0, len), ResultInfo.class);
        } catch (IOException e) {

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
                "\"}";
    }


}
