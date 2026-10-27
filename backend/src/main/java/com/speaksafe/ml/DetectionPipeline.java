package com.speaksafe.ml;

import com.speaksafe.model.Category;
import com.speaksafe.nlp.TextPreprocessor;
import com.speaksafe.nlp.TfIdfVectorizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Connects the modules in order:
 *   raw text -> Preprocessing -> Feature Extraction (TF-IDF) -> Classification -> probabilities
 */
public class DetectionPipeline {

    private final TextPreprocessor preprocessor = new TextPreprocessor();
    private final TfIdfVectorizer vectorizer = new TfIdfVectorizer();
    private final Classifier classifier;

    public DetectionPipeline(Classifier classifier) {
        this.classifier = classifier;
    }

    public void train(List<DatasetLoader.Sample> samples) {
        List<List<String>> documents = new ArrayList<>();
        List<Category> labels = new ArrayList<>();
        for (DatasetLoader.Sample sample : samples) {
            documents.add(preprocessor.process(sample.text()));
            labels.add(sample.label());
        }
        vectorizer.fit(documents);

        List<Map<Integer, Double>> vectors = new ArrayList<>();
        for (List<String> doc : documents) {
            vectors.add(vectorizer.transform(doc));
        }
        classifier.train(vectors, labels, vectorizer.size());
    }

    public Map<Category, Double> predict(String text) {
        Map<Integer, Double> vector = vectorizer.transform(preprocessor.process(text));
        if (vector.isEmpty()) {
            // No known words (e.g. "ok", emojis, a new language): treat as safe.
            Map<Category, Double> safe = new EnumMap<>(Category.class);
            for (Category c : Category.values()) safe.put(c, c == Category.SAFE ? 1.0 : 0.0);
            return safe;
        }
        return classifier.predictProbabilities(vector);
    }

    /** The category with the highest probability. */
    public Category predictCategory(String text) {
        Map<Category, Double> probabilities = predict(text);
        Category best = Category.SAFE;
        for (Map.Entry<Category, Double> e : probabilities.entrySet()) {
            if (e.getValue() > probabilities.get(best)) best = e.getKey();
        }
        return best;
    }

    public TfIdfVectorizer vectorizer() {
        return vectorizer;
    }

    public Classifier classifier() {
        return classifier;
    }

    public String modelName() {
        return classifier.name();
    }
}
