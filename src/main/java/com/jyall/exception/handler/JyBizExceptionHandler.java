package com.jyall.exception.handler;

import com.jyall.exception.JyBizException;
import com.jyall.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Business Exception Handler
 * 
 * @author guo.guanfei
 *
 */
@Provider
public class JyBizExceptionHandler extends BaseExceptionHandler implements ExceptionMapper<JyBizException> {

	private static final Logger logger = LoggerFactory.getLogger(JyBizExceptionHandler.class);

	@Override
	public Response toResponse(JyBizException ex) {
		// TODO Auto-generated method stub
		logger.error(ex.getMessage(), ex);

		if (logger.isDebugEnabled()) {
			return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage(), getErrorStackTrace(ex));
		}

		return ResponseUtil.getBizErrorResponse(ex.getCode(), ex.getMessage());
	}

}
