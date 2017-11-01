package com.jyall.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * spring的bean的版本注解
 * <p>
 * 使用在类上，用于区分接口的版本
 *
 * @author zhao.weiwei
 * Created on 2017/11/1 15:57
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanVersion {
    @AliasFor("version")
    String value() default "0.0.0";

    @AliasFor("value")
    String version() default "0.0.0";

    /**
     * 作者
     **/
    String author() default "";

    /**
     * 创作时间
     **/
    String createTime() default "";
}
