package net.red.demo.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;

import net.red.demo.entity.Event;
import net.red.demo.remote.StreamProviderService;
import net.red.demo.remote.dto.enums.EventState;
import net.red.demo.remote.dto.event.EventRemoteDto;
import net.red.demo.remote.dto.stream.EventStreamRemoteDto;

@Validated
@Component
@RequiredArgsConstructor
public class EventMapper {
    private final StreamProviderService streamProviderService;

    @NotNull
    public Event toEvent(@NotNull EventRemoteDto eventRemoteDto) {
        return new Event().setExternalId(eventRemoteDto.getEventId())
                .setProviderEventId(eventRemoteDto.getEventNumber())
                .setState(EventState.valueOf(eventRemoteDto.getEventStatusCode()))
                .setSport(streamProviderService.getSport(eventRemoteDto.getSportCode()))
                .setLeague(eventRemoteDto.getLocationName())
                .setStartDate(eventRemoteDto.getStartTime())
                .setMatchName(eventRemoteDto.getTitle())
                .setStreamNames(getSortedStreamNames(eventRemoteDto));
    }

    @NotNull
    public Map<String, EventRemoteDto> convertToExternalIdMap(@Nullable List<EventRemoteDto> eventRemoteDtoList) {
        return Optional.ofNullable(eventRemoteDtoList).orElse(List.of()).stream()
                .collect(Collectors.toMap(EventRemoteDto::getEventId, Function.identity()));
    }

    public boolean copyEventChangeablePropertiesAndGetResult(@NotNull EventRemoteDto source, @NotNull Event target) {
        boolean changed = false;
        if (!Objects.equals(EventState.valueOf(source.getEventStatusCode()), target.getState())) {
            target.setState(EventState.valueOf(source.getEventStatusCode()));
            changed = true;
        }
        if (!Objects.equals(source.getStartTime(), target.getStartDate())) {
            target.setStartDate(source.getStartTime());
            changed = true;
        }
        if (EventState.isFinishedByStateString(source.getEventStatusCode())) {
            // avoid streamNames changes for Finished remote events
            // because it needs to keep original streamNames, when event was Open
            return changed;
        }

        String[] sortedSourceStreamNames = getSortedStreamNames(source);
        if (!Arrays.equals(sortedSourceStreamNames, target.getStreamNames())) {
            target.setStreamNames(sortedSourceStreamNames);
            changed = true;
        }
        return changed;
    }

    /**
     * Needs to prevent redundant SQL UPDATE queries, cause hibernate evaluates the same Event
     * with different streamNames order as dirty entity which should be flushed.
     */
    private String[] getSortedStreamNames(@NotNull EventRemoteDto eventRemoteDto) {
        return Optional.ofNullable(eventRemoteDto.getEventStreams()).orElse(List.of()).stream()
                .map(EventStreamRemoteDto::getUniqueStreamName)
                .distinct()
                .sorted()
                .toArray(String[]::new);
    }
}