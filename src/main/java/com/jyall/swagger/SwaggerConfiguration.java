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
import com.jyall.jersey.JerseyPathConfig;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jersey.JerseyApiReader;
import com.wordnik.swagger.model.ApiInfo;
import com.wordnik.swagger.reader.ClassReaders;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * swagger的配置类
 *
 * @author: zhao.weiwei
 * @date: 2017-10-30 9:37
 * @version: 0.0.1
 * @Since: JDK 1.8
 */
@Order(100)
@Configuration
@ConditionalOnBean(annotation = EnableSwagger.class)
@EnableConfigurationProperties(SwaggerProperty.class)
public class SwaggerConfiguration extends WebMvcConfigurerAdapter implements CommandLineRunner {

    @Autowired
    private SwaggerProperty swaggerProperty;

    @Value("${spring.application.name:swagger}")
    private String applicationName;

    @Autowired
    private JerseyPathConfig pathConfig;

    /**
     * 初始化swagger的配置
     */
    @Override
    public void run(String... args) {
        String title = swaggerProperty.getTitle();
        title = StringUtils.isNotBlank(title) ? title : applicationName;
        String description = swaggerProperty.getDescription();
        SwaggerConfig config = ConfigFactory.config();
        config.setBasePath(pathConfig.getApplicationPath());
        config.setApiVersion("1.0.0");
        description = StringUtils.isNotBlank(description) ? description : title;
        config.setApiInfo(new ApiInfo(
                title,
                "<a href=\"" + pathConfig.getApplicationPath() + "/api\" target = \"_blank\">" + description + "</a>",
                null,
                null,
                null,
                null));
        ScannerFactory.setScanner(new DefaultJaxrsScanner());
        ClassReaders.setReader(new JerseyApiReader());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/", "classpath:/templates/", "classpath:/META-INF/resources/webjars/");
        super.addResourceHandlers(registry);
    }
}
