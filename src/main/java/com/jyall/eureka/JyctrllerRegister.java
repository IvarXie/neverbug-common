package com.jyall.eureka;

import com.google.common.collect.Maps;
import com.netflix.appinfo.ApplicationInfoManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * C层注册的bean
 * 使用@Singleton单态注解
 * 在 eureka.client.jyctrller.registered 为true的时候才会加载这个类
 * 并且只在 Linux上注册
 * Created by zhao.weiwei
 * create on 2017/2/14 9:45
 * the email is zhao.weiwei@jyall.com.
 */
@Component
@Order(100)
@ConditionalOnBean(annotation = EnableDiscoveryClient.class)
@ConditionalOnExpression("'${os.name}'=='Linux' && '${eureka.client.jyctrller.registered}'=='true'")
public class JyctrllerRegister implements CommandLineRunner {

    /**
     * 注入原有的服务注册与发现的配置
     **/
    @Autowired
    private EurekaClientConfigBean config;
    /**
     * 注入原有的服务注管理器
     **/
    @Autowired
    private ApplicationInfoManager applicationInfoManager;
    /**
     * C层注册的地址
     **/
    @Value("${eureka.client.jyctrller.registryUrls:}")
    private String ctrllerRegistryUrls;
    /**
     * spring的上下文
     **/
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        EurekaClientConfigBean bean = new EurekaClientConfigBean();
        /* 将原来的服务发现的属性copy到属性 */
        BeanUtils.copyProperties(this.config, bean);
        Map<String, String> serviceUrl = Maps.newHashMap();
        /* 将defaultZone的属性换成C层注册的URL */
        serviceUrl.put("defaultZone", this.ctrllerRegistryUrls);
        bean.setServiceUrl(serviceUrl);
        /* 设置注册属性 */
        bean.setRegisterWithEureka(true);
        /* 设置拉取服务的属性 */
        bean.setFetchRegistry(false);
        new CloudEurekaClient(this.applicationInfoManager, bean, applicationContext);
    }
}
