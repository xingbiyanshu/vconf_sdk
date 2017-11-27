package com.kedacom.vconfsdk.utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class KdLog {

    private static boolean isEnabled = true;

    // 日志等级
    /**调试等级。<br>仅在调试阶段有用的信息请使用该等级打印*/
    public static final int DEBUG = 0;
    /**普通信息等级。<br>默认使用该等级，该等级打印是最普遍最多的，无特殊用途请使用该等级打印*/
    public static final int INFO = 1;
    /**脉络等级。<br>该等级打印用于勾勒程序正常运行时流程概貌。
     * <br>您应该慎重添加该等级的打印，时刻关注程序整体概貌，仅在描绘概貌必要的像素点处添加该等级打印,注意“概貌”层次一致性。
     * <br>版本发布时请将日志等级设置为该等级*/
    public static final int VEIN = 2;
    /**警告等级。<br>出现了异常情况但不影响程序正常运行请使用该等级打印。
     * <br>该等级信息尽管不影响正常运行但很可能预示潜藏的问题，需仔细分析*/
    public static final int WARN = 3;
    /**错误等级。<br>影响程序正常运行的异常情形发生时加该打印。如参数非法，返回失败*/
    public static final int ERROR = 4;
    /**致命等级。<br>致命错误发生时加该打印。*/
    public static final int FATAL = 5;
    private static int level = INFO;
    /**日志等级字符串字面值*/
    private static Map<Integer, String> levStrMap = new HashMap<Integer, String>();
    static {
        levStrMap.put(DEBUG, "[D]");
        levStrMap.put(INFO, "[I]");
        levStrMap.put(VEIN, "[V]");
        levStrMap.put(WARN, "[W]");
        levStrMap.put(ERROR, "[E]");
        levStrMap.put(FATAL, "[F]");
    }

    private static boolean isFileLogInited = false;
    private static boolean isFileLogEnabled = false;
    private static BufferedWriter bufWriter;
    private static BufferedWriter bufWriter1;
    private static BufferedWriter curBw;
    private static final int WRITER_BUF_SIZE = 1024;
    private static File logFile;
    private static File logFile1;
    private static File curLf;
	private static final String LOG_FILE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ File.separator + "kedacom" + File.separator + "trace";
    private static final String LOG_FILE_NAME = "kdlog.txt";
    private static final String LOG_FILE1_NAME = "kdlog1.txt";
    private static final int LOG_FILE_SIZE_LIMIT = 1024 * 1024 * 1024;
    private static Object lock = new Object();

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");

    private static final HashMap<String, Long> timestampRecord = new HashMap<>();
    private static final int MAX_RECORD_NUM = 64;

    private static final String TAG = "KdLog";

    private KdLog() {

    }

    public static void enable(boolean isEnable) {
        if (isEnable) {
            log(INFO, TAG, "==================KdLog enabled!");
        } else {
            log(INFO, TAG, "==================KdLog disabled!");
        }
        isEnabled = isEnable;
    }

    /**set log level.
     * @param lv floor level. level less than it will not be print out*/
    public static void setLevel(int lv) {
        log(INFO, TAG, "==================Set KdLog level to " + lv);
        level = lv;
    }

    /**Interval Print, print at intervals*/
    public static void ip(String tag, int lev, int interval, String format, Object... para){
        if (!isEnabled || lev < level || null == tag || interval<=0 || null == format || null == para) {
            return;
        }
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        String pref = prefix(ste);
        Long ts = timestampRecord.get(pref);
        long timestamp = null==ts ? 0 : ts.longValue();
        long curtime = System.currentTimeMillis();
        if (interval <= curtime-timestamp){
            log(lev, tag, pref+ String.format(format, para));
            timestampRecord.put(pref, curtime);
            if (timestampRecord.size() > MAX_RECORD_NUM){
                timestampRecord.clear();
            }
        }
    }

    /**Tag Print, Print with specified tag*/
    // do NOT change method name to 'p'
    public static void tp(String tag, int lev, String format, Object... para){
        if (!isEnabled || lev < level || null == tag || null == format || null == para) {
            return;
        }
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        log(lev, tag, prefix(ste)+ String.format(format, para));
    }

    /**Print with specified lev and default tag*/
    public static void p(int lev, String format, Object... para){
        if (!isEnabled || lev < level || null == format || null == para) {
            return;
        }
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        log(lev, getClassName(ste.getClassName()), simplePrefix(ste)+ String.format(format, para));
    }

    /**Print with default lev and default tag*/
    public static void p(String format, Object... para){
        if (!isEnabled || INFO < level || null == format || null == para) {
            return;
        }
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        log(INFO, getClassName(ste.getClassName()), simplePrefix(ste) + String.format(format, para));
    }

    /**Print with default lev and default tag*/
    public static void p(String str){
        if (!isEnabled || INFO < level) {
            return;
        }
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        log(INFO, getClassName(ste.getClassName()), simplePrefix(ste) + str);
    }

    /**Raw print, no tag*/
    public static void rp(String str){
        if (!isEnabled) {
            return;
        }
        System.out.println(str);
    }


    /** Stack print, print out the call stack */
    public static void sp(String msg) {
        if (!isEnabled) {
            return;
        }

        StackTraceElement stes[] = Thread.currentThread().getStackTrace();
        StackTraceElement ste = stes[3];
        StringBuffer trace = new StringBuffer();

        trace.append(prefix(ste)).append(msg).append("\n");

        for (int i = 3, j = 0; i < stes.length; ++i, ++j) {
            trace.append("#" + j + " " + stes[i] + "\n");
        }

        System.out.println(trace.toString());
    }


    /** File print, print into file*/
    public static void fp(String msg) {
        if (!isEnabled) {
            return;
        }
        synchronized (lock) {
            fileLog(msg, false);
        }
    }

    
    /** Flush file print, print into file and flush*/
    public static void ffp(String msg) {
        if (!isEnabled) {
            return;
        }
        synchronized (lock) {
            fileLog(msg, true);
        }
    }



    private static void initFileLog() {
        if (isFileLogInited) {
            return;
        }

        isFileLogInited = true;

        logFile = createLogFile(LOG_FILE_DIR, LOG_FILE_NAME);
        logFile1 = createLogFile(LOG_FILE_DIR, LOG_FILE1_NAME);
        if (null == logFile || null == logFile1) {
            return;
        }
        curLf = logFile;

        try {
			bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)),
					WRITER_BUF_SIZE);
			bufWriter1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile1, true)),
					WRITER_BUF_SIZE);
            curBw = bufWriter;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        isFileLogEnabled = true;
    }

    
    private static File createLogFile(String dir, String filename) {
        File traceDir = new File(dir);
        if (!traceDir.exists()) {
            if (!traceDir.mkdirs()) {
                return null;
            }
        }

        File traceFile = new File(dir + File.separator + filename);
        if (!traceFile.exists()) {
            try {
                traceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        try {
            FileOutputStream fos;
            fos = new FileOutputStream(traceFile);
            fos.write((sdf.format(new Date()) + " ================================== Start Tracing... \n").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return traceFile;
    }

    
    private static void rechooseLogFile() {
        try {
            curBw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        curBw = (curLf == logFile) ? bufWriter1 : bufWriter;
    }

    
    private static void fileLog(String msg, boolean isFlush) {
        if (!isFileLogInited) {
            initFileLog();
        }

        if (!isFileLogEnabled) {
            return;
        }

        if (curLf.length() >= LOG_FILE_SIZE_LIMIT) {
            rechooseLogFile();
        }

        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
		String trace = prefix(ste) + msg + "\n";

        try {
            curBw.write(trace);
            if (isFlush) {
                curBw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String prefix(StackTraceElement ste){
		return "[" + getClassName(ste.getClassName()) + ":" + ste.getMethodName() + ":" + ste.getLineNumber() + "]"+"["+ Thread.currentThread().getName()+"]  ";
    }


    private static String simplePrefix(StackTraceElement ste){
        return  "[" + ste.getMethodName() + ":" + ste.getLineNumber() + "]"+"["+ Thread.currentThread().getName()+"]  ";
    }

	private static String getClassName(String classFullName) {
		String className = "";
		int lastSlashIndx = classFullName.lastIndexOf(".");
		if (-1 == lastSlashIndx) {
			className = classFullName;
		} else {
			className = classFullName.substring(lastSlashIndx + 1, classFullName.length());
		}
		return className;
	}

    private static void log(int lev, String tag, String content){
        tag = levStrMap.get(lev)+tag;
        switch (lev){
            case DEBUG:
                Log.v(tag, content);
                break;
            case INFO:
                Log.i(tag, content);
                break;
            case VEIN:
                Log.i(tag, content);
                break;
            case WARN:
                Log.w(tag, content);
                break;
            case ERROR:
                Log.e(tag, content);
                break;
            case FATAL:
                Log.wtf(tag, content);
                break;
        }
    }
}
