package com.kedacom.vconfsdk.utils;

/**
 * Created by Sissi on 11/7/2017.
 */
public final class StringUtils {
    public static final String MULTI_BLANK_CHARS = "\\s+";

    /**
     * 字符串是否为空白。
     * @param str 待测字符串
     * @return 返回true，若字符串为null或者字符串只包含诸如空格、制表、回车、换行等空白字符
     * <br>否则返回false
     * */
    public static boolean isBlank(String str){
        return null==str||str.trim().isEmpty();
    }
}
