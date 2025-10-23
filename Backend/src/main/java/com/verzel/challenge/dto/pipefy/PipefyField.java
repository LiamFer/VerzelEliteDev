package com.verzel.challenge.dto.pipefy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PipefyField {
    public String id;
    public String label;
}