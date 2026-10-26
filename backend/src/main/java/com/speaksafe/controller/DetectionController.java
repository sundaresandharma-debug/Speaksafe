package com.speaksafe.controller;

import com.speaksafe.ml.ModelEvaluator;
import com.speaksafe.model.Action;
import com.speaksafe.model.AnalysisResult;
import com.speaksafe.model.Report;
import com.speaksafe.service.ReportRepository;
import com.speaksafe.service.ToxicityDetector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MODULE: Input (REST API).
 * The browser extension and the web page send messages here.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class DetectionController {

    private static final int MAX_LENGTH = 5000;
    private static final int MAX_BATCH = 50;

    private final ToxicityDetector detector;
    private final ReportRepository reports;

    public DetectionController(ToxicityDetector detector, ReportRepository reports) {
        this.detector = detector;
        this.reports = reports;
    }

    /** Request bodies. autoFlag = save the message automatically if it gets blurred. */
    public record AnalyzeRequest(String text, String sourceUrl, boolean autoFlag) { }
    public record BatchRequest(List<String> texts, String sourceUrl, boolean autoFlag) { }
    public record ReportRequest(String text, String sourceUrl) { }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    /** Analyze one message. */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyze(@RequestBody AnalyzeRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyzeAndFlag(request.text(), request.sourceUrl(), request.autoFlag()));
    }

    /** Analyze many messages at once (used when the extension scans a page). */
    @PostMapping("/analyze/batch")
    public ResponseEntity<List<AnalysisResult>> analyzeBatch(@RequestBody BatchRequest request) {
        if (request.texts() == null || request.texts().size() > MAX_BATCH) {
            return ResponseEntity.badRequest().build();
        }
        List<AnalysisResult> results = new ArrayList<>();
        for (String text : request.texts()) {
            results.add(analyzeAndFlag(text == null ? "" : text, request.sourceUrl(), request.autoFlag()));
        }
        return ResponseEntity.ok(results);
    }

    /** A person reports a message to the moderators. */
    @PostMapping("/report")
    public ResponseEntity<AnalysisResult> report(@RequestBody ReportRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String text = limit(request.text());
        AnalysisResult result = detector.analyze(text);
        reports.save(text, result.score(), result.category(), request.sourceUrl(), "USER");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reports")
    public List<Report> listReports() {
        return reports.findAll();
    }

    @DeleteMapping("/reports/{id}")
    public void deleteReport(@PathVariable long id) {
        reports.delete(id);
    }

    @GetMapping("/stats")
    public Map<String, Integer> stats() {
        return reports.countByCategory();
    }

    /** Accuracy, precision, recall and F1 for Naive Bayes vs SVM. */
    @GetMapping("/evaluate")
    public List<ModelEvaluator.EvaluationResult> evaluate() {
        return detector.evaluateModels();
    }

    private AnalysisResult analyzeAndFlag(String text, String sourceUrl, boolean autoFlag) {
        text = limit(text);
        AnalysisResult result = detector.analyze(text);
        if (autoFlag && result.action() == Action.BLUR) {
            reports.save(text, result.score(), result.category(), sourceUrl, "AUTO");
        }
        return result;
    }

    private String limit(String text) {
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }
}
