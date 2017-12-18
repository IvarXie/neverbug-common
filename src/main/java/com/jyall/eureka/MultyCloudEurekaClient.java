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

import com.google.common.collect.Lists;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多注册中心获取服务的实例
 * <p>
 *
 * @create by zhao.weiwei
 * @create on 2017年5月5日上午10:26:18
 * @email is zhao.weiwei@jyall.com.
 */
@Lazy
@Configuration
@ConditionalOnProperty(name = "spring.cloud.multy.eureka.client", havingValue = "true")
public class MultyCloudEurekaClient {

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
    private List<CloudEurekaClient> clientList = Lists.newArrayList();

    @Value("${eureka.client.jyctrller.registryUrls:}")
    private String controllerUrl;
    @Value("${eureka.client.serviceUrl.defaultZone:}")
    private String serviceUrl;

    @PostConstruct
    public void init() {
        init(new String[]{serviceUrl, controllerUrl});
    }

    /**
     * 代码里面显式初始化的方法
     *
     * @param registerUrls
     */
    public void init(String[] registerUrls) {
        shutdown();
        Arrays.stream(registerUrls).filter(StringUtils::isNotBlank).forEach(url -> {
            logger.info("init the {} cloudEurekaClient start", url);
            EurekaClientConfigBean bean = new EurekaClientConfigBean();
            // 将原来的服务发现的属性copy到属性
            BeanUtils.copyProperties(this.config, bean);
            Map<String, String> serviceUrl = new HashMap<>();
            // 将defaultZone的属性换成设置的URL
            serviceUrl.put("defaultZone", url);
            bean.setServiceUrl(serviceUrl);
            // 设置注册属性，为false，不然出现多个服务
            bean.setRegisterWithEureka(false);
            // 设置拉取服务的属性，必须设置true,不然获取不到服务实例
            bean.setFetchRegistry(true);
            CloudEurekaClient cloudEurekaClient = new CloudEurekaClient(this.applicationInfoManager, bean, applicationContext);
            clientList.add(cloudEurekaClient);
            logger.info("init the {} cloudEurekaClient success", url);
        });
    }

    /**
     * 获取服务列表
     *
     * @param serviceId
     * @return
     * @throws Exception
     */
    public List<ServiceInstance> getInstances(String serviceId){
        return clientList.stream().map(cloudEurekaClient->getSerivice(serviceId, cloudEurekaClient))
                .flatMap(List::stream).collect(Collectors.toList());
    }

    private List<ServiceInstance> getSerivice(String serviceId, CloudEurekaClient cloudEurekaClient) {
        return cloudEurekaClient.getInstancesByVipAddress(serviceId, false)
                .stream().map(MultyCloudEurekaClient::assemblyEurekaServiceInstance).collect(Collectors.toList());
    }

    private static ServiceInstance assemblyEurekaServiceInstance(InstanceInfo instanceInfo) {
        try {
            /* 获取私用的构造方法 */
            Constructor<EurekaServiceInstance> con = EurekaServiceInstance.class.getDeclaredConstructor(InstanceInfo.class);
            /* 设置构造方法可用 */
            con.setAccessible(true);
            return con.newInstance(instanceInfo);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * destory的前调用该方法
     */
    public void shutdown() {
        logger.info("destory the inited cloudEurekaClient,size is {}", clientList.size());
        clientList.forEach(CloudEurekaClient::shutdown);
        clientList.clear();
        logger.info("destory the inited cloudEurekaClient success");
    }
}
