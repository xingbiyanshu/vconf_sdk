package com.kedacom.vconfsdk.utils;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sissi on 11/3/2017.
 */
public final class FileUtils {
    public static final String EMPTY_STR = "";
    //存储位置
    /** Context.getFilesDir()对应的路径*/
    public static final int APP_INNER = 1;
    /** Context.getCacheDir()对应的路径*/
    public static final int APP_INNER_CACHE = 2;
    /** Context.getExternalFilesDir()对应的路径*/
    public static final int APP_EXTERNAL = 3;
    /** Context.getExternalCacheDir()对应的路径*/
    public static final int APP_EXTERNAL_CACHE = 4;
//    /** Environment.getExternalStorageDirectory()对应的路径*/
//    public static final int EXTERNAL_STORAGE_ROOT = 5;

    private static Context ctx;

    public static void init(Application context){
        if (null != ctx){
            return;
        }
        ctx = context;
    }

    //==========================文件路径及名称处理
    /**获取文件全名。
     * @param path 文件路径
     * 例如：/sdcard/kedacom/crash.txt的文件全名为crash.txt
     * 例如：/sdcard/kedacom/的文件全名为kedacom
     * */
    public static String getFullName(String path) {
//        if ( null == path ) return null; // 不做入参检查，由调用者担责。

        String fileName;
        int lastSlashIndx = path.lastIndexOf(File.separator);
        if (-1 == lastSlashIndx){
            fileName = path;
        }else if (path.length()-1 == lastSlashIndx){//e.g. path=/sdcard/kedacom/
            return getFullName(path.substring(0, lastSlashIndx));
        }else{
            fileName = path.substring(lastSlashIndx + 1, path.length());
        }

        return fileName;
    }


    /**
     * 获取主文件名。
     * 例如：crash.txt的主文件名为crash
     * @param fullName 文件全名
     * */
    public static String getMainName(String fullName) {
        String mainName;

        int lastDotIndx = fullName.lastIndexOf(".");
        if (-1 == lastDotIndx){
            return fullName;
        }

        mainName = fullName.substring(0, lastDotIndx);

        return mainName;
    }

    /**
     * 获取文件扩展名。
     * @param fullName 文件全名
     * 例如：crash.txt的文件扩展名为txt
     * */
    public static String getExtName(String fullName) {
        int lastDotIndx = fullName.lastIndexOf(".");
        if (-1 == lastDotIndx){
            return EMPTY_STR;
        }

        return fullName.substring(lastDotIndx+1, fullName.length());
    }


    /**获取文件的MIME类型
     * @param extName 文件扩展名
     * @return 返回对应的MIME类型
     * 例如：扩展名"java"对应的MIME类型为"text/plain"
     * */
    public static String getMIMEType(String extName) {
        for(int i=0; i<ExtMIME_Map.length; ++i) {
            if(extName.equals(ExtMIME_Map[i][0])) {
                return ExtMIME_Map[i][1];
            }
        }

        return "*/*";
    }

