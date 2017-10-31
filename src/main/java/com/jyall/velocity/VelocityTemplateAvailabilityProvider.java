/*
                            _ooOoo_  
                           o8888888o  
                           88" . "88  
                           (| -_- |)  
                            O\ = /O  
                        ____/`---'\____  
                      .   ' \\| |// `.  
                       / \\||| : |||// \  
                     / _||||| -:- |||||- \  
                       | | \\\ - /// | |  
                     | \_| ''\---/'' | |  
                      \ .-\__ `-` ___/-. /  
                   ___`. .' /--.--\ `. . __  
                ."" '< `.___\_<|>_/___.' >'"".  
               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
                 \ \ `-. \_ __\ /__ _/ .-` / /  
         ======`-.____`-.___\_____/___.-`____.-'======  
                            `=---='  
  
         .............................................  
                  佛祖镇楼                  BUG辟易  
          佛曰:  
                  写字楼里写字间，写字间里程序员；  
                  程序人员写程序，又拿程序换酒钱。  
                  酒醒只在网上坐，酒醉还来网下眠；  
                  酒醉酒醒日复日，网上网下年复年。  
                  但愿老死电脑间，不愿鞠躬老板前；  
                  奔驰宝马贵者趣，公交自行程序员。  
                  别人笑我忒疯癫，我笑自己命太贱；  
                  不见满街漂亮妹，哪个归得程序员？
*/
package com.jyall.velocity;


import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * Velocity view templates.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class VelocityTemplateAvailabilityProvider
        implements TemplateAvailabilityProvider {

    @Override
    public boolean isTemplateAvailable(String view, Environment environment,
                                       ClassLoader classLoader, ResourceLoader resourceLoader) {
        if (ClassUtils.isPresent("org.apache.velocity.app.VelocityEngine", classLoader)) {
            PropertyResolver resolver = new RelaxedPropertyResolver(environment,
                    "spring.velocity.");
            String loaderPath = resolver.getProperty("resource-loader-path",
                    VelocityProperties.DEFAULT_RESOURCE_LOADER_PATH);
            String prefix = resolver.getProperty("prefix",
                    VelocityProperties.DEFAULT_PREFIX);
            String suffix = resolver.getProperty("suffix",
                    VelocityProperties.DEFAULT_SUFFIX);
            return resourceLoader.getResource(loaderPath + prefix + view + suffix)
                    .exists();
        }
        return false;
    }

}
