package com.baj.newretail.common.mybatis.exception;

/**
 * mapper不是泛型的mapepr
 *
 * @author neverbug
 * Created on 2018/6/5 16:57
 */
public class MapperUnsuitedException extends RuntimeException {
    public MapperUnsuitedException(String msg) {
        super(msg);
    }
}