    /**
     * 根据URI获取文件路径
     * */
    public static String getPathFromUri(Context context, Uri uri) {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)) {
            try {
                String[] projection = { "_data" };
                Cursor cursor = context.getContentResolver().query(uri, projection,null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    String path = cursor.getString(column_index);
                    cursor.close();
                    return path;
                }
            } catch (Exception e) {
            }
        }
        else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(scheme)) {
            return uri.getPath();
        }

        return null;
    }

    /**获取父目录。
     * @param path 文件路径
     * @return 返回该文件所在目录
     * 例如：path="/sdcard/kedacom/crash.txt", return="/sdcard/kedacom/"
     *      path="/sdcard/kedacom/", return="/sdcard/"
     *      path="/", return="/"
     * */
    public static String getParentPath(String path){
        int lastSlashIndx = path.lastIndexOf(File.separator);
        if (-1 == lastSlashIndx){ //e.g. path=crash.txt
            return ".";
        }else if (0 == lastSlashIndx){//e.g. path=/crash.txt
            return File.separator;
        }else if (path.length()-1 == lastSlashIndx){//e.g. path=/sdcard/kedacom/
            return getParentPath(path.substring(0, lastSlashIndx));
        }

        return path.substring(0, lastSlashIndx);//e.g. path=/sdcard/kedacom/crash.txt
    }

    /**
     * 是否为目录形式的路径（"/"结尾）
     * */
    public static boolean isDirPath(String path){
        int lastSlashIndx = path.lastIndexOf(File.separator);
        return path.length()-1 == lastSlashIndx;
    }


    //======================== 文件属性处理，如读写权限、日期、大小

    /**获取文件大小
     * @param path 文件路径
     * @return 文件大小。单位：字节
     * 说明：若path指向文件则计算该文件大小，若指向目录则递归计算该目录下所有文件的大小总和
     * */
    public static long getFileSize(String path) {
        return getFileSize(new File(path));
    }

    /**获取文件大小
     * @param file 文件对象
     * @return 文件大小。单位：字节
     * 说明：若file指向文件则计算该文件大小，若指向目录则递归计算该目录下所有文件的大小总和
     * */
    public static long getFileSize(File file) {
        if (!file.exists()){
            return 0;
        }
        long size = 0;

        if (file.isFile()){
            return file.length();
        }else if (file.isDirectory()){
            File flist[] = file.listFiles();
            for (File f:flist) {
                if (f.isFile()){
                    size += f.length();
                }else if (f.isDirectory()){
                    size += getFileSize(f);
                }
            }
            return size;

        }else{
            return 0;
        }
    }

    /**获取易读格式的文件大小
     * @param path 文件路径
     * @return 易读格式的文件大小，如1073741824B、1048576KB、1024MB、1GB
     * 说明：若file指向文件则计算该文件大小，若指向目录则递归计算该目录下所有文件的大小总和
     * */
    public static String getHumanReadableFileSize(String path) {
        return formatFileSize(getFileSize(path));
    }

    /**
     * 获取指定路径下可用空间大小
     * */
    public static long getAvailableVolume(File dir){
        //TODO
        return 0;
    }


    private final static long B_LIMIT = 1024;
    private final static long KB_LIMIT = 1024*1024;
    private final static long MB_LIMIT = 1024*1024*1024;
    /**
     * 格式化文件大小，以易读形式输出
     * */
    public static String formatFileSize(long fs) {
        DecimalFormat df = new DecimalFormat("#.00");
        String formatedStr = "";

        if (fs <= 0) {
            return "0B";
        }

        if (fs < B_LIMIT) {
            formatedStr = df.format((double) fs) + "B";
        } else if (fs < KB_LIMIT) {
            formatedStr = df.format((double) fs / B_LIMIT) + "KB";
        } else if (fs < MB_LIMIT) {
            formatedStr = df.format((double) fs / KB_LIMIT) + "MB";
        } else {
            formatedStr = df.format((double) fs / MB_LIMIT) + "GB";
        }

        return formatedStr;
    }

