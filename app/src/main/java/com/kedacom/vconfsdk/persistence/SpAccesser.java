package com.kedacom.vconfsdk.persistence;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kedacom.vconfsdk.utils.KdLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Created by Sissi on 11/3/2017.
 */
public final class SpAccesser {
    // 自动使用类名为文件名，字段名为key名
    // 对于sp不支持的类型自动json转换
    private static SpAccesser instance;
    private static Context ctx;
    private HashMap<String, SharedPreferences.Editor> editorMap;
    private HashMap<String, Class<?>>  clzMap;
    private Gson gson;

    private AccessHelper accessHelper;
    private Handler mainThreadHandler;

    private SpAccesser(){
        gson = new GsonBuilder().create();
        editorMap = new HashMap<>();
        clzMap = new HashMap<>();

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

    public static synchronized SpAccesser instance(){
        if (null == ctx){
            KdLog.p(KdLog.ERROR, "NOT INITED YET!");
            return null;
        }
        if (null == instance){
            instance = new SpAccesser();
        }

        return instance;
    }

    // 整存
    public boolean put(final String spName, final Object obj, final AccessListeners.OnPutFinishedListener onPutFinishedListener){
        KdLog.p("PUT SharedPreferences %s", spName);
        if (null == onPutFinishedListener){
            return doPut(spName, obj);
        }else{
            accessHelper.post(new Runnable() {
                @Override
                public void run() {
                    final boolean isSuccess = doPut(spName, obj);
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

            return true;
        }
    }


    // 整取
    public Object get(final String spName, final AccessListeners.OnGetFinishedListener onGetFinishedListener){
        KdLog.p("GET SharedPreferences %s", spName);
        if (null == onGetFinishedListener){
            return doGet(spName);
        }else{
            accessHelper.post(new Runnable() {
                @Override
                public void run() {
                    final Object obj = doGet(spName);
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




    // 零存
    public void putField(String spName, String key, Object obj){
        // TODO
    }

    // 零取
    public Object getField(String spName, String key){
        // TODO
        return null;
    }

    public void del(String spName){
        KdLog.p("DELETE SharedPreferences %s", spName);
        SharedPreferences.Editor editor = editorMap.get(spName);
        if (null == editor) {
            KdLog.p("%s not exist", spName);
            return;
        }
        editor.clear();
        editor.commit();
    }


    @SuppressLint("CommitPrefEdits")
    private boolean doPut(String spName, Object obj){
        SharedPreferences.Editor editor = editorMap.get(spName);
        if (null == editor){
            editor = ctx.getSharedPreferences(spName, Context.MODE_PRIVATE).edit();
            editorMap.put(spName, editor);
        }
        Class<?> clz = clzMap.get(spName);
        if (null == clz){
            clz = obj.getClass();
            clzMap.put(spName, clz);
        }

        Field[] fields = clz.getDeclaredFields();
        Field field;
        try {
            for (int i=0; i<fields.length; ++i){
                field = fields[i];
                field.setAccessible(true);
                putValue(editor, field.getName(), field.get(obj));
            }
        }catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }

        editor.commit();

        return true;
    }


    private Object doGet(String spName){
        if (null == editorMap.get(spName)) {
            KdLog.p("%s not put yet", spName);
            return null;
        }

        Class<?> clz = clzMap.get(spName);
        SharedPreferences sp = ctx.getSharedPreferences(spName, Context.MODE_PRIVATE);
        Object obj;
        try {
            Constructor ctor = clz.getDeclaredConstructors()[0]; // 获取“任意一个”构造函数（若写死使用默认构造函数则限制了被构造对象必须要要提供默认构造函数，这样给上层带来了很大不便）
            Class<?>[] paraTypes = ctor.getParameterTypes();
            Object[] defValues = 0==paraTypes.length ? null : defValue(paraTypes); // 使用默认值填充
            ctor.setAccessible(true);
            obj = ctor.newInstance(defValues);
            Field[] fields = obj.getClass().getDeclaredFields();
            Field field;
            String fieldName;
            try {
                for (int i=0; i<fields.length; ++i){
                    field = fields[i];
                    field.setAccessible(true);
                    fieldName = field.getName();
                    field.set(obj, getValue(sp, fieldName, field.getType()));
                }

                return obj;
            }catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void putValue(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) {
            editor.putString(key, (String)value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer)value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean)value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long)value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float)value);
        } else if (value instanceof Double) {
            editor.putFloat(key, ((Double)value).floatValue());
        }else if (value instanceof Short) {
            editor.putInt(key, (Short)value);
        } else if (value instanceof Byte) {
            editor.putInt(key, (Byte) value);
        } else if (value instanceof Character) {
            editor.putString(key, value.toString());
        } else {
            editor.putString(key, gson.toJson(value));
        }
    }

    private Object getValue(SharedPreferences sp, String key, Class<?> clz){
        if (String.class.isAssignableFrom(clz)){
            return sp.getString(key, "");
        }else if (Character.class.isAssignableFrom(clz) || char.class.isAssignableFrom(clz)){
            return sp.getString(key, "").charAt(0);
        }else if (Integer.class.isAssignableFrom(clz) || int.class.isAssignableFrom(clz)){
            return sp.getInt(key, 0);
        }else if (Short.class.isAssignableFrom(clz) || short.class.isAssignableFrom(clz)){
            return (short)sp.getInt(key, 0);
        }else if (Long.class.isAssignableFrom(clz) || long.class.isAssignableFrom(clz)){
            return sp.getLong(key, 0);
        }else if (Float.class.isAssignableFrom(clz) || float.class.isAssignableFrom(clz)){
            return sp.getFloat(key, 0);
        }else if (Double.class.isAssignableFrom(clz) || double.class.isAssignableFrom(clz)){
            return (double) sp.getFloat(key, 0);
        }else if (Boolean.class.isAssignableFrom(clz) || boolean.class.isAssignableFrom(clz)){
            return sp.getBoolean(key, false);
        } else if (Byte.class.isAssignableFrom(clz) || byte.class.isAssignableFrom(clz)) {
            return (byte)sp.getInt(key, 0);
        } else {
            return gson.fromJson(sp.getString(key, ""), clz);
        }
    }

    private Object[] defValue(Class<?>[] types){
        Object[] values = new Object[types.length];
        Class<?> clz;
        for (int i=0; i<types.length; ++i){
            clz = types[i];
            if (/*Character.TYPE.isInstance(clz)*/char.class.isAssignableFrom(clz)){
                values[i]=' ';
            }else if (int.class.isAssignableFrom(clz)){
                values[i]=0;
            }else if (short.class.isAssignableFrom(clz)){
                values[i]=(short)0;
            }else if (long.class.isAssignableFrom(clz)){
                values[i]=(long)0;
            }else if (float.class.isAssignableFrom(clz)){
                values[i]=(float)0.0;
            }else if (double.class.isAssignableFrom(clz)){
                values[i]=0.0;
            }else if (boolean.class.isAssignableFrom(clz)){
                values[i]=false;
            }else if (byte.class.isAssignableFrom(clz)) {
                values[i]=(byte)0;
            }else {
                values[i]=null;
            }
        }

        return values;
    }
}
