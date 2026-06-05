package net.red.demo.service.dto;

import net.red.demo.remote.dto.enums.EventState;

public record EventStateCountDto(EventState eventState, long count) {
}