package com.speaksafe;

import com.speaksafe.ml.ModelEvaluator;
import com.speaksafe.ml.ModelExporter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of SpeakSafe.
 *
 *   java -jar speaksafe.jar             -> starts the REST API on http://localhost:8080
 *   java -jar speaksafe.jar --evaluate  -> trains both models and prints accuracy, precision, recall, F1
 *   java -jar speaksafe.jar --export-model model.json -> saves the trained model for the browser extension
 */
@SpringBootApplication
public class SpeakSafeApplication {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--evaluate")) {
                ModelEvaluator.printComparison();
                return;
            }
            if (args[i].equals("--export-model") && i + 1 < args.length) {
                ModelExporter.export(args[i + 1], 50, 70);
                return;
            }
        }
        SpringApplication.run(SpeakSafeApplication.class, args);
    }
}
