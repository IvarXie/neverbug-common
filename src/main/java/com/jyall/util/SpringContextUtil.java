package com.jyall.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * applicationContetx的工具类
 * <P>
 * 使用延时生成的注解
 * Created by zhao.weiwei
 * create on 2017/3/1 14:36
 * the email is zhao.weiwei@jyall.com.
 */
@Lazy
@Component
public class SpringContextUtil implements ApplicationContextAware {

    public static ApplicationContext applicationContext;


    public ApplicationContext getApplicationContext() {
        return SpringContextUtil.applicationContext;
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextUtil.applicationContext = applicationContext;
    }
}
