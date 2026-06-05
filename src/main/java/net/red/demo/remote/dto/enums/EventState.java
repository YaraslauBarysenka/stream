package net.red.demo.remote.dto.enums;

import lombok.Getter;

import net.red.demo.kafka.dto.enums.StreamState;

@Getter
public enum EventState {
    // Pending: Event still to begin
    P(StreamState.PREMATCH),
    // Open: Event is in progress
    O(StreamState.LIVE),
    // Closed: Event is over
    C(StreamState.FINISHED),
    // Void: Event has been cancelled/is no longer available
    V(StreamState.FINISHED),
    ;

    EventState(StreamState streamState) {
        this.streamState = streamState;
    }

    private final StreamState streamState;

    public static boolean isNotFinished(EventState eventState) {
        return !isFinished(eventState);
    }

    public static boolean isFinished(EventState eventState) {
        return EventState.C == eventState || EventState.V == eventState;
    }

    public static boolean isFinishedByStateString(String eventStateStr) {
        return isFinished(EventState.valueOf(eventStateStr));
    }

    public static boolean isNotPendingByStateString(String eventStateStr) {
        return EventState.valueOf(eventStateStr) != EventState.P;
    }
}