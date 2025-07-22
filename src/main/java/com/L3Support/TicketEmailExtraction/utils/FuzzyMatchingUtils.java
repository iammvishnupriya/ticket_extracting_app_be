package com.L3Support.TicketEmailExtraction.utils;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for fuzzy matching operations
 * Provides helper methods for text similarity calculations
 */
@Component
@Slf4j
public class FuzzyMatchingUtils {
    
    private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    
    /**
     * Calculate Jaro-Winkler similarity between two strings
     * @param str1 First string
     * @param str2 Second string
     * @return Similarity score between 0.0 and 1.0
     */
    public double calculateJaroWinklerSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        return jaroWinklerSimilarity.apply(str1.toLowerCase(), str2.toLowerCase());
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     * @param str1 First string
     * @param str2 Second string
     * @return Edit distance (lower is more similar)
     */
    public int calculateLevenshteinDistance(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return Integer.MAX_VALUE;
        }
        return levenshteinDistance.apply(str1.toLowerCase(), str2.toLowerCase());
    }
    
    /**
     * Find the best similarity match for a search term within content
     * @param content The content to search in
     * @param searchTerm The term to search for
     * @return Best similarity score found
     */
    public double findBestSimilarityInContent(String content, String searchTerm) {
        if (content == null || searchTerm == null) {
            return 0.0;
        }
        
        String contentLower = content.toLowerCase();
        String searchLower = searchTerm.toLowerCase();
        
        // Direct substring match (highest priority)
        if (contentLower.contains(searchLower)) {
            return 1.0; // Perfect match
        }
        
        double bestSimilarity = 0.0;
        String[] words = contentLower.split("\\s+");
        
        // Check individual words
        for (String word : words) {
            double similarity = jaroWinklerSimilarity.apply(word, searchLower);
            bestSimilarity = Math.max(bestSimilarity, similarity);
        }
        
        // Check word combinations (for multi-word search terms)
        if (searchLower.contains(" ")) {
            String[] searchWords = searchLower.split("\\s+");
            for (int i = 0; i <= words.length - searchWords.length; i++) {
                StringBuilder phrase = new StringBuilder();
                for (int j = 0; j < searchWords.length; j++) {
                    if (j > 0) phrase.append(" ");
                    phrase.append(words[i + j]);
                }
                double similarity = jaroWinklerSimilarity.apply(phrase.toString(), searchLower);
                bestSimilarity = Math.max(bestSimilarity, similarity);
            }
        }
        
        return bestSimilarity;
    }
    
    /**
     * Check if any term in the list matches the content above the threshold
     * @param content Content to search in
     * @param terms List of terms to search for
     * @param threshold Minimum similarity threshold
     * @return The matching term if found, null otherwise
     */
    public String findMatchingTerm(String content, List<String> terms, double threshold) {
        for (String term : terms) {
            double similarity = findBestSimilarityInContent(content, term);
            if (similarity > threshold) {
                log.debug("🎯 Found match: '{}' with similarity {:.2f}", term, similarity);
                return term;
            }
        }
        return null;
    }
    
    /**
     * Test fuzzy matching with debug output
     * @param content Content to test
     * @param searchTerm Term to search for
     * @return Similarity score with debug logging
     */
    public double testFuzzyMatch(String content, String searchTerm) {
        double similarity = findBestSimilarityInContent(content, searchTerm);
        log.info("🧪 Fuzzy Match Test:");
        log.info("   Content: '{}'", content.length() > 100 ? content.substring(0, 100) + "..." : content);
        log.info("   Search Term: '{}'", searchTerm);
        log.info("   Similarity: {:.2f}", similarity);
        return similarity;
    }
}