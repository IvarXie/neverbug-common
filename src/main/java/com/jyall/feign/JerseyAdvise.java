/*
                            _ooOoo_  
                           o8888888o  
                           88" . "88  
                           (| -_- |)  
                            O\ = /O  
                        ____/`---'\____  
                      .   ' \\| |// `.  
                       / \\||| : |||// \  
                     / _||||| -:- |||||- \  
                       | | \\\ - /// | |  
                     | \_| ''\---/'' | |  
                      \ .-\__ `-` ___/-. /  
                   ___`. .' /--.--\ `. . __  
                ."" '< `.___\_<|>_/___.' >'"".  
               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
                 \ \ `-. \_ __\ /__ _/ .-` / /  
         ======`-.____`-.___\_____/___.-`____.-'======  
                            `=---='  
  
         .............................................  
                  佛祖镇楼                  BUG辟易  
          佛曰:  
                  写字楼里写字间，写字间里程序员；  
                  程序人员写程序，又拿程序换酒钱。  
                  酒醒只在网上坐，酒醉还来网下眠；  
                  酒醉酒醒日复日，网上网下年复年。  
                  但愿老死电脑间，不愿鞠躬老板前；  
                  奔驰宝马贵者趣，公交自行程序员。  
                  别人笑我忒疯癫，我笑自己命太贱；  
                  不见满街漂亮妹，哪个归得程序员？
*/
package com.jyall.feign;

import com.google.common.collect.Lists;
import com.jyall.annotation.EnableJersey;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * jersey的增强。自动添加 filter和resource
 * <p>
 * 扫描 Component和Path的注解
 *
 * @author zhao.weiwei
 * Created on 2017/10/30 18:46
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Component
@ConditionalOnBean(annotation = EnableJersey.class)
public class JerseyAdvise {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ResourceConfig resourceConfig;

    @PostConstruct
    public void initTheJerseyConfig() {
        /**注册jersey的Resource的过滤器**/
        long start = System.currentTimeMillis();
        logger.info("init the jersey resource start");
        logger.info("get all the Component annation beans");
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        List<Object> classes = Lists.newArrayList();
        beans.values().stream()
                .filter(k -> !AopUtils.isJdkDynamicProxy(k))
                .filter(k -> getClassOfBean(k).getAnnotation(Path.class) != null)
                .forEach(classes::add);
        logger.info("the register jersey resource size is {}", classes.size());
        classes.forEach(v -> {
            Class<?> clazz = getClassOfBean(v);
            logger.info("register the jersey resource is {}", clazz.getName());
            resourceConfig.register(clazz);
        });
        logger.info("init the jersey resource success,use {}ms", System.currentTimeMillis() - start);
        /**注册jersey的Request的过滤器**/
        logger.info("init the ContainerRequestFilter start");
        Map<String, ContainerRequestFilter> mapRequest = applicationContext.getBeansOfType(ContainerRequestFilter.class);
        logger.info("the register jersey ContainerRequestFilter size is {}", classes.size());
        mapRequest.values().forEach(v -> {
            Class<?> clazz = getClassOfBean(v);
            logger.info("regitster the ContainerRequestFilter is {}", clazz);
            resourceConfig.register(clazz);
        });
        logger.info("init the ContainerRequestFilter success");
        /**注册jersey的Response的过滤器**/
        logger.info("init the ContainerResponseFilter start");
        Map<String, ContainerResponseFilter> mapResponse = applicationContext.getBeansOfType(ContainerResponseFilter.class);
        logger.info("the register jersey ContainerResponseFilter size is {}", classes.size());
        mapResponse.values().forEach(v -> {
            Class<?> clazz = getClassOfBean(v);
            logger.info("regitster the ContainerResponseFilter is {}", clazz);
            resourceConfig.register(clazz);
        });
        logger.info("init the ContainerResponseFilter success");
        /**注册异常处理**/
        logger.info("init the ExceptionMapper start");
        Map<String, ExceptionMapper> exceptionMapperMap = applicationContext.getBeansOfType(ExceptionMapper.class);
        exceptionMapperMap.values().forEach(v -> {
            Class<?> clazz = getClassOfBean(v);
            logger.info("regitster the ExceptionMapper is {}", clazz);
            resourceConfig.register(clazz);
        });
        logger.info("init the ExceptionMapper success");

        /**注册ReaderInterceptor**/
        logger.info("init the ReaderInterceptor start");
        Map<String, ReaderInterceptor> readerInterceptorMap = applicationContext.getBeansOfType(ReaderInterceptor.class);
        readerInterceptorMap.values().forEach(v -> {
            Class<?> clazz = getClassOfBean(v);
            logger.info("regitster the ReaderInterceptor is {}", clazz);
            resourceConfig.register(clazz);
        });
        logger.info("init the ReaderInterceptor success");
        /**注册WriterInterceptor**/
        logger.info("init the ReaderInterceptor start");
        Map<String, WriterInterceptor> writerInterceptorMap = applicationContext.getBeansOfType(WriterInterceptor.class);
        writerInterceptorMap.values().forEach(v -> {
            Class<?> clazz = getClassOfBean(v);
            logger.info("regitster the WriterInterceptor is {}", clazz);
            resourceConfig.register(clazz);
        });
        logger.info("init the ReaderInterceptor success");
    }

    private Class<?> getClassOfBean(Object bean) {
        Class<?> clazz = bean.getClass();
        try {
            if (AopUtils.isAopProxy(bean)) {
                clazz = AopUtils.getTargetClass(bean);
            }
        } catch (Exception e) {
            logger.error("getClassOfBean error", e);
        }
        return clazz;
    }

    private Object getCurentBean(Object bean) {
        Object current = bean;
        try {
            if (AopUtils.isCglibProxy(bean)) {
                current = getCglibProxyTargetObject(bean);
            } else if (AopUtils.isJdkDynamicProxy(bean)) {
                current = getJdkDynamicProxyTargetObject(bean);
            }
        } catch (Exception e) {
            current = bean;
        }
        return current;
    }

    private Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        return target;
    }

    private Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
        return target;
    }
}
