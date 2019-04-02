package com.baj.newretail.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * Mybatis的列名的注解
 *
 * @create by neverbug
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyColumn {
    /**
     * 列名
     *
     * @return
     */
    String value() default "";

    /**
     * 模糊匹配查询是否精确查找，默认false
     *
     * @return
     */
    boolean accuracy() default false;
}
