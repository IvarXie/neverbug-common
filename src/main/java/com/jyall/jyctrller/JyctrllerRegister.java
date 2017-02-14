package com.jyall.jyctrller;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.JyctrllerDiscoveryClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

/**
 * C层注册的bean
 * Created by zhao.weiwei
 * create on 2017/2/14 9:45
 * the email is zhao.weiwei@jyall.com.
 */
@Lazy
@Component
@Singleton
public class JyctrllerRegister {
    @Autowired
    private EurekaClientConfigBean config;
    @Autowired
    private ApplicationInfoManager applicationInfoManager;

    // 是否需要注册Controller到Eureka
    @Value("${eureka.client.jyctrller.registered:false}")
    private boolean shouldRegCtrller;

    // Controller注册到的Eureka URL列表
    @Value("${eureka.client.jyctrller.registryUrls:}")
    private String ctrllerRegistryUrls;


    public void register() throws Exception {
        if (shouldRegCtrller) {
            EurekaClientConfigBean bean = new EurekaClientConfigBean();
            BeanUtils.copyProperties(config, bean);
            bean.getServiceUrl().put("defaultZone", ctrllerRegistryUrls);
            bean.setRegisterWithEureka(true);
            new JyctrllerDiscoveryClient(applicationInfoManager, bean);
        }
    }
}
