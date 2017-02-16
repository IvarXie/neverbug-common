package com.netflix.discovery;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.LookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhao.weiwei
 * create on 2017/2/14 18:14
 * the email is zhao.weiwei@jyall.com.
 */
public class JyctrllerDiscoveryManager {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryManager.class);
    private JyctrllerDiscoveryClient discoveryClient;

    private EurekaInstanceConfig eurekaInstanceConfig;
    private EurekaClientConfig eurekaClientConfig;
    private static final JyctrllerDiscoveryManager s_instance = new JyctrllerDiscoveryManager();

    private JyctrllerDiscoveryManager() {
    }

    public static JyctrllerDiscoveryManager getInstance() {
        return s_instance;
    }

    public void setDiscoveryClient(JyctrllerDiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public void setEurekaClientConfig(EurekaClientConfig eurekaClientConfig) {
        this.eurekaClientConfig = eurekaClientConfig;
    }

    public void setEurekaInstanceConfig(EurekaInstanceConfig eurekaInstanceConfig) {
        this.eurekaInstanceConfig = eurekaInstanceConfig;
    }

    public void initComponent(EurekaInstanceConfig config,
                              EurekaClientConfig eurekaConfig, JyctrllerDiscoveryClient.DiscoveryClientOptionalArgs args) {
        this.eurekaInstanceConfig = config;
        this.eurekaClientConfig = eurekaConfig;
        if (ApplicationInfoManager.getInstance().getInfo() == null)
            ApplicationInfoManager.getInstance().initComponent(config);
        InstanceInfo info = ApplicationInfoManager.getInstance().getInfo();
        discoveryClient = new JyctrllerDiscoveryClient(info, eurekaConfig);
    }

    public void initComponent(EurekaInstanceConfig config, EurekaClientConfig eurekaConfig) {
        initComponent(config, eurekaConfig, null);
    }

    public void shutdownComponent() {
        if (discoveryClient != null) {
            try {
                discoveryClient.shutdown();
                discoveryClient = null;
            } catch (Throwable th) {
                logger.error("Error in shutting down client", th);
            }
        }
    }

    public LookupService getLookupService() {
        return discoveryClient;
    }

    public JyctrllerDiscoveryClient getDiscoveryClient() {
        return discoveryClient;
    }

    public EurekaClient getEurekaClient() {
        return discoveryClient;
    }

    public EurekaClientConfig getEurekaClientConfig() {
        return eurekaClientConfig;
    }

    public EurekaInstanceConfig getEurekaInstanceConfig() {
        return eurekaInstanceConfig;
    }
}
