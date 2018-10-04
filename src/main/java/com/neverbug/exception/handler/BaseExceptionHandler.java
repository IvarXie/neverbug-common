package com.neverbug.exception.handler;

import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.ws.rs.ext.ExceptionMapper;

/**
 * Unchecked Exception Handler
 *
 * @author guo.guanfei
 */
public abstract class BaseExceptionHandler<E extends Throwable> implements ExceptionMapper<E> {
    protected String getErrorStackTrace(Throwable ex) {
        return ExceptionUtils.getStackTrace(ex);
    }
}
