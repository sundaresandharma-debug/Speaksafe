package com.speaksafe.ml;

import com.speaksafe.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Evaluation: splits the dataset into 80% training and 20% testing data,
 * trains a model, and measures accuracy, precision, recall, F1-score and speed.
 */
public class ModelEvaluator {

    /** Scores for one category. */
    public record ClassMetrics(double precision, double recall, double f1, int support) { }

    /** Full result for one model. */
    public record EvaluationResult(String model,
                                   int trainSize,
                                   int testSize,
                                   double accuracy,
                                   double macroPrecision,
                                   double macroRecall,
                                   double macroF1,
                                   double avgMillisPerMessage,
                                   Map<Category, ClassMetrics> perCategory,
                                   int[][] confusionMatrix) { }

    public static EvaluationResult evaluate(Supplier<Classifier> modelFactory, List<DatasetLoader.Sample> data) {
        List<DatasetLoader.Sample> train = new ArrayList<>();
        List<DatasetLoader.Sample> test = new ArrayList<>();
        splitStratified(data, train, test);

        DetectionPipeline pipeline = new DetectionPipeline(modelFactory.get());
        pipeline.train(train);

        int k = Category.values().length;
        int[][] confusion = new int[k][k];   // rows = actual, columns = predicted
        long start = System.nanoTime();
        for (DatasetLoader.Sample sample : test) {
            Category predicted = pipeline.predictCategory(sample.text());
            confusion[sample.label().ordinal()][predicted.ordinal()]++;
        }
        double avgMillis = (System.nanoTime() - start) / 1_000_000.0 / test.size();

        int correct = 0;
        double sumP = 0, sumR = 0, sumF = 0;
        Map<Category, ClassMetrics> perCategory = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            int i = c.ordinal();
            int truePositive = confusion[i][i];
            int predictedAsC = 0, actuallyC = 0;
            for (int j = 0; j < k; j++) {
                predictedAsC += confusion[j][i];
                actuallyC += confusion[i][j];
            }
            correct += truePositive;
            double precision = predictedAsC == 0 ? 0 : (double) truePositive / predictedAsC;
            double recall = actuallyC == 0 ? 0 : (double) truePositive / actuallyC;
            double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
            perCategory.put(c, new ClassMetrics(precision, recall, f1, actuallyC));
            sumP += precision;
            sumR += recall;
            sumF += f1;
        }
        return new EvaluationResult(pipeline.modelName(), train.size(), test.size(),
                (double) correct / test.size(), sumP / k, sumR / k, sumF / k,
                avgMillis, perCategory, confusion);
    }

    /** Puts 80% of every category into training and 20% into testing, the same way every run. */
    private static void splitStratified(List<DatasetLoader.Sample> data,
                                        List<DatasetLoader.Sample> train,
                                        List<DatasetLoader.Sample> test) {
        Random random = new Random(7);
        for (Category c : Category.values()) {
            List<DatasetLoader.Sample> group = new ArrayList<>();
            for (DatasetLoader.Sample s : data) {
                if (s.label() == c) group.add(s);
            }
            Collections.shuffle(group, random);
            int cut = (int) Math.round(group.size() * 0.8);
            train.addAll(group.subList(0, cut));
            test.addAll(group.subList(cut, group.size()));
        }
    }

    /** Runs both models and prints a comparison table (used with --evaluate). */
    public static void printComparison() {
        List<DatasetLoader.Sample> data = DatasetLoader.load(DatasetLoader.DEFAULT_DATASET);
        System.out.println("\n================ SpeakSafe Model Evaluation ================");
        System.out.println("Dataset size: " + data.size() + " messages (80% train / 20% test)\n");

        List<EvaluationResult> results = List.of(
                evaluate(NaiveBayesClassifier::new, data),
                evaluate(SvmClassifier::new, data));

        System.out.printf("%-14s %9s %10s %8s %9s %12s%n",
                "Model", "Accuracy", "Precision", "Recall", "F1-score", "ms/message");
        for (EvaluationResult r : results) {
            System.out.printf("%-14s %8.1f%% %9.1f%% %7.1f%% %8.1f%% %12.3f%n", r.model(),
                    r.accuracy() * 100, r.macroPrecision() * 100, r.macroRecall() * 100,
                    r.macroF1() * 100, r.avgMillisPerMessage());
        }

        for (EvaluationResult r : results) {
            System.out.println("\n--- " + r.model() + ": per category ---");
            System.out.printf("%-12s %10s %8s %9s %8s%n", "Category", "Precision", "Recall", "F1-score", "Support");
            for (Map.Entry<Category, ClassMetrics> e : r.perCategory().entrySet()) {
                ClassMetrics m = e.getValue();
                System.out.printf("%-12s %9.1f%% %7.1f%% %8.1f%% %8d%n", e.getKey(),
                        m.precision() * 100, m.recall() * 100, m.f1() * 100, m.support());
            }
            System.out.println("Confusion matrix (rows = actual, columns = predicted):");
            System.out.printf("%-12s", "");
            for (Category c : Category.values()) System.out.printf("%12s", c);
            System.out.println();
            for (Category actual : Category.values()) {
                System.out.printf("%-12s", actual);
                for (Category predicted : Category.values()) {
                    System.out.printf("%12d", r.confusionMatrix()[actual.ordinal()][predicted.ordinal()]);
                }
                System.out.println();
            }
        }
        System.out.println("============================================================\n");
    }
}
