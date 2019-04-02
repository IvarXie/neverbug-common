package com.neverbug.common.mybatis.exception;

/**
 * 一对一的解析异常
 *
 * @author neverbug
 * Created on 2018/5/26 14:48
 */
public class One2OneException extends RuntimeException {
    public One2OneException(String msg) {
        super(msg);
    }
}
