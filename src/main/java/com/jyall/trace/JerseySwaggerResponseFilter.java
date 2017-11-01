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

import com.wordnik.swagger.model.*;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Option;
import scala.Some;
import scala.collection.immutable.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * swagger 参数的根据trace动态添加
 * <p>
 * 可以动态配置  trace.heaers属性，多个属性用逗号隔开
 *
 * @author zhao.weiwei
 * Created on 2017/10/31 19:35
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Component
public class JerseySwaggerResponseFilter implements ContainerResponseFilter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TraceProperty traceProperty;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String url = requestContext.getUriInfo().getPath();
        if (url.contains("api-docs/") && (responseContext.getEntity() instanceof ApiListing)) {
            ApiListing apiListing = (ApiListing) responseContext.getEntity();
            try {
                int count = apiListing.apis().size();
                for (int i = 0; i < count; i++) {
                    ApiDescription description = apiListing.apis().apply(i);
                    int operations = description.operations().size();
                    for (int j = 0; j < operations; j++) {
                        Set<String> headers = traceProperty.getHeaders();
                        Operation operation = description.operations().apply(j);
                        List<Parameter> list = operation.parameters();
                        Parameter[] parameters = new Parameter[list.size() + headers.size()];
                        for (int t = 0; t < list.size(); t++) {
                            parameters[t] = list.apply(t);
                        }
                        int headerIndexStart = list.size();
                        for (String header : headers) {
                            Option<String> desc = new Some<>(header);
                            Option<String> defaultValue = new Some<>("wolfking");
                            Option<String> paramAccess = new Some<>("");
                            AllowableValues allowableValues = AnyAllowableValues$.MODULE$;
                            Parameter parameter = new Parameter(header, desc, defaultValue, false, false, "string", allowableValues, "header", paramAccess);
                            parameters[headerIndexStart++] = parameter;
                        }
                        List<Parameter> pList = List.fromArray(parameters);
                        Set<Field> set = ReflectionUtils.getFields(Operation.class, ReflectionUtils.withName("parameters"));
                        set.forEach(field -> {
                            try {
                                field.setAccessible(true);
                                field.set(operation, pList);
                            } catch (Exception e) {
                                logger.error("assemeble add param error", e);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                logger.error("assemeble add param error", e);
            }
        }
    }
}
