package com.jyall.feign.jaxrs;

import com.jyall.annotation.EnableJersey;
import feign.Contract;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 覆盖Spring Cloud Feign Client的默认配置
 *
 * @author guo.guanfei
 */
@Configuration
@ConditionalOnBean(annotation = EnableJersey.class)
public class JyallFeignClientConfig {

    @Bean
    public Contract feignContract() {
        return new JaxrsContract();
    }
}
