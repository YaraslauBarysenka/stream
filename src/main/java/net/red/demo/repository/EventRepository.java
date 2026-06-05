package net.red.demo.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import net.red.demo.entity.Event;
import net.red.demo.service.dto.EventStateCountDto;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByExternalIdIn(Collection<String> externalIds);
    List<Event> findByStartDateGreaterThanEqualAndStartDateLessThan(OffsetDateTime start, OffsetDateTime end);
    Slice<Event> findByStartDateLessThan(OffsetDateTime threshold, Pageable pageable);
    Optional<Event> findByExternalId(String externalId);
    @Query("SELECT new net.red.demo.service.dto.EventStateCountDto(state, COUNT(state)) FROM Event GROUP BY state")
    List<EventStateCountDto> getEventStateCount();
}