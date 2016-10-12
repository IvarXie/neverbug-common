package com.netflix.discovery;

import com.eureka2.shading.apache.avro.reflect.Nullable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.appinfo.*;
import com.netflix.appinfo.InstanceInfo.ActionType;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryClient.DiscoveryClientOptionalArgs;
import com.netflix.discovery.endpoint.DnsResolver;
import com.netflix.discovery.endpoint.EndpointUtils;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.resolver.ClosableResolver;
import com.netflix.discovery.shared.transport.*;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl
        .EurekaJerseyClientBuilder;
import com.netflix.discovery.util.ThresholdLevelsMetric;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationContext;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.netflix.discovery.EurekaClientNames.METRIC_REGISTRATION_PREFIX;
import static com.netflix.discovery.EurekaClientNames.METRIC_REGISTRY_PREFIX;

/**
 * The class that is instrumental for interactions with <tt>Eureka Server</tt>.
 * <p>
 * <p>
 * <tt>Eureka Client</tt> is responsible for a) <em>Registering</em> the
 * instance with <tt>Eureka Server</tt> b) <em>Renewal</em>of the lease with
 * <tt>Eureka Server</tt> c) <em>Cancellation</em> of the lease from
 * <tt>Eureka Server</tt> during shutdown
 * <p>
 * d) <em>Querying</em> the list of services/instances registered with
 * <tt>Eureka Server</tt>
 * <p>
 * <p>
 * <p>
 * <tt>Eureka Client</tt> needs a configured list of <tt>Eureka Server</tt>
 * {@link java.net.URL}s to talk to.These {@link java.net.URL}s are typically
 * amazon elastic eips which do not change. All of the functions defined above
 * fail-over to other {@link java.net.URL}s specified in the list in the case of
 * failure.
 * </p>
 *
 * @author Karthik Ranganathan, Greg Kim
 * @author Spencer Gibb
 */
@Singleton
public class JyallDiscoveryClient implements EurekaClient {
    private static final Logger logger = LoggerFactory.getLogger(JyallDiscoveryClient.class);

    // Constants
    public static final int MAX_FOLLOWED_REDIRECTS = 10;
    public static final String HTTP_X_DISCOVERY_ALLOW_REDIRECT = "X-Discovery-AllowRedirect";

    private static final String VALUE_DELIMITER = ",";
    private static final String COMMA_STRING = VALUE_DELIMITER;
//    private static final String DISCOVERY_APPID = "DISCOVERY";
    private static final String UNKNOWN = "UNKNOWN";

    private static final Pattern REDIRECT_PATH_REGEX = Pattern.compile("(.*/v2/)apps(/.*)?$");

    // Timers
    private static final String PREFIX = "JyallDiscoveryClient_";
    private final Timer getServiceUrlsDnsTimer = Monitors.newTimer(PREFIX
            + "GetServiceUrlsFromDNS");
    private final Timer registerTimer = Monitors.newTimer(PREFIX + "REGISTER");
    private final Timer refreshTimer = Monitors.newTimer(PREFIX + "REFRESH");
    private final Timer refreshDeltaTimer = Monitors.newTimer(PREFIX + "RefreshDelta");
    private final Timer renewTimer = Monitors.newTimer(PREFIX + "RENEW");
    private final Timer cancelTimer = Monitors.newTimer(PREFIX + "CANCEL");
    private final Timer fetchRegistryTimer = Monitors.newTimer(PREFIX + "FetchRegistry");
    // Counter
    private final Counter serverRetryCounter = Monitors.newCounter(PREFIX + "Retry");
    private final Counter allServerFailureCount = Monitors.newCounter(PREFIX + "Failed");
    private final Counter reregisterCounter = Monitors.newCounter(PREFIX + "Reregister");
    private final Counter reconcileHashCodesMismatch = Monitors.newCounter(PREFIX +
            "ReconcileHashCodeMismatch");

    // instance variables
    /**
     * A scheduler to be used for the following 3 tasks: - updating service urls
     * - scheduling a TimedSuperVisorTask
     */
    private final ScheduledExecutorService scheduler;
    // additional executors for supervised subtasks
    private final ThreadPoolExecutor heartbeatExecutor;
    private final ThreadPoolExecutor cacheRefreshExecutor;

    private Provider<HealthCheckHandler> healthCheckHandlerProvider;
    private Provider<HealthCheckCallback> healthCheckCallbackProvider;
    private final AtomicReference<List<String>> eurekaServiceUrls = new AtomicReference<>();
    private final AtomicReference<Applications> localRegionApps = new AtomicReference<>();
    private final Lock fetchRegistryUpdateLock = new ReentrantLock();
    // monotonically increasing generation counter to ensure stale threads do
    // not reset registry to an older version
    private final AtomicLong fetchRegistryGeneration;
    private final ApplicationInfoManager applicationInfoManager;
    private final InstanceInfo instanceInfo;
    private final EurekaAccept clientAccept;
    private final AtomicReference<String> remoteRegionsToFetch;
    private final AtomicReference<String[]> remoteRegionsRef;
    private final InstanceRegionChecker instanceRegionChecker;
    private final AtomicReference<String> lastQueryRedirect = new AtomicReference<>();
    private final AtomicReference<String> lastRegisterRedirect = new AtomicReference<>();
    private EventBus eventBus;
    private final EndpointUtils.ServiceUrlRandomizer urlRandomizer;
    private final Provider<BackupRegistry> backupRegistryProvider;
    private final EurekaTransport eurekaTransport;
    private final ApacheHttpClient4 discoveryApacheClient;
    private EurekaJerseyClient discoveryJerseyClient;

    private volatile HealthCheckHandler healthCheckHandler;
    private volatile Map<String, Applications> remoteRegionVsApps = new ConcurrentHashMap<>();
    private volatile InstanceInfo.InstanceStatus lastRemoteInstanceStatus = InstanceInfo
            .InstanceStatus.UNKNOWN;

    private String appPathIdentifier;
    private boolean isRegisteredWithDiscovery = false;
    private ApplicationInfoManager.StatusChangeListener statusChangeListener;

    private JyallInstanceInfoReplicator instanceInfoReplicator;

    private volatile int registrySize = 0;
    private volatile long lastSuccessfulRegistryFetchTimestamp = -1;
    private volatile long lastSuccessfulHeartbeatTimestamp = -1;
    private final ThresholdLevelsMetric heartbeatStalenessMonitor;
    private final ThresholdLevelsMetric registryStalenessMonitor;

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    protected final EurekaClientConfig clientConfig;
    protected final EurekaTransportConfig transportConfig;

//    private final long initTimestampMs;

    // ----- by guo.guanfei, BEGIN

    public static final String PROP_CONTROLLER_SHOULD_REGISTERED = "${eureka.client.jyctrller" +
            ".registered}";
    public static final String PROP_CONTROLLER_REGISTRY_URLS = "${eureka.client.jyctrller" +
            ".registryUrls}";

    private String ctrllerRegistryUrls;

    private Collection<ClientFilter> additionalFilters;
    private final AtomicLong cacheRefreshedCount = new AtomicLong(0);
    private ApplicationContext context;

    // ----- by guo.guanfei, END

    private enum Action {
        REGISTER, CANCEL, RENEW, REFRESH, REFRESH_DELTA
    }

    private static final class EurekaTransport {
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

    public JyallDiscoveryClient(ApplicationInfoManager applicationInfoManager,
                                final EurekaClientConfig config, DiscoveryClientOptionalArgs args,
                                ApplicationContext context, String controllerRegistryUrls) {
        this(applicationInfoManager, config, args, new BackupRegistryProvider(config), context, controllerRegistryUrls);
    }

