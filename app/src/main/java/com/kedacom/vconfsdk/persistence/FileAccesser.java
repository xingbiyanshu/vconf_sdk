package com.kedacom.vconfsdk.persistence;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.kedacom.vconfsdk.utils.KdLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Sissi on 11/3/2017.
 */

public final class FileAccesser {
    private static FileAccesser instance;
    private static Context ctx;

    private AccessHelper accessHelper;
    private Handler mainThreadHandler;


    private FileAccesser(){
        accessHelper = AccessHelper.instance();
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized void init(Application context){
        if (null != ctx){
            KdLog.p(KdLog.WARN, "INITED ALREADY!");
            return;
        }

        ctx = context;
    }

    public static synchronized FileAccesser instance(){
        if (null == ctx){
            KdLog.p(KdLog.ERROR, "NOT INITED YET!");
            return null;
        }
        if (null == instance){
            instance = new FileAccesser();
        }

        return instance;
    }

    public boolean put(/*TODO 重命名为serialize*/final String path, final Object data/*TODO 类型为serializable*/, final AccessListeners.OnPutFinishedListener onPutFinishedListener){
        KdLog.p("path=%s", path);
        File file = new File(getDir(path));
        if (!file.exists()){
            file.mkdirs();
        }
        if (!(data instanceof Serializable)){
            return false;
        }

        if (null == onPutFinishedListener){
            return serialize(path, (Serializable)data);
        }else{
            accessHelper.post(new Runnable() {
                @Override
                public void run() {
                    final boolean isSuccess = serialize(path, (Serializable)data);
                    mainThreadHandler.post(new Runnable() { // 在主线程通知用户以免去用户多线程之忧
                        @Override
                        public void run() {
                            if (isSuccess) {
                                onPutFinishedListener.onPutSuccess();
                            }else{
                                onPutFinishedListener.onPutFailed();
                            }
                        }
                    });
                }
            });

            return true; // 此处返回true仅表示存储数据请求已成功发起
        }
    }

    public Object get(final String path, final AccessListeners.OnGetFinishedListener onGetFinishedListener){
        KdLog.p("path=%s", path);
        if (null == onGetFinishedListener){
            return unserialize(path);
        }else{
            accessHelper.post(new Runnable() {
                @Override
                public void run() {
                    final Object obj = unserialize(path);
                    mainThreadHandler.post(new Runnable() { // 在主线程通知用户以免去用户多线程之忧
                        @Override
                        public void run() {
                            if (null != obj) {
                                onGetFinishedListener.onGetSuccess(obj);
                            }else{
                                onGetFinishedListener.onGetFailed();
                            }
                        }
                    });
                }
            });

            return null;
        }
    }

    public void del(String path){
        KdLog.p("path=%s", path);
        File file = new File(path);
        if (file.exists() && file.isFile()){
            file.delete();
        }
    }

    //TODO 字节数组存为文件
    public boolean buf2File(byte[] buf, String filepath){
        return true;
    }

    //TODO 流存为文件
    public void stream2file(/*InputStream is,*/ String filepath){

    }

    //TODO bitmap存为文件
    public boolean bmp2File(/*Bitmap bmp, */String filepath){
        return true;
    }

    /**序列化*/
    private boolean serialize(String filepath, Serializable obj){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath));
            oos.writeObject(obj);
            oos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**反序列化*/
    private Object unserialize(String filepath){
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
            Object obj = ois.readObject();
            ois.close();
            return obj;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    // TODO 放到FileUtils里面
    private String getDir(String filepath){
        int lastSlashIndx = filepath.lastIndexOf(File.separator);
        if (-1 == lastSlashIndx){
            return null;
        }else if (0 == lastSlashIndx){
            return File.separator;
        }

        return filepath.substring(0, lastSlashIndx);
    }

}
