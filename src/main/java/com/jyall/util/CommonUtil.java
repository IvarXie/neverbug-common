package com.jyall.util;

import org.apache.commons.lang.exception.ExceptionUtils;

public class CommonUtil {
    /**
     * 获取方法名
     *
     * @return
     */
    public static String getMethodName() {
        try {
            return Thread.currentThread().getStackTrace()[2].getMethodName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取异常堆栈
     *
     * @param e
     * @return
     */
    public static String getException(Throwable e) {
        return ExceptionUtils.getFullStackTrace(e);
    }

}
