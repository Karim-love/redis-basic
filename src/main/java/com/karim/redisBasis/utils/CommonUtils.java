package com.karim.redisBasis.utils;

/**
 * Created by sblim
 * Date : 2021-12-23
 * Time : 오후 6:00
 */
public class CommonUtils {
    public static String getStackTrace(Exception e) {
        StackTraceElement[] element = e.getStackTrace();
        StringBuilder str = new StringBuilder();

        for(int i = 0; i < element.length; ++i) {
            if (i == 0) {
                str.append(e.toString()).append("\n").append(element[i].getClassName()).append(" : ").append(e.getLocalizedMessage()).append("\n");
            }
            str.append("\tat ").append(element[i].getClassName()).append(".").append(element[i].getMethodName()).append("(").append(element[i].getFileName()).append(":").append(element[i].getLineNumber()).append(")").append("\n");
        }
        return str.toString();
    }
}
