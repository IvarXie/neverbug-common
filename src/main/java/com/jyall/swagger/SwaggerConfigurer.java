/* =======================================================
 * 金色家网络科技有限公司-技术中心
 * 日 期：2016-4-1 9:37
 * 作 者：li.jianqiu
 * 版 本：0.0.1
 * 描 述：TODO
 * ========================================================
 */
package com.jyall.swagger;

import com.google.common.collect.Lists;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * swagger的配置类
 *
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

    @Value("${spring.application.name:swagger}")
    private String applicationName;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/", "classpath:/templates/", "classpath:/META-INF/resources/webjars/");
        super.addResourceHandlers(registry);
    }

    @PostConstruct
    public void initSwagger() {
        String title = swaggerProperty.getTitle();
        title = StringUtils.isNotBlank(title) ? title : applicationName;
        String description = swaggerProperty.getDescription();
        SwaggerConfig config = ConfigFactory.config();
        config.setBasePath("/v1");
        config.setApiVersion("1.0.0");
        description = StringUtils.isNotBlank(description) ? description : title;
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
        List<String> list = Lists.newArrayList("/swagger");
        if (swaggerProperty.isEnableRoot2swagger()) {
            list.add("/");
        }
        return new ServletRegistrationBean(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.sendRedirect("/swagger/index.html");
            }
        }, list.toArray(new String[list.size()]));
    }
}
