package com.kk.fileupload;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by zhangkai on 2017/6/6.
 */

public abstract class IBigFileUpload {
    protected Context context;

    protected File bigFile; //大文件
    protected RandomAccessFile randomAccessFile;
    protected String md5Check;     //文件唯一标识

    protected int readSize = 1464; //每次读取大小
    protected BigFileUploadListener listener;

    public IBigFileUpload(Builder builder) {
        this.context = builder.context;
        this.bigFile = builder.bigFile;
        this.listener = builder.listener;
        this.readSize = builder.readSize;
        md5Check = getFileMD5(bigFile);
        setRandomAccessFile();
    }

    public interface BigFileUploadListener {
        void success(ResonseInfo<String> info);

        void failure(ResonseInfo<String> info);

        void process(float percent);
    }

    public static abstract class Builder {
        protected Context context;

        protected File bigFile; //大文件
        protected int readSize = 1464;//每次读取大小
        protected BigFileUploadListener listener;
    }

    public static class ResultInfo {
        public int code;
        public String msg;
        public long package_index;
        public String url;

    }

    public static class ResonseInfo<T> {
        public int code;
        public String msg;
        public T data;
    }

    protected void setRandomAccessFile() {
        try {
            randomAccessFile = new RandomAccessFile(bigFile, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected ResonseInfo<String> toResponseInfo(ResultInfo info) {
        ResonseInfo<String> resonseInfo = new ResonseInfo<>();
        if (info == null) {
            resonseInfo.code = -1000;
            resonseInfo.msg = "服务器异常，请重试";
        } else {
            resonseInfo.code = info.code;
            resonseInfo.msg = info.msg;
            resonseInfo.data = info.url;
        }
        return resonseInfo;
    }

    private String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        String mdStr = bigInt.toString(16);
        int slen = 32 - mdStr.length();
        for (int i = 0; i < slen; i++) {
            mdStr = 0 + mdStr;
        }
        return mdStr;
    }

    public abstract void upload();
}
