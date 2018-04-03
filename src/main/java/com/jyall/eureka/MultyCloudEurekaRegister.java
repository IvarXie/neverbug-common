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
package com.jyall.eureka;

import com.google.common.collect.Maps;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.DiscoveryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 多注册中心获取服务的实例
 *
 * @create by zhao.weiwei
 * @create on 2017年5月5日上午10:26:18
 * @email is zhao.weiwei@jyall.com.
 */
@Component
@EnableConfigurationProperties(MultyCloudEurekaConfig.class)
@ConditionalOnProperty(name = "spring.cloud.multy.register.enabled", havingValue = "true")
public class MultyCloudEurekaRegister implements CommandLineRunner {

    /**
     * slf4j日志
     */
    private Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 注入原有的服务注册与发现的配置
     **/
    @Autowired
    private EurekaClientConfigBean config;
    /**
     * 注入原有的服务注管理器
     **/
    @Autowired
    private ApplicationInfoManager applicationInfoManager;
    /**
     * applicationContext 实例
     **/
    @Autowired
    private ApplicationContext applicationContext;
    /**
     * client 列表
     */
    private Map<String, CloudEurekaClient> cloudEurekaClientMap = Maps.newConcurrentMap();

    /**
     * 多注册中心的配置
     */
    @Autowired
    private MultyCloudEurekaConfig multyCloudEurekaConfig;


    @Override
    public void run(String... args) {
        logger.info("register the eureka service start");
        multyCloudEurekaConfig.getRegisters().forEach(this::initCloudEurekaClient);
        logger.info("register the eureka service success");
    }

    /**
     * 注册服务到eureka
     *
     * @param env
     */
    public boolean register(String env) {
        if (!cloudEurekaClientMap.containsValue(env)) {
            multyCloudEurekaConfig.getRegisters().stream()
                    .filter(r -> r.getEnv().equals(env))
                    .forEach(this::initCloudEurekaClient);
        }
        return true;
    }

    /**
     * 从eureka 不再注册
     *
     * @param env
     */
    public boolean unregister(String env) throws Exception {
        CloudEurekaClient cloudEurekaClient = cloudEurekaClientMap.get(env);
        if (cloudEurekaClient != null) {
            try {
                Method method = DiscoveryClient.class.getDeclaredMethod("unregister");
                method.setAccessible(true);
                method.invoke(cloudEurekaClient);
                logger.info("[{}] unregister {}", env, "success");
                method = DiscoveryClient.class.getDeclaredMethod("cancelScheduledTasks");
                method.setAccessible(true);
                method.invoke(cloudEurekaClient);
                cloudEurekaClientMap.remove(env);
                return true;
            } catch (Exception e) {
                logger.error("unregister error", e);
                throw e;
            }
        } else {
            logger.error("unregister [{}] env eureka client is not exists", env);
            return false;
        }
    }

    /**
     * 初始化 CloudEurekaClient
     *
     * @param eurekaRegister 注册的信息，主要是env 和 url属性
     */
    private void initCloudEurekaClient(EurekaRegister eurekaRegister) {
        logger.info("new [{}] eureka client,url is [{}]", eurekaRegister.getEnv(), eurekaRegister.getUrl());
        EurekaClientConfigBean bean = new EurekaClientConfigBean();
        /* 将原来的服务发现的属性copy到属性 */
        BeanUtils.copyProperties(this.config, bean);
        Map<String, String> serviceUrl = Maps.newHashMap();
        /* 将defaultZone的属性换成C层注册的URL */
        serviceUrl.put("defaultZone", eurekaRegister.getUrl());
        bean.setServiceUrl(serviceUrl);
        /* 设置注册属性 */
        bean.setRegisterWithEureka(true);
        /* 设置拉取服务的属性 */
        bean.setFetchRegistry(false);
        CloudEurekaClient cloudEurekaClient = new CloudEurekaClient(this.applicationInfoManager, bean, applicationContext);
        cloudEurekaClientMap.put(eurekaRegister.getEnv(), cloudEurekaClient);
        logger.info("new [{}] eureka client success", eurekaRegister.getEnv());
    }
}
