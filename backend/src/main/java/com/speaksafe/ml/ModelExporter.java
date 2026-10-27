package com.speaksafe.ml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speaksafe.model.Category;
import com.speaksafe.nlp.TextPreprocessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Exports the trained Linear SVM as a JSON file, so the same model can also run
 * inside the browser extension and the web demo without the Java server.
 *
 *   java -jar speaksafe.jar --export-model ../extension/model.json
 */
public class ModelExporter {

    public static void export(String path, int warnThreshold, int blurThreshold) throws IOException {
        List<DatasetLoader.Sample> dataset = DatasetLoader.load(DatasetLoader.DEFAULT_DATASET);
        SvmClassifier svm = new SvmClassifier();
        DetectionPipeline pipeline = new DetectionPipeline(svm);
        pipeline.train(dataset);

        // Put the features in index order so the arrays below line up with "terms".
        Map<String, Integer> vocabulary = pipeline.vectorizer().vocabulary();
        String[] terms = new String[vocabulary.size()];
        vocabulary.forEach((term, index) -> terms[index] = term);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("name", "SpeakSafe Linear SVM");
        model.put("trainedOn", dataset.size());
        model.put("warnThreshold", warnThreshold);
        model.put("blurThreshold", blurThreshold);
        model.put("categories", Category.values());
        model.put("stopwords", new TreeSet<>(TextPreprocessor.stopwords()));
        model.put("terms", terms);
        model.put("idf", round(pipeline.vectorizer().idf()));
        List<double[]> weights = new ArrayList<>();
        for (double[] row : svm.weights()) weights.add(round(row));
        model.put("weights", weights);
        model.put("bias", round(svm.bias()));

        new ObjectMapper().writeValue(new File(path), model);
        System.out.println("Model exported to " + path + " (" + terms.length + " features)");
    }

    /** Keeps 5 decimal places, which makes the file much smaller without changing results. */
    private static double[] round(double[] values) {
        double[] rounded = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            rounded[i] = Math.round(values[i] * 100000) / 100000.0;
        }
        return rounded;
    }
}
