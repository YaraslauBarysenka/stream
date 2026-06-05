package net.red.demo.mapper;

import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.common.exception.StreamException;
import net.red.demo.entity.Change;
import net.red.demo.entity.Event;
import net.red.demo.kafka.dto.StreamChange;
import net.red.demo.mapper.parser.EventMatchNameParser;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
public class ChangeMapper {
    private final EventMatchNameParser eventMatchNameParser;

    @NotNull
    public List<Change> toStreamChangeListAndFilterInvalid(@NotNull Stream<Event> events) {
        return events.filter(this::isMatchNameParseable)
                .map(this::toChange)
                .toList();
    }

    /**
     * @throws StreamException when {@link Event#getMatchName()} is invalid to parse home and away competitors
     */
    @Nonnull
    private Change toChange(@Nonnull Event event) {
        return new Change()
                .setContent(toStreamChange(event));
    }

    private boolean isMatchNameParseable(@Nonnull Event event) {
        var isMatchNameParseable = eventMatchNameParser.isMatchNameParseable(event.getMatchName());
        if (!isMatchNameParseable) {
            log.warn("'{}' Event matchName is not parseable. eventExternalId: {}", event.getMatchName(), event.getExternalId());
        }
        return isMatchNameParseable;
    }

    /**
     * @throws StreamException when {@link Event#getMatchName()} is invalid to parse home and away competitors
     */
    @Nonnull
    private StreamChange toStreamChange(@Nonnull Event event) {
        var competitors = eventMatchNameParser.parse(event.getMatchName());
        return new StreamChange()
                .setId(event.getExternalId())
                .setState(event.getState().getStreamState())
                .setSport(event.getSport())
                .setLeague(event.getLeague())
                .setStartDate(event.getStartDate())
                .setCompetitorHome(competitors.getFirst())
                .setCompetitorAway(competitors.getSecond())
                .setMatchName(event.getMatchName());
    }
}
