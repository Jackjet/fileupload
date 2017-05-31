package com.kk.fileupload;

import com.kk.utils.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

/**
 * Created by zhangkai on 2017/5/24.
 */

public class SocketBigFileUpload {

    public SocketBigFileUpload(){
        try {
            Socket socket = new Socket("192.168.80.110",1234);
            OutputStream outStream = socket.getOutputStream();
            outStream.write("{'a':1, 'b':2}".getBytes());
            PushbackInputStream inputStream  = new  PushbackInputStream(socket.getInputStream());

            byte[] buffer = new byte[100];
            inputStream.read(buffer, 0, 20);
            LogUtil.msg(new String(buffer)+"");
            outStream.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