    @Inject
    JyallDiscoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config,
                         DiscoveryClientOptionalArgs args, Provider<BackupRegistry>
                                 backupRegistryProvider,
                         ApplicationContext context, String ctrllerRegistryUrls) {

        // ----- by guo.guanfei, BEGIN

        this.context = context;
        this.ctrllerRegistryUrls = ctrllerRegistryUrls;

        // if (args != null) {

        // this.healthCheckCallbackProvider =
        // args.healthCheckCallbackProvider;
        // this.healthCheckHandlerProvider =
        // args.healthCheckHandlerProvider;
        // this.eventBus = args.eventBus;
        // this.discoveryJerseyClient = args.eurekaJerseyClient;

        // Field[] fields = args.getClass().getDeclaredFields();
        // try {
        // for (Field field : fields) {
        //
        // // 设置私有变量为可访问
        // field.setAccessible(true);
        //
        // if ("healthCheckCallbackProvider".equals(field.getName())) {
        // this.healthCheckCallbackProvider = (Provider<HealthCheckCallback>)
        // field.get(args);
        // } else if ("healthCheckHandlerProvider".equals(field.getName())) {
        // this.healthCheckHandlerProvider = (Provider<HealthCheckHandler>)
        // field.get(args);
        // } else if ("eventBus".equals(field.getName())) {
        // this.eventBus = (EventBus) field.get(args);
        // } else if ("eurekaJerseyClient".equals(field.getName())) {
        // this.discoveryJerseyClient = (EurekaJerseyClient) field.get(args);
        // } else if ("additionalFilters".equals(field.getName())) {
        // this.additionalFilters = (Collection<ClientFilter>) field.get(args);
        // }
        //
        // // 设置私有变量为不可访问
        // field.setAccessible(false);
        //
        // }
        // } catch (Exception ex) {
        // ex.printStackTrace();
        // }

        // } else {
        this.healthCheckCallbackProvider = null;
        this.healthCheckHandlerProvider = null;
        this.eventBus = null;
        this.discoveryJerseyClient = null;
        // }

        // ----- by guo.guanfei, END

        this.applicationInfoManager = applicationInfoManager;
        InstanceInfo myInfo = applicationInfoManager.getInfo();

        this.backupRegistryProvider = backupRegistryProvider;

        try {
            scheduler = Executors.newScheduledThreadPool(3,
                    new ThreadFactoryBuilder().setNameFormat("JyallDiscoveryClient-%d").setDaemon
                            (true).build());
            clientConfig = config;
            transportConfig = config.getTransportConfig();
            instanceInfo = myInfo;
            if (myInfo != null) {
                appPathIdentifier = instanceInfo.getAppName() + "/" + instanceInfo.getId();
            } else {
                logger.warn("Setting instanceInfo to a passed in null value");
            }

            this.urlRandomizer = new EndpointUtils.InstanceInfoBasedUrlRandomizer(instanceInfo);
            String[] availZones = clientConfig.getAvailabilityZones(clientConfig.getRegion());
            final String zone = InstanceInfo.getZone(availZones, myInfo);
            localRegionApps.set(new Applications());

            // use direct handoff
            heartbeatExecutor = new ThreadPoolExecutor(1, clientConfig
                    .getHeartbeatExecutorThreadPoolSize(), 0,
                    TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

            // use direct handoff
            cacheRefreshExecutor = new ThreadPoolExecutor(1, clientConfig
                    .getCacheRefreshExecutorThreadPoolSize(), 0,
                    TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

            fetchRegistryGeneration = new AtomicLong(0);

            clientAccept = EurekaAccept.fromString(clientConfig.getClientDataAccept());

            eurekaTransport = new EurekaTransport();
            scheduleServerEndpointTask(eurekaTransport, zone);

            if (discoveryJerseyClient == null) { // if not injected, create one

                EurekaJerseyClientBuilder clientBuilder = new EurekaJerseyClientBuilder()
                        .withUserAgent("Java-EurekaClient")
                        .withConnectionTimeout(clientConfig.getEurekaServerConnectTimeoutSeconds
                                () * 1000)
                        .withReadTimeout(clientConfig.getEurekaServerReadTimeoutSeconds() * 1000)
                        .withMaxConnectionsPerHost(clientConfig
                                .getEurekaServerTotalConnectionsPerHost())
                        .withMaxTotalConnections(clientConfig.getEurekaServerTotalConnections())
                        .withConnectionIdleTimeout(clientConfig
                                .getEurekaConnectionIdleTimeoutSeconds())
                        .withEncoder(clientConfig.getEncoderName())
                        .withDecoder(clientConfig.getDecoderName(), clientConfig
                                .getClientDataAccept());

                if (eurekaServiceUrls.get().get(0).startsWith("https://") && "true"
                        .equals(System.getProperty("com.netflix.eureka" +
                                ".shouldSSLConnectionsUseSystemSocketFactory"))) {
                    clientBuilder.withClientName("DiscoveryClient-HTTPClient-System")
                            .withSystemSSLConfiguration();
                } else if (clientConfig.getProxyHost() != null && clientConfig.getProxyPort() !=
                        null) {
                    clientBuilder.withClientName("Proxy-DiscoveryClient-HTTPClient").withProxy(
                            clientConfig.getProxyHost(), clientConfig.getProxyPort(),
                            clientConfig.getProxyUserName(),
                            clientConfig.getProxyPassword());
                } else {
                    clientBuilder.withClientName("DiscoveryClient-HTTPClient");
                }
                discoveryJerseyClient = clientBuilder.build();
            }

            discoveryApacheClient = discoveryJerseyClient.getClient();

            remoteRegionsToFetch = new AtomicReference<String>(clientConfig
                    .fetchRegistryForRemoteRegions());
            remoteRegionsRef = new AtomicReference<>(
                    remoteRegionsToFetch.get() == null ? null : remoteRegionsToFetch.get().split
                            (","));

            AzToRegionMapper azToRegionMapper;
            if (clientConfig.shouldUseDnsForFetchingServiceUrls()) {
                azToRegionMapper = new DNSBasedAzToRegionMapper(clientConfig);
            } else {
                azToRegionMapper = new PropertyBasedAzToRegionMapper(clientConfig);
            }
            if (null != remoteRegionsToFetch.get()) {
                azToRegionMapper.setRegionsToFetch(remoteRegionsToFetch.get().split(","));
            }

            instanceRegionChecker = new InstanceRegionChecker(azToRegionMapper, clientConfig
                    .getRegion());

            boolean enableGZIPContentEncodingFilter = config.shouldGZipContent();
            // should we enable GZip decoding of responses based on Response
            // Headers?
            if (enableGZIPContentEncodingFilter) {
                // compressed only if there exists a 'Content-Encoding' header
                // whose value is "gzip"
                discoveryApacheClient.addFilter(new GZIPContentEncodingFilter(false));
            }

            // always enable client identity headers
            String ip = instanceInfo == null ? null : instanceInfo.getIPAddr();
            EurekaClientIdentity identity = new EurekaClientIdentity(ip);
            discoveryApacheClient.addFilter(new EurekaIdentityHeaderFilter(identity));

            // ----- by guo.guanfei, BEGIN

            // add additional ClientFilters if specified
            // if (args != null && args.additionalFilters != null) {
            // for (ClientFilter filter : args.additionalFilters) {
            // discoveryApacheClient.addFilter(filter);
            // }
            // }

            // 因为args.additionalFilters为私有属性，所以通过反射来获取之；
            // this.additionalFilters 为 args.additionalFilters的句柄
            if (args != null && this.additionalFilters != null) {
                this.additionalFilters.forEach(discoveryApacheClient::addFilter);
            }

            // ----- by guo.guanfei, END
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JyallDiscoveryClient!", e);
        }

        if (clientConfig.shouldFetchRegistry() && !fetchRegistry(false)) {
            fetchRegistryFromBackup();
        }

        initScheduledTasks();
        try {
            Monitors.registerObject(this);
        } catch (Exception e) {
            logger.warn("Cannot register timers", e);
        }
        this.heartbeatStalenessMonitor = new ThresholdLevelsMetric(this,
                METRIC_REGISTRATION_PREFIX + "lastHeartbeatSec_", new long[]{15L, 30L, 60L, 120L,
                240L, 480L});
        this.registryStalenessMonitor = new ThresholdLevelsMetric(this, METRIC_REGISTRY_PREFIX +
                "lastUpdateSec_",
                new long[]{15L, 30L, 60L, 120L, 240L, 480L});

//        initTimestampMs = System.currentTimeMillis();
    }

    private void scheduleServerEndpointTask(EurekaTransport eurekaTransport, String zone) {

        // old method (just from dns)
        eurekaServiceUrls.set(timedGetDiscoveryServiceUrls(zone));
        scheduler.scheduleWithFixedDelay(getServiceUrlUpdateTask(zone),
                clientConfig.getEurekaServiceUrlPollIntervalSeconds(),
                clientConfig.getEurekaServiceUrlPollIntervalSeconds(), TimeUnit.SECONDS);

        eurekaTransport.bootstrapResolver = EurekaHttpClients.newBootstrapResolver(clientConfig,
                applicationInfoManager.getInfo());

        eurekaTransport.transportClientFactory = EurekaHttpClients.newTransportClientFactory
                (clientConfig,
                        applicationInfoManager.getInfo());

        // ----- by guo.guanfei
        // if (clientConfig.shouldRegisterWithEureka()) {
        // 在这儿，Controller必须注册
//        if (true) {
        EurekaHttpClientFactory newRegistrationClientFactory = null;
        EurekaHttpClient newRegistrationClient = null;
        try {
            newRegistrationClientFactory = EurekaHttpClients.registrationClientFactory(
                    eurekaTransport.bootstrapResolver, eurekaTransport
                            .transportClientFactory, transportConfig);
            newRegistrationClient = newRegistrationClientFactory.newClient();
        } catch (Exception e) {
            logger.warn("Experimental transport initialization failure", e);
        }
        eurekaTransport.registrationClientFactory = newRegistrationClientFactory;
        eurekaTransport.registrationClient = newRegistrationClient;
//        }

        // new method (resolve from primary servers for read)
        // Configure new transport layer (candidate for injecting in the future)
        if (clientConfig.shouldFetchRegistry()) {
            EurekaHttpClientFactory newQueryClientFactory = null;
            EurekaHttpClient newQueryClient = null;
            try {
                newQueryClientFactory = EurekaHttpClients.queryClientFactory(eurekaTransport
                                .bootstrapResolver,
                        eurekaTransport.transportClientFactory, clientConfig, transportConfig,
                        applicationInfoManager.getInfo(), (stalenessThreshold, timeUnit) -> {
                            long thresholdInMs = TimeUnit.MILLISECONDS.convert
                                    (stalenessThreshold, timeUnit);
                            long delay = getLastSuccessfulRegistryFetchTimePeriod();
                            if (delay > thresholdInMs) {
                                logger.info("Local registry is too stale for local lookup. " +
                                                "Threshold:{}, actual:{}",
                                        thresholdInMs, delay);
                                return null;
                            } else {
                                return localRegionApps.get();
                            }
                        });
                newQueryClient = newQueryClientFactory.newClient();
            } catch (Exception e) {
                logger.warn("Experimental transport initialization failure", e);
            }
            eurekaTransport.queryClientFactory = newQueryClientFactory;
            eurekaTransport.queryClient = newQueryClient;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.shared.LookupService#getApplication(java.lang.
     * String)
     */
    @Override
    public Application getApplication(String appName) {
        return getApplications().getRegisteredApplications(appName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.shared.LookupService#getApplications()
     */
    @Override
    public Applications getApplications() {
        return localRegionApps.get();
    }

    @Override
    public Applications getApplicationsForARegion(@Nullable String region) {
        if (instanceRegionChecker.isLocalRegion(region)) {
            return localRegionApps.get();
        } else {
            return remoteRegionVsApps.get(region);
        }
    }

    @Override
    public Set<String> getAllKnownRegions() {
        String localRegion = instanceRegionChecker.getLocalRegion();
        if (!remoteRegionVsApps.isEmpty()) {
            Set<String> regions = remoteRegionVsApps.keySet();
            Set<String> toReturn = new HashSet<String>(regions);
            toReturn.add(localRegion);
            return toReturn;
        } else {
            return Collections.singleton(localRegion);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.shared.LookupService#getInstancesById(java.lang.
     * String)
     */
    @Override
    public List<InstanceInfo> getInstancesById(String id) {
        List<InstanceInfo> instancesList = new ArrayList<InstanceInfo>();
        for (Application app : this.getApplications().getRegisteredApplications()) {
            InstanceInfo info = app.getByInstanceId(id);
            if (info != null) {
                instancesList.add(info);
            }
        }
        return instancesList;
    }

    /**
     * Register {@link HealthCheckCallback} with the eureka client.
     * <p>
     * Once registered, the eureka client will invoke the
     * {@link HealthCheckCallback} in intervals specified by
     * {@link EurekaClientConfig#getInstanceInfoReplicationIntervalSeconds()}.
     *
     * @param callback app specific healthcheck.
     * @deprecated Use
     */
    @Deprecated
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
        }
    }

    /**
     * Gets the list of instances matching the given VIP Address.
     *
     * @param vipAddress - The VIP address to match the instances for.
     * @param secure     - true if it is a secure vip address, false otherwise
     * @return - The list of {@link InstanceInfo} objects matching the criteria
     */
    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure) {
        return getInstancesByVipAddress(vipAddress, secure, instanceRegionChecker.getLocalRegion());
    }

    /**
     * Gets the list of instances matching the given VIP Address in the passed
     * region.
     *
     * @param vipAddress - The VIP address to match the instances for.
     * @param secure     - true if it is a secure vip address, false otherwise
     * @param region     - region from which the instances are to be fetched. If
     *                   <code>null</code> then local region is assumed.
     * @return - The list of {@link InstanceInfo} objects matching the criteria,
     * empty list if not instances found.
     */
    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure,
                                                       @Nullable String region) {
        if (vipAddress == null) {
            throw new IllegalArgumentException("Supplied VIP Address cannot be null");
        }
        Applications applications;
        if (instanceRegionChecker.isLocalRegion(region)) {
            applications = this.localRegionApps.get();
        } else {
            applications = remoteRegionVsApps.get(region);
            if (null == applications) {
                logger.debug("No applications are defined for region {}, so returning an empty " +
                        "instance list for vip "
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

    /**
     * Gets the list of instances matching the given VIP Address and the given
     * application name if both of them are not null. If one of them is null,
     * then that criterion is completely ignored for matching instances.
     *
     * @param vipAddress - The VIP address to match the instances for.
     * @param appName    - The applicationName to match the instances for.
     * @param secure     - true if it is a secure vip address, false otherwise.
     * @return - The list of {@link InstanceInfo} objects matching the criteria.
     */
    @Override
    public List<InstanceInfo> getInstancesByVipAddressAndAppName(String vipAddress, String
            appName, boolean secure) {

        List<InstanceInfo> result = new ArrayList<InstanceInfo>();
        if (vipAddress == null && appName == null) {
            throw new IllegalArgumentException("Supplied VIP Address and application name cannot " +
                    "both be null");
        } else if (vipAddress != null && appName == null) {
            return getInstancesByVipAddress(vipAddress, secure);
        } else if (vipAddress == null /*&& appName != null*/) {
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
                String[] instanceVipAddresses = instanceVipAddress.split(COMMA_STRING);

                // If the VIP Address is delimited by a comma, then consider to
                // be a list of VIP Addresses.
                // Try to match at least one in the list, if it matches then
                // return the instance info for the same
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

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.shared.LookupService#getNextServerFromEureka(java
     * .lang.String, boolean)
     */
    @Override
    public InstanceInfo getNextServerFromEureka(String virtualHostname, boolean secure) {
        List<InstanceInfo> instanceInfoList = this.getInstancesByVipAddress(virtualHostname,
                secure);
        if (instanceInfoList == null || instanceInfoList.isEmpty()) {
            throw new RuntimeException("No matches for the virtual host name :" + virtualHostname);
        }
        Applications apps = this.localRegionApps.get();
        int index = (int) (apps.getNextIndex(virtualHostname.toUpperCase(Locale.ROOT), secure)
                .incrementAndGet()
                % instanceInfoList.size());
        return instanceInfoList.get(index);
    }

    /**
     * Get all applications registered with a specific eureka service.
     *
     * @param serviceUrl - The string representation of the service url.
     * @return - The registry information containing all applications.
     */
    @Override
    public Applications getApplications(String serviceUrl) {
        if (shouldUseExperimentalTransportForQuery()) {
            try {
                EurekaHttpResponse<Applications> response = clientConfig
                        .getRegistryRefreshSingleVipAddress() == null
                        ? eurekaTransport.queryClient.getApplications()
                        : eurekaTransport.queryClient.getVip(clientConfig
                        .getRegistryRefreshSingleVipAddress());
                if (response.getStatusCode() == 200) {
                    logger.debug(PREFIX + appPathIdentifier + " -  refresh status: " + response
                            .getStatusCode());
                    return response.getEntity();
                }
                logger.error(PREFIX + appPathIdentifier + " - was unable to refresh its cache! " +
                        "status = "
                        + response.getStatusCode());
            } catch (Exception th) {
                logger.error(
                        PREFIX + appPathIdentifier + " - was unable to refresh its cache! status " +
                                "= " + th.getMessage(),
                        th);
            }
        } else {
            ClientResponse response = null;
            try {
                response = makeRemoteCall(Action.REFRESH);
                Applications apps = response.getEntity(Applications.class);
                logger.debug(PREFIX + appPathIdentifier + " -  refresh status: " + response
                        .getStatus());
                return apps;
            } catch (Exception th) {
                logger.error(
                        PREFIX + appPathIdentifier + " - was unable to refresh its cache! status " +
                                "= " + th.getMessage(),
                        th);
            } finally {
                closeResponse(response);
            }
        }
        return null;
    }

    /**
     * Checks to see if the eureka client registration is enabled.
     *
     * @param myInfo - The instance info object
     * @return - true, if the instance should be registered with eureka, false
     * otherwise
     */
    private boolean shouldRegister(InstanceInfo myInfo) {
        // ----- by guo.guanfei

        // if (!clientConfig.shouldRegisterWithEureka()) {
        // return false;
        // }

        // Controller必须注册
        return true;
    }

    /**
     * Register with the eureka service by making the appropriate REST call.
     */
    boolean register() throws Exception {
        logger.info(PREFIX + appPathIdentifier + ": registering service...");
        if (shouldUseExperimentalTransportForRegistration()) {
            EurekaHttpResponse<Void> httpResponse;
            try {
                httpResponse = eurekaTransport.registrationClient.register(instanceInfo);
            } catch (Exception e) {
                logger.warn("{} - registration failed {}", PREFIX + appPathIdentifier, e
                        .getMessage(), e);
                throw e;
            }
            isRegisteredWithDiscovery = true;
            if (logger.isInfoEnabled()) {
                logger.info("{} - registration status: {}", PREFIX + appPathIdentifier,
                        httpResponse.getStatusCode());
            }
            return httpResponse.getStatusCode() == 204;
        } else {
            ClientResponse response = null;
            try {
                response = makeRemoteCall(Action.REGISTER);
                isRegisteredWithDiscovery = true;
                logger.info("{} - registration status: {}", PREFIX + appPathIdentifier,
                        response != null ? response.getStatus() : "not sent");
                return response != null && response.getStatus() == 204;
            } catch (Exception e) {
                logger.warn("{} - registration failed {}", PREFIX + appPathIdentifier, e
                        .getMessage(), e);
                throw e;
            } finally {
                closeResponse(response);
            }
        }
    }

    /**
     * RENEW with the eureka service by making the appropriate REST call
     */
    boolean renew() {
        if (shouldUseExperimentalTransportForRegistration()) {
            try {
                EurekaHttpResponse<InstanceInfo> httpResponse = eurekaTransport
                        .registrationClient.sendHeartBeat(instanceInfo.getAppName(),
                        instanceInfo.getId(), instanceInfo, null);
                logger.debug("{} - Heartbeat status: {}", PREFIX + appPathIdentifier,
                        httpResponse.getStatusCode());
                if (httpResponse.getStatusCode() == 404) {
                    reregisterCounter.increment();
                    logger.info("{} - Re-registering apps/{}", PREFIX + appPathIdentifier,
                            instanceInfo.getAppName());
                    return register();
                }
                return httpResponse.getStatusCode() == 200;
            } catch (Exception e) {
                logger.error("{} - was unable to send heartbeat!", PREFIX + appPathIdentifier, e);
                return false;
            }
        } else {
            ClientResponse response = null;
            try {
                response = makeRemoteCall(Action.RENEW);
                logger.debug("{} - Heartbeat status: {}", PREFIX + appPathIdentifier,
                        response != null ? response.getStatus() : "not sent");
                if (response == null) {
                    return false;
                }
                if (response.getStatus() == 404) {
                    reregisterCounter.increment();
                    logger.info("{} - Re-registering apps/{}", PREFIX + appPathIdentifier,
                            instanceInfo.getAppName());
                    return register();
                }
            } catch (Exception e) {
                logger.error("{} - was unable to send heartbeat!", PREFIX + appPathIdentifier, e);
                return false;
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            return true;
        }
    }

    /**
     * @param instanceZone   The zone in which the client resides
     * @param preferSameZone true if we have to prefer the same zone as the client, false
     *                       otherwise
     * @return The list of all eureka service urls for the eureka client to talk
     * to
     * @deprecated see replacement in
     * {@link com.netflix.discovery.endpoint.EndpointUtils}
     * <p>
     * Get the list of all eureka service urls from properties file
     * for the eureka client to talk to.
     */
    @Deprecated
    @Override
    public List<String> getServiceUrlsFromConfig(String instanceZone, boolean preferSameZone) {
        return EndpointUtils.getServiceUrlsFromConfig(clientConfig, instanceZone, preferSameZone);
    }

    /**
     * Shuts down Eureka Client. Also sends a deregistration request to the
     * eureka server.
     */
    @PreDestroy
    @Override
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            if (statusChangeListener != null && applicationInfoManager != null) {
                applicationInfoManager.unregisterStatusChangeListener(statusChangeListener.getId());
            }

            cancelScheduledTasks();

            // If APPINFO was registered
            if (instanceInfo != null && shouldRegister(instanceInfo)) {
                instanceInfo.setStatus(InstanceStatus.DOWN);
                unregister();
            }

            if (discoveryJerseyClient != null) {
                discoveryJerseyClient.destroyResources();
            }

            eurekaTransport.shutdown();

            heartbeatStalenessMonitor.shutdown();
            registryStalenessMonitor.shutdown();
        }
    }

    /**
     * unregister w/ the eureka service.
     */
    void unregister() {
        if (shouldUseExperimentalTransportForRegistration()) {
            try {
                EurekaHttpResponse<Void> httpResponse = eurekaTransport.registrationClient
                        .cancel(instanceInfo.getAppName(), instanceInfo.getId());
                logger.info(PREFIX + appPathIdentifier + " - deregister  status: " + httpResponse
                        .getStatusCode());
            } catch (Exception e) {
                logger.error(PREFIX + appPathIdentifier + " - de-registration failed" + e
                        .getMessage(), e);
            }
        } else {
            ClientResponse response = null;
            try {
                response = makeRemoteCall(Action.CANCEL);
                logger.info(PREFIX + appPathIdentifier + " - deregister  status: "
                        + (response != null ? response.getStatus() : "not registered"));
            } catch (Exception e) {
                logger.error(PREFIX + appPathIdentifier + " - de-registration failed" + e
                        .getMessage(), e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
    }

    /**
     * Fetches the registry information.
     * <p>
     * <p>
     * This method tries to get only deltas after the first fetch unless there
     * is an issue in reconciling eureka server and client registry information.
     * </p>
     *
     * @param forceFullRegistryFetch Forces a full registry fetch.
     * @return true if the registry was fetched
     */
    private boolean fetchRegistry(boolean forceFullRegistryFetch) {
        Stopwatch tracer = fetchRegistryTimer.start();

        try {
            // If the delta is disabled or if it is the first time, get all
            // applications
            Applications applications = getApplications();

            // Client application does not have latest
            // library supporting delta
            if (clientConfig.shouldDisableDelta()
                    || (!Strings.isNullOrEmpty(clientConfig.getRegistryRefreshSingleVipAddress())
                    || forceFullRegistryFetch)
                    || (applications == null
                    || applications.getRegisteredApplications().isEmpty()
                    || applications.getVersion() == -1)) {
                logger.info("Disable delta property : {}", clientConfig.shouldDisableDelta());
                logger.info("Single vip registry refresh property : {}",
                        clientConfig.getRegistryRefreshSingleVipAddress());
                logger.info("Force full registry fetch : {}", forceFullRegistryFetch);
                logger.info("Application is null : {}", applications == null);
                logger.info("Registered Applications size is zero : {}",
                        applications == null || applications.getRegisteredApplications().isEmpty());
                logger.info("Application version is -1: {}", applications == null || applications
                        .getVersion() == -1);
                getAndStoreFullRegistry();
            } else {
                getAndUpdateDelta(applications);
            }
            if (applications != null) {
                applications.setAppsHashCode(applications.getReconcileHashCode());
            }
            logTotalInstances();
        } catch (Exception e) {
            logger.error(PREFIX + appPathIdentifier + " - was unable to refresh its cache! status" +
                            " = " + e.getMessage(),
                    e);
            return false;
        } finally {
            if (tracer != null) {
                tracer.stop();
            }
        }

        // Notify about cache refresh before updating the instance remote status
        onCacheRefreshed();

        // Update remote status based on refreshed data held in the cache
        updateInstanceRemoteStatus();

        // registry was fetched successfully, so return true
        return true;
    }

    private synchronized void updateInstanceRemoteStatus() {
        // Determine this instance's status for this app and set to UNKNOWN if
        // not found
        InstanceInfo.InstanceStatus currentRemoteInstanceStatus = null;
        if (instanceInfo.getAppName() != null) {
            Application app = getApplication(instanceInfo.getAppName());
            if (app != null) {
                InstanceInfo remoteInstanceInfo = app.getByInstanceId(instanceInfo.getId());
                if (remoteInstanceInfo != null) {
                    currentRemoteInstanceStatus = remoteInstanceInfo.getStatus();
                }
            }
        }
        if (currentRemoteInstanceStatus == null) {
            currentRemoteInstanceStatus = InstanceInfo.InstanceStatus.UNKNOWN;
        }

        // Notify if status changed
        if (lastRemoteInstanceStatus != currentRemoteInstanceStatus) {
            onRemoteStatusChanged(lastRemoteInstanceStatus, currentRemoteInstanceStatus);
            lastRemoteInstanceStatus = currentRemoteInstanceStatus;
        }
    }

    /**
     * @return Return he current instance status as seen on the Eureka server.
     */
    @Override
    public InstanceInfo.InstanceStatus getInstanceRemoteStatus() {
        return lastRemoteInstanceStatus;
    }

    private String getReconcileHashCode(Applications applications) {
        TreeMap<String, AtomicInteger> instanceCountMap = new TreeMap<String, AtomicInteger>();
        if (isFetchingRemoteRegionRegistries()) {
            for (Applications remoteApp : remoteRegionVsApps.values()) {
                remoteApp.populateInstanceCountMap(instanceCountMap);
            }
        }
        applications.populateInstanceCountMap(instanceCountMap);
        return Applications.getReconcileHashCode(instanceCountMap);
    }

    /**
     * Gets the full registry information from the eureka server and stores it
     * locally. When applying the full registry, the following flow is observed:
     * <p>
     * if (update generation have not advanced (due to another thread))
     * atomically set the registry to the new registry fi
     *
     * @return the full registry information.
     * @throws Exception on error.
     */
    private void getAndStoreFullRegistry() throws Exception {
        long currentUpdateGeneration = fetchRegistryGeneration.get();

        logger.info("Getting all instance registry info from the eureka server");

        Applications apps = null;
        if (shouldUseExperimentalTransportForQuery()) {
            EurekaHttpResponse<Applications> httpResponse = clientConfig
                    .getRegistryRefreshSingleVipAddress() == null
                    ? eurekaTransport.queryClient.getApplications(remoteRegionsRef.get())
                    : eurekaTransport.queryClient.getVip(clientConfig
                            .getRegistryRefreshSingleVipAddress(),
                    remoteRegionsRef.get());
            if (httpResponse.getStatusCode() == Status.OK.getStatusCode()) {
                apps = httpResponse.getEntity();
            }
            logger.info("The response status is {}", httpResponse.getStatusCode());
        } else {
            ClientResponse response = makeRemoteCall(Action.REFRESH);
            try {
                if (response.getStatus() == Status.OK.getStatusCode()) {
                    apps = response.getEntity(Applications.class);
                }
                logger.info("The response status is {}", response.getStatus());
            } finally {
                closeResponse(response);
            }
        }

        if (apps == null) {
            logger.error("The application is null for some reason. Not storing this information");
        } else if (fetchRegistryGeneration.compareAndSet(currentUpdateGeneration,
                currentUpdateGeneration + 1)) {
            localRegionApps.set(this.filterAndShuffle(apps));
            logger.debug("Got full registry with apps hashcode {}", apps.getAppsHashCode());
        } else {
            logger.warn("Not updating applications as another thread is updating it already");
        }
    }

    /**
     * Get the delta registry information from the eureka server and update it
     * locally. When applying the delta, the following flow is observed:
     * <p>
     * if (update generation have not advanced (due to another thread))
     * atomically try to: update application with the delta and get
     * reconcileHashCode abort entire processing otherwise do reconciliation if
     * reconcileHashCode clash fi
     *
     * @return the client response
     * @throws Exception on error
     */
    private void getAndUpdateDelta(Applications applications) throws Exception {
        long currentUpdateGeneration = fetchRegistryGeneration.get();

        Applications delta = null;
        if (shouldUseExperimentalTransportForQuery()) {
            EurekaHttpResponse<Applications> httpResponse = eurekaTransport.queryClient
                    .getDelta(remoteRegionsRef.get());
            if (httpResponse.getStatusCode() == Status.OK.getStatusCode()) {
                delta = httpResponse.getEntity();
            }
        } else {
            ClientResponse response = makeRemoteCall(Action.REFRESH_DELTA);
            try {
                if (response.getStatus() == Status.OK.getStatusCode()) {
                    delta = response.getEntity(Applications.class);
                }
            } finally {
                closeResponse(response);
            }
        }

        if (delta == null) {
            logger.warn("The server does not allow the delta revision to be applied because it is" +
                    " not safe. "
                    + "Hence got the full registry.");
            getAndStoreFullRegistry();
        } else if (fetchRegistryGeneration.compareAndSet(currentUpdateGeneration,
                currentUpdateGeneration + 1)) {
            logger.debug("Got delta update with apps hashcode {}", delta.getAppsHashCode());
            String reconcileHashCode = "";
            if (fetchRegistryUpdateLock.tryLock()) {
                try {
                    updateDelta(delta);
                    reconcileHashCode = getReconcileHashCode(applications);
                } finally {
                    fetchRegistryUpdateLock.unlock();
                }
            } else {
                logger.warn("Cannot acquire update lock, aborting getAndUpdateDelta");
            }
            // There is a diff in number of instances for some reason
            if (!reconcileHashCode.equals(delta.getAppsHashCode()) || clientConfig
                    .shouldLogDeltaDiff()) {
                // makes a remoteCall
                reconcileAndLogDifference(delta, reconcileHashCode);
            }
        } else {
            logger.warn("Not updating application delta as another thread is updating it already");
            logger.debug("Ignoring delta update with apps hashcode {}, as another thread is " +
                            "updating it already",
                    delta.getAppsHashCode());
        }
    }

    /**
     * Logs the total number of non-filtered instances stored locally.
     */
    private void logTotalInstances() {
        if (logger.isDebugEnabled()) {
            int totInstances = 0;
            for (Application application : getApplications().getRegisteredApplications()) {
                totInstances += application.getInstancesAsIsFromEureka().size();
            }
            logger.debug("The total number of all instances in the client now is {}", totInstances);
        }
    }

    /**
     * Reconcile the eureka server and client registry information and logs the
     * differences if any. When reconciling, the following flow is observed:
     * <p>
     * make a remote call to the server for the full registry calculate and log
     * differences if (update generation have not advanced (due to another
     * thread)) atomically set the registry to the new registry fi
     *
     * @param delta             the last delta registry information received from the eureka
     *                          server.
     * @param reconcileHashCode the hashcode generated by the server for reconciliation.
     * @return ClientResponse the HTTP response object.
     * @throws Exception on any error.
     */
    private void reconcileAndLogDifference(Applications delta, String reconcileHashCode) throws
            Exception {
        logger.warn("The Reconcile hashcodes do not match, client : {}, server : {}. Getting the " +
                        "full registry",
                reconcileHashCode, delta.getAppsHashCode());

        reconcileHashCodesMismatch.increment();

        long currentUpdateGeneration = fetchRegistryGeneration.get();

        Applications serverApps = null;
        if (shouldUseExperimentalTransportForQuery()) {
            EurekaHttpResponse<Applications> httpResponse = clientConfig
                    .getRegistryRefreshSingleVipAddress() == null
                    ? eurekaTransport.queryClient.getApplications(remoteRegionsRef.get())
                    : eurekaTransport.queryClient.getVip(clientConfig
                            .getRegistryRefreshSingleVipAddress(),
                    remoteRegionsRef.get());
            serverApps = httpResponse.getEntity();
        } else {
            ClientResponse response = makeRemoteCall(Action.REFRESH);
            try {
                serverApps = response.getEntity(Applications.class);
            } finally {
                closeResponse(response);
            }
        }

        if (serverApps == null) {
            logger.warn("Cannot fetch full registry from the server; reconciliation failure");
            return;
        }

        try {
            Map<String, List<String>> reconcileDiffMap = getApplications().getReconcileMapDiff
                    (serverApps);
            StringBuilder reconcileBuilder = new StringBuilder("");
            for (Map.Entry<String, List<String>> mapEntry : reconcileDiffMap.entrySet()) {
                reconcileBuilder.append(mapEntry.getKey()).append(": ");
                for (String displayString : mapEntry.getValue()) {
                    reconcileBuilder.append(displayString);
                }
                reconcileBuilder.append('\n');
            }
            String reconcileString = reconcileBuilder.toString();
            logger.warn("The reconcile string is {}", reconcileString);
        } catch (Exception e) {
            logger.error("Could not calculate reconcile string ", e);
        }

        if (fetchRegistryGeneration.compareAndSet(currentUpdateGeneration,
                currentUpdateGeneration + 1)) {
            localRegionApps.set(this.filterAndShuffle(serverApps));
            getApplications().setVersion(delta.getVersion());
            logger.warn("The Reconcile hashcodes after complete sync up, client : {}, server : {}.",
                    getApplications().getReconcileHashCode(), delta.getAppsHashCode());
        } else {
            logger.warn("Not setting the applications map as another thread has advanced the " +
                    "update generation");
        }
    }

    /**
     * Updates the delta information fetches from the eureka server into the
     * local cache.
     *
     * @param delta the delta information received from eureka server in the last
     *              poll cycle.
     */
    private void updateDelta(Applications delta) {
        int deltaCount = 0;
        for (Application app : delta.getRegisteredApplications()) {
            for (InstanceInfo instance : app.getInstances()) {
                Applications applications = getApplications();
                String instanceRegion = instanceRegionChecker.getInstanceRegion(instance);
                if (!instanceRegionChecker.isLocalRegion(instanceRegion)) {
                    Applications remoteApps = remoteRegionVsApps.get(instanceRegion);
                    if (null == remoteApps) {
                        remoteApps = new Applications();
                        remoteRegionVsApps.put(instanceRegion, remoteApps);
                    }
                    applications = remoteApps;
                }

                ++deltaCount;
                if (ActionType.ADDED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance
                            .getAppName());
                    if (existingApp == null) {
                        applications.addApplication(app);
                    }
                    logger.debug("Added instance {} to the existing apps in region {}", instance
                                    .getId(),
                            instanceRegion);
                    applications.getRegisteredApplications(instance.getAppName()).addInstance
                            (instance);
                } else if (ActionType.MODIFIED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance
                            .getAppName());
                    if (existingApp == null) {
                        applications.addApplication(app);
                    }
                    logger.debug("Modified instance {} to the existing apps ", instance.getId());

                    applications.getRegisteredApplications(instance.getAppName()).addInstance
                            (instance);

                } else if (ActionType.DELETED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance
                            .getAppName());
                    if (existingApp == null) {
                        applications.addApplication(app);
                    }
                    logger.debug("Deleted instance {} to the existing apps ", instance.getId());
                    applications.getRegisteredApplications(instance.getAppName()).removeInstance
                            (instance);
                }
            }
        }
        logger.debug("The total number of instances fetched by the delta processor : {}",
                deltaCount);

        getApplications().setVersion(delta.getVersion());
        getApplications().shuffleInstances(clientConfig.shouldFilterOnlyUpInstances());

        for (Applications applications : remoteRegionVsApps.values()) {
            applications.setVersion(delta.getVersion());
            applications.shuffleInstances(clientConfig.shouldFilterOnlyUpInstances());
        }
    }

    /**
     * Makes remote calls with the corresponding action(register,renew etc).
     *
     * @param action the action to be performed on eureka server.
     * @return ClientResponse the HTTP response object.
     * @throws Exception on any error.
     */
    private ClientResponse makeRemoteCall(Action action) throws Exception {
        ClientResponse response;
        if (isQueryAction(action)) {
            response = makeRemoteCallToRedirectedServer(lastQueryRedirect, action);
        } else {
            response = makeRemoteCallToRedirectedServer(lastRegisterRedirect, action);
        }
        if (response == null) {
            response = makeRemoteCall(action, 0);
        }
        return response;
    }

    private ClientResponse makeRemoteCallToRedirectedServer(AtomicReference<String> lastRedirect,
                                                            Action action) {
        String lastRedirectUrl = lastRedirect.get();
        if (lastRedirectUrl != null) {
            try {
                ClientResponse clientResponse = makeRemoteCall(action, lastRedirectUrl);
                int status = clientResponse.getStatus();
                if (status >= 200 && status < 300) {
                    return clientResponse;
                }
                serverRetryCounter.increment();
                lastRedirect.compareAndSet(lastRedirectUrl, null);
            } catch (Exception e) {
                logger.warn("Remote call to last redirect address failed; retrying from " +
                        "configured service URL list", e);
                serverRetryCounter.increment();
                lastRedirect.compareAndSet(lastRedirectUrl, null);
            }
        }
        return null;
    }

    private static boolean isQueryAction(Action action) {
        return action == Action.REFRESH || action == Action.REFRESH_DELTA;
    }

    /**
     * Makes remote calls with the corresponding action(register,renew etc).
     *
     * @param action the action to be performed on eureka server.
     *               <p>
     *               Try the fallback servers in case of problems communicating to
     *               the primary one.
     * @return ClientResponse the HTTP response object.
     * @throws Exception on any error.
     */
    private ClientResponse makeRemoteCall(Action action, int serviceUrlIndex) throws Exception {
        String serviceUrl;
        try {
            serviceUrl = eurekaServiceUrls.get().get(serviceUrlIndex);
            return makeRemoteCallWithFollowRedirect(action, serviceUrl);
        } catch (Exception t) {
            int serviceUrlSize = serviceUrlIndex + 1;
            if (eurekaServiceUrls.get().size() > serviceUrlSize) {
                logger.warn("Trying backup: " + eurekaServiceUrls.get().get(serviceUrlSize));
                serverRetryCounter.increment();
                return makeRemoteCall(action, serviceUrlSize);
            } else {
                allServerFailureCount.increment();
                logger.error("Can't contact any eureka nodes - possibly a security group issue?",
                        t);
                throw t;
            }
        }
    }

    private ClientResponse makeRemoteCallWithFollowRedirect(Action action, String serviceUrl)
            throws Exception {
        URI targetUrl = new URI(serviceUrl);
        for (int followRedirectCount = 0; followRedirectCount < MAX_FOLLOWED_REDIRECTS;
             followRedirectCount++) {
            ClientResponse clientResponse = makeRemoteCall(action, targetUrl.toString());
            if (clientResponse != null && clientResponse.getStatus() != 302) {
                if (followRedirectCount > 0) {
                    if (isQueryAction(action)) {
                        lastQueryRedirect.set(targetUrl.toString());
                    } else {
                        lastRegisterRedirect.set(targetUrl.toString());
                    }
                }
                return clientResponse;
            }
            if (clientResponse != null) {
                targetUrl = getRedirectBaseUri(clientResponse.getLocation());
            }
            if (targetUrl == null) {
                throw new IOException("Invalid redirect URL " + clientResponse.getLocation());
            }
        }
        String message = "Follow redirect limit crossed for URI " + serviceUrl;
        logger.warn(message);
        throw new IOException(message);
    }

    private static URI getRedirectBaseUri(URI targetUrl) {
        Matcher pathMatcher = REDIRECT_PATH_REGEX.matcher(targetUrl.getPath());
        if (pathMatcher.matches()) {
            return UriBuilder.fromUri(targetUrl).host(DnsResolver.resolve(targetUrl.getHost()))
                    .replacePath(pathMatcher.group(1)).replaceQuery(null).build();
        }
        logger.warn("Invalid redirect URL {}", targetUrl);
        return null;
    }

    /**
     * Makes remote calls with the corresponding action(register,renew etc).
     *
     * @param action the action to be performed on eureka server.
     * @return ClientResponse the HTTP response object.
     * @throws Exception on any error.
     */
    private ClientResponse makeRemoteCall(Action action, String serviceUrl) throws Exception {
        String urlPath = null;
        Stopwatch tracer = null;
        ClientResponse response = null;
        logger.debug("Discovery Client talking to the server {}, action {}", serviceUrl, action);
        try {
            // If the application is unknown do not register/renew/cancel but
            // refresh
            if (UNKNOWN.equals(instanceInfo.getAppName())
                    && !Action.REFRESH.equals(action)
                    && !Action.REFRESH_DELTA.equals(action)) {
                return null;
            }
            WebResource r = discoveryApacheClient.resource(serviceUrl);
            if (clientConfig.allowRedirects()) {
                r.header(HTTP_X_DISCOVERY_ALLOW_REDIRECT, "true");
            }
            String remoteRegionsToFetchStr;
            switch (action) {
                case RENEW:
                    tracer = renewTimer.start();
                    urlPath = "apps/" + appPathIdentifier;
                    response = r.path(urlPath)
                            .queryParam("status", instanceInfo.getStatus().toString())
                            .queryParam("lastDirtyTimestamp", instanceInfo.getLastDirtyTimestamp()
                                    .toString()).put(ClientResponse.class);
                    break;
                case REFRESH:
                    tracer = refreshTimer.start();
                    final String vipAddress = clientConfig.getRegistryRefreshSingleVipAddress();
                    urlPath = vipAddress == null ? "apps/" : "vips/" + vipAddress;
                    remoteRegionsToFetchStr = remoteRegionsToFetch.get();
                    if (!Strings.isNullOrEmpty(remoteRegionsToFetchStr)) {
                        urlPath += "?regions=" + remoteRegionsToFetchStr;
                    }
                    response = getUrl(serviceUrl + urlPath);
                    break;
                case REFRESH_DELTA:
                    tracer = refreshDeltaTimer.start();
                    urlPath = "apps/delta";
                    remoteRegionsToFetchStr = remoteRegionsToFetch.get();
                    if (!Strings.isNullOrEmpty(remoteRegionsToFetchStr)) {
                        urlPath += "?regions=" + remoteRegionsToFetchStr;
                    }
                    response = getUrl(serviceUrl + urlPath);
                    break;
                case REGISTER:
                    tracer = registerTimer.start();
                    urlPath = "apps/" + instanceInfo.getAppName();
                    response = r.path(urlPath).type(MediaType.APPLICATION_JSON_TYPE)
                            .post(ClientResponse.class, instanceInfo);
                    break;
                case CANCEL:
                    tracer = cancelTimer.start();
                    urlPath = "apps/" + appPathIdentifier;
                    response = r.path(urlPath).delete(ClientResponse.class);
                    // Return without during de-registration if it is not registered
                    // already and if we get a 404
                    if ((!isRegisteredWithDiscovery) && (response != null && response.getStatus()
                            == Status.NOT_FOUND.getStatusCode())) {
                        return response;
                    }
                    break;
                default:
            }
            if (response == null) {
                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Finished a call to service url {} and url path {} with status code " +
                                "{}.",
                        serviceUrl, urlPath, String.valueOf(response.getStatus()));
            }
            if (isOk(action, response.getStatus())) {
                return response;
            } else {
                logger.warn("Action: " + action + "  => returned status of " + response.getStatus
                        () + " from "
                        + serviceUrl + urlPath);
                throw new RuntimeException("Bad status: " + response.getStatus());
            }
        } catch (RuntimeException t) {
            closeResponse(response);
            logger.warn("Can't get a response from " + serviceUrl + urlPath, t);
            throw t;
        } finally {
            if (tracer != null) {
                tracer.stop();
            }
        }
    }

    /**
     * Close HTTP response object and its respective resources.
     *
     * @param response the HttpResponse object.
     */
    private void closeResponse(ClientResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (Exception th) {
                logger.error("Cannot release response resource :", th);
            }
        }
    }

    /**
     * Initializes all scheduled tasks.
     */
    private void initScheduledTasks() {
        if (clientConfig.shouldFetchRegistry()) {
            // registry cache refresh timer
            int registryFetchIntervalSeconds = clientConfig.getRegistryFetchIntervalSeconds();
            int expBackOffBound = clientConfig.getCacheRefreshExecutorExponentialBackOffBound();
            scheduler.schedule(
                    new TimedSupervisorTask("cacheRefresh", scheduler, cacheRefreshExecutor,
                            registryFetchIntervalSeconds, TimeUnit.SECONDS, expBackOffBound, new
                            CacheRefreshThread()),
                    registryFetchIntervalSeconds, TimeUnit.SECONDS);
        }

        if (shouldRegister(instanceInfo)) {
            int renewalIntervalInSecs = instanceInfo.getLeaseInfo().getRenewalIntervalInSecs();
            int expBackOffBound = clientConfig.getHeartbeatExecutorExponentialBackOffBound();
            logger.info("Starting heartbeat executor: " + "renew interval is: " +
                    renewalIntervalInSecs);

            // Heartbeat timer
            scheduler.schedule(
                    new TimedSupervisorTask("heartbeat", scheduler, heartbeatExecutor,
                            renewalIntervalInSecs,
                            TimeUnit.SECONDS, expBackOffBound, new HeartbeatThread()),
                    renewalIntervalInSecs, TimeUnit.SECONDS);

            // InstanceInfo replicator
            instanceInfoReplicator = new JyallInstanceInfoReplicator(this, instanceInfo,
                    clientConfig.getInstanceInfoReplicationIntervalSeconds(), 2); // burstSize

            statusChangeListener = new ApplicationInfoManager.StatusChangeListener() {
                @Override
                public String getId() {
                    return "statusChangeListener";
                }

                @Override
                public void notify(StatusChangeEvent statusChangeEvent) {
                    if (InstanceStatus.DOWN == statusChangeEvent.getStatus()
                            || InstanceStatus.DOWN == statusChangeEvent.getPreviousStatus()) {
                        // log at warn level if DOWN was involved
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

            instanceInfoReplicator.start(clientConfig
                    .getInitialInstanceInfoReplicationIntervalSeconds());
        } else {
            logger.info("Not registering with Eureka server per configuration");
        }
    }

    private void cancelScheduledTasks() {
        if (instanceInfoReplicator != null) {
            instanceInfoReplicator.stop();
        }
        heartbeatExecutor.shutdownNow();
        cacheRefreshExecutor.shutdownNow();
        scheduler.shutdownNow();
    }

    /**
     * @param instanceZone   The zone in which the client resides.
     * @param preferSameZone true if we have to prefer the same zone as the client, false
     *                       otherwise.
     * @return The list of all eureka service urls for the eureka client to talk
     * to.
     * @deprecated see replacement in
     * {@link com.netflix.discovery.endpoint.EndpointUtils}
     * <p>
     * Get the list of all eureka service urls from DNS for the
     * eureka client to talk to. The client picks up the service url
     * from its zone and then fails over to other zones randomly. If
     * there are multiple servers in the same zone, the client once
     * again picks one randomly. This way the traffic will be
     * distributed in the case of failures.
     */
    @Deprecated
    @Override
    public List<String> getServiceUrlsFromDNS(String instanceZone, boolean preferSameZone) {
        return EndpointUtils.getServiceUrlsFromDNS(clientConfig, instanceZone, preferSameZone,
                urlRandomizer);
    }

    /**
     * @deprecated see replacement in
     * {@link com.netflix.discovery.endpoint.EndpointUtils}
     */
    @Deprecated
    @Override
    public List<String> getDiscoveryServiceUrls(String zone) {
        // ----- by guo.guanfei
        // return EndpointUtils.getDiscoveryServiceUrls(clientConfig, zone,
        // urlRandomizer);
        return timedGetDiscoveryServiceUrls(zone);

    }

    private List<String> timedGetDiscoveryServiceUrls(String zone) {
        boolean shouldUseDns = clientConfig.shouldUseDnsForFetchingServiceUrls();
        if (shouldUseDns) {
            Stopwatch t = getServiceUrlsDnsTimer.start();
            List<String> result = EndpointUtils.getServiceUrlsFromDNS(clientConfig, zone,
                    clientConfig.shouldPreferSameZoneEureka(), urlRandomizer);
            t.stop();
            return result;
        }

        // ----- by guo.guanfei, BEGIN
        if (null != ctrllerRegistryUrls && ctrllerRegistryUrls.length() > 0) {
            return Arrays.asList(ctrllerRegistryUrls.split(","));
        }
        return new ArrayList<>();
        // ----- by guo.guanfei, END
    }

    /**
     * @param dnsName The dns name of the zone-specific CNAME
     * @param type    CNAME or EIP that needs to be retrieved
     * @return The list of EC2 URLs associated with the dns name
     * @deprecated see replacement in
     * {@link com.netflix.discovery.endpoint.EndpointUtils}
     * <p>
     * Get the list of EC2 URLs given the zone name.
     */
    @Deprecated
    public static Set<String> getEC2DiscoveryUrlsFromZone(String dnsName, EndpointUtils
            .DiscoveryUrlType type) {
        return EndpointUtils.getEC2DiscoveryUrlsFromZone(dnsName, type);
    }

    /**
     * Check if the http status code is a success for the given action.
     */
    private boolean isOk(Action action, int httpStatus) {
//        if (httpStatus >= 200 && httpStatus < 300 || httpStatus == 302) {
//            return true;
//        } else if (Action.RENEW == action && httpStatus == 404) {
//            return true;
//        } else if (Action.REFRESH_DELTA == action && (httpStatus == 403 || httpStatus == 404)) {
//            return true;
//        } else {
//            return false;
//        }
        return httpStatus >= 200 && httpStatus < 300
                || httpStatus == 302
                || Action.RENEW == action && httpStatus == 404
                || Action.REFRESH_DELTA == action && (httpStatus == 403 || httpStatus == 404);
    }

    /**
     * Returns the eureka server which this eureka client communicates with.
     *
     * @return - The instance information that describes the eureka server.
     */
    @SuppressWarnings("unused")
//    private InstanceInfo getCoordinatingServer() {
//        Application app = getApplication(DISCOVERY_APPID);
//        List<InstanceInfo> discoveryInstances = null;
//        InstanceInfo instanceToReturn = null;
//
//        if (app != null) {
//            discoveryInstances = app.getInstances();
//        }
//
//        if (discoveryInstances != null) {
//            for (InstanceInfo instance : discoveryInstances) {
//                if ((instance != null) && (instance.isCoordinatingDiscoveryServer())) {
//                    instanceToReturn = instance;
//                    break;
//                }
//            }
//        }
//        return instanceToReturn;
//    }

    private ClientResponse getUrl(String fullServiceUrl) {
        return discoveryApacheClient.resource(fullServiceUrl)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(EurekaAccept.HTTP_X_EUREKA_ACCEPT, clientAccept.name())
                .get(ClientResponse.class);
    }

    /**
     * REFRESH the current local instanceInfo. Note that after a valid refresh
     * where changes are observed, the isDirty flag on the instanceInfo is set
     * to true
     */
    void refreshInstanceInfo() {
        applicationInfoManager.refreshDataCenterInfoIfRequired();
        applicationInfoManager.refreshLeaseInfoIfRequired();

        InstanceStatus status;
        try {
            status = getHealthCheckHandler().getStatus(instanceInfo.getStatus());
        } catch (Exception e) {
            logger.warn("Exception from healthcheckHandler.getStatus, setting status to DOWN", e);
            status = InstanceStatus.DOWN;
        }

        if (null != status) {
            instanceInfo.setStatus(status);
        }
    }

    /**
     * The heartbeat task that renews the lease in the given intervals.
     */
    private class HeartbeatThread implements Runnable {
        @Override
        public void run() {
            if (renew()) {
                lastSuccessfulHeartbeatTimestamp = System.currentTimeMillis();
            }
        }
    }

    @SuppressWarnings("unused")
    @VisibleForTesting
    JyallInstanceInfoReplicator getInstanceInfoReplicator() {
        return instanceInfoReplicator;
    }

    @SuppressWarnings("unused")
    @VisibleForTesting
    InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    @Override
    public HealthCheckHandler getHealthCheckHandler() {

        if (healthCheckHandler == null) {
            if (null != healthCheckHandlerProvider) {
                healthCheckHandler = healthCheckHandlerProvider.get();
            } else if (null != healthCheckCallbackProvider) {
                healthCheckHandler = new HealthCheckCallbackToHandlerBridge
                        (healthCheckCallbackProvider.get());
            }

            if (null == healthCheckHandler) {
                healthCheckHandler = new HealthCheckCallbackToHandlerBridge(null);
            }
        }

        return healthCheckHandler;
    }

    /**
     * The task that fetches the registry information at specified intervals.
     */
    class CacheRefreshThread implements Runnable {
        @Override
        public void run() {
            try {
                boolean isFetchingRemoteRegionRegistries = isFetchingRemoteRegionRegistries();

                boolean remoteRegionsModified = false;
                // This makes sure that a dynamic change to remote regions to
                // fetch is honored.
                String latestRemoteRegions = clientConfig.fetchRegistryForRemoteRegions();
                if (null != latestRemoteRegions) {
                    String currentRemoteRegions = remoteRegionsToFetch.get();
                    if (!latestRemoteRegions.equals(currentRemoteRegions)) {
                        // Both remoteRegionsToFetch and
                        // AzToRegionMapper.regionsToFetch need to be in sync
                        synchronized (instanceRegionChecker.getAzToRegionMapper()) {
                            if (remoteRegionsToFetch.compareAndSet(currentRemoteRegions,
                                    latestRemoteRegions)) {
                                String[] remoteRegions = latestRemoteRegions.split(",");
                                remoteRegionsRef.set(remoteRegions);
                                instanceRegionChecker.getAzToRegionMapper().setRegionsToFetch
                                        (remoteRegions);
                                remoteRegionsModified = true;
                            } else {
                                logger.info(
                                        "Remote regions to fetch modified concurrently,"
                                                + " ignoring change from {} to {}",
                                        currentRemoteRegions, latestRemoteRegions);
                            }
                        }
                    } else {
                        // Just refresh mapping to reflect any DNS/Property
                        // change
                        instanceRegionChecker.getAzToRegionMapper().refreshMapping();
                    }
                }

                boolean success = fetchRegistry(remoteRegionsModified);
                if (success) {
                    registrySize = localRegionApps.get().size();
                    lastSuccessfulRegistryFetchTimestamp = System.currentTimeMillis();
                }

                if (logger.isDebugEnabled()) {
                    StringBuilder allAppsHashCodes = new StringBuilder();
                    allAppsHashCodes.append("Local region apps hashcode: ");
                    allAppsHashCodes.append(localRegionApps.get().getAppsHashCode());
                    allAppsHashCodes.append(", is fetching remote regions? ");
                    allAppsHashCodes.append(isFetchingRemoteRegionRegistries);
                    for (Map.Entry<String, Applications> entry : remoteRegionVsApps.entrySet()) {
                        allAppsHashCodes.append(", Remote region: ");
                        allAppsHashCodes.append(entry.getKey());
                        allAppsHashCodes.append(" , apps hashcode: ");
                        allAppsHashCodes.append(entry.getValue().getAppsHashCode());
                    }
                    logger.debug("Completed cache refresh task for discovery. All Apps hash code " +
                                    "is {} ",
                            allAppsHashCodes.toString());
                }
            } catch (Exception th) {
                logger.error("Cannot fetch registry from server", th);
            }
        }
    }

    /**
     * Fetch the registry information from back up registry if all eureka server
     * urls are unreachable.
     */
    private void fetchRegistryFromBackup() {
        try {
            @SuppressWarnings("deprecation")
            BackupRegistry backupRegistryInstance = newBackupRegistryInstance();
            // backward compatibility with the old protected method, in
            // case it is being used.
            if (null == backupRegistryInstance) {
                backupRegistryInstance = backupRegistryProvider.get();
            }

            if (null != backupRegistryInstance) {
                Applications apps = null;
                if (isFetchingRemoteRegionRegistries()) {
                    String remoteRegionsStr = remoteRegionsToFetch.get();
                    if (null != remoteRegionsStr) {
                        apps = backupRegistryInstance.fetchRegistry(remoteRegionsStr.split(","));
                    }
                } else {
                    apps = backupRegistryInstance.fetchRegistry();
                }
                if (apps != null) {
                    final Applications applications = this.filterAndShuffle(apps);
                    applications.setAppsHashCode(applications.getReconcileHashCode());
                    localRegionApps.set(applications);
                    logTotalInstances();
                    logger.info("Fetched registry successfully from the backup");
                }
            } else {
                logger.warn("No backup registry instance defined & unable to find any discovery " +
                        "servers.");
            }
        } catch (Exception e) {
            logger.warn("Cannot fetch applications from apps although backup registry was " +
                    "specified", e);
        }
    }

    /**
     * @deprecated Use injection to provide {@link BackupRegistry}
     * implementation.
     */
    @Deprecated
    @Nullable
    protected BackupRegistry newBackupRegistryInstance()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return null;
    }

    /**
     * Gets the task that is responsible for fetching the eureka service Urls.
     *
     * @param zone the zone in which the instance resides.
     * @return TimerTask the task which executes periodically.
     */
    private TimerTask getServiceUrlUpdateTask(final String zone) {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    List<String> serviceUrlList = timedGetDiscoveryServiceUrls(zone);
                    if (serviceUrlList == null || serviceUrlList.isEmpty()) {
                        logger.warn("The service url list is empty");
                    } else if (!serviceUrlList.equals(eurekaServiceUrls.get())) {
                        logger.info("Updating the serviceUrls as they seem to have changed from " +
                                        "{} to {}",
                                Arrays.toString(eurekaServiceUrls.get().toArray()),
                                Arrays.toString(serviceUrlList.toArray()));
                        eurekaServiceUrls.set(serviceUrlList);
                    }
                } catch (Exception e) {
                    logger.error("Cannot get the eureka service urls :", e);
                }
            }
        };
    }

    /**
     * Gets the <em>applications</em> after filtering the applications for
     * instances with only UP states and shuffling them.
     * <p>
     * <p>
     * The filtering depends on the option specified by the configuration
     * {@link EurekaClientConfig#shouldFilterOnlyUpInstances()}. Shuffling helps
     * in randomizing the applications list there by avoiding the same instances
     * receiving traffic during start ups.
     * </p>
     *
     * @param apps The applications that needs to be filtered and shuffled.
     * @return The applications after the filter and the shuffle.
     */
    private Applications filterAndShuffle(Applications apps) {
        if (apps != null) {
            if (isFetchingRemoteRegionRegistries()) {
                Map<String, Applications> map = new ConcurrentHashMap<>();
                apps.shuffleAndIndexInstances(map, clientConfig, instanceRegionChecker);
                for (Applications applications : map.values()) {
                    applications.shuffleInstances(clientConfig.shouldFilterOnlyUpInstances());
                }
                this.remoteRegionVsApps = map;
            } else {
                apps.shuffleInstances(clientConfig.shouldFilterOnlyUpInstances());
            }
        }
        return apps;
    }

    private boolean isFetchingRemoteRegionRegistries() {
        return null != remoteRegionsToFetch.get();
    }

    private boolean shouldUseExperimentalTransportForQuery() {
        if (eurekaTransport.queryClient == null) {
            return false;
        }
        String enabled = clientConfig.getExperimental("transport.query.enabled");
        return enabled != null && "true".equalsIgnoreCase(enabled);
    }

    private boolean shouldUseExperimentalTransportForRegistration() {
        if (eurekaTransport.registrationClient == null) {
            return false;
        }
        String enabled = clientConfig.getExperimental("transport.registration.enabled");
        return enabled != null && "true".equalsIgnoreCase(enabled);
    }

    /**
     * Invoked when the remote status of this client has changed. Subclasses may
     * override this method to implement custom behavior if needed.
     *
     * @param oldStatus the previous remote {@link InstanceStatus}
     * @param newStatus the new remote {@link InstanceStatus}
     */
    protected void onRemoteStatusChanged(InstanceInfo.InstanceStatus oldStatus, InstanceInfo
            .InstanceStatus newStatus) {
        fireEvent(new StatusChangeEvent(oldStatus, newStatus));
    }

    /**
     * Invoked every time the local registry cache is refreshed (whether changes
     * have been detected or not).
     * <p>
     * Subclasses may override this method to implement custom behavior if
     * needed.
     */
    protected void onCacheRefreshed() {
        // ----- by guo.guanfei
        // fireEvent(new CacheRefreshedEvent());

        // might be called during construction and will be null
        if (this.cacheRefreshedCount != null) {
            long newCount = this.cacheRefreshedCount.incrementAndGet();
            logger.trace("onCacheRefreshed called with count: " + newCount);
            this.context.publishEvent(new HeartbeatEvent(this, newCount));
        }
    }

    /**
     * Send the given event on the EventBus if one is available
     *
     * @param event the event to send on the eventBus
     */
    protected void fireEvent(DiscoveryEvent event) {
        // Publish event if an EventBus is available
        if (eventBus != null) {
            eventBus.publish(event);
        }
    }

    public long getLastSuccessfulHeartbeatTimePeriod() {
        return lastSuccessfulHeartbeatTimestamp < 0 ? lastSuccessfulHeartbeatTimestamp
                : System.currentTimeMillis() - lastSuccessfulHeartbeatTimestamp;
    }

    public long getLastSuccessfulRegistryFetchTimePeriod() {
        return lastSuccessfulRegistryFetchTimestamp < 0 ? lastSuccessfulRegistryFetchTimestamp
                : System.currentTimeMillis() - lastSuccessfulRegistryFetchTimestamp;
    }

//    @SuppressWarnings("unused")
//    @com.netflix.servo.annotations.Monitor(name = METRIC_REGISTRATION_PREFIX
//            + "lastSuccessfulHeartbeatTimePeriod", description = "How much time has passed from " +
//            "last successful heartbeat", type = DataSourceType.GAUGE)
//    private long getLastSuccessfulHeartbeatTimePeriodInternal() {
//        long delay = getLastSuccessfulHeartbeatTimePeriod();
//        heartbeatStalenessMonitor.update(computeStalenessMonitorDelay(delay));
//        return delay;
//    }

    // for metrics only
//    @SuppressWarnings("unused")
//    @com.netflix.servo.annotations.Monitor(
//            name = METRIC_REGISTRY_PREFIX + "lastSuccessfulRegistryFetchTimePeriod",
//            description = "How much time has passed from last successful local registry update",
//            type = DataSourceType.GAUGE)
//    private long getLastSuccessfulRegistryFetchTimePeriodInternal() {
//        long delay = getLastSuccessfulRegistryFetchTimePeriod();
//        registryStalenessMonitor.update(computeStalenessMonitorDelay(delay));
//        return delay;
//    }

    @SuppressWarnings("unused")
    @com.netflix.servo.annotations.Monitor(name = METRIC_REGISTRY_PREFIX + "localRegistrySize",
            description = "Count of instances in the local registry", type = DataSourceType.GAUGE)
    public int localRegistrySize() {
        return registrySize;
    }

//    private long computeStalenessMonitorDelay(long delay) {
//        return delay < 0 ? System.currentTimeMillis() - initTimestampMs : delay;
//    }
}
