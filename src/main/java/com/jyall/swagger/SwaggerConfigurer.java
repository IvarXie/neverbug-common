/* =======================================================
 * 金色家网络科技有限公司-技术中心
 * 日 期：2016-4-1 9:37
 * 作 者：li.jianqiu
 * 版 本：0.0.1
 * 描 述：TODO
 * ========================================================
 */
package com.jyall.swagger;
/**
 * Created by li.jianqiu on 2016-4-1.
 */

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jersey.JerseyApiReader;
import com.wordnik.swagger.model.ApiInfo;
import com.wordnik.swagger.reader.ClassReaders;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @ClassName: SwaggerConfigurer </p>
 * @Author: li.jianqiu</p>
 * @Date: 2016-4-1 9:37 </p>
 * @Version: 0.0.1</p>
 * @Since: JDK 1.8</p>
 * @See: TODO</p>
 */
@Configuration
public class SwaggerConfigurer extends WebMvcConfigurerAdapter {
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**").addResourceLocations("classpath:/static/","classpath:/templates/","classpath:/META-INF/resources/webjars/");
		super.addResourceHandlers(registry);
	}

	public static void initSwagger(String title, String description) {
		SwaggerConfig config = ConfigFactory.config();
		config.setBasePath("/v1");
		config.setApiVersion("1.0.0");
		config.setApiInfo(new ApiInfo(
				title,
				"<a href=\"/api\">" + description + "</a>",
				null,
				null,
				null,
				null));
		ScannerFactory.setScanner(new DefaultJaxrsScanner());
		ClassReaders.setReader(new JerseyApiReader());
	}
}
