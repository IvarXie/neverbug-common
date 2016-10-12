package com.jyall.feign.jaxrs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jyall.feign.jaxrs.JAXRSContract;

import feign.Contract;

/**
 * 覆盖Spring Cloud Feign Client的默认配置
 * 
 * @author guo.guanfei
 * 
 */
@Configuration
public class JyallFeignClientConfig {

	// 使用JAX-RS 1.1 注解格式
	@Bean
	public Contract feignContract() {
		return new JAXRSContract();
	}
}
