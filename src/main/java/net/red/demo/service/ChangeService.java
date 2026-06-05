package net.red.demo.service;

import java.util.List;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.entity.Change;
import net.red.demo.entity.Change_;
import net.red.demo.entity.Event;
import net.red.demo.mapper.ChangeMapper;
import net.red.demo.repository.ChangeRepository;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ChangeService {
    public static final Sort BATCH_SORT = Sort.by(Sort.Direction.ASC, Change_.ID);
    private final ChangeRepository changeRepository;
    private final ChangeMapper changeMapper;

    @NotNull
    public Page<Change> findEventChangeBatch(@Positive int batchSize) {
        var pageable = PageRequest.of(0, batchSize, BATCH_SORT);
        return changeRepository.findAll(pageable);
    }

    public void deleteAllInBatch(@NotNull List<Change> changes) {
        changeRepository.deleteAllInBatch(changes);
    }

    public void buildAndSaveAllChanges(@NotNull Stream<Event> events) {
        var streamChangeList = changeMapper.toStreamChangeListAndFilterInvalid(events);
        changeRepository.saveAll(streamChangeList);
        log.debug("Stream Change list saved. changeCount: {}", streamChangeList.size());
    }
}