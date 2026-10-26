package com.speaksafe;

import com.speaksafe.ml.ModelEvaluator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of SpeakSafe.
 *
 *   java -jar speaksafe.jar             -> starts the REST API on http://localhost:8080
 *   java -jar speaksafe.jar --evaluate  -> trains both models and prints accuracy, precision, recall, F1
 */
@SpringBootApplication
public class SpeakSafeApplication {

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--evaluate")) {
                ModelEvaluator.printComparison();
                return;
            }
        }
        SpringApplication.run(SpeakSafeApplication.class, args);
    }
}
