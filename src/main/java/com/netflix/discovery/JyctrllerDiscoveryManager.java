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

    /**
     * Initializes the <tt>Discovery Client</tt> with the given configuration.
     *
     * @param config
     *            the instance info configuration that will be used for
     *            registration with Eureka.
     * @param eurekaConfig the eureka client configuration of the instance.
     */
    public void initComponent(EurekaInstanceConfig config,
                              EurekaClientConfig eurekaConfig, JyctrllerDiscoveryClient.DiscoveryClientOptionalArgs args) {
        this.eurekaInstanceConfig = config;
        this.eurekaClientConfig = eurekaConfig;
        if (ApplicationInfoManager.getInstance().getInfo() == null) {
            // Initialize application info
            ApplicationInfoManager.getInstance().initComponent(config);
        }
        InstanceInfo info = ApplicationInfoManager.getInstance().getInfo();
        discoveryClient = new JyctrllerDiscoveryClient(info,eurekaConfig);
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
    public JyctrllerDiscoveryClient getDiscoveryClient() {
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
