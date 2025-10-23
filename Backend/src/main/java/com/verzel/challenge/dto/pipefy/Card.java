package com.verzel.challenge.dto.pipefy;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    public String id;
    public String title;
    public List<Field> fields;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Field {
        public String name;
        public String value;
    }
}

