package net.red.demo.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.distributedjobs.spring.DistributedJob;
import net.red.demo.config.MetricsConfig;
import net.red.demo.config.properties.EventChangeSendJobProperties;
import net.red.demo.service.ChangeSendService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventChangeSendJob {
    private final ChangeSendService changeSendService;
    private final EventChangeSendJobProperties eventChangeSendJobProperties;

    @Timed(MetricsConfig.JOB_METRIC_NAME)
    @DistributedJob("InfrontEventChangeSendJob")
    @Scheduled(initialDelayString = "#{eventChangeSendJobProperties.initialDelay}",
            fixedDelayString = "#{eventChangeSendJobProperties.fixedDelay}")
    public void process() {
        log.debug("Event changes sending is started");
        try {
            changeSendService.send(eventChangeSendJobProperties.getBatchSize());
            log.debug("Event changes sending is finished successfully");
        } catch (RuntimeException ex) {
            log.error("Event changes sending is failed", ex);
        }
    }
}