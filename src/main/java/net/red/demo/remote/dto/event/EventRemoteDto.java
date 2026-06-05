package net.red.demo.remote.dto.event;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import net.red.demo.common.json.annotation.JsonUtcOffsetDatetime;
import net.red.demo.remote.dto.stream.EventStreamRemoteDto;

@Data
public class EventRemoteDto {
    private String eventId;
    private String locationName;
    private String sportCode;
    private String eventStatusCode;
    private Long eventNumber;
    private String title;
    @JsonUtcOffsetDatetime
    private OffsetDateTime startTime; // format: 2020-11-09T:02:00:00 in UTC
    @JsonUtcOffsetDatetime
    private OffsetDateTime estimatedEndTime; // format: 2020-11-09T:03:30:00 in UTC
    @JsonIgnore
    private List<EventStreamRemoteDto> eventStreams;
}