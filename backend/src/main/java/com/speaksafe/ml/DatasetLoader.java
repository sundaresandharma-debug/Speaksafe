package com.speaksafe.ml;

import com.speaksafe.model.Category;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the labelled dataset (CSV file with the columns: label,text).
 * Example line:   insult,"you are such a loser"
 */
public class DatasetLoader {

    public static final String DEFAULT_DATASET = "/dataset/speaksafe_dataset.csv";

    /** One labelled example. */
    public record Sample(String text, Category label) { }

    public static List<Sample> load(String resourcePath) {
        List<Sample> samples = new ArrayList<>();
        InputStream in = DatasetLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Dataset not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = reader.readLine();   // skip header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                int comma = line.indexOf(',');
                String label = line.substring(0, comma);
                String text = line.substring(comma + 1).trim();
                if (text.startsWith("\"") && text.endsWith("\"")) {
                    text = text.substring(1, text.length() - 1).replace("\"\"", "\"");
                }
                samples.add(new Sample(text, Category.fromLabel(label)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return samples;
    }
}