//    private static double FormetFileSize(long fileS, int sizeType) {
//     DecimalFormat df = new DecimalFormat("#.00");
//     double fileSizeLong = 0;
//     switch (sizeType) {
//     case SIZETYPE_B:
//      fileSizeLong = Double.valueOf(df.format((double) fileS));
//      break;
//     case SIZETYPE_KB:
//      fileSizeLong = Double.valueOf(df.format((double) fileS / 1024));
//      break;
//     case SIZETYPE_MB:
//      fileSizeLong = Double.valueOf(df.format((double) fileS / 1048576));
//      break;
//     case SIZETYPE_GB:
//      fileSizeLong = Double.valueOf(df
//        .format((double) fileS / 1073741824));
//      break;
//     default:
//      break;
//     }
//     return fileSizeLong;
//    }


    //===================== 文件创建、删除、移动
    /**文件是否存在。
     * @return 存在返回true，不存在返回false。文件可能实际存在但没有访问权限，此种情形仍返回false。
     * */
    public static boolean isExists(String filePath){
        return new File(filePath).exists();
    }

    /**
     * 创建文件。（目录也是文件）
     * <br>若文件已存在则直接返回已存在的文件；
     * <br>若路径表示文件则创建文件，若表示目录，参考{@link #isDirPath(String)}，则创建目录；
     * <br>若路径中包含不存在的目录则会递归创建所有不存在的目录；
     * @param path 文件路径
     * @return 成功返回创建的file，失败返回null
     * */
    public static File createFile(String path){
        KdLog.p(KdLog.VEIN, "=>path=%s", path);
        File file = new File(path);
        if (file.exists()){
            KdLog.p(KdLog.WARN, "file.exists()");
            return file;
        }

        if (isDirPath(path)){
            KdLog.p(KdLog.VEIN, "file.isDirectory()");
            if (!file.mkdirs()){
                KdLog.p(KdLog.ERROR, "FAILED to create dir %s!", file.getAbsolutePath());
                return null;
            }
        }else {
            KdLog.p(KdLog.VEIN, "file.isFile()");
            File parent = file.getParentFile();
            if (!parent.exists()){
                KdLog.p(KdLog.VEIN, "!parent.exists()");
                if (!parent.mkdirs()){
                    KdLog.p(KdLog.ERROR, "FAILED to create dir %s!", parent.getAbsolutePath());
                    return null;
                }
            }
            try {
                KdLog.p(KdLog.VEIN, "file.createNewFile()");
                file.createNewFile();
            } catch (IOException e) {
                KdLog.p(KdLog.ERROR, "FAILED to create file %s!", file.getAbsolutePath());
                e.printStackTrace();
                return null;
            }
        }
//
        KdLog.p(KdLog.VEIN, "<=");
        return file;
    }

    /**
     * 创建文件。
     * @param dir 文件所在目录
     * @param relativePath 相对于所在目录的路径
     * @return 成功返回创建的file，失败返回null
     * @see #createFile(String)
     * */
    public static File createFile(String dir, String relativePath){
        return createFile(dir+File.separator+relativePath);
    }

    /**
     * 创建文件
     * @param location 文件所在位置 {@link #APP_INNER}, {@link #APP_INNER_CACHE}, {@link #APP_EXTERNAL}, {@link #APP_EXTERNAL_CACHE}
     * @param relativePath 相对于所在位置的路径
     * @return 成功返回创建的file，失败返回null
     * @see #createFile(String)
     * */
    public static File createFile(int location, String relativePath){
        File dir;
        switch (location){
            case APP_INNER:
                return createFile(ctx.getFilesDir().getAbsolutePath()+File.separator+relativePath);
            case APP_INNER_CACHE:
                return createFile(ctx.getCacheDir().getAbsolutePath()+File.separator+relativePath);
            case APP_EXTERNAL:
                dir = ctx.getExternalFilesDir(null);
                if (null==dir) return null;
                return createFile(dir.getAbsolutePath()+File.separator+relativePath);
            case APP_EXTERNAL_CACHE:
                dir = ctx.getExternalCacheDir();
                if (null==dir) return null;
                return createFile(dir.getAbsolutePath()+File.separator+relativePath);
//            case EXTERNAL_STORAGE_ROOT:
//                dir = Environment.getExternalStorageDirectory();
//                if (null==dir) return null;
//                return createFile(dir.getAbsolutePath()+File.separator+relativePath);
            default:
                return null;
        }
    }

    /**
     * 删除文件。
     * <br>若待删对象为目录则会删除该目录及其下所有内容
     * @param path 待删文件路径
     * @return 成功返回true，失败返回false
     * */
    public static boolean deleteFile(String path) {
        return deleteFile(new File(path));
    }

    /**
     * 删除文件。
     * <br>若待删对象为目录则会删除该目录及其下所有内容
     * @param file 待删文件对象
     * @return 成功返回true，失败返回false
     * */
    public static boolean deleteFile(File file) {
        KdLog.p("filepath=%s", file.getAbsolutePath());
        if (!file.exists()){
            KdLog.p(KdLog.WARN, "%s NOT EXISTS!", file.getAbsolutePath());
            return true;
        }

        if (file.isFile()){
            return file.delete();
        }else if (file.isDirectory()){
            boolean ret = true;
            File[] childFiles = file.listFiles();
            if (null == childFiles){
                // 比如属于root的文件夹您无权访问
                KdLog.p("FAILED to read %s, PERMISSION DENIED!", file.getAbsolutePath());
                return false;
            }
            KdLog.p("childFiles=%s, len=%s", childFiles, childFiles.length);
            for (File f : childFiles) {
                if (!deleteFile(f)) {
                    ret = false;
                }
            }
            return file.delete() && ret;
        }else{
            KdLog.p(KdLog.WARN, "UNKNOWN file type %s", file.getAbsolutePath());
            return false;
        }
    }

    /**
     * 批量删除文件。
     * @param pathList 待删文件路径列表
     * @see #deleteFile(String)
     * @return 成功返回true，失败返回false。有一个文件删除失败则为失败
     * */
    public static boolean deleteFiles(List<String> pathList){
        boolean ret = true;
        for (String path:pathList){
            if (!deleteFile(path)){
                ret = false;
            }
        }

        return ret;
    }

    /**
     * 拷贝文件 <br> 若目标路径包含不存在的目录，则这些目录会被创建；
     * @param srcPath 源文件路径
     * @param dstDirPath 目标目录路径
     * @param dstFileName 目标文件名，若为null或空白字符串则使用源文件名称
     * @return 成功返回目标文件，失败返回null
     * */
    public static File copyFile(String srcPath, String dstDirPath, String dstFileName){
        KdLog.p("srcPath=%s, dstDir=%s, dstFilename=%s", srcPath, dstDirPath, dstFileName);
        File srcFile = new File(srcPath);
        if (!srcFile.exists()){
            KdLog.p(KdLog.ERROR, "%s is NOT EXISTS", srcPath);
//            throw new FileNotFoundException();
            return null;
        }

        File dstDir = new File(dstDirPath);
        if (!dstDir.exists()){
            if (!dstDir.mkdirs()){
                KdLog.p(KdLog.ERROR, "FAILED to recursively make dir %s", dstDirPath);
                return null;
            }
        }
        if(StringUtils.isBlank(dstFileName)){
            dstFileName = getFullName(srcPath);
        }

        if (srcFile.isFile()){
            String dstPath = dstDir.getAbsolutePath()+File.separator+dstFileName;
            File dstFile = new File(dstPath);
            try {
                FileInputStream fis = new FileInputStream(srcFile);
                FileOutputStream fos = new FileOutputStream(dstFile);
                int bufSiz = fis.available();
                byte[] buf = new byte[bufSiz];
                while (fis.read(buf)>0){
                    fos.write(buf);
                }
                fis.close();
                fos.close();
            } catch (IOException e) {
                KdLog.p(KdLog.ERROR, "FAILED to copy %s to %s", srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
                e.printStackTrace();
                return null;
            }
        }else if (srcFile.isDirectory()){
            File[] childs = srcFile.listFiles();
            if (null == childs){
                // 比如属于root的文件夹您无权访问
                KdLog.p("FAILED to read %s, PERMISSION DENIED!", srcFile.getAbsolutePath());
                return null;
            }
            for (File f:childs){
                copyFile(f.getAbsolutePath(), dstDir+File.separator+srcFile.getName(), f.getName());
            }
        }else{

        }

        return new File(dstDir+File.separator+srcFile.getName());
    }

    /**
     * 移动文件 <br> 若目标路径包含不存在的目录，则这些目录会被创建；
     * <br><b>若目标文件已存在则该目标文件会被覆盖，特别地若目标文件为已存在的目录则会清空并删除整个目录</b>，请确认这是否是您期望的行为，
     * <br>您可以使用{@link #isExists(String)}预先判断目标文件是否存在，使用{@link #isDirPath(String)} 判断目标文件是否为目录。
     * @param srcPath 源文件路径，若源文件路径指向目录则移动整个目录。
     * @param dstDirPath 目标文件所处目录
     * @param dstFileName 目标文件名，若为null或空白字符串则使用源文件名称
     * @return 成功返回目标文件，失败返回null
     * */
    public static File moveFile(String srcPath, String dstDirPath, String dstFileName){
        KdLog.p("srcPath=%s, dstDir=%s, dstFilename=%s", srcPath, dstDirPath, dstFileName);
        File srcFile = new File(srcPath);
        if (!srcFile.exists()){
            KdLog.p(KdLog.ERROR, "%s is NOT EXISTS", srcPath);
            return null;
        }
        if (isInExternalStorage(srcPath) == isInExternalStorage(dstDirPath)){
            /*若源文件和目标文件同处内部存储或同处外部存储，则移动文件只需重命名文件即可无需真正的数据拷贝*/
            File dstDir = new File(dstDirPath);
            if (!dstDir.exists()){
                if (!dstDir.mkdirs()){
                    KdLog.p(KdLog.ERROR, "FAILED to recursively make dir %s", dstDirPath);
                    return null;
                }
            }
            if(StringUtils.isBlank(dstFileName)){
                dstFileName = getFullName(srcPath);
            }
            String dstPath = dstDir.getAbsolutePath()+File.separator+dstFileName;
            File dstFile = new File(dstPath);
            if (dstFile.isDirectory()){ // 目标文件为目录需先删除该目录，因为非空目录默认不能被覆盖
                if (!deleteFile(dstFile)){
                    KdLog.p(KdLog.ERROR, "FAILED to delete %s", dstFile.getAbsolutePath());
                    return null;
                }
            }
            if (!srcFile.renameTo(dstFile)){
                KdLog.p(KdLog.ERROR, "FAILED to rename %s to %s", srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
                return null;
            }

            return dstFile;

        }else{
            /*若源文件和目标文件身处不同存储空间，则移动文件不能通过重命名完成，需先拷贝数据到目标文件再删除源文件*/
            File dstFile = copyFile(srcPath, dstDirPath, dstFileName);
            deleteFile(srcFile);
            return dstFile;
        }
    }

    //===================== 文件读写
    /**
     * 读取文件
     * @param path 目标文件路径
     * @return 若成功以单一字符串形式返回文件所有内容，若失败返回null
     * */
    public static String read2Str(String path){
        List<String> lines = readByLine(path, -1);
        if (null == lines){
            return null;
        }
        StringBuffer tmp = new StringBuffer();
        for (String line : lines){
            tmp.append(line);
        }
        return tmp.toString();
    }

    /**
     * 按行读取文件
     * @param path 目标文件路径
     * @return 若成功以行列表形式返回文件所有内容，若失败返回null
     * */
    public static List<String> readByLine(String path){
        return readByLine(path, -1);
    }

    /**
     * 按行读取文件，指定最大读取行数
     * @param path 目标文件路径
     * @param lineNum 最大读取行数，若小于等于0则表示读取所有行
     * @return 若成功以行列表形式返回文件第一行到指定行数之间所有行，若失败返回null
     * */
    public static List<String> readByLine(String path, int lineNum){
        File file = createFile(path);
        if (null==file){
            return null;
        }
        if (!file.canRead()){
            KdLog.p(KdLog.ERROR, "%s NOT READABLE", file.getAbsolutePath());
            return null;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<String> lines = new ArrayList<>();
        String line;
        try {
            if (lineNum>0) {
                int count=0;
                while (null != (line = reader.readLine())
                        && count < lineNum) {
                    lines.add(line);
                    ++count;
                }
            }else{
                while (null != (line = reader.readLine())) {
                    lines.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            KdLog.p(KdLog.ERROR, "FAILED to read line from %s", file.getAbsolutePath());
            e.printStackTrace();
            return null;
        }

        return lines;
    }

    /**
     * 读取res/raw下面的文件
     * */
    public static String readFromResRaw(int resId){
        // TODO
        return null;
    }

    /**
     * 读取assets下面的文件
     * @param fileName 待读取的文件名
     * */
    public static String readFromAssets(String fileName){
        // TODO
        return null;
    }

//    private static Bitmap readFromAssets(String fileName){
//
//    }

    public static String readFromStream(InputStream is){
        // TODO
        return null;
    }

    /**
     * 拷贝assets文件到指定路径
     * @param assetName assets文件名
     * @param filePath 拷贝的目标路径
     * @return 若成功返回拷贝生成的目标文件，若失败返回null
     * */
    public static File asset2File(String assetName, String filePath){
        AssetManager am = ctx.getAssets();
        InputStream is;
        try {
            is = am.open(assetName);
            File file = stream2file(is, filePath);
            is.close();
            return file;
        } catch (IOException e) {
            KdLog.p(KdLog.ERROR, "FAILED to open asset %s", assetName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 拷贝res/raw文件到指定路径
     * @param resId raw文件对应的资源id
     * @param filePath 拷贝的目标路径
     * @return 若成功返回拷贝生成的目标文件，若失败返回null
     * */
    public static File resRaw2File(int resId, String filePath){
        InputStream is = ctx.getResources().openRawResource(resId);
        File file = stream2file(is, filePath);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }


    /**
     * 将输入流写入文件
     * @param is 输入流
     * @param filepath 目标文件路径
     * @return 若成功返回生成的目标文件，若失败返回null
     * */
    public static File stream2file(InputStream is, String filepath){
        File file = createFile(filepath);
        if (null==file){
            return null;
        }

        int length;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[1024*1024];
            while ((length=is.read(buf)) > 0){
                fos.write(buf, 0, length);
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }


    /**
     * 将字节数组写入文件
     * @param buf 字节数组
     * @param filepath 目标文件路径
     * @return 若成功返回生成的目标文件，若失败返回null
     * */
    public static File buf2file(byte[] buf, String filepath){
        File file = createFile(filepath);
        if (null==file){
            return null;
        }

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buf);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * 将字符串写入文件
     * @param content 待写入内容
     * @param charSet 字符集
     * @param filepath 目标文件路径
     * @return 若成功返回生成的目标文件，若失败返回null
     * */
    public static File str2File(String content, String charSet, String filepath) {
        File file = createFile(filepath);
        if (null==file){
            return null;
        }
        if (StringUtils.isBlank(charSet)){
            charSet = "UTF-8";
        }

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charSet));
            bw.write(content);
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return file;
    }



    public static String[] split(String srcFilePath, long splitIntoSize) throws IOException{
        return split(srcFilePath, splitIntoSize, null);
    }

    public static String[] split(String srcFilePath, long splitIntoSize, String saveDirPath) throws IOException{
        if (splitIntoSize <= 0) {
            return null;
        }

        File file = new File(srcFilePath);
        if (!file.exists() || (!file.isFile())) {
            return null;
        }

        File saveDir = null;
        if (null == saveDirPath){
            saveDirPath = getParentPath(srcFilePath)+getMainName(getFullName(srcFilePath))+"_splited";
        }

        saveDir = new File(saveDirPath);
        if (!saveDir.exists()){
            saveDir.mkdirs();
        }

        long fileLength = file.length();

        int num = (fileLength % splitIntoSize != 0) ? (int) (fileLength / splitIntoSize + 1)
                : (int) (fileLength / splitIntoSize);

        String[] fileNames = new String[num];

        FileInputStream in = new FileInputStream(file);

        final int BUF_SIZE = 1024*1024;
        byte[] buf = new byte[BUF_SIZE];
        int lenToRead=0;
        int lenRead=0;
        int totalLenRead=0;
        int lenToWrite=0;

        for (int i = 0; i < num; i++) {
            File outFile = new File(saveDir, getMainName(file.getName()) + "_part" + i + getExtName(file.getName()));


            FileOutputStream out = new FileOutputStream(outFile);

            totalLenRead = 0;
            while (totalLenRead < splitIntoSize){
                lenToRead = BUF_SIZE < (int)(splitIntoSize - lenRead) ? BUF_SIZE : (int)(splitIntoSize - lenRead);
                lenRead = in.read(buf, 0, lenToRead);
                if (-1 == lenRead){
                    break;
                }
                totalLenRead += lenRead;
                lenToWrite = lenRead;

                out.write(buf, 0, lenToWrite);
            }
            out.close();
            fileNames[i] = outFile.getAbsolutePath();
        }
        in.close();

        return fileNames;
    }


    public static int merge(String[] srcFileList, String dstFile) throws IOException{
        if (null == srcFileList || null == dstFile){
            return -1;
        }

        for (String file : srcFileList){
            if (!new File(file).exists()){
                return -1;
            }
        }

        File df = new File(dstFile);
        if (!df.getParentFile().exists()){
            df.getParentFile().mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(df);

        final int BUF_SIZE = 1024*10;
        byte[] buf = new byte[BUF_SIZE];

        int fileNum = srcFileList.length;
        for (int i=0, lenToRead=0, lenRead=0, totalLenRead=0;
             i<fileNum;
             ++i){
            File sf = new File(srcFileList[i]);

            long sfLen = sf.length();
            FileInputStream fis = new FileInputStream(sf);

            totalLenRead=0;
            while (totalLenRead < sfLen){
                lenToRead = BUF_SIZE < (int)(sfLen - totalLenRead) ? BUF_SIZE : (int)(sfLen - totalLenRead);
                lenRead = fis.read(buf, 0, lenToRead);
                totalLenRead += lenRead;

                fos.write(buf, 0, lenRead);
            }

            fis.close();
        }

        fos.close();

        return 0;
    }




    public static void bmp2file(Bitmap bmp, String filepath, Bitmap.CompressFormat format){
        if (null==bmp || null==filepath){
            return;
        }

        File parentDir = new File(getParentPath(filepath));
        if (!parentDir.exists()){
            parentDir.mkdirs();
        }

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        bmp.compress(format, 100, fout);
        try {
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //======================= 目录相关
    /**
     * 获取目录内文件名称列表
     * @param dirPath 目录路径
     * @param recursively 是否递归
     * @return 成功返回目录内文件名列表，失败返回null
     * */
    public static List<String> getFileNameList(String dirPath, boolean recursively){
        return getFileNameList(new File(dirPath), recursively);
    }// 没必要提供获取文件完整路径的版本，若用户需要完整路径可自行用目录路径和文件名拼接

    /**
     * 获取目录内文件名称列表
     * @param dir 目录文件
     * @param recursively 是否递归
     * @return 成功返回目录内文件名列表，失败返回null
     * */
    public static List<String> getFileNameList(File dir, boolean recursively){
        if (!dir.exists() || !dir.isDirectory()){
            return null;
        }

        if (!recursively) {
            return Arrays.asList(dir.list());
        }else {
            List<String> fileNameList = new ArrayList<>();
            File[] files = dir.listFiles();
            for (File f : files){
                if (f.isDirectory()){
                    fileNameList.addAll(getFileNameList(f, recursively));
                }else{
                    fileNameList.add(f.getName());
                }
            }

            return fileNameList;
        }
    }

    /**
     * 获取目录内指定扩展名的文件名称列表
     * @param dirPath 目录路径
     * @param extName 扩展名
     * @param recursively 是否递归
     * @return 成功返回目录内指定扩展名的文件名列表，失败返回null
     * */
    public static List<String> getFileNameListByExtName(String dirPath, final String extName, boolean recursively){
        return getFileNameListByExtName(new File(dirPath), extName, recursively);
    }

    /**
     * 获取目录内指定扩展名的文件名称列表
     * @param dir 目录文件
     * @param extName 扩展名
     * @param recursively 是否递归
     * @return 成功返回目录内指定扩展名的文件名列表，失败返回null
     * */
    public static List<String> getFileNameListByExtName(File dir, final String extName, boolean recursively){
        if (!dir.exists() || !dir.isDirectory()){
            return null;
        }
        final String dotExtName = extName.startsWith(".") ? extName : "."+extName;
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(dotExtName);
            }
        };

        List<String> fileNameList = new ArrayList<>();
        File[] files = dir.listFiles(filter);
        if (!recursively) {
            for (File f : files){
                if (f.isFile()) {
                    fileNameList.add(f.getName());
                }
            }
        }else {
            for (File f : files){
                if (f.isDirectory()){
                    fileNameList.addAll(getFileNameListByExtName(f, extName, recursively));
                }else{
                    fileNameList.add(f.getName());
                }
            }
        }

        return fileNameList;
    }

    /**清空目录
     * @param dirPath 目录路径
     * */
    public static void clearDir(String dirPath){
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()){
            return;
        }

        File[] childFiles = dir.listFiles();
        for (File f:childFiles){
            deleteFile(f);
        }
    }

    /**
     * 删除目录下指定扩展名的文件
     * @param dirPath 目录路径
     * @param extName 扩展名
     * */
    public static void deleteFromDirByExtName(String dirPath, final String extName){
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()){
            return;
        }
        final String dotExtName = extName.startsWith(".") ? extName : "."+extName;
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(dotExtName);
            }
        });
        for (File f:files){
            deleteFile(f);
        }
    }


    //======================= 存储相关
    /**路径是否在外部存储中*/
    public static boolean isInExternalStorage(String path){
        if (null==path){
            return false;
        }
        File f = Environment.getExternalStorageDirectory();
        if (null==f){
            return false;
        }
        return path.startsWith(f.getAbsolutePath());
    }

    /**获取外部存储路径*/
    public static String getExternalStoragePath(){
        File f = Environment.getExternalStorageDirectory();
        if (null==f){
            return null;
        }
        return f.getAbsolutePath();
    }

    /**外部存储是否可写*/
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**外部存储是否可读*/
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

//    /**
//     * 获取真实sdcard路径
//     * */
//    public static String getRealSdcardPath() throws IOException {
//        File file = new File("/proc/mounts");
//        if (file.canRead()) {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//            String line;
//            while (null != (line = reader.readLine())){
//                String[] parts = line.split(StringUtils.MULTI_BLANK_CHARS);
//                if (parts.length >= 2) {
//                    if (parts[0].contains("/vold/")) {
//                        return parts[1];
//                    }
//                }
//            }
//        }
//
//        return null;
//    }


    private static final String[][] ExtMIME_Map={
        /*{扩展名， 	 MIME类型} */
            {"",        "*/*"},
            {"txt",     "text/plain"},
            {"log",     "text/plain"},
            {"xml",     "text/plain"},
            {"conf",    "text/plain"},
            {"h",  	    "text/plain"},
            {"c",  	    "text/plain"},
            {"cpp",     "text/plain"},
            {"java",    "text/plain"},
            {"sh", 	    "text/plain"},
            {"htm",     "text/html"},
            {"html",    "text/html"},
            {"pdf",     "application/pdf"},
            {"doc",     "application/msword"},
            {"ppt",     "application/vnd.ms-powerpoint"},
            {"xls", 	"application/vnd.ms-excel"},
            {"bmp",     "image/bmp"},
            {"jpeg",    "image/jpeg"},
            {"png",     "image/png"},
            {"gif",   	"image/gif"}
    };

}
