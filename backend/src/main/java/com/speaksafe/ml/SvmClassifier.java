package com.speaksafe.ml;

import com.speaksafe.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MODULE: Classification (model 2 of 2).
 * Linear Support Vector Machine (SVM), "one-vs-rest":
 * one straight-line separator is trained per category (e.g. THREAT vs everything else).
 * Training uses stochastic gradient descent on the hinge loss.
 */
public class SvmClassifier implements Classifier {

    private static final int EPOCHS = 30;
    private static final double LEARNING_RATE = 0.1;
    private static final double REGULARIZATION = 0.0001;

    private final Category[] categories = Category.values();
    private double[][] weights;   // one weight per feature, per category
    private double[] bias;

    @Override
    public void train(List<Map<Integer, Double>> vectors, List<Category> labels, int featureCount) {
        weights = new double[categories.length][featureCount];
        bias = new double[categories.length];

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) order.add(i);
        Random random = new Random(42);   // fixed seed -> same result every run

        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            Collections.shuffle(order, random);
            for (int i : order) {
                Map<Integer, Double> x = vectors.get(i);
                for (int c = 0; c < categories.length; c++) {
                    double y = labels.get(i).ordinal() == c ? 1.0 : -1.0;
                    double margin = y * score(c, x);
                    shrinkWeights(c, x);
                    if (margin < 1) {   // wrong or not confident enough -> move the line
                        for (Map.Entry<Integer, Double> e : x.entrySet()) {
                            weights[c][e.getKey()] += LEARNING_RATE * y * e.getValue();
                        }
                        bias[c] += LEARNING_RATE * y;
                    }
                }
            }
        }
    }

    /** L2 regularization, applied only to the features present in this example (keeps training fast). */
    private void shrinkWeights(int c, Map<Integer, Double> x) {
        for (int f : x.keySet()) {
            weights[c][f] *= (1 - LEARNING_RATE * REGULARIZATION);
        }
    }

    private double score(int c, Map<Integer, Double> x) {
        double s = bias[c];
        for (Map.Entry<Integer, Double> e : x.entrySet()) {
            s += weights[c][e.getKey()] * e.getValue();
        }
        return s;
    }

    @Override
    public Map<Category, Double> predictProbabilities(Map<Integer, Double> vector) {
        // An SVM gives distances, not probabilities. We squash each distance with a
        // sigmoid and then rescale so all categories add up to 1.
        double[] p = new double[categories.length];
        double sum = 0;
        for (int c = 0; c < categories.length; c++) {
            p[c] = 1.0 / (1.0 + Math.exp(-3.0 * score(c, vector)));
            sum += p[c];
        }
        Map<Category, Double> probabilities = new EnumMap<>(Category.class);
        for (int c = 0; c < categories.length; c++) {
            probabilities.put(categories[c], p[c] / sum);
        }
        return probabilities;
    }

    @Override
    public String name() {
        return "Linear SVM";
    }
}
