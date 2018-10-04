package com.neverbug.exception.handler;

import com.neverbug.annotation.EnableJersey;
import com.neverbug.exception.JypayBizException;
import com.neverbug.util.ResponseUtil;
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
public class JypayBizExceptionHandler extends BaseExceptionHandler<JypayBizException> {

    private static final Logger logger = LoggerFactory.getLogger(JypayBizExceptionHandler.class);

    @Override
    public Response toResponse(JypayBizException ex) {
        logger.error(ex.getMessage(), ex);
        if (logger.isDebugEnabled()) {
            return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage(), getErrorStackTrace(ex));
        }
        return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage());
    }

}
