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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * swagger 的属性配置属性
 * <p>
 *
 * @author zhao.weiwei
 * Created on 2017/10/30 16:29
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Configuration
@ConfigurationProperties(prefix = "trace")
public class TraceProperty {

    private String headers = "merchantCode,version,appid,tokenId";

    private String defaults = ",0.0.1,,";

    public Set<String> getHeaders() {
        if (StringUtils.isNotEmpty(headers)) {
            return Sets.newHashSet(headers.split(","));
        } else {
            return Sets.newHashSet();
        }
    }

    /**
     * 获取header 和 默认
     *
     * @return
     */
    public Map<String, String> getHeaderMap() {
        Map<String, String> map = Maps.newHashMap();
        String[] headerArray = headers.split(",");
        String[] defautValueArray = defaults.split(",");
        for (int i = 0; i < headers.length(); i++) {
            String value = defautValueArray.length > i ? defautValueArray[i] : "";
            map.put(headerArray[i], value);
        }
        return map;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public void setDefaults(String defaults) {
        this.defaults = defaults;
    }
}
