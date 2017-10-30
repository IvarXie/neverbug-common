package com.jyall.jyctrller;

import com.google.common.collect.Maps;
import com.netflix.appinfo.ApplicationInfoManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.Map;

/**
 * C层注册的bean
 * <p>
 * 使用@Singleton单态注解
 * <p>
 * 使用@Lazy,延时加载 Created by zhao.weiwei create on 2017/2/14 9:45 the email is
 * zhao.weiwei@jyall.com.
 */
@Lazy
@Singleton
@Component
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
@ConditionalOnProperty(name = "eureka.client.jyctrller.registered", havingValue = "true")
public class JyctrllerRegister {

    // 注入原有的服务注册与发现的配置
    @Autowired
    private EurekaClientConfigBean config;
    // 注入原有的服务注管理器
    @Autowired
    private ApplicationInfoManager applicationInfoManager;
    // 是否注册到C层的标志，默认是不注册
//    @Value("${eureka.client.jyctrller.registered:false}")
//    private boolean shouldRegCtrller;
    // C层注册的地址
    @Value("${eureka.client.jyctrller.registryUrls:}")
    private String ctrllerRegistryUrls;
    // spring的上下文
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void register() throws Exception {
//        if (this.shouldRegCtrller) {
        EurekaClientConfigBean bean = new EurekaClientConfigBean();
        // 将原来的服务发现的属性copy到属性
        BeanUtils.copyProperties(this.config, bean);
        Map<String, String> serviceUrl = Maps.newHashMap();
        // 将defaultZone的属性换成C层注册的URL
        serviceUrl.put("defaultZone", this.ctrllerRegistryUrls);
        bean.setServiceUrl(serviceUrl);
        // 设置注册属性
        bean.setRegisterWithEureka(true);
        // 设置拉取服务的属性
        bean.setFetchRegistry(false);
        new CloudEurekaClient(this.applicationInfoManager, bean, applicationContext);
//        }
    }
}
