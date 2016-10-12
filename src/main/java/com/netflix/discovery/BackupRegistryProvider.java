package com.netflix.discovery;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupRegistryProvider implements Provider<BackupRegistry> {
    private static final Logger logger = LoggerFactory.getLogger(BackupRegistryProvider.class);
    private volatile BackupRegistry backupRegistryInstance;
    EurekaClientConfig config;

    public BackupRegistryProvider(final EurekaClientConfig config) {
        this.config = config;
    }

    @Override
    public synchronized BackupRegistry get() {
        if (backupRegistryInstance == null) {
            String backupRegistryClassName = config.getBackupRegistryImpl();
            if (null != backupRegistryClassName) {
                try {
                    backupRegistryInstance = (BackupRegistry) Class.forName
                            (backupRegistryClassName).newInstance();
                } catch (InstantiationException | IllegalAccessException
                        | ClassNotFoundException e) {
                    logger.error("Error instantiating BackupRegistry.", e);
                }
            }
            logger.warn("Using default backup registry");
            backupRegistryInstance = new NotImplementedRegistryImpl();
        }
        return backupRegistryInstance;
    }
}
