package com.tinfig.wsosc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FaceMessage {
    public String type;
    public FaceMessageData data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaceMessageData {
        public float timestamp;
        public List<Face> faces;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Measurements {
        public Float interocularDistance;
        public Map<String, Float> orientation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Face {
        public Map<String, Float> emotions;
        public Map<String, Float> expressions;
        public Map<String, String> appearance;
        public Measurements measurements;
        public Map<String, Map<String, Float>> featurePoints;
    }
}
