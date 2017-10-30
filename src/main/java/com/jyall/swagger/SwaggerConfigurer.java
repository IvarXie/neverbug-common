/* =======================================================
 * 金色家网络科技有限公司-技术中心
 * 日 期：2016-4-1 9:37
 * 作 者：li.jianqiu
 * 版 本：0.0.1
 * 描 述：TODO
 * ========================================================
 */
package com.jyall.swagger;

import com.jyall.annotation.EnableSwagger;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jersey.JerseyApiReader;
import com.wordnik.swagger.model.ApiInfo;
import com.wordnik.swagger.reader.ClassReaders;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;

/**
 * swagger的配置类
 * @author: zhao.weiwei</p>
 * @date: 2017-10-30 9:37 </p>
 * @version: 0.0.1</p>
 * @Since: JDK 1.8</p>
 */
@Configuration
@ConditionalOnBean(annotation = EnableSwagger.class)
@EnableConfigurationProperties(SwaggerProperty.class)
public class SwaggerConfigurer extends WebMvcConfigurerAdapter {

    @Autowired
    private SwaggerProperty swaggerProperty;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/", "classpath:/templates/", "classpath:/META-INF/resources/webjars/");
        super.addResourceHandlers(registry);
    }

    @PostConstruct
    public void initSwagger() {
        String title = swaggerProperty.getTitle();
        String description = swaggerProperty.getDescription();
        SwaggerConfig config = ConfigFactory.config();
        config.setBasePath("/v1");
        config.setApiVersion("1.0.0");
        if (StringUtils.isBlank(description)) {
            description = title;
        }
        config.setApiInfo(new ApiInfo(
                title,
                "<a href=\"/api\" target = \"_blank\">" + description + "</a>",
                null,
                null,
                null,
                null));
        ScannerFactory.setScanner(new DefaultJaxrsScanner());
        ClassReaders.setReader(new JerseyApiReader());
    }

    @Bean("swaggerServlet")
    public ServletRegistrationBean swagger() {
        return new ServletRegistrationBean(new SwaggerServlet(), "/swagger");
    }

    @Bean("rootSwaggerServlet")
    @ConditionalOnProperty(name = "spring.swagger.enableRoot2swagger", havingValue = "true")
    public ServletRegistrationBean rootSwagger() {
        return new ServletRegistrationBean(new SwaggerServlet(), "/");
    }
}
