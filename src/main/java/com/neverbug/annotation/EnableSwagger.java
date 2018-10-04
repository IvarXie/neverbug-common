package com.neverbug.annotation;

import java.lang.annotation.*;

/**
 * swagger的自动加载
 *
 * @author zhao.weiwei
 * Created on 2017/10/30 16:21
 *
 *
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableSwagger {
}
