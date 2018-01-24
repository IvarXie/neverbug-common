package com.jyall.exception;

import com.alibaba.fastjson.JSON;
import feign.FeignException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.TimeoutException;

/**
 * * RESTful服务返回的错误信息
 * *
 * * @author guo.guanfei
 */
public class ErrorMsg implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ErrorMsg.class);
    private static final long serialVersionUID = 2640926329092743174L;

    private static String errorIndex = "content:";
    /**
     * * 统一错误码
     **/
    private int code;
    /**
     * * 错误信息摘要
     **/
    private String message;
    /**
     * * 错误信息详情（主要用于调试）
     **/
    private String detail = "";

    public ErrorMsg() {
        super();
    }

    public ErrorMsg(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorMsg(int code, String message, String detail) {
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    public ErrorMsg(ErrorCode errorCode) {
        this.code = errorCode.value();
        this.message = errorCode.msg();
    }

    public ErrorMsg(ErrorCode errorCode, String detail) {
        this.code = errorCode.value();
        this.message = errorCode.msg();
        this.detail = detail;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public static ErrorMsg parse(Throwable e) {
        if (e instanceof TimeoutException) {
            return new ErrorMsg(ErrorCode.SYS_ERROR_RPC_CONNECTION, "远程服务调用超时");
        } else if (e.getCause() != null && e.getCause().getClass() == TimeoutException.class) {
            return new ErrorMsg(ErrorCode.SYS_ERROR_RPC_CONNECTION, "远程服务调用超时");
        }
        String err = e.getMessage();
        try {
            if (e instanceof FeignException) {
                err = e.getMessage();
            } else if (e.getCause() != null && e.getCause().getClass() == FeignException.class) {
                err = e.getCause().getMessage();
            }
            if (StringUtils.isNotEmpty(err)) {
                if (!err.contains(errorIndex)) {
                    err = ExceptionUtils.getFullStackTrace(e);
                    if (!err.contains(errorIndex)) {
                        logger.error("其他错误", e);
                        return new ErrorMsg(ErrorCode.BIZ_ERROR.value(), e.getMessage(), ExceptionUtils.getFullStackTrace(e));
                    } else {
                        err = err.split("content:")[1];
                        if (err.contains("{") && err.contains("}")) {
                            err = err.substring(err.indexOf("{"), err.indexOf("}") + 1);
                            return JSON.parseObject(err, ErrorMsg.class);
                        } else {
                            logger.error("其他错误", e);
                            return new ErrorMsg(ErrorCode.BIZ_ERROR.value(), e.getMessage(), ExceptionUtils.getFullStackTrace(e));
                        }
                    }
                } else {
                    return JSON.parseObject(err.split("content:")[1], ErrorMsg.class);
                }
            } else {
                return new ErrorMsg(ErrorCode.GENERIC_ERROR.value(), e.getClass().getName(), ExceptionUtils.getFullStackTrace(e));
            }
        } catch (Exception e1) {
            logger.error("从异常信息中解析ErrorMsg失败", e1);
            return new ErrorMsg(ErrorCode.SYS_ERROR, StringUtils.isNotEmpty(err) ? err : ExceptionUtils.getFullStackTrace(e));
        }
    }
}

