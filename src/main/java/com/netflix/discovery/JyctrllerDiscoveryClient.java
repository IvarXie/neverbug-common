package com.netflix.discovery;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.netflix.appinfo.*;
import com.netflix.discovery.endpoint.EndpointUtils;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.resolver.ClosableResolver;
import com.netflix.discovery.shared.resolver.aws.ApplicationsResolver;
import com.netflix.discovery.shared.transport.*;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.util.ThresholdLevelsMetric;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Provider;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.netflix.discovery.EurekaClientNames.METRIC_REGISTRATION_PREFIX;

/**
 * C层注册的discoveryClient
 * 精简了一些不必要的属性和操作
 * Created by zhao.weiwei
 * create on 2017/2/14 18:07
 * the email is zhao.weiwei@jyall.com.
 */
public class JyctrllerDiscoveryClient implements EurekaClient {
    private static Logger logger = LoggerFactory.getLogger(JyctrllerDiscoveryClient.class);


    private static String VALUE_DELIMITER = ",";
    private static String COMMA_STRING = VALUE_DELIMITER;

    // Timers
    private static String PREFIX = "JyctrllerDiscoveryClient_";
    private Counter REREGISTER_COUNTER = Monitors.newCounter(PREFIX + "Reregister");


    private ScheduledExecutorService scheduler = null;
    // additional executors for supervised subtasks
    private ThreadPoolExecutor heartbeatExecutor = null;
    private ThreadPoolExecutor cacheRefreshExecutor = null;

    private Provider<HealthCheckHandler> healthCheckHandlerProvider = null;
    private Provider<HealthCheckCallback> healthCheckCallbackProvider = null;
    private AtomicReference<Applications> localRegionApps = new AtomicReference<Applications>();
    // monotonically increasing generation counter to ensure stale threads do not reset registry to an older version
    private ApplicationInfoManager applicationInfoManager = null;
    private InstanceInfo instanceInfo = null;
    private AtomicReference<String> remoteRegionsToFetch = null;
    private InstanceRegionChecker instanceRegionChecker = null;

    private EndpointUtils.ServiceUrlRandomizer urlRandomizer = null;
    private JyctrllerDiscoveryClient.EurekaTransport eurekaTransport = null;

    private volatile HealthCheckHandler healthCheckHandler;
    private volatile Map<String, Applications> remoteRegionVsApps = new ConcurrentHashMap<>();
    private volatile InstanceInfo.InstanceStatus lastRemoteInstanceStatus = InstanceInfo.InstanceStatus.UNKNOWN;
    private CopyOnWriteArraySet<EurekaEventListener> eventListeners = new CopyOnWriteArraySet<>();

    private String appPathIdentifier;
    private ApplicationInfoManager.StatusChangeListener statusChangeListener;

    private JyctrllerInstanceInfoReplicator instanceInfoReplicator;

    private volatile long lastSuccessfulRegistryFetchTimestamp = -1;
    private volatile long lastSuccessfulHeartbeatTimestamp = -1;
    private ThresholdLevelsMetric heartbeatStalenessMonitor = null;
    private ThresholdLevelsMetric registryStalenessMonitor = null;

    private AtomicBoolean isShutdown = new AtomicBoolean(false);

    protected EurekaClientConfig clientConfig = null;
    protected EurekaTransportConfig transportConfig = null;

    private long initTimestampMs = 10;

    public static String ctrllerRegistryUrls ;
//    public static String defaultZones;

    private static class EurekaTransport {
        private ClosableResolver bootstrapResolver;
        private TransportClientFactory transportClientFactory;

        private EurekaHttpClient registrationClient;
        private EurekaHttpClientFactory registrationClientFactory;

        private EurekaHttpClient queryClient;
        private EurekaHttpClientFactory queryClientFactory;

