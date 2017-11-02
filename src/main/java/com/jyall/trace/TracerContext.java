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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * trace日志追踪的上下文
 * <p>
 * 使用lazy，延迟加载
 *
 * @author zhao.weiwei
 * Created on 2017/10/31 18:23
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Lazy
@Component
public class TracerContext {

    public static final String MERCHANT_CODE = "merchantCode";
    public static final String VERSION = "version";
    public static final String APP_ID = "appid";
    public static final String TOKEN_ID = "tokenId";

    @Autowired
    private Tracer tracer;

    /**
     * 获取商户code
     *
     * @return
     */
    public String getMerchantCode() {
        return tracer.getCurrentSpan().tags().get(MERCHANT_CODE);
    }

    /**
     * 获取版本号
     *
     * @return
     */
    public String getVersion() {
        return tracer.getCurrentSpan().tags().get(VERSION);
    }

    /**
     * 获取appid
     *
     * @return
     */
    public String getAppid() {
        return tracer.getCurrentSpan().tags().get(APP_ID);
    }

    /**
     * 获取tokenID
     *
     * @return
     */
    public String getTokenId() {
        return tracer.getCurrentSpan().tags().get(TOKEN_ID);
    }

    /**
     * 获取trace的tag
     *
     * @param tagName
     * @return
     */
    public String getTag(String tagName) {
        return tracer.getCurrentSpan().tags().get(tagName);
    }
}
