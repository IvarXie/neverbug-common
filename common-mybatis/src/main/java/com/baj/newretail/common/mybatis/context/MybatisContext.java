package com.baj.newretail.common.mybatis.context;

import com.baj.newretail.common.mybatis.service.BaseService;
import com.baj.newretail.common.mybatis.service.IBaseUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * mybatis的spring上下文
 * 主要提供逻辑删除的一些属性和AbstractDomain的配置
 *
 * @author neverbug
 * Created on 2018/5/21 13:02
 */
@Component
public class MybatisContext implements ApplicationContextAware, EnvironmentAware {

    /**
     * slf4j日志
     */
    private static Logger logger = LoggerFactory.getLogger(MybatisContext.class);

    /**
     * 获取用户ID的service
     */
    private static IBaseUserService baseUserService;

    /**
     * 逻辑删除的标志位
     */
    private static boolean LOGIC_DELETE = false;
    /**
     * spring的上下文
     */
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        MybatisContext.applicationContext = applicationContext;
        try {
            baseUserService = applicationContext.getBean(IBaseUserService.class);
        } catch (Exception e) {
            logger.warn("get userIdService error", e);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        /*设置mybatis的逻辑删除的标志位*/
        try {
            LOGIC_DELETE = environment.getProperty("mybatis.logic-delete",Boolean.class,true);
        } catch (Exception e) {
            LOGIC_DELETE = false;
            logger.warn("get logic-delete flag error ", e);
        }
    }

    /**
     * 获取用户ID
     *
     * @return
     */
    public static String getUserId() {
        return baseUserService != null ? baseUserService.getUserId() : "";
    }

    /**
     * 是否是逻辑删除
     *
     * @return
     */
    public static boolean isLogicDelete() {
        return LOGIC_DELETE;
    }

    /**
     * 获取实例对应的Service
     *
     * @param entityClass
     * @return
     */
    public static BaseService getBaseServiceByEntityClass(Class<?> entityClass) {
        Map<String, BaseService> serviceMap = applicationContext.getBeansOfType(BaseService.class);
        for (BaseService baseService : serviceMap.values()) {
            if (entityClass.equals(baseService.getEntityClass())) {
                return baseService;
            }
        }
        return null;
    }
}
