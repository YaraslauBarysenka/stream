package net.red.demo.service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.config.properties.EventSyncJobProperties;
import net.red.demo.entity.Event;
import net.red.demo.mapper.EventMapper;
import net.red.demo.remote.StreamProviderService;
import net.red.demo.remote.dto.enums.EventState;
import net.red.demo.remote.dto.event.EventRemoteDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSyncService {
    private final StreamProviderService streamProviderService;
    private final EventMapper eventMapper;
    private final EventService eventService;
    private final EventSyncJobProperties eventSyncJobProperties;

    public void sync() {
        var absentEvents = new HashMap<String, Event>();
        var eventDate = getStartLocalDateInUtc(eventSyncJobProperties.getStartDay());
        for (int days = 0; days < eventSyncJobProperties.getDayCount(); days++) {
            log.debug("Sync started for eventDate: {}", eventDate);
            syncByEventDateQuietly(eventDate, absentEvents);
            eventDate = eventDate.plusDays(1);
        }
        finishAbsentEvents(absentEvents);
    }

    private void syncByEventDateQuietly(LocalDate eventDate, Map<String, Event> absentEvents) {
        try {
            syncByEventDate(eventDate, absentEvents);
        } catch (Exception e) {
            log.error("Sync failed by eventDate: {}", eventDate, e);
        }
    }

    /*
    Incoming remote Events by eventDate (start_date) = 15
        ext_id, start_date
        1,      15         - case1: same exists external_id and same startDate - needs update if its needed
        4,      15         - case2: same exists external_id and other startDate in future  15 -> 16 - needs update if its needed
        3,      15         - case3: same exists external_id and other startDate in past    14 -> 15 - needs update if its needed
        5,      15         - case4: external_id not exists in db - create new one if its needed

        Exists DB Events by eventDate (start_date) = 15
        ext_id, start_date
        1,      15         - case1: same external_id and startDate - needs update if its needed
        2,      15         - case5: shifted to in far the future out of Job time range - needs to collect it and log and may be finished manually to create in the feature ?
    */
    private void syncByEventDate(@Nonnull LocalDate eventDate,
                                 @Nonnull Map<String, Event> absentEvents) {
        var eventRemoteList = streamProviderService.getAllEventsByEventDateWithStreamsForPendingAndOpenEvent(eventDate);
        var extIdToEventRemoteMap = eventMapper.convertToExternalIdMap(eventRemoteList);
        updateNotShiftedEvents(eventDate, absentEvents, extIdToEventRemoteMap);
        updateShiftedEvents(extIdToEventRemoteMap);
        saveNewEvents(extIdToEventRemoteMap);
    }

    private void updateNotShiftedEvents(@Nonnull LocalDate eventDate,
                                        @Nonnull Map<String, Event> absentEvents,
                                        @Nonnull Map<String, EventRemoteDto> extIdToEventRemoteMap) {
        var extIdToEventDbMap = eventService.findByEventDate(eventDate);
        var eventsToUpdateWithSameStartDate = new ArrayList<Event>();

        for(var it = extIdToEventRemoteMap.entrySet().iterator(); it.hasNext();) {
            var eventRemoteEntry = it.next();
            var extIdRemote = eventRemoteEntry.getKey();
            var eventRemote = eventRemoteEntry.getValue();

            if (extIdToEventDbMap.containsKey(extIdRemote)) {
                // case1: same exists external_id and same startDate - needs update if its needed
                var eventDb = extIdToEventDbMap.get(extIdRemote);
                copyChangeablePropertiesAndCollectChangedStreams(eventDb, eventRemote, eventsToUpdateWithSameStartDate);
                // remove matched events by external_id and start_date
                it.remove();
                extIdToEventDbMap.remove(extIdRemote);
            }
            // case5: remove orphan DB Events which are included in Provider's response
            absentEvents.remove(extIdRemote);
        }
        // case5: collect orphan DB Events which are not included in Provider's response by current eventDate (start_date)
        absentEvents.putAll(extIdToEventDbMap);

        // case1: save exists Events in Db, where event.startDate is without changes
        eventService.saveEventsAndChanges(eventsToUpdateWithSameStartDate);
        log.debug("Not shifted Events saved. eventCount: {}, events: {}",
                eventsToUpdateWithSameStartDate.size(), getShortEventDetailsForLog(eventsToUpdateWithSameStartDate));
    }

    private void updateShiftedEvents(@Nonnull Map<String, EventRemoteDto> extIdToEventRemoteMap) {
        // now extIdToEventRemoteMap contains only new Events and shifted Events by start_date
        var extIdToShiftedEventDbMap = eventService.findByExternalIds(extIdToEventRemoteMap.keySet());

        // case2 and case3: update already exists in Db, but shifted Events
        var eventsToUpdateWithShiftedStartDate = new ArrayList<Event>();
        for(var it = extIdToEventRemoteMap.entrySet().iterator(); it.hasNext();) {
            var eventRemoteEntry = it.next();
            var extIdRemote = eventRemoteEntry.getKey();
            var eventRemote = eventRemoteEntry.getValue();

            var eventDbShifted = extIdToShiftedEventDbMap.get(extIdRemote);
            if (eventDbShifted != null) {
                copyChangeablePropertiesAndCollectChangedStreams(eventDbShifted, eventRemote, eventsToUpdateWithShiftedStartDate);
                // remove shifted Events, so extIdToEventRemoteMap will contain only new Events
                it.remove();
            }
        }
        eventService.saveEventsAndChanges(eventsToUpdateWithShiftedStartDate);
        log.debug("Shifted Events saved. eventCount: {}, events: {}",
                eventsToUpdateWithShiftedStartDate.size(), getShortEventDetailsForLog(eventsToUpdateWithShiftedStartDate));
    }

    private void saveNewEvents(Map<String, EventRemoteDto> extIdToEventRemoteMap) {
        // case4: save new and NOT Finished Events
        var eventsToCreate = extIdToEventRemoteMap.values()
                .stream()
                .map(eventMapper::toEvent)
                .filter(event -> EventState.isNotFinished(event.getState()))
                .toList();
        eventService.saveEventsAndChanges(eventsToCreate);
        log.debug("New Events saved. eventCount: {}, events: {}",
                eventsToCreate.size(), getShortEventDetailsForLog(eventsToCreate));
    }

    private void copyChangeablePropertiesAndCollectChangedStreams(@Nullable Event local,
                                                                  @Nonnull EventRemoteDto remote,
                                                                  @Nonnull List<Event> eventsToUpdate) {
        if (local == null) {
            return;
        }
        var changed = eventMapper.copyEventChangeablePropertiesAndGetResult(remote, local);
        if(changed) {
            eventsToUpdate.add(local);
        }
    }

    @Nonnull
    private LocalDate getStartLocalDateInUtc(int startDay) {
        var now = LocalDate.now(ZoneOffset.UTC);
        if (startDay >= 0) {
            return now.plusDays(startDay);
        }
        return now.minusDays(Math.abs(startDay));
    }

    @Nonnull
    private String getShortEventDetailsForLog(@Nonnull Collection<Event> events) {
        return events.stream()
                .map(event -> MessageFormat.format("'{'{0}, {1}'}'", event.getExternalId(), event.getState()))
                .toList()
                .toString();
    }

    private void finishAbsentEvents(@Nonnull Map<String, Event> absentEventMap) {
        if (absentEventMap.isEmpty()) {
            log.debug("Absent Events not found");
            return;
        }
        // case5: Events exists in DB, but these Events are absent in all Provider's responses in during Job execution
        // reasons: Provider`s issue or Events start_date were shifted out of Job time range
        log.warn("Absent Events found. eventCount: {}, eventExternalIds: {}",
                absentEventMap.size(), absentEventMap.keySet());
        // filter and update events to finish
        Collection<Event> absentEvents = absentEventMap.values();
        for(var iterator = absentEvents.iterator(); iterator.hasNext();) {
            Event event = iterator.next();
            if (EventState.isFinished(event.getState())) {
                // remove already finished
                iterator.remove();
            } else {
                // update event to finish
                event.setState(EventState.C);
            }
        }
        if (absentEvents.isEmpty()) {
            log.debug("All absent Events are already finished");
            return;
        }
        eventService.saveEventsAndChanges(absentEvents);
        log.warn("Absent Events are finished. eventCount: {}, events: {}",
                absentEvents.size(), getShortEventDetailsForLog(absentEvents));
    }
}