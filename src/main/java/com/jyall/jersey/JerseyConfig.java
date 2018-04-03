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
package com.jyall.jersey;

import com.jyall.annotation.EnableJersey;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;
import java.util.UUID;

/**
 * jersey的自动加载
 *
 * @author zhao.weiwei
 * Created on 2017/10/30 17:05
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Configuration
@ConditionalOnBean(annotation = EnableJersey.class)
public class JerseyConfig {

    /**
     * 用户可以自定义ResourceConfig
     *
     * @return
     */
    @ConditionalOnMissingBean
    @Bean(name = "defaultJerseyResourceConfig")
    public ResourceConfig resourceConfig() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.makeClass(getClass().getPackage() + "." + UUID.randomUUID().toString().replaceAll("-", "").toUpperCase());
        CtClass superClass = pool.get(ResourceConfig.class.getName());
        cc.setSuperclass(superClass);
        ClassFile ccFile = cc.getClassFile();
        ConstPool constPool = ccFile.getConstPool();
        AnnotationsAttribute annotations = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(ApplicationPath.class.getName(), constPool);
        annotation.addMemberValue("value", new StringMemberValue("/v1", constPool));
        annotations.addAnnotation(annotation);
        ccFile.addAttribute(annotations);
        return ResourceConfig.class.cast(cc.toClass().newInstance());
    }
}
