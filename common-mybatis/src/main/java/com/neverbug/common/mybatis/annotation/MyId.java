package com.neverbug.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * mybatis的ID主键的注解
 *
 * @create by neverbug
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyId {
    String value() default "";
}
