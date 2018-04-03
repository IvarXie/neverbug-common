/********************************************************
 ***                     _ooOoo_                       ***
 ***                    o8888888o                      ***
 ***                    88" . "88                      ***
 ***                    (| -_- |)                      ***
 ***                    O\  =  /O                      ***
 ***                 ____/`---'\____                   ***
 ***               .'  \\|     |//  `.                 ***
 ***              /  \\|||  :  |||//  \                ***
 ***             /  _||||| -:- |||||-  \               ***
 ***             |   | \\\  -  /// |   |               ***
 ***             | \_|  ''\---/''  |   |               ***
 ***             \  .-\__  `-`  ___/-. /               ***
 ***           ___`. .'  /--.--\  `. . __              ***
 ***        ."" '<  `.___\_<|>_/___.'  >'"".           ***
 ***       | | :  `- \`.;`\ _ /`;.`/ - ` : | |         ***
 ***       \  \ `-.   \_ __\ /__ _/   .-` /  /         ***
 ***  ======`-.____`-.___\_____/___.-`____.-'======    ***
 ***                     `=---='                       ***
 ***   .............................................   ***
 ***         佛祖镇楼                  BUG辟易         ***
 ***   佛曰:                                           ***
 ***           写字楼里写字间，写字间里程序员；        ***
 ***           程序人员写程序，又拿程序换酒钱。        ***
 ***           酒醒只在网上坐，酒醉还来网下眠；        ***
 ***           酒醉酒醒日复日，网上网下年复年。        ***
 ***           但愿老死电脑间，不愿鞠躬老板前；        ***
 ***           奔驰宝马贵者趣，公交自行程序员。        ***
 ***           别人笑我忒疯癫，我笑自己命太贱；        ***
 ***           不见满街漂亮妹，哪个归得程序员？        ***
 *********************************************************
 ***               江城子 . 程序员之歌                 ***
 ***           十年生死两茫茫，写程序，到天亮。        ***
 ***               千行代码，Bug何处藏。               ***
 ***           纵使上线又怎样，朝令改，夕断肠。        ***
 ***           领导每天新想法，天天改，日日忙。        ***
 ***               相顾无言，惟有泪千行。              ***
 ***           每晚灯火阑珊处，夜难寐，加班狂。        ***
 *********************************************************
 ***      .--,       .--,                              ***
 ***      ( (  \.---./  ) )                            ***
 ***       '.__/o   o\__.'                             ***
 ***          {=  ^  =}              高山仰止,         ***
 ***           >  -  <               景行行止.         ***
 ***          /       \              虽不能至,         ***
 ***         //       \\             心向往之。        ***
 ***        //|   .   |\\                              ***
 ***        "'\       /'"_.-~^`'-.                     ***
 ***           \  _  /--'         `                    ***
 ***         ___)( )(___                               ***
 ***        (((__) (__)))                              ***
 ********************************************************/
package com.jyall.util;


import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 反射的工具类
 *
 * @author zhao.weiwei
 * Created on 2018/3/16 12:49
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
public class ReflectUtils {
    /**
     * 根据类别获取类上的注解
     *
     * @param clazz  原始类
     * @param tClass 注解类
     * @param <T>    注解的泛型规约
     * @return 返回注解
     */
    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> tClass) {
        while (!clazz.equals(Object.class)) {
            T t = clazz.getAnnotation(tClass);
            if (t != null) {
                return t;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * 设置属性
     *
     * @param targetObject
     * @param fieldName
     * @param fieldObject
     * @throws Exception
     */
    public static void setField(Object targetObject, String fieldName, Object fieldObject) throws Exception {
        Class<?> clazz = targetObject.getClass();
        while (!clazz.equals(Object.class)) {
            Field field = clazz.getDeclaredField(fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(targetObject, fieldObject);
                break;
            }
        }
    }

    /**
     * copy属性生成bean。
     * 必须有默认的无参数的构造方法
     *
     * @param src    原对象
     * @param target 目标对象的class
     * @param ignore 忽略的属性
     * @param <T>
     * @return
     */
    public static <T> T copy(Object src, Class<T> target, String... ignore) throws Exception {
        T t = target.newInstance();
        BeanUtils.copyProperties(src, t, ignore);
        return t;
    }

    /**
     * 获取所有加载的class
     *
     * @return
     */
    public static List<String> getLoadClasses() throws Exception {
        List<String> list = Lists.newArrayList();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        while (true) {
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
                Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
                ucpField.setAccessible(true);
                URL[] urls = URLClassLoader.class.cast(urlClassLoader).getURLs();
                Arrays.stream(urls).forEach(url -> {
                    try {
                        String protocol = url.getProtocol();
                        if ("jar".equals(protocol)) {
                            JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
                            list.addAll(getClassNameByJar(jarFile));
                        } else if ("file".equals(protocol)) {
                            JarFile jarFile = new JarFile(URLDecoder.decode(url.getFile(), "UTF-8"));
                            list.addAll(getClassNameByJar(jarFile));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            classLoader = classLoader.getParent();
            if (classLoader == null) {
                break;
            }
        }
        return list;
    }

    private static List<String> getClassNameByJar(JarFile jarFile) {
        List<String> jarClass = new ArrayList<>();
        Enumeration<JarEntry> entrys = jarFile.entries();
        while (entrys.hasMoreElements()) {
            JarEntry jarEntry = entrys.nextElement();
            String entryName = jarEntry.getName();
            if (entryName.endsWith(".class")) {
                entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                jarClass.add(entryName);
            }
        }
        return jarClass;
    }

}
