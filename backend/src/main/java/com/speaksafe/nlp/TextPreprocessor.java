package com.speaksafe.nlp;

import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MODULE: Preprocessing.
 * Cleans raw text and turns it into a list of word stems, for example:
 *   "You are SUCH an 1d10t!!! http://x.com"  ->  [you, such, idiot]
 */
public class TextPreprocessor {

    // Common words that carry no meaning. Words like "you", "not", "all" and "die"
    // are kept on purpose because they matter a lot for bullying and threats.
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "is", "am", "are", "was", "were", "be", "been", "being",
            "and", "or", "but", "if", "of", "at", "by", "for", "with", "about", "to",
            "from", "in", "on", "this", "that", "these", "those", "it", "its", "so",
            "as", "do", "does", "did", "have", "has", "had", "just", "very", "can",
            "will", "would", "should", "could", "i", "me", "my", "we", "our", "us",
            "then", "than", "there", "here", "what", "which", "who", "when", "where",
            "how", "too", "also", "such", "some", "any", "into", "up", "out", "over");

    private final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
    private final PorterStemmer stemmer = new PorterStemmer();

    public List<String> process(String text) {
        if (text == null) {
            return List.of();
        }
        String clean = text.toLowerCase();
        clean = clean.replaceAll("https?://\\S+|www\\.\\S+", " ");   // remove links
        clean = clean.replaceAll("@\\w+", " ");                     // remove @mentions
        clean = expandContractions(clean);                          // you're -> you are
        clean = normalizeLeetspeak(clean);                          // 1d10t -> idiot
        clean = clean.replaceAll("(.)\\1{2,}", "$1");               // stuuupid -> stupid
        clean = clean.replaceAll("[^a-z\\s]", " ");                 // remove symbols and digits

        List<String> tokens = new ArrayList<>();
        for (String token : tokenizer.tokenize(clean)) {
            if (token.length() > 1 && !STOPWORDS.contains(token)) {
                tokens.add(stemmer.stem(token));
            }
        }
        return tokens;
    }

    /** Writes short forms out in full, so "I'll" and "you're" become real words instead of "ll" and "re". */
    private String expandContractions(String text) {
        return text.replace('\u2019', '\'')                       // curly apostrophe -> straight
                   .replaceAll("\\bcan't\\b", "can not")
                   .replaceAll("\\bwon't\\b", "will not")
                   .replaceAll("n't\\b", " not")
                   .replaceAll("'re\\b", " are")
                   .replaceAll("'ll\\b", " will")
                   .replaceAll("'m\\b", " am")
                   .replaceAll("'ve\\b", " have")
                   .replaceAll("'d\\b", " would")
                   .replaceAll("'s\\b", "");
    }

    /** Replaces digits and symbols used to hide bad words, but only inside words that contain letters. */
    private String normalizeLeetspeak(String text) {
        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.matches(".*[a-z].*")) {
                word = word.replace('0', 'o').replace('1', 'i').replace('3', 'e')
                           .replace('4', 'a').replace('5', 's').replace('7', 't')
                           .replace('@', 'a').replace('$', 's');
            }
            result.append(word).append(' ');
        }
        return result.toString();
    }
}
