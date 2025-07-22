package com.L3Support.TicketEmailExtraction.controller;

import com.L3Support.TicketEmailExtraction.enums.Project;
import com.L3Support.TicketEmailExtraction.service.TextEmailParserService;
import com.L3Support.TicketEmailExtraction.utils.FuzzyMatchingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fuzzy-test")
@Slf4j
@Tag(name = "Fuzzy Matching Test", description = "Test fuzzy matching capabilities")
@CrossOrigin(origins = {
    "http://localhost:3000", 
    "http://localhost:3001", 
    "http://localhost:4200", 
    "http://localhost:5173"
})
public class FuzzyMatchingTestController {

    private final FuzzyMatchingUtils fuzzyMatchingUtils;
    private final TextEmailParserService textEmailParserService;

    public FuzzyMatchingTestController(FuzzyMatchingUtils fuzzyMatchingUtils, TextEmailParserService textEmailParserService) {
        this.fuzzyMatchingUtils = fuzzyMatchingUtils;
        this.textEmailParserService = textEmailParserService;
    }

    @Operation(summary = "Test project name fuzzy matching", 
               description = "Test how well the system matches project names with typos")
    @PostMapping("/project-matching")
    public ResponseEntity<Map<String, Object>> testProjectMatching(@RequestBody Map<String, String> request) {
        try {
            String testContent = request.get("content");
            if (testContent == null || testContent.trim().isEmpty()) {
                testContent = "We have an urgnt isue with the CK Alumi portal that needs enhancment";
            }

            Map<String, Object> results = new HashMap<>();
            results.put("testContent", testContent);
            results.put("timestamp", System.currentTimeMillis());

            // Test various project name variations
            Map<String, Double> projectMatches = new HashMap<>();
            List<String> testTerms = Arrays.asList(
                "ck alumni", "ckalumni", "ck alumi", "ckalumi", "ck-alumni", 
                "hepl portal", "heplportal", "hepl portl", "material receipt", 
                "materialreceipt", "my buddy", "mybuddy", "livewire", "ckpl"
            );

            for (String term : testTerms) {
                double similarity = fuzzyMatchingUtils.findBestSimilarityInContent(testContent, term);
                projectMatches.put(term, similarity);
            }

            results.put("projectMatches", projectMatches);

            // Test actual email parsing
            String sampleEmail = String.format("""
                From: test@hepl.com
                Sent: Friday, January 15, 2025 2:30 PM
                To: arun.se@hepl.com
                Subject: Test Email
                
                %s
                
                This is a test email for fuzzy matching.
                """, testContent);

            try {
                var ticket = textEmailParserService.parseEmailToTicket(sampleEmail);
                results.put("extractedProject", ticket.getProject().getDisplayName());
                results.put("extractedPriority", ticket.getPriority().toString());
                results.put("extractedBugType", ticket.getBugType().toString());
            } catch (Exception e) {
                results.put("parsingError", e.getMessage());
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ Error in fuzzy matching test: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Test failed: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @Operation(summary = "Test similarity calculation", 
               description = "Calculate similarity between two strings")
    @PostMapping("/similarity")
    public ResponseEntity<Map<String, Object>> testSimilarity(@RequestBody Map<String, String> request) {
        try {
            String str1 = request.get("string1");
            String str2 = request.get("string2");

            if (str1 == null || str2 == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Both 'string1' and 'string2' are required",
                    "timestamp", System.currentTimeMillis()
                ));
            }

            double jaroWinkler = fuzzyMatchingUtils.calculateJaroWinklerSimilarity(str1, str2);
            int levenshtein = fuzzyMatchingUtils.calculateLevenshteinDistance(str1, str2);
            double contentSimilarity = fuzzyMatchingUtils.findBestSimilarityInContent(str1, str2);

            Map<String, Object> results = Map.of(
                "string1", str1,
                "string2", str2,
                "jaroWinklerSimilarity", jaroWinkler,
                "levenshteinDistance", levenshtein,
                "contentSimilarity", contentSimilarity,
                "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ Error in similarity test: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Test failed: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @Operation(summary = "Get fuzzy matching examples", 
               description = "Get examples of fuzzy matching scenarios")
    @GetMapping("/examples")
    public ResponseEntity<Map<String, Object>> getFuzzyMatchingExamples() {
        Map<String, Object> examples = new HashMap<>();
        
        // Project name examples
        Map<String, List<String>> projectExamples = new HashMap<>();
        projectExamples.put("CK Alumni", Arrays.asList("ck alumni", "ckalumni", "ck alumi", "ck-alumni", "c k alumni"));
        projectExamples.put("HEPL Portal", Arrays.asList("hepl portal", "heplportal", "hepl portl", "h e p l portal"));
        projectExamples.put("Material Receipt", Arrays.asList("material receipt", "materialreceipt", "material reciept", "mat receipt"));
        
        examples.put("projectVariations", projectExamples);
        
        // Priority examples
        Map<String, List<String>> priorityExamples = new HashMap<>();
        priorityExamples.put("High Priority", Arrays.asList("urgent", "urgnt", "critical", "critcal", "high priority", "asap"));
        priorityExamples.put("Low Priority", Arrays.asList("low", "lo", "minor", "minr", "not urgent", "when possible"));
        
        examples.put("priorityVariations", priorityExamples);
        
        // Bug type examples
        Map<String, List<String>> bugTypeExamples = new HashMap<>();
        bugTypeExamples.put("Enhancement", Arrays.asList("enhancement", "enhancment", "feature", "featur", "improvement"));
        bugTypeExamples.put("Bug", Arrays.asList("bug", "bg", "error", "eror", "issue", "isue", "problem", "problm"));
        
        examples.put("bugTypeVariations", bugTypeExamples);
        
        examples.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(examples);
    }
}