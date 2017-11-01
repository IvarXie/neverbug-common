package com.jyall.util;

import com.google.common.collect.Maps;
import com.jyall.annotation.BeanVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

/**
 * applicationContetx的工具类
 * <p>
 * Created by zhao.weiwei
 * create on 2017/3/1 14:36
 * the email is zhao.weiwei@jyall.com.
 */
@Component("jyallSpringContextUtil")
public class SpringContextUtil implements ApplicationContextAware {

    private static Map<Class<?>, TreeMap<Integer, Object>> beanMap = Maps.newHashMap();

    public static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
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

    /**
     * 获取最后的版本
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getLastVersionBean(Class<T> clazz) {
        assemblyBeans(clazz);
        TreeMap<Integer, Object> currentBeans = beanMap.get(clazz);
        return clazz.cast(currentBeans.size() > 0 ? currentBeans.lastEntry().getValue() : null);
    }

    /**
     * 获取第一个版本的spring的bean
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getFirstVersionBean(Class<T> clazz) {
        assemblyBeans(clazz);
        TreeMap<Integer, Object> currentBeans = beanMap.get(clazz);
        return clazz.cast(currentBeans.size() > 0 ? currentBeans.firstEntry().getValue() : null);
    }

    public static <T> T getVersionBean(Class<T> clazz, String version) {
        int ver = Integer.parseInt(version.replaceAll("\\.", ""));
        assemblyBeans(clazz);
        TreeMap<Integer, Object> currentBeans = beanMap.get(clazz);
        if (!currentBeans.isEmpty()) {
            if (currentBeans.lastEntry().getKey() <= ver) {
                return clazz.cast(currentBeans.lastEntry().getValue());
            } else if (currentBeans.containsKey(ver)) {
                return clazz.cast(currentBeans.get(ver));
            }
        }
        return null;
    }

    /**
     * 构建bean的集合
     *
     * @param clazz
     * @param <T>
     */
    private static <T> void assemblyBeans(Class<T> clazz) {
        if (!beanMap.containsKey(clazz)) {
            Map<String, T> map = applicationContext.getBeansOfType(clazz);
            TreeMap<Integer, Object> treeMap = new TreeMap<>();
            if (!map.isEmpty()) {
                map.entrySet().stream()
                        .filter(entry -> entry.getValue().getClass().getAnnotation(BeanVersion.class) != null)
                        .forEach(entry -> treeMap.put(Integer.parseInt(entry.getValue().getClass().getAnnotation(BeanVersion.class).value().replaceAll("\\.", "")), entry.getValue()));
            }
            beanMap.put(clazz, treeMap);
        }
    }
}
