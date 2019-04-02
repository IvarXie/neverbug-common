package com.neverbug.common.mybatis.exception;

/**
 * 一对一一对多的，另外一个实体的service找不到的异常
 *
 * @author neverbubg
 * Created on 2018/5/28 11:15
 */
public class BaseServiceNotFoundException extends RuntimeException {

    public BaseServiceNotFoundException(String msg) {
        super(msg);
    }
}
