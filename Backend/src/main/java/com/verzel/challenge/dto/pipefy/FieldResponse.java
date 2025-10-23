package com.verzel.challenge.dto.pipefy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldResponse {
    public FieldData data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldData {
        public FieldPipe pipe;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldPipe {
        @JsonProperty("start_form_fields")
        public List<PipefyField> startFormFields;
    }
}
