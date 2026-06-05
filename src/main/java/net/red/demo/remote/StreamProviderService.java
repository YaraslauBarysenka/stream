package net.red.demo.remote;

import static java.text.MessageFormat.format;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.config.MetricsConfig;
import net.red.demo.config.properties.StreamProviderProperties;
import net.red.demo.remote.client.StreamProviderClient;
import net.red.demo.remote.dto.enums.EventState;
import net.red.demo.remote.dto.event.EventRemoteDto;
import net.red.demo.remote.dto.stream.EventStreamRemoteDto;
import net.red.demo.remote.exception.StreamProviderException;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class StreamProviderService {
    public static final String EVENT_DATE_TIME_PATTERN = "dd-MM-yyyy";
    public static final DateTimeFormatter EVENT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(EVENT_DATE_TIME_PATTERN);

    private final StreamProviderClient streamProviderClient;
    private final StreamProviderProperties streamProviderProperties;

    @NotNull
    @Timed(MetricsConfig.APP_METRIC_NAME)
    public List<EventRemoteDto> getAllEventsByEventDateWithStreamsForPendingAndOpenEvent(@NotNull LocalDate eventDate) {
        var events = getEvents(eventDate);

        for (EventRemoteDto remoteEvent : events) {
            substituteTitleIfNeeded(remoteEvent);
            markEventAsClosedIfEventIsPendingAndEstimatedEndTimeIsInPast(remoteEvent);
            if (EventState.isFinishedByStateString(remoteEvent.getEventStatusCode())) {
                // EventStreams are available only for Pending and Open Events, otherwise error may occur
                continue;
            }
            setEventStreamsQuietly(remoteEvent);
        }

        log.debug("All events with streams loaded by eventDate: {}. events: {}", eventDate, events);
        return events;
    }

    private void substituteTitleIfNeeded(EventRemoteDto remoteEvent) {
        var originalTitle = remoteEvent.getTitle();
        var substitutedTitle = streamProviderProperties.getEventTitleSubstitutions()
                .get(originalTitle);
        if (substitutedTitle != null) {
            remoteEvent.setTitle(substitutedTitle);
            log.warn("RemoteEvent[id={}] title substituted {} -> {}",
                    remoteEvent.getEventId(), originalTitle, substitutedTitle);
        }
    }

    private void markEventAsClosedIfEventIsPendingAndEstimatedEndTimeIsInPast(@NotNull EventRemoteDto remoteEvent) {
        if (EventState.isNotPendingByStateString(remoteEvent.getEventStatusCode())) {
            return; // skip if event status is not Pending
        }
        var eventEstimatedEndTime = remoteEvent.getEstimatedEndTime();
        if (eventEstimatedEndTime == null) {
            return; // skip if eventEstimatedEndTime is null
        }

        var now = OffsetDateTime.now(ZoneOffset.UTC);
        // eventEstimatedEndTime < now, eventEstimatedEndTime is in the past
        if (eventEstimatedEndTime.isBefore(now)) {
            remoteEvent.setEventStatusCode(EventState.C.name());
            log.warn("Event is closed: status was pending and estimatedEndTime is in the past. event: {}, now: {}", remoteEvent, now);
        }
    }

    private void setEventStreamsQuietly(@NotNull EventRemoteDto remoteEvent) {
        try {
            List<EventStreamRemoteDto> remoteEventStreams = getEventStreams(remoteEvent.getEventId());
            remoteEvent.setEventStreams(remoteEventStreams);
        } catch (StreamProviderException e) {
            log.error("Could not set EventStreams for remoteEvent: {}", remoteEvent, e);
        }
    }

    @NotNull
    private List<EventRemoteDto> getEvents(@NotNull LocalDate eventDate) {
        try {
            log.trace("Start events loading by eventDate: {}", eventDate);
            var eventRemoteDtoList = streamProviderClient.getEvents(streamProviderProperties.getCustomerUid(),
                    eventDate.format(EVENT_DATE_TIME_FORMATTER)).getEvents();
            log.trace("Finish events loading by eventDate: {}. eventCount: {}", eventDate, eventRemoteDtoList.size());
            return eventRemoteDtoList;
        } catch (Exception e) {
            throw new StreamProviderException(format("Could not load Events by eventDate: {0}", eventDate), e);
        }
    }

    @NotNull
    private List<EventStreamRemoteDto> getEventStreams(@NotBlank String eventId) {
        try {
            log.trace("Start event streams loading by eventId: {}, ", eventId);
            var eventStreamRemoteDtoList = streamProviderClient.getEventStreams(streamProviderProperties.getCustomerUid(), eventId)
                            .getEventStreams();
            log.trace("Finish event streams loading by eventId: {}. eventStreamsCount: {}", eventId, eventStreamRemoteDtoList.size());
            return eventStreamRemoteDtoList;
        } catch (Exception e) {
            throw new StreamProviderException(format("Could not load EventStreams by eventId: {0}", eventId), e);
        }
    }

    @Nullable
    @Timed(MetricsConfig.APP_METRIC_NAME)
    public String getStreamLink(@NotBlank String userId,
                                @NotBlank String userIp,
                                @NotBlank String eventId,
                                @NotBlank String streamName) {
        try {
            log.trace("Start event stream link loading by userId: {}, userIp: {}, eventId: {}, streamName: {}",
                    userId, userIp, eventId, streamName);
            var streamLink = streamProviderClient.getEventStreamLink(streamProviderProperties.getCustomerUid(), userId, userIp,
                    eventId, streamName, streamProviderProperties.getRedirectURL()).getStreamLink();
            log.trace("Finish event stream link loading by userId: {}, eventId: {}, streamName: {}. streamLink: {}",
                    userId, eventId, streamName, streamLink);
            return streamLink;
        } catch (Exception e) {
            throw new StreamProviderException(format("Could not load stream link by userId: {0}, userIp: {1}, eventId: {2}, streamName: {3}",
                    userId, userIp, eventId, streamName), e);
        }
    }

    @Nullable
    @Timed(MetricsConfig.APP_METRIC_NAME)
    public String getSport(@Nullable String sportCode) {
        var sportsMap = streamProviderClient.getSportsMap(streamProviderProperties.getCustomerUid());
        var sport = sportsMap.get(sportCode);
        if (StringUtils.isBlank(sport)) {
            log.info("Sport not found for sportCode {}. Try to evict cache and get again", sportCode);
            streamProviderClient.evictSportsCache();
            sportsMap = streamProviderClient.getSportsMap(streamProviderProperties.getCustomerUid());
            sport = sportsMap.get(sportCode);
            if (StringUtils.isBlank(sport)) {
                throw new StreamProviderException(format("Sport not found for sportCode: {0}", sportCode));
            }
        }
        log.trace("Sport found for sportCode: {}", sportCode);
        return sport;
    }
}