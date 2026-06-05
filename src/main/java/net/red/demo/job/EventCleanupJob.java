package net.red.demo.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.distributedjobs.spring.DistributedJob;
import net.red.demo.config.MetricsConfig;
import net.red.demo.service.EventCleanupService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventCleanupJob {
    private final EventCleanupService cleanupExpiredEvents;

    @Timed(MetricsConfig.JOB_METRIC_NAME)
    @DistributedJob("InfrontEventCleanupJob")
    @Scheduled(cron = "#{eventCleanupJobProperties.cleanUpCronExpression}")
    public void process() {
        try {
            log.info("Cleanup is started");
            cleanupExpiredEvents.cleanupExpiredEvents();
            log.info("Cleanup is finished successfully");
        } catch (RuntimeException e) {
            log.error("Cleanup is failed", e);
        }
    }
}