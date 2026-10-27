package com.speaksafe;

import com.speaksafe.nlp.TextPreprocessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextPreprocessorTest {

    private final TextPreprocessor preprocessor = new TextPreprocessor();

    @Test
    void removesLinksMentionsAndStopwords() {
        List<String> tokens = preprocessor.process("@rahul The link is http://x.com and it is GREAT");
        assertEquals(List.of("link", "great"), tokens);
    }

    @Test
    void undoesLeetspeakAndRepeatedLetters() {
        assertTrue(preprocessor.process("you are a l0s3r").contains("loser"));
        assertTrue(preprocessor.process("stuuuupid").contains("stupid"));
    }

    @Test
    void undoesMaskedSwearWords() {
        assertEquals(List.of("fuck", "off"), preprocessor.process("f*ck off"));
        assertEquals(List.of("fuck", "you"), preprocessor.process("f**k you"));
        assertEquals(List.of("shit"), preprocessor.process("sh*t"));
    }

    @Test
    void stemsWords() {
        assertEquals(List.of("you", "idiot"), preprocessor.process("you idiots"));
    }
}
