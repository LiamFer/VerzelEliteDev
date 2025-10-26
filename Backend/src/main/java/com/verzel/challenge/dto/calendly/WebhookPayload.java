package com.verzel.challenge.dto.calendly;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    private String email;
    private String name;
    private ScheduledEvent scheduled_event;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledEvent {
        private String name;
        private String start_time;
        private String end_time;
        private Location location;
        @JsonProperty("event_memberships")
        private List<EventMembership> eventMemberships;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        @JsonProperty("join_url")
        private String joinUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMembership {
        private String user;
        @JsonProperty("user_email")
        private String userEmail;
        @JsonProperty("user_name")
        private String userName;
    }

    public String getInviteeEmail() {
        if (email != null) return email;
        if (scheduled_event != null && scheduled_event.getEventMemberships() != null && !scheduled_event.getEventMemberships().isEmpty()) {
            return scheduled_event.getEventMemberships().get(0).getUserEmail();
        }
        return null;
    }

    public String getMeetingLink() {
        if (scheduled_event != null && scheduled_event.getLocation() != null) {
            return scheduled_event.getLocation().getJoinUrl();
        }
        return null;
    }
}