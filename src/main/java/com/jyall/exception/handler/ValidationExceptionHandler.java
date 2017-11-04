/* =======================================================
 * 金色家网络科技有限公司-技术中心
 * 日 期：2016-3-28 13:46
 * 作 者：li.jianqiu
 * 版 本：0.0.1
 * 描 述：TODO
 * ========================================================
 */
package com.jyall.exception.handler;

import com.alibaba.fastjson.JSON;
import com.jyall.annotation.EnableJersey;
import com.jyall.exception.ErrorCode;
import com.jyall.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: ValidationException </p>
 * @Author: li.jianqiu</p>
 * @Date: 2016-3-28 13:46 </p>
 * @Version: 0.0.1</p>
 * @Since: JDK 1.8</p>
 * @See: TODO</p>
 */
@Component
@ConditionalOnBean(annotation = EnableJersey.class)
public class ValidationExceptionHandler extends BaseExceptionHandler<javax.validation.ValidationException> {
    private static final Logger logger = LoggerFactory.getLogger(ValidationExceptionHandler.class);

    @Override
    public Response toResponse(javax.validation.ValidationException e) {
        String msg;
        try {
            ConstraintViolationException cve = ConstraintViolationException.class.cast(e);
            List<String> list = cve.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
            msg = JSON.toJSONString(list);
        } catch (ClassCastException e1) {
            logger.error("cast error!", e1);
            msg = "unknown error";
        }
        return ResponseUtil.getBizErrorResponse(ErrorCode.BIZ_ERROR_PARAMETER.value(), msg, getErrorStackTrace(e));
    }
}
