package com.jyall.jyctrller;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.LookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhao.weiwei
 * create on 2017/2/14 18:14
 * the email is zhao.weiwei@jyall.com.
 */
public class JyallCDiscoveryManager {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryManager.class);
    private JyallCDiscoveryClient discoveryClient;

    private EurekaInstanceConfig eurekaInstanceConfig;
    private EurekaClientConfig eurekaClientConfig;
    private static final JyallCDiscoveryManager s_instance = new JyallCDiscoveryManager();

    private JyallCDiscoveryManager() {
    }

    public static JyallCDiscoveryManager getInstance() {
        return s_instance;
    }

    public void setDiscoveryClient(JyallCDiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public void setEurekaClientConfig(EurekaClientConfig eurekaClientConfig) {
        this.eurekaClientConfig = eurekaClientConfig;
    }

    public void setEurekaInstanceConfig(EurekaInstanceConfig eurekaInstanceConfig) {
        this.eurekaInstanceConfig = eurekaInstanceConfig;
    }

    /**
     * Initializes the <tt>Discovery Client</tt> with the given configuration.
     *
     * @param config
     *            the instance info configuration that will be used for
     *            registration with Eureka.
     * @param eurekaConfig the eureka client configuration of the instance.
     */
    public void initComponent(EurekaInstanceConfig config,
                              EurekaClientConfig eurekaConfig, JyallCDiscoveryClient.DiscoveryClientOptionalArgs args) {
        this.eurekaInstanceConfig = config;
        this.eurekaClientConfig = eurekaConfig;
        if (ApplicationInfoManager.getInstance().getInfo() == null) {
            // Initialize application info
            ApplicationInfoManager.getInstance().initComponent(config);
        }
        InstanceInfo info = ApplicationInfoManager.getInstance().getInfo();
//        discoveryClient = new JyallCDiscoveryClient(info, eurekaConfig, args);
        discoveryClient = new JyallCDiscoveryClient(info,eurekaConfig);
    }

    public void initComponent(EurekaInstanceConfig config,
                              EurekaClientConfig eurekaConfig) {
        initComponent(config, eurekaConfig, null);
    }

    /**
     * Shuts down the <tt>Discovery Client</tt> which unregisters the
     * information about this instance from the <tt>Discovery Server</tt>.
     */
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

    /**
     * @deprecated use {@link #getEurekaClient()}
     *
     * Get the {@link DiscoveryClient}.
     * @return the client that is used to talk to eureka.
     */
    @Deprecated
    public JyallCDiscoveryClient getDiscoveryClient() {
        return discoveryClient;
    }

    /**
     *
     * Get the {@link EurekaClient} implementation.
     * @return the client that is used to talk to eureka.
     */
    public EurekaClient getEurekaClient() {
        return discoveryClient;
    }

    /**
     * Get the instance of {@link EurekaClientConfig} this instance was initialized with.
     * @return the instance of {@link EurekaClientConfig} this instance was initialized with.
     */
    public EurekaClientConfig getEurekaClientConfig() {
        return eurekaClientConfig;
    }

    /**
     * Get the instance of {@link EurekaInstanceConfig} this instance was initialized with.
     * @return the instance of {@link EurekaInstanceConfig} this instance was initialized with.
     */
    public EurekaInstanceConfig getEurekaInstanceConfig() {
        return eurekaInstanceConfig;
    }
}
