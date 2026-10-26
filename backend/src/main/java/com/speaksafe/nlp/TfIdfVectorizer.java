package com.speaksafe.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * MODULE: Feature Extraction.
 * Converts a list of words into numbers using TF-IDF.
 * It uses single words ("kill") and word pairs ("kill you") as features.
 * Vectors are stored as a map of {feature index -> weight}, because most weights are zero.
 */
public class TfIdfVectorizer {

    private final Map<String, Integer> vocabulary = new HashMap<>();
    private double[] idf;

    /** Learns the vocabulary and IDF weights from the training documents. */
    public void fit(List<List<String>> documents) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<String> doc : documents) {
            for (String term : new HashSet<>(terms(doc))) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        for (String term : documentFrequency.keySet()) {
            vocabulary.put(term, vocabulary.size());
        }
        int n = documents.size();
        idf = new double[vocabulary.size()];
        for (Map.Entry<String, Integer> entry : vocabulary.entrySet()) {
            int df = documentFrequency.get(entry.getKey());
            idf[entry.getValue()] = Math.log((1.0 + n) / (1.0 + df)) + 1.0;
        }
    }

    /** Turns one document into a normalized TF-IDF vector. Unknown words are ignored. */
    public Map<Integer, Double> transform(List<String> doc) {
        Map<Integer, Double> vector = new HashMap<>();
        for (String term : terms(doc)) {
            Integer index = vocabulary.get(term);
            if (index != null) {
                vector.merge(index, 1.0, Double::sum);         // term frequency
            }
        }
        double length = 0;
        for (Map.Entry<Integer, Double> entry : vector.entrySet()) {
            double weight = entry.getValue() * idf[entry.getKey()];
            entry.setValue(weight);
            length += weight * weight;
        }
        double norm = Math.sqrt(length);
        if (norm > 0) {
            vector.replaceAll((index, weight) -> weight / norm);
        }
        return vector;
    }

    public int size() {
        return vocabulary.size();
    }

    /** Single words plus pairs of neighbouring words. */
    private List<String> terms(List<String> words) {
        List<String> terms = new ArrayList<>(words);
        for (int i = 0; i + 1 < words.size(); i++) {
            terms.add(words.get(i) + "_" + words.get(i + 1));
        }
        return terms;
    }
}
