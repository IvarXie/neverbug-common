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
package com.jyall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 家园网环境配置
 * <p>
 * Created by zhao.weiwei
 * Created on 2017/8/1 9:11
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Component
public class JyallEnvConfig {

    @Value("${spring.cloud.config.label:dev}")
    private String env;

    /**
     * 是否是线上环境
     *
     * @return
     */
    public boolean isOnline() {
        return "online".equalsIgnoreCase(env);
    }

    /**
     * 是否是开发环境
     *
     * @return
     */
    public boolean isDev() {
        return "dev".equalsIgnoreCase(env);
    }

    /**
     * 是否是测试环境
     *
     * @return
     */
    public boolean isTest() {
        return "test".equalsIgnoreCase(env);
    }

    /**
     * 是否是预发布环境
     *
     * @return
     */
    public boolean isPreonline() {
        return "preonline".equalsIgnoreCase(env);
    }

    /**
     * 是否是性能环境
     *
     * @return
     */
    public boolean isPerformance() {
        return "performance".equalsIgnoreCase(env);
    }
}
