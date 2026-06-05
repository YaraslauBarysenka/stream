package net.red.demo.remote.client;

import static net.red.demo.config.CacheConfig.SPORTS_CACHE_NAME;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

import net.red.demo.remote.dto.event.EventListRemoteDto;
import net.red.demo.remote.dto.sport.SportListRemoteDto;
import net.red.demo.remote.dto.sport.SportRemoteDto;
import net.red.demo.remote.dto.stream.EventStreamLinkRemoteDto;
import net.red.demo.remote.dto.stream.EventStreamListRemoteDto;

@FeignClient(name = "provider-api", url = "${stream.provider.base-url}")
public interface StreamProviderClient {

    /**
     * This method call will return all events for a particular date.
     * <p>
     * Note: This method is rate limited at 0.1 requests per second on provider side.
     *
     * @param customerUid our company’s identifier, this is provided by iGame
     * @param eventDate   date of the event. Date passed with format “DD-MM-YYYY”
     * @return events
     */
    @Retry(name = "getEvents")
    @RateLimiter(name = "getEvents")
    @GetMapping("/event/all/{customerUid}/{eventDate}")
    EventListRemoteDto getEvents(@PathVariable String customerUid,
                                 @PathVariable String eventDate);

    /**
     * This method will return all available streams for a specific event.
     * We provide HLS streams for all content but iGame Media also provides the industry’s first
     * HESP ultra-low latency (ULL) streams for select content.
     * -HLS
     * -ULL
     * <p>
     * Note: This method is rate limited at 150 requests per second on provider side.
     *
     * @param customerUid our company’s identifier, this is provided by iGame
     * @param eventId     ID of specific event you wish to stream
     * @return event's streams
     */
    @GetMapping("/stream/{customerUid}/{eventId}")
    EventStreamListRemoteDto getEventStreams(@PathVariable String customerUid,
                                             @PathVariable String eventId);

    /**
     * This method returns an iGame Player link that can be used on your sites.
     * <p>
     * Note: This method is rate limited at 150 requests per second by default however
     * iGame will work with each customer to adjust this limit to fit their needs
     * <p>
     * IMPORTANT NOTE: For server-side calls please specify server IP in ‘X-Forwarded-For’ header
     *
     * @param customerUid our company’s identifier, this is provided by iGame
     * @param userId      unique identifier for end-user
     * @param userIp      IP address of end-user
     * @param eventId     ID of specific event you wish to stream.
     * @param streamName  this value can be found by getting the list of streams for event
     * @param redirectURL a mandatory legacy parameter that no longer functions and won't redirect users to another page after. Pass any valid URL to this parameter
     * @return streamLink
     */
    @PostMapping("/stream/link")
    EventStreamLinkRemoteDto getEventStreamLink(@RequestParam String customerUid,
                                                @RequestParam String userId,
                                                @RequestParam String userIp,
                                                @RequestParam String eventId,
                                                @RequestParam String streamName,
                                                @RequestParam String redirectURL);

    /**
     * You can retrieve a list of sports and their codes.
     *
     * @param customerUid our company’s identifier, this is provided by iGame
     * @return sports and their codes
     */
    @GetMapping("/sport/all/{customerUid}")
    SportListRemoteDto getAllSport(@PathVariable String customerUid);

    @Cacheable(value = SPORTS_CACHE_NAME, unless = "#result==null or #result.empty")
    default Map<String, String> getSportsMap(String customerUid) {
        return getAllSport(customerUid).getSports()
                .stream()
                .collect(Collectors.toMap(
                        SportRemoteDto::getCode,
                        SportRemoteDto::getName
                ));
    }

    @CacheEvict(value = SPORTS_CACHE_NAME)
    default void evictSportsCache() {
    }
}