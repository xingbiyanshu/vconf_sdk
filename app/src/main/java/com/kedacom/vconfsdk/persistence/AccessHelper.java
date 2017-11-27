package com.kedacom.vconfsdk.persistence;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

/**
 * Created by Sissi on 11/6/2017.
 */
class AccessHelper{
    private static AccessHelper instance;
    private Thread thread;
    private Handler handler;

    private AccessHelper(){
        initThread();
    }

    static AccessHelper instance(){
        if (null == instance){
            instance = new AccessHelper();
        }

        return instance;
    }

    void post(Runnable runnable){
        handler.post(runnable);
    }

    void postDelayed(Runnable runnable, long delayMillis){
        handler.postDelayed(runnable, delayMillis);
    }

    private void initThread(){
        final Object lock = new Object();
        thread = new Thread(){
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();

                handler = new Handler();
                synchronized (lock){lock.notify();}

                Looper.loop();
            }
        };

        thread.setName("persistence.AccessHelper");

        thread.start();

        if (null == handler){
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
