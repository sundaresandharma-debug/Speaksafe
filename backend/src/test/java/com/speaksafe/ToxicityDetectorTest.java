package com.speaksafe;

import com.speaksafe.ml.DatasetLoader;
import com.speaksafe.ml.ModelEvaluator;
import com.speaksafe.ml.NaiveBayesClassifier;
import com.speaksafe.model.Action;
import com.speaksafe.model.AnalysisResult;
import com.speaksafe.model.Category;
import com.speaksafe.service.ToxicityDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToxicityDetectorTest {

    private final ToxicityDetector detector = new ToxicityDetector("svm", 50, 70);

    @Test
    void detectsEachCategory() {
        assertEquals(Category.INSULT, detector.analyze("you are a complete idiot").category());
        assertEquals(Category.THREAT, detector.analyze("I will beat you up after school").category());
        assertEquals(Category.HATE_SPEECH, detector.analyze("refugees should go back where they came from").category());
    }

    @Test
    void safeMessagesAreNotFlagged() {
        AnalysisResult result = detector.analyze("thanks for helping me with the homework");
        assertEquals(Category.SAFE, result.category());
        assertEquals(Action.NONE, result.action());
        assertTrue(result.score() < 50);
    }

    @Test
    void everydayMessagesAreNotFlagged() {
        String[] messages = {"You", "I'll upload them to the drive tonight", "I'll call you tomorrow",
                "you're welcome!", "she's coming back from Delhi on monday", "you look nice in that photo",
                "the Christian club is organising a charity run", "this homework is killing me lol"};
        for (String message : messages) {
            assertEquals(Action.NONE, detector.analyze(message).action(), message);
        }
    }

    @Test
    void profanityAimedAtSomeoneIsFlagged() {
        String[] messages = {"fuck off", "fuck you", "f*ck off", "f**k you", "you piece of shit", "what an asshole"};
        for (String message : messages) {
            assertEquals(Category.INSULT, detector.analyze(message).category(), message);
            assertEquals(Action.BLUR, detector.analyze(message).action(), message);
        }
    }

    @Test
    void mildExclamationsAreNotFlagged() {
        assertEquals(Action.NONE, detector.analyze("damn this assignment is long").action());
    }

    @Test
    void unknownWordsAreSafe() {
        assertEquals(0, detector.analyze("😊👍").score());
    }

    @Test
    void toxicMessagesAreBlurred() {
        AnalysisResult result = detector.analyze("I know where you live");
        assertEquals(Action.BLUR, result.action());
        assertTrue(result.score() >= 70);
    }

    @Test
    void evaluationProducesSensibleMetrics() {
        ModelEvaluator.EvaluationResult result = ModelEvaluator.evaluate(
                NaiveBayesClassifier::new, DatasetLoader.load(DatasetLoader.DEFAULT_DATASET));
        assertTrue(result.accuracy() > 0.8);
        assertTrue(result.testSize() > 0);
    }
}
