package net.red.demo.remote.dto.event;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class EventListRemoteDto {
    private List<EventRemoteDto> events = new ArrayList<>();
}