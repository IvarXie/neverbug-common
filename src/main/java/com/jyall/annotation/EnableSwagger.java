package com.jyall.annotation;

import java.lang.annotation.*;

/**
 * swagger的自动加载
 *
 * @author zhao.weiwei
 * Created on 2017/10/30 16:21
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableSwagger {
}
