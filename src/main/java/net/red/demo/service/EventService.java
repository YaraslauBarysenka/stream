package net.red.demo.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.common.util.DateUtils;
import net.red.demo.entity.Event;
import net.red.demo.remote.dto.enums.EventState;
import net.red.demo.repository.EventRepository;
import net.red.demo.service.dto.EventStateCountDto;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ChangeService changeService;

    /**
     * @throws EntityNotFoundException in case {@link Event} not found
     */
    @NotNull
    public Event findByExternalId(@NotNull String externalId) {
        return eventRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("Could not found Event entity by eventExternalId: " + externalId));
    }

    @NotNull
    public Map<String, Event> findByExternalIds(@NotNull Set<String> externalIds) {
        return eventRepository.findByExternalIdIn(externalIds).stream()
                .collect(Collectors.toMap(Event::getExternalId, Function.identity()));
    }

    @NotNull
    public Map<String, Event> findByEventDate(@NotNull LocalDate eventDate) {
        var startOfDay = DateUtils.toUtcStartOfDay(eventDate);
        var endOfDay = startOfDay.plusDays(1);
        return eventRepository.findByStartDateGreaterThanEqualAndStartDateLessThan(startOfDay, endOfDay).stream()
                .collect(Collectors.toMap(Event::getExternalId, Function.identity()));
    }

    public void saveAll(@NotNull Collection<Event> events) {
        eventRepository.saveAll(events);
    }

    public void deleteAllInBatch(@NotNull Collection<Event> events) {
        eventRepository.deleteAllInBatch(events);
    }

    @NotNull
    public Slice<Event> findByStartDateLessThan(@NotNull OffsetDateTime threshold,
                                                @NotNull Pageable pageable) {
        return eventRepository.findByStartDateLessThan(threshold, pageable);
    }

    /**
     * Needs to save {@link Event} list with related {@link net.red.demo.entity.Change} list in the same transaction.
     */
    public void saveEventsAndChanges(@NotNull Collection<Event> events) {
        this.saveAll(events);
        changeService.buildAndSaveAllChanges(events.stream());
    }

    @NotNull
    public List<EventStateCountDto> getEventStateCount() {
        var eventStateCountList = eventRepository.getEventStateCount();
        if (eventStateCountList.size() == EventState.values().length) {
            // all EventStates found, returns result as is
            return eventStateCountList;
        }

        var eventStateSet = eventStateCountList.stream()
                .map(EventStateCountDto::eventState)
                .collect(Collectors.toUnmodifiableSet());
        Arrays.stream(EventState.values()).forEach(eventState -> {
            if (!eventStateSet.contains(eventState)) {
                // add EventStateCountDto with 0 count, when specific EventState not found in DB
                eventStateCountList.add(new EventStateCountDto(eventState, 0));
            }
        });
        return eventStateCountList;
    }
}