package com.jyall.jyctrller;

import com.netflix.appinfo.ApplicationInfoManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

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
    @Value("${eureka.client.jyctrller.registered:false}")
    private boolean shouldRegCtrller;
    @Value("${eureka.client.jyctrller.registryUrls:}")
    private String ctrllerRegistryUrls;
    @Autowired
    private ApplicationContext applicationContext;

    public void register() throws Exception {
        if (this.shouldRegCtrller) {
            EurekaClientConfigBean bean = new EurekaClientConfigBean();
            BeanUtils.copyProperties(this.config, bean);
            Map<String, String> serviceUrl = new HashMap<>();
            serviceUrl.put("defaultZone", this.ctrllerRegistryUrls);
            bean.setServiceUrl(serviceUrl);
            bean.setRegisterWithEureka(true);
            bean.setFetchRegistry(false);
            new CloudEurekaClient(this.applicationInfoManager, bean, null, applicationContext);
        }
    }
}
