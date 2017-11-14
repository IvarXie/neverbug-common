package com.jyall.util;

import com.jyall.exception.ErrorCode;
import com.jyall.exception.ErrorMsg;
import org.springframework.http.ResponseEntity;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * HTTP响应工具类
 *
 * @author guo.guanfei
 */
public class ResponseUtil {
    private ResponseUtil() {}

    /**
     * 一般HTTP响应
     *
     * @param status HTTP响应状态码
     * @param obj    返回的对象
     * @return
     */
    public static Response getResponse(int status, Object obj) {
        return Response.status(status).entity(obj)
                .header("Access-Control-Allow-Origin","*")
                .type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * 一般HTTP响应
     *
     * @param entity ResponseEntity对象
     * @return
     */
    public static Response getResponse(ResponseEntity<?> entity) {
        return Response.status(entity.getStatusCode().value()).entity(entity.getBody())
                .type(MediaType.APPLICATION_JSON)
                .header("Access-Control-Allow-Origin","*")
                .build();
    }

    /**
     * 成功的HTTP响应
     *
     * @param obj
     * @return
     */
    public static Response getOkResponse(Object obj) {
        return Response.ok(obj)
                .header("Access-Control-Allow-Origin","*")
                .type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * 业务错误的HTTP响应
     *
     * @param code 统一错误码
     * @param msg  错误摘要信息
     * @return
     */
    public static Response getBizErrorResponse(int code, String msg) {
        return makeResponse(ErrorCode.BIZ_ERROR, new ErrorMsg(code, msg));
    }

    /**
     * 业务错误的HTTP响应
     *
     * @param code   统一错误码
     * @param msg    错误摘要信息
     * @param detail 错误详细信息（一般用于调试）
     * @return
     */
    public static Response getBizErrorResponse(int code, String msg, String detail) {
        return makeResponse(ErrorCode.BIZ_ERROR, new ErrorMsg(code, msg, detail));
    }

    /**
     * 业务错误的HTTP响应
     *
     * @param errorCode 异常码对象
     * @return
     */
    public static Response getBizErrorResponse(ErrorCode errorCode) {
        return makeResponse(ErrorCode.BIZ_ERROR, new ErrorMsg(errorCode.value(), errorCode.msg()));
    }

    /**
     * 业务错误的HTTP响应
     *
     * @param errorMsg 异常信息
     * @return
     */
    public static Response getBizErrorResponse(ErrorMsg errorMsg) {
        return makeResponse(ErrorCode.BIZ_ERROR, errorMsg);
    }

    /**
     * 业务错误的HTTP响应
     *
     * @param errorCode 异常码对象
     * @param detail    错误详细信息（一般用于调试）
     * @return
     */
    public static Response getBizErrorResponse(ErrorCode errorCode, String detail) {
        return makeResponse(ErrorCode.BIZ_ERROR, new ErrorMsg(errorCode.value(), errorCode.msg(), detail));
    }

    /**
     * 系统错误的HTTP响应
     *
     * @param code 统一错误码
     * @param msg  错误摘要信息
     * @return
     */
    public static Response getSysErrorResponse(int code, String msg) {
        return makeResponse(ErrorCode.SYS_ERROR, new ErrorMsg(code, msg));
    }

    /**
     * 系统错误的HTTP响应
     *
     * @param errorCode 异常码对象
     * @return
     */
    public static Response getSysErrorResponse(ErrorCode errorCode) {
        return makeResponse(ErrorCode.SYS_ERROR, new ErrorMsg(errorCode.value(), errorCode.msg()));
    }

    /**
     * 系统错误的HTTP响应
     *
     * @param errorCode 异常码对象
     * @param detail    错误详细信息（一般用于调试）
     * @return
     */
    public static Response getSysErrorResponse(ErrorCode errorCode, String detail) {
        return makeResponse(ErrorCode.SYS_ERROR, new ErrorMsg(errorCode.value(), errorCode.msg(), detail));
    }

    /**
     * 系统错误的HTTP响应
     *
     * @param code   统一错误码
     * @param msg    错误摘要信息
     * @param detail 错误详细信息（一般用于调试）
     * @return
     */
    public static Response getSysErrorResponse(int code, String msg, String detail) {
        return makeResponse(ErrorCode.SYS_ERROR, new ErrorMsg(code, msg, detail));
    }

    /**
     * 系统错误的HTTP响应
     *
     * @param errorMsg 异常信息
     * @return
     */
    public static Response getSysErrorResponse(ErrorMsg errorMsg) {
        return makeResponse(ErrorCode.SYS_ERROR, errorMsg);
    }

    /**
     * 生成Response对象
     *
     * @param errorCode http状态码
     * @param entity    body实体
     * @return response对象
     */
    private static Response makeResponse(ErrorCode errorCode, Object entity) {
        return Response
                .status(errorCode.value())
				.header("Access-Control-Allow-Origin","*")
                .entity(entity)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
