package net.red.demo.remote.dto.sport;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SportListRemoteDto {
    private List<SportRemoteDto> sports = new ArrayList<>();
}