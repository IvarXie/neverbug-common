package com.jyall.feign.jaxrs;

import feign.Contract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 覆盖Spring Cloud Feign Client的默认配置
 *
 * @author guo.guanfei
 */
@Configuration
public class JyallFeignClientConfig {

    @Bean
    public Contract feignContract() {
        return new JaxrsContract();
    }
}
