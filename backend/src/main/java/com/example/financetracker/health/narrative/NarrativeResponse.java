package com.example.financetracker.health.narrative;

public class NarrativeResponse {

    private String narrative;
    private String tone; // positive | warning | critical

    public NarrativeResponse() {
    }

    public NarrativeResponse(String narrative, String tone) {
        this.narrative = narrative;
        this.tone = tone;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }
}
