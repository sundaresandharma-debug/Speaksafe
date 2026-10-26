package com.speaksafe.model;

import java.util.Map;

/**
 * Output of the detection engine for one message.
 *
 * @param text          the original message
 * @param score         toxicity score from 0 (safe) to 100 (very toxic)
 * @param category      SAFE, INSULT, THREAT or HATE_SPEECH
 * @param action        NONE, WARN or BLUR
 * @param probabilities probability of each category (0.0 - 1.0)
 * @param model         name of the model that produced the result
 */
public record AnalysisResult(String text,
                             int score,
                             Category category,
                             Action action,
                             Map<Category, Double> probabilities,
                             String model) {
}
