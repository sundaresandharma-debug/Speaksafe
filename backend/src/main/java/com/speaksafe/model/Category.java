package com.speaksafe.model;

/** The labels a message can receive. */
public enum Category {
    SAFE,
    INSULT,
    THREAT,
    HATE_SPEECH;

    /** Reads a label from the dataset, e.g. "hate_speech" or "HATE SPEECH". */
    public static Category fromLabel(String label) {
        return Category.valueOf(label.trim().toUpperCase().replace(' ', '_'));
    }
}
