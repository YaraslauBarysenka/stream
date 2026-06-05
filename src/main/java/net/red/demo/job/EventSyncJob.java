package net.red.demo.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.distributedjobs.spring.DistributedJob;
import net.red.demo.config.MetricsConfig;
import net.red.demo.service.EventMetricService;
import net.red.demo.service.EventSyncService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSyncJob {
    private final EventSyncService eventSyncService;
    private final EventMetricService eventMetricService;

    @Timed(MetricsConfig.JOB_METRIC_NAME)
    @DistributedJob("StreamEventSyncJob")
    @Scheduled(initialDelayString = "#{eventSyncJobProperties.getInitialDelay}",
            fixedDelayString = "#{eventSyncJobProperties.getFixedDelay}")
    public void process() {
        try {
            log.debug("Sync is started");
            eventSyncService.sync();
            eventMetricService.publishMetrics();
            log.debug("Sync is finished successfully");
        } catch (RuntimeException e) {
            log.error("Sync is failed", e);
        }
    }
}