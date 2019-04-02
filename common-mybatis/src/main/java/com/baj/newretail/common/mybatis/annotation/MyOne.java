package com.baj.newretail.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 一对一的映射关系
 *
 * @author neverbug
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyOne {
    /**
     * 一对多关系里面 自己的关联字段
     *
     * @return
     */
    String refrenceOwnField();

    /**
     * 一对一关系里面 另一个实体的查询字段
     *
     * @return
     */
    String refrenceOtherField();

    /**
     * 另一个实体是不是主键
     * 你默认false
     *
     * @return
     */
    boolean primary() default false;
}
