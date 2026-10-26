package com.speaksafe.ml;

import com.speaksafe.model.Category;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * MODULE: Classification (model 1 of 2).
 * Multinomial Naive Bayes: learns how often each word appears in each category,
 * then picks the category that best explains the words of a new message.
 */
public class NaiveBayesClassifier implements Classifier {

    private static final double SMOOTHING = 0.1;   // stops unseen words from giving zero probability

    private final Category[] categories = Category.values();
    private double[] logPrior;                     // log P(category)
    private double[][] logLikelihood;              // log P(word | category)

    @Override
    public void train(List<Map<Integer, Double>> vectors, List<Category> labels, int featureCount) {
        int k = categories.length;
        double[] docsPerClass = new double[k];
        double[][] wordWeight = new double[k][featureCount];
        double[] totalWeight = new double[k];

        for (int i = 0; i < vectors.size(); i++) {
            int c = labels.get(i).ordinal();
            docsPerClass[c]++;
            for (Map.Entry<Integer, Double> entry : vectors.get(i).entrySet()) {
                wordWeight[c][entry.getKey()] += entry.getValue();
                totalWeight[c] += entry.getValue();
            }
        }

        logPrior = new double[k];
        logLikelihood = new double[k][featureCount];
        for (int c = 0; c < k; c++) {
            logPrior[c] = Math.log(docsPerClass[c] / vectors.size());
            double denominator = totalWeight[c] + SMOOTHING * featureCount;
            for (int f = 0; f < featureCount; f++) {
                logLikelihood[c][f] = Math.log((wordWeight[c][f] + SMOOTHING) / denominator);
            }
        }
    }

    @Override
    public Map<Category, Double> predictProbabilities(Map<Integer, Double> vector) {
        double[] logScores = new double[categories.length];
        for (int c = 0; c < categories.length; c++) {
            logScores[c] = logPrior[c];
            for (Map.Entry<Integer, Double> entry : vector.entrySet()) {
                logScores[c] += entry.getValue() * logLikelihood[c][entry.getKey()];
            }
        }
        return toProbabilities(logScores);
    }

    /** Softmax: converts log scores into probabilities that add up to 1. */
    private Map<Category, Double> toProbabilities(double[] logScores) {
        double max = Double.NEGATIVE_INFINITY;
        for (double s : logScores) max = Math.max(max, s);
        double sum = 0;
        double[] exp = new double[logScores.length];
        for (int c = 0; c < logScores.length; c++) {
            exp[c] = Math.exp(logScores[c] - max);
            sum += exp[c];
        }
        Map<Category, Double> probabilities = new EnumMap<>(Category.class);
        for (int c = 0; c < categories.length; c++) {
            probabilities.put(categories[c], exp[c] / sum);
        }
        return probabilities;
    }

    @Override
    public String name() {
        return "Naive Bayes";
    }
}
