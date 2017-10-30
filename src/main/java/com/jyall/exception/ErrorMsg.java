package com.jyall.exception;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.TimeoutException;

/**
 * RESTful服务返回的错误信息
 *
 * @author guo.guanfei
 */
public class ErrorMsg implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ErrorMsg.class);
    private static final long serialVersionUID = 2640926329092743174L;

    // 统一错误码
    private int code;
    // 错误信息摘要
    private String message;
    // 错误信息详情（主要用于调试）
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

    public static ErrorMsg parse(Exception e) {
        String err = e.getCause().toString();
        try {
            if (e.getCause().getClass() == TimeoutException.class) {
                return new ErrorMsg(ErrorCode.SYS_ERROR_RPC_CONNECTION, "远程服务调用超时");
            } else if (err.indexOf("content:") < 0) {
                logger.debug("其他错误", e);
                return new ErrorMsg(ErrorCode.BIZ_ERROR.value(), e.getMessage());
            } else {
                return JSON.parseObject(err.split("content:")[1], ErrorMsg.class);
            }
        } catch (Exception e1) {
            logger.error("从异常信息中解析ErrorMsg失败", e1);
            return new ErrorMsg(ErrorCode.SYS_ERROR, err);
        }
    }
}
