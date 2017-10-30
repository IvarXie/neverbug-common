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

import com.jyall.annotation.EnableJersey;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import java.util.Map;

/**
 * jersey的自动加载
 * <p>
 *
 * @author zhao.weiwei
 * Created on 2017/10/30 17:05
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Configuration
@ApplicationPath("/v1")
@ConditionalOnClass({ResourceConfig.class})
@ConditionalOnBean(annotation = EnableJersey.class)
public class JerseyConfig extends ResourceConfig implements ApplicationContextAware {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private ApplicationContext applicationContext;

    public JerseyConfig() {
        // 注册异常处理类和swagger相关Provider
        packages("com.jyall.exception.handler", "com.wordnik.swagger.jersey.listing");
    }

    @PostConstruct
    public void initTheJerseyConfig() {
        long start = System.currentTimeMillis();
        logger.info("init the jersey resource start");
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        beans.forEach((k, v) -> {
            Class<?> clazz = getClassOfBean(v);
            Path path = clazz.getAnnotation(Path.class);
            if (path != null) {
                logger.info("register the jersey resource is {}", clazz.getName());
                register(clazz);
            }
        });
        logger.info("init the jersey resource success,use {}ms", System.currentTimeMillis() - start);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
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
}
