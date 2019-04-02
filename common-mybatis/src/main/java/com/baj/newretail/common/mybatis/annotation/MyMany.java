package com.baj.newretail.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 一对多的映射关系
 * 只支持Set List Array的格式
 *
 * @author neverbug
 * Created on 2018/5/24 11:06
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyMany {
    /**
     * 一对多关系里面 one的关联字段
     *
     * @return
     */
    String refrenceOwnField();

    /**
     * 一对多关系里面 many的查询字段
     *
     * @return
     */
    String refrenceOtherField();
}
