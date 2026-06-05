package net.red.demo.remote.dto.stream;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class EventStreamListRemoteDto {
    List<EventStreamRemoteDto> eventStreams = new ArrayList<>();
}