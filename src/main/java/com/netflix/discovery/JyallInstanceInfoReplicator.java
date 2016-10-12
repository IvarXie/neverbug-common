package com.netflix.discovery;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A task for updating and replicating the local instanceinfo to the remote
 * server. Properties of this task are: - configured with a single update thread
 * to guarantee sequential update to the remote server - update tasks can be
 * scheduled on-demand via onDemandUpdate() - task processing is rate limited by
 * burstSize - a new update task is always scheduled automatically after an
 * earlier update task. However if an on-demand task is started, the scheduled
 * automatic update task is discarded (and a new one will be scheduled after the
 * new on-demand update).
 *
 * @author dliu
 */
class JyallInstanceInfoReplicator implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(JyallInstanceInfoReplicator.class);

	private final JyallDiscoveryClient discoveryClient;
	private final InstanceInfo instanceInfo;

	private final int replicationIntervalSeconds;
	private final ScheduledExecutorService scheduler;
	private final AtomicReference<Future> scheduledPeriodicRef;

	private final AtomicBoolean started;
	private final RateLimiter rateLimiter;
	private final int burstSize;
	private final int allowedRatePerMinute;

	JyallInstanceInfoReplicator(JyallDiscoveryClient discoveryClient, InstanceInfo instanceInfo,
			int replicationIntervalSeconds, int burstSize) {
		this.discoveryClient = discoveryClient;
		this.instanceInfo = instanceInfo;
		this.scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
				.setNameFormat("DiscoveryClient-InstanceInfoReplicator-%d").setDaemon(true).build());

		this.scheduledPeriodicRef = new AtomicReference<Future>();

		this.started = new AtomicBoolean(false);
		this.rateLimiter = new RateLimiter(TimeUnit.MINUTES);
		this.replicationIntervalSeconds = replicationIntervalSeconds;
		this.burstSize = burstSize;

		this.allowedRatePerMinute = 60 * this.burstSize / this.replicationIntervalSeconds;
		logger.info("InstanceInfoReplicator onDemand update allowed rate per min is {}", allowedRatePerMinute);
	}

	public void start(int initialDelayMs) {
		if (started.compareAndSet(false, true)) {
			instanceInfo.setIsDirty(); // for initial register
			scheduler.schedule(this, initialDelayMs, TimeUnit.SECONDS);
		}
	}

	public void stop() {
		scheduler.shutdownNow();
		started.set(false);
	}

	public boolean onDemandUpdate() {
		if (rateLimiter.acquire(burstSize, allowedRatePerMinute)) {
			scheduler.submit((Runnable) () -> {
                logger.debug("Executing on-demand update of local InstanceInfo");

                // cancel the latest scheduled update, it will be
                // rescheduled at the end of run()
                Future latestPeriodic = scheduledPeriodicRef.get();
                if (latestPeriodic != null && !latestPeriodic.isDone()) {
                    latestPeriodic.cancel(false);
                }

                JyallInstanceInfoReplicator.this.start(0);
            });
			return true;
		} else {
			logger.warn("Ignoring onDemand update due to rate limiter");
			return false;
		}
	}

	@Override
	public void run() {
		try {
			discoveryClient.refreshInstanceInfo();

			Long dirtyTimestamp = instanceInfo.isDirtyWithTime();
			if (dirtyTimestamp != null) {
				discoveryClient.register();
				instanceInfo.unsetIsDirty(dirtyTimestamp);
			}
		} catch (Exception t) {
			logger.warn("There was a problem with the instance info replicator", t);
		} finally {
			Future next = scheduler.schedule(this, replicationIntervalSeconds, TimeUnit.SECONDS);
			scheduledPeriodicRef.set(next);
		}
	}

}