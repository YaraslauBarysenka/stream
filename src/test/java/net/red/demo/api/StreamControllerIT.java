package net.red.demo.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import net.red.demo.AbstractIT;
import net.red.demo.config.properties.StreamProviderProperties;
import net.red.demo.remote.client.StreamProviderClient;
import net.red.demo.remote.dto.enums.EventState;
import net.red.demo.remote.dto.stream.EventStreamLinkRemoteDto;
import net.red.demo.service.EventService;
import net.red.demo.test.util.EventTestDateUtils;

class StreamControllerIT extends AbstractIT {
    private static final String URL = "/streams/v1/events/{id}";
    private static final String NOT_FOUND_EVENT_EXTERNAL_ID = "10";
    private static final String CUSTOMER_IP = "127.0.0.1";
    private static final String CUSTOMER_ID = "123";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private StreamProviderClient streamProviderClient;
    @Autowired
    private EventService eventService;
    @Autowired
    private StreamProviderProperties streamProviderProperties;

    @CsvSource({
            "P",
            "O",
            "C",
            "V",
    })
    @ParameterizedTest
    void getStreamUrl(String evenStateStr) throws Exception {
        var event = EventTestDateUtils.buildEvent(1L).setState(EventState.valueOf(evenStateStr));
        eventService.saveAll(List.of(event));
        var expected = new EventStreamLinkRemoteDto()
                .setStreamLink("https://player.igamemedia.com/live-playerlegacy?tid=1234567&tk=12345");
        when(streamProviderClient.getEventStreamLink(eq(streamProviderProperties.getCustomerUid()), eq(CUSTOMER_ID),
                eq(CUSTOMER_IP), eq(event.getExternalId()), eq(event.getStreamNames()[0]), anyString())).thenReturn(expected);
        mockMvc.perform(MockMvcRequestBuilders.get(URL, event.getExternalId())
                        .param("customerIP", CUSTOMER_IP)
                        .param("customerId", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").hasJsonPath())
                .andExpect(MockMvcResultMatchers.jsonPath("$.url", Matchers.is(expected.getStreamLink())));
    }

    @Test
    void getStreamUrlWhenEventNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL, NOT_FOUND_EVENT_EXTERNAL_ID)
                        .param("customerIP", CUSTOMER_IP)
                        .param("customerId", CUSTOMER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$").hasJsonPath())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorDescription",
                        Matchers.is("Could not found Event entity by eventExternalId: 10")));
    }

    @Test
    void getStreamUrlWhenCustomerIsNegative() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL, NOT_FOUND_EVENT_EXTERNAL_ID)
                        .param("customerIP", CUSTOMER_IP)
                        .param("customerId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$").hasJsonPath())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorDescription",
                        Matchers.is("Invalid request content. {customerId=must be greater than 0}")));
    }

    @CsvSource({
            "P",
            "O",
            "C",
            "V",
    })
    @ParameterizedTest
    void getStreamUrlWhenStreamNamesAreEmpty(String evenStateStr) throws Exception {
        var event = EventTestDateUtils.buildEvent(2L)
                .setState(EventState.valueOf(evenStateStr))
                .setStreamNames(null);
        eventService.saveAll(List.of(event));
        mockMvc.perform(MockMvcRequestBuilders.get(URL, event.getExternalId())
                        .param("customerIP", CUSTOMER_IP)
                        .param("customerId", CUSTOMER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$").hasJsonPath())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorDescription",
                        Matchers.is("No available stream names. eventExternalId: ext_id_2, eventState: " + evenStateStr)));
    }
}