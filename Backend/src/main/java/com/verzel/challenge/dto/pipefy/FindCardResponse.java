package com.verzel.challenge.dto.pipefy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FindCardResponse {
    public Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public FindCards findCards;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FindCards {
            public List<Edge> edges;

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Edge {
                public Card node;
            }
        }
    }
}
