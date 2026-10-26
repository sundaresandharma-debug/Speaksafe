package com.speaksafe.service;

import com.speaksafe.ml.DatasetLoader;
import com.speaksafe.ml.DetectionPipeline;
import com.speaksafe.ml.ModelEvaluator;
import com.speaksafe.ml.NaiveBayesClassifier;
import com.speaksafe.ml.SvmClassifier;
import com.speaksafe.model.Action;
import com.speaksafe.model.AnalysisResult;
import com.speaksafe.model.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MODULE: Categorization + Scoring.
 * Trains the chosen model when the app starts, then for every message:
 *   1. gets category probabilities from the model
 *   2. computes a toxicity score from 0 to 100
 *   3. picks the category label and the action (NONE / WARN / BLUR)
 */
@Service
public class ToxicityDetector {

    private final DetectionPipeline pipeline;
    private final List<DatasetLoader.Sample> dataset;
    private final int warnThreshold;
    private final int blurThreshold;

    public ToxicityDetector(@Value("${speaksafe.model:svm}") String modelName,
                            @Value("${speaksafe.warn-threshold:50}") int warnThreshold,
                            @Value("${speaksafe.blur-threshold:70}") int blurThreshold) {
        this.warnThreshold = warnThreshold;
        this.blurThreshold = blurThreshold;
        this.dataset = DatasetLoader.load(DatasetLoader.DEFAULT_DATASET);
        this.pipeline = new DetectionPipeline(
                modelName.equalsIgnoreCase("naive-bayes") ? new NaiveBayesClassifier() : new SvmClassifier());
        pipeline.train(dataset);
        System.out.println("SpeakSafe: trained " + pipeline.modelName() + " on " + dataset.size() + " messages");
    }

    public AnalysisResult analyze(String text) {
        Map<Category, Double> probabilities = pipeline.predict(text);

        // Toxicity score = how sure the model is that the message is NOT safe.
        int score = (int) Math.round((1.0 - probabilities.get(Category.SAFE)) * 100);

        // Below the warning threshold the message is SAFE.
        // Otherwise it gets the most likely toxic category (insult, threat or hate speech).
        Category category = Category.SAFE;
        if (score >= warnThreshold) {
            double best = -1;
            for (Map.Entry<Category, Double> e : probabilities.entrySet()) {
                if (e.getKey() != Category.SAFE && e.getValue() > best) {
                    best = e.getValue();
                    category = e.getKey();
                }
            }
        }

        return new AnalysisResult(text, score, category, decideAction(category, score),
                probabilities, pipeline.modelName());
    }

    /** MODULE: Action. Decides what the browser extension should do. */
    private Action decideAction(Category category, int score) {
        if (category == Category.SAFE) return Action.NONE;
        if (score >= blurThreshold) return Action.BLUR;
        return Action.WARN;
    }

    /** Evaluates both models on the dataset (for the results page). */
    public List<ModelEvaluator.EvaluationResult> evaluateModels() {
        return List.of(
                ModelEvaluator.evaluate(NaiveBayesClassifier::new, dataset),
                ModelEvaluator.evaluate(SvmClassifier::new, dataset));
    }
}
