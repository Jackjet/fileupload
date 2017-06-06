package com.kk.fileupload;

import android.content.Context;

import com.alibaba.fastjson.TypeReference;
import com.kk.securityhttp.domain.ResultInfo;
import com.kk.securityhttp.engin.HttpCoreEngin;
import com.kk.securityhttp.net.contains.HttpConfig;
import com.kk.securityhttp.net.entry.UpFileInfo2;
import com.kk.utils.FPUitl;
import com.kk.utils.LogUtil;
import com.kk.utils.PreferenceUtil;
import com.kk.utils.security.Md5;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.functions.Action1;

/**
 * Created by zhangkai on 2017/5/23.
 */

public class HttpBigFileUpload extends IBigFileUpload {

    private String url;
    private int chunkLength = 1024 * 1024 * 2; //分片字节数
    private int chunkRetry = 2; //重试次数

    private List<ChunkInfo> chunkInfos = new ArrayList<>(); //分片信息
    private int chunks = 1; //分片数量

    private int retryTimes = 0;
    private int uploadNums = 0;


    private List<Integer> uploadIndexs;  //成功上传记录
    private int successNums = 0;

    public HttpBigFileUpload(Builder builder) {
        super(builder);
        this.chunkLength = builder.chunkLength;
        this.url = builder.url;
        this.chunkRetry = builder.chunkRetry;
        this.maxThreadsNums = builder.maxThreadsNums;
        setChunkInfos();
        uploadIndexs = readRecord();
        successNums = uploadIndexs.size();
    }

    public static class Builder extends IBigFileUpload.Builder {
        private int chunkLength = 1024 * 1024 * 2; //分片字节数
        private int chunkRetry = 2; //重试次数
        private int maxThreadsNums = 8; //上传最大线程数
        private String url;

        public Builder setChunkLength(int chunkLength) {
            this.chunkLength = chunkLength;
            return this;
        }

        public Builder setMaxThreadsNums(int maxThreadsNums) {
            this.maxThreadsNums = maxThreadsNums;
            return this;
        }

        public Builder setChunkRetry(int chunkRetry) {
            this.chunkRetry = chunkRetry;
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

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public HttpBigFileUpload build() {
            return new HttpBigFileUpload(this);
        }
    }

    public static class ChunkInfo {
        private String type = "chunkCheck";
        private int chunkIndex;
        private int size;
    }

    private byte[] getBufferByChunkInfo(ChunkInfo chunkInfo) {
        byte[] buffer = null;
        int read;
        long readLength = 0;
        try {
            int offset = chunkInfo.chunkIndex * chunkLength;
            randomAccessFile.seek(offset);
            buffer = new byte[chunkInfo.size];
            while (readLength <= chunkInfo.size - readSize) {
                read = randomAccessFile.read(buffer, (int) readLength, this.readSize);
                if (read == -1) break;
                readLength += read;
            }
            if (readLength < chunkInfo.size) {
                randomAccessFile.read(buffer, (int) readLength, (int) (chunkInfo.size - readLength));
            }
        } catch (Exception e) {

        }
        return buffer;
    }


    private void saveRecord(String index) {
        String record = PreferenceUtil.getImpl(context).getString(md5Check, "");
        record = record + "," + index;
        FPUitl.putString(context, md5Check, record);
    }

    private List readRecord() {
        List<Integer> indexs = new ArrayList();
        String record = PreferenceUtil.getImpl(context).getString(md5Check, "");
        String[] records = record.split(",");
        for (String index : records) {
            if (index != null && !index.isEmpty()) {
                indexs.add(Integer.parseInt(index));
            }
        }
        return indexs;
    }

    private void clearRecord() {
        PreferenceUtil.getImpl(context).putString(md5Check, "");
    }

    public void setChunkInfos() {
        long fileLength = bigFile.length();
        chunks = (int) (fileLength / chunkLength + (fileLength % chunkLength > 0 ? 1 : 0));
        for (int i = 0; i < chunks; i++) {
            int offset = i * chunkLength;
            int tempChunkLength = chunkLength;
            if (chunkLength + offset > fileLength) {
                tempChunkLength = (int) (fileLength - offset);
            }
            ChunkInfo chunkInfo = new ChunkInfo();
            chunkInfo.chunkIndex = i;
            chunkInfo.size = tempChunkLength;
            chunkInfos.add(chunkInfo);
        }
    }

    public void upload(final ChunkInfo chunkInfo) {
        Map params = new HashMap();
        params.put("type", chunkInfo.type + "");
        params.put("chunkIndex", chunkInfo.chunkIndex + "");
        params.put("size", chunkInfo.size + "");
        params.put("file", md5Check);

        UpFileInfo2 upFileInfo2 = new UpFileInfo2();
        upFileInfo2.name = md5Check;
        upFileInfo2.filename = md5Check;
        upFileInfo2.buffer = getBufferByChunkInfo(chunkInfo);
        HttpCoreEngin.get(context).rxuploadFile(url, new TypeReference<ResultInfo>() {
                }.getType(), upFileInfo2,
                params,
                false)
                .subscribe
                        (new Action1<ResultInfo>() {
                            @Override
                            public void call(final ResultInfo resultInfo) {
                                synchronized (HttpBigFileUpload.class) {
                                    uploadNums++;
                                    if (resultInfo.code == HttpConfig.STATUS_OK) {
                                        saveRecord(chunkInfo.chunkIndex + "");
                                        successNums++;
                                        if (listener != null) {
                                            listener.process((successNums) /
                                                    (float) chunks);
                                        }
                                    }
                                    done(resultInfo);
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                synchronized (HttpBigFileUpload.class) {
                                    uploadNums++;
                                    done(null);
                                }
                            }
                        });
    }

    public void retry() {
        if (successNums < chunks && uploadNums >= chunks && retryTimes < chunkRetry) {
            retryTimes++;
            uploadNums = 0;
            upload();
        }
    }

    private void callback(ResultInfo resultInfo) {
        if (uploadNums >= chunks) {
            uploadNums = 0;
            if (successNums >= chunks) {
                successNums = 0;
                clearRecord();
                if (listener != null) {
                    listener.success(toResponseInfo(resultInfo));
                }
            } else {
                if (listener != null) {
                    listener.failure(toResponseInfo(resultInfo));
                }
            }
        }
    }

    private void done(ResultInfo resultInfo) {
        upload2();
        retry();
        callback(resultInfo);
    }


    private int maxThreadsNums = 8; //上传最大线程数
    private int times = 0;    //当前上传的次数

    private void upload2() {
        if (uploadNums % maxThreadsNums == 0) {
            times++;
            upload();
        }
    }


    @Override
    public void upload() {
        int start = times * maxThreadsNums;
        for (int i = start; i < start + maxThreadsNums && i < chunkInfos.size(); i++) {
            ChunkInfo chunkInfo = chunkInfos.get(i);
            boolean needUpload = true;
            for (Integer index : uploadIndexs) {
                if (index == chunkInfo.chunkIndex) {
                    needUpload = false;
                }
            }
            if (needUpload) {
                upload(chunkInfo);
            }
        }
    }


}
