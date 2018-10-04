package com.neverbug.annotation;

import java.lang.annotation.*;

/**
 * jersey的自动加载，不需要配置
 * @author neverbug
 * Created on 2017/10/30 16:21
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableJersey {
}
