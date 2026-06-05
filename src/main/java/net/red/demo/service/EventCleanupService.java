package net.red.demo.service;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.config.properties.EventCleanupJobProperties;
import net.red.demo.entity.Event_;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCleanupService {
    private final EventService eventService;
    private final EventCleanupJobProperties eventCleanupJobProperties;

    public void cleanupExpiredEvents() {
        Duration expiredDuration = eventCleanupJobProperties.getCleanUpEventExpiredDuration();
        int cleanUpBatchSize = eventCleanupJobProperties.getCleanUpBatchSize();
        var threshold = OffsetDateTime.now().minus(eventCleanupJobProperties.getCleanUpEventExpiredDuration());
        log.info("Cleanup expired events. Threshold: {}, batchSize: {}", threshold, cleanUpBatchSize);
        var page = PageRequest.of(0, cleanUpBatchSize, Sort.by(Event_.ID));
        boolean hasNext;
        do {
            var slice = eventService.findByStartDateLessThan(threshold, page);
            if (slice.hasContent()) {
                eventService.deleteAllInBatch(slice.getContent());
                log.info("Events were deleted after {} days of expiration: {}", expiredDuration.toDays(), slice.getContent().size());
            }
            hasNext = slice.hasNext();
        } while (hasNext);
    }
}