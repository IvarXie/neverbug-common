package com.baj.newretail.common.mybatis.domain;

import com.baj.newretail.common.mybatis.annotation.MyOrderBy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

/**
 * 带排序的抽象类
 *
 * @author neverbug
 * Created on 2018/4/8 10:01
 */
public abstract class AbstractOrderBy extends AbstractToString{
    @MyOrderBy
    @Expose(serialize = false, deserialize = false)
    private String orderBy;

    @JsonIgnore
    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
}
