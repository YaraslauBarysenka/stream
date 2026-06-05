package net.red.demo.service;

import static net.red.demo.test.util.EventTestDateUtils.EXT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.red.demo.AbstractIT;
import net.red.demo.entity.Event;
import net.red.demo.remote.dto.enums.EventState;
import net.red.demo.repository.EventRepository;
import net.red.demo.service.dto.EventStateCountDto;
import net.red.demo.test.util.EventTestDateUtils;

class EventServiceIT extends AbstractIT {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventService eventService;

    @AfterEach
    void tearDown() {
        eventRepository.deleteAll();
    }

    @Test
    void saveAllAndDeleteAllInBatch() {
        List<Event> events = EventTestDateUtils.buildRandomEventList();
        eventService.saveAll(events);
        assertEquals(events.size(), eventRepository.count());

        eventService.deleteAllInBatch(events);
        assertEquals(0, eventRepository.count());
    }

    @Test
    void saveAllAndDeleteOneEventStream() {
        Long eventId = 1L;
        String extId = EXT_ID + eventId;
        Event event = EventTestDateUtils.buildEvent(eventId);
        eventService.saveAll(List.of(event));

        int streamCount = event.getStreamNames().length;
        event.setStreamNames(ArrayUtils.removeElement(event.getStreamNames(), event.getStreamNames()[0]));
        eventService.saveAll(List.of(event));

        Map<String, Event> map = eventService.findByExternalIds(Set.of(extId));
        assertEquals(1, map.size());
        assertEquals(--streamCount, map.get(extId).getStreamNames().length);
    }

    @Test
    void findByEventDate() {
        List<Event> events = EventTestDateUtils.buildRandomEventList();
        eventService.saveAll(events);
        int expectedEvents = events.size();
        assertEquals(expectedEvents, eventRepository.count());

        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        var actualEvents = eventService.findByEventDate(now);
        assertEquals(expectedEvents, actualEvents.size());

        actualEvents = eventService.findByEventDate(now.plusDays(1));
        assertEquals(0, actualEvents.size());
        LocalDate tomorrow = now.minusDays(1);
        actualEvents = eventService.findByEventDate(tomorrow);
        assertEquals(0, actualEvents.size());
    }

    @Test
    void getEventStateCount() {
        // verify when no events
        var eventStateCountDtoList = eventService.getEventStateCount();
        assertNotNull(eventStateCountDtoList);
        assertEquals(4, eventStateCountDtoList.size());
        var eventStateCountDtoMap = toMap(eventStateCountDtoList);
        assertEquals(0, eventStateCountDtoMap.get(EventState.O));
        assertEquals(0, eventStateCountDtoMap.get(EventState.P));
        assertEquals(0, eventStateCountDtoMap.get(EventState.C));
        assertEquals(0, eventStateCountDtoMap.get(EventState.V));

        // populate events
        Event event1 = EventTestDateUtils.buildEvent(1L);
        Event event2 = EventTestDateUtils.buildEvent(2L);
        Event event3 = EventTestDateUtils.buildEvent(3L);
        Event event4 = EventTestDateUtils.buildEvent(4L).setState(EventState.P);
        Event event5 = EventTestDateUtils.buildEvent(5L).setState(EventState.P);
        Event event6 = EventTestDateUtils.buildEvent(6L).setState(EventState.C);
        eventService.saveAll(List.of(event1, event2, event3, event4, event5, event6));

        // verify when events are present
        eventStateCountDtoList = eventService.getEventStateCount();
        assertNotNull(eventStateCountDtoList);
        assertEquals(4, eventStateCountDtoList.size());
        eventStateCountDtoMap = toMap(eventStateCountDtoList);
        assertEquals(3, eventStateCountDtoMap.get(EventState.O));
        assertEquals(2, eventStateCountDtoMap.get(EventState.P));
        assertEquals(1, eventStateCountDtoMap.get(EventState.C));
        assertEquals(0, eventStateCountDtoMap.get(EventState.V));
    }

    private Map<EventState, Long> toMap(List<EventStateCountDto> eventStateCountDtoList) {
        return eventStateCountDtoList.stream()
                .collect(Collectors.toMap(EventStateCountDto::eventState, EventStateCountDto::count));
    }
}