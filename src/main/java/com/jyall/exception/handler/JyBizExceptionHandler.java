package com.jyall.exception.handler;

import com.jyall.annotation.EnableJersey;
import com.jyall.exception.JyBizException;
import com.jyall.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;

/**
 * Business Exception Handler
 *
 * @author guo.guanfei
 */
@Component
@ConditionalOnBean(annotation = EnableJersey.class)
public class JyBizExceptionHandler extends BaseExceptionHandler<JyBizException> {

    private static final Logger logger = LoggerFactory.getLogger(JyBizExceptionHandler.class);

    @Override
    public Response toResponse(JyBizException ex) {
        logger.error(ex.getMessage(), ex);
        if (logger.isDebugEnabled()) {
            return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage(), getErrorStackTrace(ex));
        }
        return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage());
    }

}
