package com.speaksafe.ml;

import com.speaksafe.model.Category;

import java.util.List;
import java.util.Map;

/** Common interface so the Naive Bayes and SVM models can be swapped and compared. */
public interface Classifier {

    /** Learns from TF-IDF vectors and their correct labels. */
    void train(List<Map<Integer, Double>> vectors, List<Category> labels, int featureCount);

    /** Returns the probability (0.0 - 1.0) of each category for one TF-IDF vector. */
    Map<Category, Double> predictProbabilities(Map<Integer, Double> vector);

    String name();
}
