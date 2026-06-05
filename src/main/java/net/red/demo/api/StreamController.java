package net.red.demo.api;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import net.red.demo.api.dto.StreamUrlDto;
import net.red.demo.api.filter.StreamFilter;
import net.red.demo.service.StreamService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("local/*/streams/v1")
public class StreamController {
    private final StreamService streamService;

    @GetMapping("events/{externalId}")
    public StreamUrlDto getStreamUrl(@PathVariable String externalId, @Valid StreamFilter filter) {
        return streamService.getStreamUrl(externalId, filter);
    }
}