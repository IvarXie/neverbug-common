package com.jyall.exception.handler;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jyall.exception.JypayBizException;
import com.jyall.util.ResponseUtil;

/**
 * Business Exception Handler
 * 
 * @author guo.guanfei
 *
 */
@Provider
public class JypayBizExceptionHandler extends BaseExceptionHandler implements ExceptionMapper<JypayBizException> {

	private static final Logger logger = LoggerFactory.getLogger(JypayBizExceptionHandler.class);

	@Override
	public Response toResponse(JypayBizException ex) {
		logger.error(ex.getMessage(), ex);
		if (logger.isDebugEnabled()) 
			return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage(), getErrorStackTrace(ex));
		return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage());
	}

}
