package net.red.demo.service;

import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.entity.Change;
import net.red.demo.kafka.StreamChangeKafkaProducer;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ChangeSendService {
    private final StreamChangeKafkaProducer streamChangeKafkaProducer;
    private final ChangeService changeService;

    public void send(@Positive int batchSize) {
        Page<Change> changesToSend;
        do {
            changesToSend = changeService.findEventChangeBatch(batchSize);
            changesToSend.map(Change::getContent)
                    .forEach(streamChangeKafkaProducer::send);
            changeService.deleteAllInBatch(changesToSend.getContent());
            log.debug("Batch Changes was sent. batchSize: {}, numOfElements: {}",
                    batchSize, changesToSend.getNumberOfElements());
        } while (changesToSend.hasNext());
    }
}