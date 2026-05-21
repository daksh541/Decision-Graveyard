package com.sai.decisiongraveyard.model;

public class HeatmapCell {

    private final long timestamp;
    private final int intensity;
    private final String label;

    public HeatmapCell(long timestamp, int intensity, String label) {
        this.timestamp = timestamp;
        this.intensity = Math.max(0, Math.min(4, intensity));
        this.label = label == null ? "" : label;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getIntensity() {
        return intensity;
    }

    public String getLabel() {
        return label;
    }
}
