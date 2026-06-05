package net.red.demo.service;

import static java.text.MessageFormat.format;

import java.util.Arrays;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.api.dto.StreamUrlDto;
import net.red.demo.api.filter.StreamFilter;
import net.red.demo.common.exception.StreamBadRequestException;
import net.red.demo.config.MetricsConfig;
import net.red.demo.config.properties.StreamProviderProperties;
import net.red.demo.entity.Event;
import net.red.demo.remote.StreamProviderService;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamService {
    private final EventService eventStreamService;
    private final StreamProviderService streamProviderService;
    private final StreamProviderProperties streamProviderProperties;

    @Timed(MetricsConfig.APP_METRIC_NAME)
    public @Nonnull StreamUrlDto getStreamUrl(@Nonnull String eventExternalId,
                                              @Nonnull StreamFilter streamFilter) {
        var event = eventStreamService.findByExternalId(eventExternalId);
        var streamName = getStreamName(event);
        var streamLink = streamProviderService.getStreamLink(String.valueOf(streamFilter.getCustomerId()),
                streamFilter.getCustomerIP(), event.getExternalId(), streamName);
        if (StringUtils.isBlank(streamLink)) {
            log.warn("Event stream link not found by eventExternalId: {}, streamName: {}, streamFilter: {}. streamLink: {}, eventState: {}, streamNames: {}",
                    eventExternalId, streamName, streamFilter, streamLink, event.getState(), event.getStreamNames());
        }
        return new StreamUrlDto().setUrl(streamLink);
    }

    private @Nullable String getStreamName(@Nonnull Event event) {
        var availableStreamNameArray = event.getStreamNames();
        if (ArrayUtils.isEmpty(availableStreamNameArray)) {
            throw new StreamBadRequestException(format("No available stream names. eventExternalId: {0}, eventState: {1}",
                    event.getExternalId(), event.getState())); // leads to BAD_REQUEST HTTP response status
        }

        return Arrays.stream(availableStreamNameArray)
                .filter(sn -> StringUtils.equalsIgnoreCase(sn, streamProviderProperties.getPreferableStreamName()))
                .findAny()
                .orElse(ArrayUtils.get(availableStreamNameArray, 0));
    }
}