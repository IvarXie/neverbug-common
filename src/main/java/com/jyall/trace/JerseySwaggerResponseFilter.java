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
import com.wordnik.swagger.model.*;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import scala.Option;
import scala.Some;
import scala.collection.immutable.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
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
@ConditionalOnBean(annotation = EnableJersey.class)
public class JerseySwaggerResponseFilter implements ContainerResponseFilter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TraceProperty traceProperty;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        //获取请求的URL，一般是  api-docs/T，T是配置的swagger的PATH
        String url = requestContext.getUriInfo().getPath();
        Set<String> headers = traceProperty.getHeaders();
        //映射路径api-docs/  和 ApiListing 的 返回实体 ，还有 header的长度大于0
        if (url.contains("api-docs/") && (responseContext.getEntity() instanceof ApiListing) && headers.size() > 0) {
            logger.info("current url is [{}]", url);
            logger.info("add the trace header param start", url);
            ApiListing apiListing = (ApiListing) responseContext.getEntity();
            Map<String, String> headerMap = traceProperty.getHeaderMap();
            try {
                int count = apiListing.apis().size();
                for (int i = 0; i < count; i++) {
                    ApiDescription description = apiListing.apis().apply(i);
                    int operations = description.operations().size();
                    for (int j = 0; j < operations; j++) {
                        Operation operation = description.operations().apply(j);
                        List<Parameter> list = operation.parameters();
                        //构建parameters数组，在原来的长度上加上header的长度
                        Parameter[] parameters = new Parameter[list.size() + headers.size()];
                        for (int t = 0; t < list.size(); t++) {
                            parameters[t] = list.apply(t);
                        }
                        int headerIndexStart = list.size();
                        for (String header : headers) {
                            logger.info("add the header param is {}", header);
                            //参数的描述
                            Option<String> desc = new Some<>(header);
                            //参数的默认值
                            Option<String> defaultValue = new Some<>(headerMap.getOrDefault(header, ""));
                            Option<String> paramAccess = new Some<>("");
                            //swagger的选值范围
                            AllowableValues allowableValues = AnyAllowableValues$.MODULE$;
                            //添加的参数
                            Parameter parameter = new Parameter(header, desc, defaultValue, false, false, "string", allowableValues, "header", paramAccess);
                            //在原有的parameter的基础上添加参数
                            parameters[headerIndexStart++] = parameter;
                            logger.info("add the header param {} success", header);
                        }
                        //重新构建参数。由于是scala的，List的用法比较怪异
                        List<Parameter> pList = List.fromArray(parameters);
                        //使用反射获取parameters的Field，此处最好使用ReflectionUtils的，不要使用原生的反射
                        Set<Field> set = ReflectionUtils.getFields(Operation.class, ReflectionUtils.withName("parameters"));
                        set.forEach(field -> {
                            try {
                                field.setAccessible(true);
                                //由于没有原声的方法，只能使用反射来替换parameters参数
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
            logger.info("add the trace header param start success", url);
        }
    }
}
