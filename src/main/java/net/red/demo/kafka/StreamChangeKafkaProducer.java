package net.red.demo.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import net.red.demo.kafka.dto.StreamChange;

@Component
public class StreamChangeKafkaProducer extends AbstractKafkaProducer<String, StreamChange, StreamChange> {
    private final KafkaProperties kafkaProperties;

    public StreamChangeKafkaProducer(KafkaProperties kafkaProperties, KafkaTemplate<String, StreamChange> streamChangeKafkaTemplate) {
        super(streamChangeKafkaTemplate);
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    protected ProducerRecord<String, StreamChange> createProducerRecord(StreamChange message) {
        var defaultTopic = kafkaProperties.getTemplate().getDefaultTopic();
        return new ProducerRecord<>(defaultTopic, null, null,
                message.getId(), message);
    }
}