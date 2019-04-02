package com.neverbug.common.mybatis.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

/**
 * 带toString的抽象类
 * @author neverbug
 */
public abstract class AbstractToString {
    @Override
    public String toString() {
        try {
            return new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(toJson(this)));
        } catch (Exception e) {
            return toJson(this);
        }
    }

    /**
     * 将实体转换为json
     *
     * @param obj
     * @return
     */
    private String toJson(Object obj) {
        Gson gsonDate = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();
        return gsonDate.toJson(obj);
    }
}