        void shutdown() {
            if (registrationClientFactory != null) {
                registrationClientFactory.shutdown();
            }

            if (queryClientFactory != null) {
                queryClientFactory.shutdown();
            }

            if (registrationClient != null) {
                registrationClient.shutdown();
            }

            if (queryClient != null) {
                queryClient.shutdown();
            }

            if (transportClientFactory != null) {
                transportClientFactory.shutdown();
            }

            if (bootstrapResolver != null) {
                bootstrapResolver.shutdown();
            }
        }
    }

    public static class DiscoveryClientOptionalArgs {
        private Provider<HealthCheckCallback> healthCheckCallbackProvider;

        private Provider<HealthCheckHandler> healthCheckHandlerProvider;

        private Collection<ClientFilter> additionalFilters;

        private EurekaJerseyClient eurekaJerseyClient;

        private Set<EurekaEventListener> eventListeners;

        @Inject(optional = true)
        public void setEventListeners(Set<EurekaEventListener> listeners) {
            if (eventListeners == null) {
                eventListeners = new HashSet<>();
            }
            eventListeners.addAll(listeners);
        }

        @Inject(optional = true)
        public void setEventBus(EventBus eventBus) {
            if (eventListeners == null) {
                eventListeners = new HashSet<>();
            }

            eventListeners.add(new EurekaEventListener() {
                @Override
                public void onEvent(EurekaEvent event) {
                    eventBus.publish(event);
                }
            });
        }

        @Inject(optional = true)
        public void setHealthCheckCallbackProvider(Provider<HealthCheckCallback> healthCheckCallbackProvider) {
            this.healthCheckCallbackProvider = healthCheckCallbackProvider;
        }

        @Inject(optional = true)
        public void setHealthCheckHandlerProvider(Provider<HealthCheckHandler> healthCheckHandlerProvider) {
            this.healthCheckHandlerProvider = healthCheckHandlerProvider;
        }

        @Inject(optional = true)
        public void setAdditionalFilters(Collection<ClientFilter> additionalFilters) {
            this.additionalFilters = additionalFilters;
        }

        @Inject(optional = true)
        public void setEurekaJerseyClient(EurekaJerseyClient eurekaJerseyClient) {
            this.eurekaJerseyClient = eurekaJerseyClient;
        }

        Set<EurekaEventListener> getEventListeners() {
            return eventListeners == null ? Collections.<EurekaEventListener>emptySet() : eventListeners;
        }
    }


    public JyctrllerDiscoveryClient(InstanceInfo myInfo, EurekaClientConfig config) {
        this(myInfo, config, null);
    }

    public JyctrllerDiscoveryClient(InstanceInfo myInfo, EurekaClientConfig config, JyctrllerDiscoveryClient.DiscoveryClientOptionalArgs args) {
        this(ApplicationInfoManager.getInstance(), config, args);
    }

