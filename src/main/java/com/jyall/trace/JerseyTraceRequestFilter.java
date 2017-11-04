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
package com.jyall.trace;

import com.jyall.annotation.EnableJersey;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * jerseyRequest的trace添加到currentTag
 * <p>
 * 可以动态配置  trace.heaers属性，多个属性用逗号隔开
 *
 * @author zhao.weiwei
 * Created on 2017/10/31 16:43
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Configuration
@ConditionalOnBean(annotation = EnableJersey.class)
public class JerseyTraceRequestFilter implements ContainerRequestFilter {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private TraceProperty traceProperty;

    @Autowired
    private Tracer tracer;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            logger.info("\nthe jersey request method is {},url is {}", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri().toString());
            logger.debug("requestHeader is {}", requestContext.getHeaders());
            logger.debug("requestCookie is {}", requestContext.getCookies());
        } catch (Exception e) {
        }
        logger.debug("JerseyTraceRequestFilter add trace tag");
        Set<String> set = traceProperty.getHeaders();
        MultivaluedMap<String, String> map = requestContext.getHeaders();
        map.keySet().stream().filter(set::contains).forEach(k -> {
            if (CollectionUtils.isNotEmpty(map.get(k))) {
                String value = map.get(k).get(0);
                logger.debug("add tag [{}={}]", k, value);
                tracer.getCurrentSpan().tag(k, value);
                set.remove(k);
            }
        });
        Collection<String> collection = requestContext.getPropertyNames();
        collection.stream().filter(set::contains).forEach(k -> {
            String value = String.valueOf(requestContext.getProperty(k));
            logger.debug("add tag [{}={}]", k, value);
            tracer.getCurrentSpan().tag(k, value);
            set.remove(k);
        });
        logger.debug("JerseyTraceRequestFilter add trace tag success");
    }
}
