package com.adobe.aam.shredder.ec2.runner;

import com.adobe.aam.shredder.ec2.aws.LifecycleHandler;
import com.adobe.aam.shredder.ec2.aws.LifecycleHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;

public class BlockOnShutdownFailure {

    private static final Logger LOG = LoggerFactory.getLogger(BlockOnShutdownFailure.class);
    private final long maxWaitTimeMs;
    private final LifecycleHandler lifecycleHandler;

    @Inject
    public BlockOnShutdownFailure(@Named("maxWaitTimeOnShutdownFailure") Duration maxWaitTime,
                                  LifecycleHandler lifecycleHandler) {
        this.maxWaitTimeMs = maxWaitTime.toMillis();
        this.lifecycleHandler = lifecycleHandler;
    }

    public void blockOnShutdownFailure(LifecycleHook trigger) {
        if (maxWaitTimeMs <= 0) {
            return;
        }

        LOG.info("Blocking execution for {}ms", maxWaitTimeMs);
        int heartbeatIntervalMs = 60 * 1000;
        long startTimeMs = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTimeMs <= maxWaitTimeMs) {
            lifecycleHandler.sendHeartbeat(trigger);
            try {
                Thread.sleep(heartbeatIntervalMs);
            } catch (InterruptedException e) {
                LOG.error("Block interrupted.", e);
            }
        }
    }
}
