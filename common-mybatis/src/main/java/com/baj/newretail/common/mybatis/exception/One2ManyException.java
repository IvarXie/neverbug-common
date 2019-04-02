package com.baj.newretail.common.mybatis.exception;

/**
 * 一对多的解析异常
 *
 * @author neverbug
 * Created on 2018/5/26 14:48
 */
public class One2ManyException extends RuntimeException {
    public One2ManyException(String msg) {
        super(msg);
    }
}
