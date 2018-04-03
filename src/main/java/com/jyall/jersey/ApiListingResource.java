/********************************************************
 ***                     _ooOoo_                       ***
 ***                    o8888888o                      ***
 ***                    88" . "88                      ***
 ***                    (| -_- |)                      ***
 ***                    O\  =  /O                      ***
 ***                 ____/`---'\____                   ***
 ***               .'  \\|     |//  `.                 ***
 ***              /  \\|||  :  |||//  \                ***
 ***             /  _||||| -:- |||||-  \               ***
 ***             |   | \\\  -  /// |   |               ***
 ***             | \_|  ''\---/''  |   |               ***
 ***             \  .-\__  `-`  ___/-. /               ***
 ***           ___`. .'  /--.--\  `. . __              ***
 ***        ."" '<  `.___\_<|>_/___.'  >'"".           ***
 ***       | | :  `- \`.;`\ _ /`;.`/ - ` : | |         ***
 ***       \  \ `-.   \_ __\ /__ _/   .-` /  /         ***
 ***  ======`-.____`-.___\_____/___.-`____.-'======    ***
 ***                     `=---='                       ***
 ***   .............................................   ***
 ***         佛祖镇楼                  BUG辟易         ***
 ***   佛曰:                                           ***
 ***           写字楼里写字间，写字间里程序员；        ***
 ***           程序人员写程序，又拿程序换酒钱。        ***
 ***           酒醒只在网上坐，酒醉还来网下眠；        ***
 ***           酒醉酒醒日复日，网上网下年复年。        ***
 ***           但愿老死电脑间，不愿鞠躬老板前；        ***
 ***           奔驰宝马贵者趣，公交自行程序员。        ***
 ***           别人笑我忒疯癫，我笑自己命太贱；        ***
 ***           不见满街漂亮妹，哪个归得程序员？        ***
 *********************************************************
 ***               江城子 . 程序员之歌                 ***
 ***           十年生死两茫茫，写程序，到天亮。        ***
 ***               千行代码，Bug何处藏。               ***
 ***           纵使上线又怎样，朝令改，夕断肠。        ***
 ***           领导每天新想法，天天改，日日忙。        ***
 ***               相顾无言，惟有泪千行。              ***
 ***           每晚灯火阑珊处，夜难寐，加班狂。        ***
 *********************************************************
 ***      .--,       .--,                              ***
 ***      ( (  \.---./  ) )                            ***
 ***       '.__/o   o\__.'                             ***
 ***          {=  ^  =}              高山仰止,         ***
 ***           >  -  <               景行行止.         ***
 ***          /       \              虽不能至,         ***
 ***         //       \\             心向往之。        ***
 ***        //|   .   |\\                              ***
 ***        "'\       /'"_.-~^`'-.                     ***
 ***           \  _  /--'         `                    ***
 ***         ___)( )(___                               ***
 ***        (((__) (__)))                              ***
 ********************************************************/
package com.jyall.jersey;

import com.google.common.collect.Sets;
import com.jyall.trace.TraceProperty;
import com.jyall.util.ReflectUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.model.*;
import org.glassfish.jersey.servlet.WebConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Some;
import scala.collection.immutable.List;
import scala.reflect.ScalaSignature;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.Set;

/**
 * 统一添加swagger-header的
 *
 * @author zhao.weiwei
 * Created on 2018/4/3 10:40
 * Email is zhao.weiwei@jyall.com
 * Copyright is 家园云网络科技有限公司
 */
@Component
@Path("/api-docs")
@Api("/api-docs")
@Produces({"application/json"})
@ScalaSignature(bytes = "\u0006\u0001I2A!\u0001\u0002\u0001\u001b\t1\u0012\t]5MSN$\u0018N\\4SKN|WO]2f\u0015N{eJ\u0003\u0002\u0004\t\u00059A.[:uS:<'BA\u0003\u0007\u0003\u0019QWM]:fs*\u0011q\u0001C\u0001\bg^\fwmZ3s\u0015\tI!\"A\u0004x_J$g.[6\u000b\u0003-\t1aY8n\u0007\u0001\u0019\"\u0001\u0001\b\u0011\u0005=\u0001R\"\u0001\u0002\n\u0005E\u0011!AE!qS2K7\u000f^5oOJ+7o\\;sG\u0016DQa\u0005\u0001\u0005\u0002Q\ta\u0001P5oSRtD#A\u000b\u0011\u0005=\u0001\u0001\u0006\u0002\u0001\u0018C\t\u0002\"\u0001G\u0010\u000e\u0003eQ!AG\u000e\u0002\u0005I\u001c(B\u0001\u000f\u001e\u0003\t98OC\u0001\u001f\u0003\u0015Q\u0017M^1y\u0013\t\u0001\u0013D\u0001\u0005Qe>$WoY3t\u0003\u00151\u0018\r\\;fY\u0005\u0019\u0013%\u0001\u0013\u0002!\u0005\u0004\b\u000f\\5dCRLwN\\\u0018kg>t\u0007\u0006\u0002\u0001'C1\u0002\"a\n\u0016\u000e\u0003!R!!\u000b\u0004\u0002\u0017\u0005tgn\u001c;bi&|gn]\u0005\u0003W!\u00121!\u00119jC\u0005i\u0013!C\u0018ba&lCm\\2tQ\u0011\u0001q&\t\u0017\u0011\u0005a\u0001\u0014BA\u0019\u001a\u0005\u0011\u0001\u0016\r\u001e5")
public class ApiListingResource extends com.wordnik.swagger.jersey.listing.ApiListingResource {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TraceProperty traceProperty;

    @Override
    public Response apiDeclaration(String route, Application app, WebConfig wc, HttpHeaders httpHeaders, UriInfo uriInfo) {/*自定义的header参数*/
        Response response = super.apiDeclaration(route, app, wc, httpHeaders, uriInfo);
        ApiListing apiListing = (ApiListing) response.getEntity();
        Set<String> headers = traceProperty.getHeaders();
        if (headers.size() > 0) {
            Map<String, String> headerMap = traceProperty.getHeaderMap();
            int count = apiListing.apis().size();
            for (int i = 0; i < count; i++) {
                ApiDescription description = apiListing.apis().apply(i);
                int operations = description.operations().size();
                for (int j = 0; j < operations; j++) {
                    Operation operation = description.operations().apply(j);
                    List<Parameter> list = operation.parameters();
                    /*删除已有的header*/
                    headers.removeAll(currentHeaderParams(list));
                    logger.info("the method {} add the header is {}", operation.method(), headers);
                    /* 添加自定义的header参数 */
                    for (String header : headers) {
                        list = list.$colon$colon(new Parameter(header, new Some<>(header), new Some<>(headerMap.getOrDefault(header, "")), false, false, "string", AnyAllowableValues$.MODULE$, "header", new Some<>("")));
                    }
                    try {/*使用反射获取parameters的Field*/
                        ReflectUtils.setField(operation, "parameters", list);
                    } catch (Exception e) {
                        logger.error("assemeble add param error", e);
                    }
                }
            }
        }
        return response;
    }

    /**
     * 获取已经有的header 参数
     *
     * @param list
     * @return
     */
    private Set<String> currentHeaderParams(List<Parameter> list) {
        Set<String> headers = Sets.newHashSet();
        for (int t = 0; t < list.size(); t++) {
            Parameter parameter = list.apply(t);
            if ("header".equals(parameter.paramType())) {
                headers.add(parameter.name());
            }
        }
        return headers;
    }
}
