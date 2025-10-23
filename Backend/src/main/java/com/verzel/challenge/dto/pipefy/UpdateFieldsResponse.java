package com.verzel.challenge.dto.pipefy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateFieldsResponse {
    public Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public UpdateFieldsValues updateFieldsValues;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UpdateFieldsValues {
            public boolean success;
        }
    }
}
