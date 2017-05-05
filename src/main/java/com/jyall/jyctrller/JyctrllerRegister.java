package com.jyall.jyctrller;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;


/**
 * C层注册的bean
 * <P>
 * 使用@Singleton单态注解
 * <P>
 * 使用@Lazy,延时加载 Created by zhao.weiwei create on 2017/2/14 9:45 the email is
 * zhao.weiwei@jyall.com.
 */
@Lazy
@Component
@Singleton
public class JyctrllerRegister {

	// 注入原有的服务注册与发现的配置
	@Autowired
	private EurekaClientConfigBean config;
	// 注入原有的服务注管理器
	@Autowired
	private ApplicationInfoManager applicationInfoManager;
	// 是否注册到C层的标志，默认是不注册
	@Value("${eureka.client.jyctrller.registered:false}")
	private boolean shouldRegCtrller;
	// C层注册的地址
	@Value("${eureka.client.jyctrller.registryUrls:}")
	private String ctrllerRegistryUrls;
	// spring的上下文
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private DiscoveryClient discoveryClient;

	private CloudEurekaClient cloudEurekaClient;

	public void register() throws Exception {
		if (this.shouldRegCtrller) {
			EurekaClientConfigBean bean = new EurekaClientConfigBean();
			// 将原来的服务发现的属性copy到属性
			BeanUtils.copyProperties(this.config, bean);
			Map<String, String> serviceUrl = new HashMap<>();
			// 将defaultZone的属性换成C层注册的URL
			serviceUrl.put("defaultZone", this.ctrllerRegistryUrls);
			bean.setServiceUrl(serviceUrl);
			// 设置注册属性
			bean.setRegisterWithEureka(true);
			// 设置拉取服务的属性
			bean.setFetchRegistry(true);
			cloudEurekaClient = new CloudEurekaClient(this.applicationInfoManager, bean, null, applicationContext);
		}
	}
	/**
	 * 获取服务列表
	 * @param serviceId
	 * @return
	 * @throws Exception
	 */
	public List<ServiceInstance> getInstances(String serviceId) throws Exception {
		List<ServiceInstance> list = discoveryClient.getInstances(serviceId);
		list.addAll(getSerivice(serviceId));
		return list;
	}

	private List<ServiceInstance> getSerivice(String serviceId) throws Exception {
		List<InstanceInfo> infos = this.cloudEurekaClient.getInstancesByVipAddress(serviceId, false);
		List<ServiceInstance> instances = new ArrayList<>();
		for (InstanceInfo info : infos) {
			Constructor<?> con = EurekaServiceInstance.class.getDeclaredConstructor(InstanceInfo.class);
			con.setAccessible(true);
			instances.add(EurekaServiceInstance.class.cast(con.newInstance((info))));
		}
		return instances;
	}

}