    public JyctrllerDiscoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config) {
        this(applicationInfoManager, config, null);
    }

    public JyctrllerDiscoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config, JyctrllerDiscoveryClient.DiscoveryClientOptionalArgs args) {
        this(applicationInfoManager, config, args, new Provider<BackupRegistry>() {
            private volatile BackupRegistry backupRegistryInstance;

            @Override
            public synchronized BackupRegistry get() {
                if (backupRegistryInstance == null) {
                    String backupRegistryClassName = config.getBackupRegistryImpl();
                    if (null != backupRegistryClassName) {
                        try {
                            backupRegistryInstance = (BackupRegistry) Class.forName(backupRegistryClassName).newInstance();
                            logger.info("Enabled backup registry of type " + backupRegistryInstance.getClass());
                        } catch (Exception e) {
                            logger.error("Error instantiating BackupRegistry.", e);
                        }
                    }
                    if (backupRegistryInstance == null) {
                        logger.warn("Using default backup registry implementation which does not do anything.");
                        backupRegistryInstance = new NotImplementedRegistryImpl();
                    }
                }

                return backupRegistryInstance;
            }
        });
    }

    @Inject
    JyctrllerDiscoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config, JyctrllerDiscoveryClient.DiscoveryClientOptionalArgs args,
                             Provider<BackupRegistry> backupRegistryProvider) {
        if (args != null) {
            this.healthCheckHandlerProvider = args.healthCheckHandlerProvider;
            this.healthCheckCallbackProvider = args.healthCheckCallbackProvider;
            this.eventListeners.addAll(args.getEventListeners());
        } else {
            this.healthCheckCallbackProvider = null;
            this.healthCheckHandlerProvider = null;
        }
        this.applicationInfoManager = applicationInfoManager;
        InstanceInfo myInfo = applicationInfoManager.getInfo();
        clientConfig = config;
        transportConfig = config.getTransportConfig();
        instanceInfo = myInfo;
        if (myInfo != null) {
            appPathIdentifier = instanceInfo.getAppName() + "/" + instanceInfo.getId();
        } else {
            logger.warn("Setting instanceInfo to a passed in null value");
        }


        this.urlRandomizer = new EndpointUtils.InstanceInfoBasedUrlRandomizer(instanceInfo);
        localRegionApps.set(new Applications());


        remoteRegionsToFetch = new AtomicReference<String>(clientConfig.fetchRegistryForRemoteRegions());


        this.registryStalenessMonitor = ThresholdLevelsMetric.NO_OP_METRIC;

        this.heartbeatStalenessMonitor = new ThresholdLevelsMetric(this, METRIC_REGISTRATION_PREFIX + "lastHeartbeatSec_", new long[]{15L, 30L, 60L, 120L, 240L, 480L});


        try {
            scheduler = Executors.newScheduledThreadPool(3,
                    new ThreadFactoryBuilder()
                            .setNameFormat("JyctrllerDiscoveryClient-%d")
                            .setDaemon(true)
                            .build());

            heartbeatExecutor = new ThreadPoolExecutor(
                    1, clientConfig.getHeartbeatExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("JyctrllerDiscoveryClient-HeartbeatExecutor-%d")
                            .setDaemon(true)
                            .build()
            );  // use direct handoff

            cacheRefreshExecutor = new ThreadPoolExecutor(
                    1, clientConfig.getCacheRefreshExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("JyctrllerDiscoveryClient-CacheRefreshExecutor-%d")
                            .setDaemon(true)
                            .build()
            );  // use direct handoff

            eurekaTransport = new JyctrllerDiscoveryClient.EurekaTransport();
            scheduleServerEndpointTask(eurekaTransport, args);

            AzToRegionMapper azToRegionMapper;
            if (clientConfig.shouldUseDnsForFetchingServiceUrls()) {
                azToRegionMapper = new DNSBasedAzToRegionMapper(clientConfig);
            } else {
                azToRegionMapper = new PropertyBasedAzToRegionMapper(clientConfig);
            }
            if (null != remoteRegionsToFetch.get()) {
                azToRegionMapper.setRegionsToFetch(remoteRegionsToFetch.get().split(","));
            }
            instanceRegionChecker = new InstanceRegionChecker(azToRegionMapper, clientConfig.getRegion());
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize JyctrllerDiscoveryClient!", e);
        }

        initScheduledTasks();
        JyctrllerDiscoveryManager.getInstance().setDiscoveryClient(this);
        JyctrllerDiscoveryManager.getInstance().setEurekaClientConfig(config);

        initTimestampMs = System.currentTimeMillis();
        logger.info("JyctrllerDiscovery Client initialized at timestamp {} with initial instances count: {}",
                initTimestampMs, this.getApplications().size());
    }

    private void scheduleServerEndpointTask(JyctrllerDiscoveryClient.EurekaTransport eurekaTransport,
                                            JyctrllerDiscoveryClient.DiscoveryClientOptionalArgs args) {

        Collection<ClientFilter> additionalFilters = args == null
                ? Collections.emptyList()
                : args.additionalFilters;

        EurekaJerseyClient providedJerseyClient = args == null
                ? null
                : args.eurekaJerseyClient;

        eurekaTransport.transportClientFactory = providedJerseyClient == null
                ? TransportClientFactories.newTransportClientFactory(clientConfig, additionalFilters, applicationInfoManager.getInfo())
                : TransportClientFactories.newTransportClientFactory(additionalFilters, providedJerseyClient);

        ApplicationsResolver.ApplicationsSource applicationsSource = new ApplicationsResolver.ApplicationsSource() {
            @Override
            public Applications getApplications(int stalenessThreshold, TimeUnit timeUnit) {
                long thresholdInMs = TimeUnit.MILLISECONDS.convert(stalenessThreshold, timeUnit);
                long delay = getLastSuccessfulRegistryFetchTimePeriod();
                if (delay > thresholdInMs) {
                    logger.info("Local registry is too stale for local lookup. Threshold:{}, actual:{}",
                            thresholdInMs, delay);
                    return null;
                } else {
                    return localRegionApps.get();
                }
            }
        };

        eurekaTransport.bootstrapResolver = EurekaHttpClients.newBootstrapResolver(
                clientConfig,
                transportConfig,
                eurekaTransport.transportClientFactory,
                applicationInfoManager.getInfo(),
                applicationsSource
        );

        EurekaHttpClientFactory newRegistrationClientFactory = null;
        EurekaHttpClient newRegistrationClient = null;
        try {
            newRegistrationClientFactory = EurekaHttpClients.registrationClientFactory(
                    eurekaTransport.bootstrapResolver,
                    eurekaTransport.transportClientFactory,
                    transportConfig
            );
            newRegistrationClient = newRegistrationClientFactory.newClient();
        } catch (Exception e) {
            logger.warn("Transport initialization failure", e);
        }
        eurekaTransport.registrationClientFactory = newRegistrationClientFactory;
        eurekaTransport.registrationClient = newRegistrationClient;
    }

    @Override
    public Application getApplication(String appName) {
        return getApplications().getRegisteredApplications(appName);
    }

    @Override
    public Applications getApplications() {
        return localRegionApps.get();
    }

    @Override
    public Applications getApplicationsForARegion(String region) {
        if (instanceRegionChecker.isLocalRegion(region)) {
            return localRegionApps.get();
        } else {
            return remoteRegionVsApps.get(region);
        }
    }

    public Set<String> getAllKnownRegions() {
        String localRegion = instanceRegionChecker.getLocalRegion();
        if (!remoteRegionVsApps.isEmpty()) {
            Set<String> regions = remoteRegionVsApps.keySet();
            Set<String> toReturn = new HashSet<>(regions);
            toReturn.add(localRegion);
            return toReturn;
        } else {
            return Collections.singleton(localRegion);
        }
    }


    @Override
    public List<InstanceInfo> getInstancesById(String id) {
        List<InstanceInfo> instancesList = new ArrayList<InstanceInfo>();
        for (Application app : this.getApplications()
                .getRegisteredApplications()) {
            InstanceInfo instanceInfo = app.getByInstanceId(id);
            if (instanceInfo != null) {
                instancesList.add(instanceInfo);
            }
        }
        return instancesList;
    }

    @Override
    public void registerHealthCheckCallback(HealthCheckCallback callback) {
        if (instanceInfo == null) {
            logger.error("Cannot register a listener for instance info since it is null!");
        }
        if (callback != null) {
            healthCheckHandler = new HealthCheckCallbackToHandlerBridge(callback);
        }
    }

    @Override
    public void registerHealthCheck(HealthCheckHandler healthCheckHandler) {
        if (instanceInfo == null) {
            logger.error("Cannot register a healthcheck handler when instance info is null!");
        }
        if (healthCheckHandler != null) {
            this.healthCheckHandler = healthCheckHandler;
            if (instanceInfoReplicator != null) {
                instanceInfoReplicator.onDemandUpdate();
            }
        }
    }

    @Override
    public void registerEventListener(EurekaEventListener eventListener) {
        this.eventListeners.add(eventListener);
    }

    @Override
    public boolean unregisterEventListener(EurekaEventListener eventListener) {
        return this.eventListeners.remove(eventListener);
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure) {
        return getInstancesByVipAddress(vipAddress, secure, instanceRegionChecker.getLocalRegion());
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure,
                                                       String region) {
        if (vipAddress == null) {
            throw new IllegalArgumentException(
                    "Supplied VIP Address cannot be null");
        }
        Applications applications;
        if (instanceRegionChecker.isLocalRegion(region)) {
            applications = this.localRegionApps.get();
        } else {
            applications = remoteRegionVsApps.get(region);
            if (null == applications) {
                logger.debug("No applications are defined for region {}, so returning an empty instance list for vip "
                        + "address {}.", region, vipAddress);
                return Collections.emptyList();
            }
        }

        if (!secure) {
            return applications.getInstancesByVirtualHostName(vipAddress);
        } else {
            return applications.getInstancesBySecureVirtualHostName(vipAddress);

        }

    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddressAndAppName(
            String vipAddress, String appName, boolean secure) {

        List<InstanceInfo> result = new ArrayList<InstanceInfo>();
        if (vipAddress == null && appName == null) {
            throw new IllegalArgumentException(
                    "Supplied VIP Address and application name cannot both be null");
        } else if (vipAddress != null && appName == null) {
            return getInstancesByVipAddress(vipAddress, secure);
        } else if (vipAddress == null && appName != null) {
            Application application = getApplication(appName);
            if (application != null) {
                result = application.getInstances();
            }
            return result;
        }

        String instanceVipAddress;
        for (Application app : getApplications().getRegisteredApplications()) {
            for (InstanceInfo instance : app.getInstances()) {
                if (secure) {
                    instanceVipAddress = instance.getSecureVipAddress();
                } else {
                    instanceVipAddress = instance.getVIPAddress();
                }
                if (instanceVipAddress == null) {
                    continue;
                }
                String[] instanceVipAddresses = instanceVipAddress
                        .split(COMMA_STRING);

                for (String vipAddressFromList : instanceVipAddresses) {
                    if (vipAddress.equalsIgnoreCase(vipAddressFromList.trim())
                            && appName.equalsIgnoreCase(instance.getAppName())) {
                        result.add(instance);
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public InstanceInfo getNextServerFromEureka(String virtualHostname, boolean secure) {
        List<InstanceInfo> instanceInfoList = this.getInstancesByVipAddress(
                virtualHostname, secure);
        if (instanceInfoList == null || instanceInfoList.isEmpty()) {
            throw new RuntimeException("No matches for the virtual host name :"
                    + virtualHostname);
        }
        Applications apps = this.localRegionApps.get();
        int index = (int) (apps.getNextIndex(virtualHostname.toUpperCase(Locale.ROOT),
                secure).incrementAndGet() % instanceInfoList.size());
        return instanceInfoList.get(index);
    }

    @Override
    public Applications getApplications(String serviceUrl) {
        try {
            EurekaHttpResponse<Applications> response = clientConfig.getRegistryRefreshSingleVipAddress() == null
                    ? eurekaTransport.queryClient.getApplications()
                    : eurekaTransport.queryClient.getVip(clientConfig.getRegistryRefreshSingleVipAddress());
            if (response.getStatusCode() == 200) {
                logger.debug(PREFIX + appPathIdentifier + " -  refresh status: " + response.getStatusCode());
                return response.getEntity();
            }
            logger.error(PREFIX + appPathIdentifier + " - was unable to refresh its cache! status = " + response.getStatusCode());
        } catch (Throwable th) {
            logger.error(PREFIX + appPathIdentifier + " - was unable to refresh its cache! status = " + th.getMessage(), th);
        }
        return null;
    }

    boolean register() throws Throwable {
        logger.info(PREFIX + appPathIdentifier + ": registering service...");
        EurekaHttpResponse<Void> httpResponse;
        try {
            httpResponse = eurekaTransport.registrationClient.register(instanceInfo);
        } catch (Exception e) {
            logger.warn("{} - registration failed {}", PREFIX + appPathIdentifier, e.getMessage(), e);
            throw e;
        }
        if (logger.isInfoEnabled()) {
            logger.info("{} - registration status: {}", PREFIX + appPathIdentifier, httpResponse.getStatusCode());
        }
        return httpResponse.getStatusCode() == 204;
    }

    boolean renew() {
        logger.info("heart beat renew start");
        long start = System.currentTimeMillis();
        EurekaHttpResponse<InstanceInfo> httpResponse;
        try {
            httpResponse = eurekaTransport.registrationClient.sendHeartBeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null);
            logger.debug("{} - Heartbeat status: {}", PREFIX + appPathIdentifier, httpResponse.getStatusCode());
            if (httpResponse.getStatusCode() == 404) {
                REREGISTER_COUNTER.increment();
                logger.info("{} - Re-registering apps/{}", PREFIX + appPathIdentifier, instanceInfo.getAppName());
                return register();
            }
            return httpResponse.getStatusCode() == 200;
        } catch (Throwable e) {
            logger.error("{} - was unable to send heartbeat!", PREFIX + appPathIdentifier, e);
            return false;
        } finally {
            logger.info("heart beat renew end ,use {}ms", System.currentTimeMillis() - start);
        }
    }

    @Override
    public List<String> getServiceUrlsFromConfig(String instanceZone, boolean preferSameZone) {
        String[] urls = JyctrllerDiscoveryClient.ctrllerRegistryUrls.split(",");
        return Arrays.asList(urls);
    }

    @PreDestroy
    @Override
    public synchronized void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info("Shutting down JyctrllerDiscoveryClient ...");

            if (statusChangeListener != null && applicationInfoManager != null) {
                applicationInfoManager.unregisterStatusChangeListener(statusChangeListener.getId());
            }

            cancelScheduledTasks();

            // If APPINFO was registered
            if (applicationInfoManager != null && clientConfig.shouldRegisterWithEureka()) {
                applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
                unregister();
            }

            if (eurekaTransport != null) {
                eurekaTransport.shutdown();
            }

            heartbeatStalenessMonitor.shutdown();
            registryStalenessMonitor.shutdown();

            logger.info("Completed shut down of DiscoveryClient");
        }
    }

    void unregister() {
        if (eurekaTransport != null && eurekaTransport.registrationClient != null) {
            try {
                logger.info("Unregistering ...");
                EurekaHttpResponse<Void> httpResponse = eurekaTransport.registrationClient.cancel(instanceInfo.getAppName(), instanceInfo.getId());
                logger.info(PREFIX + appPathIdentifier + " - deregister  status: " + httpResponse.getStatusCode());
            } catch (Exception e) {
                logger.error(PREFIX + appPathIdentifier + " - de-registration failed" + e.getMessage(), e);
            }
        }
    }

    @Override
    public InstanceInfo.InstanceStatus getInstanceRemoteStatus() {
        return lastRemoteInstanceStatus;
    }

    private void initScheduledTasks() {

        int renewalIntervalInSecs = instanceInfo.getLeaseInfo().getRenewalIntervalInSecs();
        int expBackOffBound = clientConfig.getHeartbeatExecutorExponentialBackOffBound();
        logger.info("Starting heartbeat executor: " + "renew interval is: " + renewalIntervalInSecs);

        // Heartbeat timer
        scheduler.schedule(
                new TimedSupervisorTask(
                        "heartbeat",
                        scheduler,
                        heartbeatExecutor,
                        renewalIntervalInSecs,
                        TimeUnit.SECONDS,
                        expBackOffBound,
                        new JyctrllerDiscoveryClient.HeartbeatThread()
                ),
                renewalIntervalInSecs, TimeUnit.SECONDS);

        // InstanceInfo replicator
        instanceInfoReplicator = new JyctrllerInstanceInfoReplicator(
                this,
                instanceInfo,
                clientConfig.getInstanceInfoReplicationIntervalSeconds(),
                2); // burstSize

        statusChangeListener = new ApplicationInfoManager.StatusChangeListener() {
            @Override
            public String getId() {
                return "statusChangeListener";
            }

            @Override
            public void notify(StatusChangeEvent statusChangeEvent) {
                if (InstanceInfo.InstanceStatus.DOWN == statusChangeEvent.getStatus() ||
                        InstanceInfo.InstanceStatus.DOWN == statusChangeEvent.getPreviousStatus()) {
                    logger.warn("Saw local status change event {}", statusChangeEvent);
                } else {
                    logger.info("Saw local status change event {}", statusChangeEvent);
                }
                instanceInfoReplicator.onDemandUpdate();
            }
        };
        if (clientConfig.shouldOnDemandUpdateStatusChange()) {
            applicationInfoManager.registerStatusChangeListener(statusChangeListener);
        }
        instanceInfoReplicator.start(clientConfig.getInitialInstanceInfoReplicationIntervalSeconds());
    }

    private void cancelScheduledTasks() {
        if (instanceInfoReplicator != null) {
            instanceInfoReplicator.stop();
        }
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        if (cacheRefreshExecutor != null) {
            cacheRefreshExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public List<String> getServiceUrlsFromDNS(String instanceZone, boolean preferSameZone) {
        String[] urls = JyctrllerDiscoveryClient.ctrllerRegistryUrls.split(",");
        return Arrays.asList(urls);
    }


    @Override
    public List<String> getDiscoveryServiceUrls(String zone) {
        String[] urls = JyctrllerDiscoveryClient.ctrllerRegistryUrls.split(",");
        return Arrays.asList(urls);
    }


    void refreshInstanceInfo() {

        try {
            applicationInfoManager.refreshDataCenterInfoIfRequired();
            applicationInfoManager.refreshLeaseInfoIfRequired();

            InstanceInfo.InstanceStatus status;
            try {
                status = getHealthCheckHandler().getStatus(instanceInfo.getStatus());
            } catch (Exception e) {
                logger.warn("Exception from healthcheckHandler.getStatus, setting status to DOWN", e);
                status = InstanceInfo.InstanceStatus.DOWN;
            }
            if (null != status) {
                applicationInfoManager.setInstanceStatus(status);
            }
        } catch (Exception e) {
            logger.error("",e);
            throw  e;
        }


    }

    private class HeartbeatThread implements Runnable {

        public void run() {
            if (renew()) {
                lastSuccessfulHeartbeatTimestamp = System.currentTimeMillis();
            }
        }
    }

    @Override
    public HealthCheckHandler getHealthCheckHandler() {
        if (healthCheckHandler == null) {
            if (null != healthCheckHandlerProvider) {
                healthCheckHandler = healthCheckHandlerProvider.get();
            } else if (null != healthCheckCallbackProvider) {
                healthCheckHandler = new HealthCheckCallbackToHandlerBridge(healthCheckCallbackProvider.get());
            }

            if (null == healthCheckHandler) {
                healthCheckHandler = new HealthCheckCallbackToHandlerBridge(null);
            }
        }
        return healthCheckHandler;
    }


    public long getLastSuccessfulRegistryFetchTimePeriod() {
        return lastSuccessfulRegistryFetchTimestamp < 0
                ? lastSuccessfulRegistryFetchTimestamp
                : System.currentTimeMillis() - lastSuccessfulRegistryFetchTimestamp;
    }
}