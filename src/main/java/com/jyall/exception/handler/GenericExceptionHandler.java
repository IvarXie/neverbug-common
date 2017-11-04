package com.jyall.exception.handler;

import com.alibaba.fastjson.JSON;
import com.jyall.annotation.EnableJersey;
import com.jyall.exception.ErrorCode;
import com.jyall.exception.ErrorMsg;
import com.jyall.exception.JyBizException;
import com.jyall.util.ResponseUtil;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Unchecked Exception Handler
 *
 * @author guo.guanfei
 */
//@Provider
@Configuration
@ConditionalOnBean(annotation = EnableJersey.class)
public class GenericExceptionHandler extends BaseExceptionHandler<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(GenericExceptionHandler.class);

    @Override
    public Response toResponse(Throwable ex) {
        logger.error(ex.getMessage(), ex);

        ErrorMsg errMsg = new ErrorMsg(ErrorCode.GENERIC_ERROR.value(), ex.getMessage());
        // 被截获异常无有效信息时，使用通用错误的描述信息
        if (null == errMsg.getMessage()) {
            errMsg.setMessage(ErrorCode.GENERIC_ERROR.msg());
        }
        // 调试开关打开，返回详细的错误堆栈信息
        if (logger.isDebugEnabled()) {
            errMsg.setDetail(getErrorStackTrace(ex));
        }

        int status = ErrorCode.SYS_ERROR.value();
        if (ex instanceof WebApplicationException) {
            status = ((WebApplicationException) ex).getResponse().getStatus();
        } else if (ex instanceof UndeclaredThrowableException) {
            try {
                JyBizException jyEx = JyBizException.class.cast(((UndeclaredThrowableException) ex).getUndeclaredThrowable());
                errMsg.setCode(jyEx.getCode());
                errMsg.setMessage(jyEx.getMessage());
            } catch (Exception e) {
                // 未定义异常不是JyBizException
                logger.debug("非JyBizException的未定义异常", e);
            }
        } else if (ex instanceof HystrixRuntimeException) {
            // 处理Controller使用FeignClient调用Service时收到的异常
            try {
                status = ((FeignException) ex.getCause()).status();
                String jsonStr = ex.getCause().toString().split("content:")[1];
                errMsg = JSON.parseObject(jsonStr, ErrorMsg.class);
            } catch (Exception feignEx) {
                logger.error("解析FeignClient异常中的ErrorMsg信息出错", feignEx);
            }
        }
        return ResponseUtil.getResponse(status, errMsg);
    }

}
