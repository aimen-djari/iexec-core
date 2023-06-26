package com.iexec.core.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BlockchainConnectionHealthIndicator implements HealthIndicator {

    private final Web3jService web3jService;
    /**
     * Interval between 2 requests onto the chain.
     */
    private final Duration pollingInterval;
    /**
     * Number of consecutive failures before declaring this Scheduler is out-of-service.
     */
    private final int outOfServiceThreshold;
    private final ScheduledExecutorService monitoringExecutor;

    /**
     * Current number of consecutive failures.
     */
    private int consecutiveFailures = 0;
    private LocalDateTime firstFailure = null;
    private boolean outOfService = false;

    /**
     * Required for test purposes, can't test lambdas equality.
     */
    final Runnable checkConnectionRunnable = this::checkConnection;
    /**
     * Required for test purposes.
     */
    private final Clock clock;

    @Autowired
    public BlockchainConnectionHealthIndicator(Web3jService web3jService,
                                               ChainConfig chainConfig,
                                               @Value("${chain.health.pollingIntervalInBlocks}") int pollingIntervalInBlocks,
                                               @Value("${chain.health.outOfServiceThreshold}") int outOfServiceThreshold) {
        this(
                web3jService,
                chainConfig,
                pollingIntervalInBlocks,
                outOfServiceThreshold,
                Executors.newSingleThreadScheduledExecutor(),
                Clock.systemDefaultZone()
        );
    }

    BlockchainConnectionHealthIndicator(Web3jService web3jService,
                                        ChainConfig chainConfig,
                                        int pollingIntervalInBlocks,
                                        int outOfServiceThreshold,
                                        ScheduledExecutorService monitoringExecutor,
                                        Clock clock) {
        this.web3jService = web3jService;
        this.pollingInterval = chainConfig.getBlockTime().multipliedBy(pollingIntervalInBlocks);
        this.outOfServiceThreshold = outOfServiceThreshold;
        this.monitoringExecutor = monitoringExecutor;
        this.clock = clock;
    }

    @PostConstruct
    void scheduleMonitoring() {
        monitoringExecutor.scheduleAtFixedRate(checkConnectionRunnable, 0, pollingInterval.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Check blockchain is reachable by retrieving the latest block number.
     * <p>
     * If it isn't, then increment {@link BlockchainConnectionHealthIndicator#consecutiveFailures} counter.
     * If counter reaches {@link BlockchainConnectionHealthIndicator#outOfServiceThreshold},
     * then this Health Indicator becomes {@link Status#OUT_OF_SERVICE}.
     * <p>
     * If blockchain is reachable, then reset the counter.
     * <p>
     * /!\ If the indicator becomes {@code OUT_OF_SERVICE}, then it stays as is until the Scheduler is restarted
     * even if the blockchain is later available again.
     */
    void checkConnection() {
        final long latestBlockNumber = web3jService.getLatestBlockNumber();
        if (latestBlockNumber == 0) {
            connectionFailed();
        } else {
            connectionSucceeded();
        }
    }

    /**
     * Increment the {@link BlockchainConnectionHealthIndicator#consecutiveFailures} counter.
     * <p>
     * If {@link BlockchainConnectionHealthIndicator#outOfServiceThreshold} has been reached,
     * then set {@link BlockchainConnectionHealthIndicator#outOfService} to {@code true}.
     * <p>
     * If first failure, set the {@link BlockchainConnectionHealthIndicator#firstFailure} to current time.
     */
    private void connectionFailed() {
        ++consecutiveFailures;
        if (consecutiveFailures >= outOfServiceThreshold) {
            outOfService = true;
            log.error("Blockchain hasn't been accessed for a long period. " +
                    "This Scheduler is now OUT-OF-SERVICE until it is restarted." +
                    " [unavailabilityPeriod:{}]", pollingInterval.multipliedBy(outOfServiceThreshold));
        } else {
            if (consecutiveFailures == 1) {
                firstFailure = LocalDateTime.now(clock);
            }
            log.warn("Blockchain is unavailable. Will retry connection." +
                            " [unavailabilityPeriod:{}, nextRetry:{}]",
                    pollingInterval.multipliedBy(consecutiveFailures), pollingInterval);
        }
    }

    /**
     * Reset {@link BlockchainConnectionHealthIndicator#consecutiveFailures} to {@code 0}.
     * <p>
     * If never been OUT-OF-SERVICE, then:
     * <ul>
     *     <li>Reset the {@link BlockchainConnectionHealthIndicator#firstFailure} var to {@code null};</li>
     *     <li>Log a "connection restored" message.</li>
     * </ul>
     */
    private void connectionSucceeded() {
        if (!outOfService) {
            if (consecutiveFailures > 0) {
                log.info("Blockchain connection is now restored after a period of unavailability." +
                        " [unavailabilityPeriod:{}]", pollingInterval.multipliedBy(consecutiveFailures));
            }
            firstFailure = null;
        }
        consecutiveFailures = 0;
    }

    @Override
    public Health health() {
        final Health.Builder healthBuilder = outOfService
                ? Health.outOfService()
                : Health.up();

        if (firstFailure != null) {
            healthBuilder.withDetail("firstFailure", firstFailure);
        }

        return healthBuilder
                .withDetail("consecutiveFailures", consecutiveFailures)
                .withDetail("pollingInterval", pollingInterval)
                .withDetail("outOfServiceThreshold", outOfServiceThreshold)
                .build();
    }
}
