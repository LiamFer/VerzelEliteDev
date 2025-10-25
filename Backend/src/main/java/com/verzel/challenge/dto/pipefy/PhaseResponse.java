package com.verzel.challenge.dto.pipefy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhaseResponse {
    public PhaseData data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhaseData {
        public PhasePipe pipe;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhasePipe {
        public List<Phase> phases;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Phase {
        public String id;
        public String name;
    }
}
