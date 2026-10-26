package com.speaksafe.model;

/** A flagged or reported message stored in the database. */
public record Report(long id,
                     String text,
                     int score,
                     Category category,
                     String sourceUrl,
                     String reportedBy,
                     String createdAt) {
}
