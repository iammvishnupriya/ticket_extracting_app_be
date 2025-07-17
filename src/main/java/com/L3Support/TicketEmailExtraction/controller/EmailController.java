package com.L3Support.TicketEmailExtraction.controller;

import com.L3Support.TicketEmailExtraction.service.FileBasedEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "L3 Support Email Processing", description = "File-based email processing for L3 support tickets")
public class EmailController {

    private final FileBasedEmailService emailService;

    @Operation(summary = "Process all pending email files", 
               description = "Processes all email files in the input folder and extracts L3 support tickets")
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processEmails() {
        try {
            log.info("🔄 Processing email files...");
            emailService.processAllEmails();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "✅ Email processing completed successfully!",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Email processing failed: {}", e.getMessage());
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "❌ Email processing failed: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Get processing statistics", 
               description = "Returns counts of pending, processed, and error files")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProcessingStats() {
        try {
            FileBasedEmailService.ProcessingStats stats = emailService.getProcessingStats();
            
            Map<String, Object> response = Map.of(
                "pending", stats.getPendingCount(),
                "processed", stats.getProcessedCount(),
                "errors", stats.getErrorCount(),
                "total", stats.getPendingCount() + stats.getProcessedCount() + stats.getErrorCount(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get statistics: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @Operation(summary = "List files in different folders", 
               description = "Lists files in pending, processed, and error folders")
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> listFiles() {
        try {
            Map<String, Object> response = Map.of(
                "pending", getFilesInFolder("./emails"),
                "processed", getFilesInFolder("./emails/processed"),
                "errors", getFilesInFolder("./emails/error"),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to list files: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @Operation(summary = "Test system health", 
               description = "Checks if all required folders exist and are accessible")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        try {
            // Create directories if they don't exist
            Files.createDirectories(Paths.get("./emails"));
            Files.createDirectories(Paths.get("./emails/processed"));
            Files.createDirectories(Paths.get("./emails/error"));
            
            Map<String, Object> response = Map.of(
                "status", "healthy",
                "message", "✅ All systems operational",
                "folders", Map.of(
                    "input", "./emails",
                    "processed", "./emails/processed",
                    "error", "./emails/error"
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "unhealthy",
                "message", "❌ System check failed: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    private List<String> getFilesInFolder(String folderPath) {
        List<String> files = new ArrayList<>();
        try {
            Path path = Paths.get(folderPath);
            if (Files.exists(path)) {
                Files.list(path)
                    .filter(Files::isRegularFile)
                    .filter(file -> {
                        String name = file.getFileName().toString().toLowerCase();
                        return name.endsWith(".txt") || name.endsWith(".eml") || name.endsWith(".msg");
                    })
                    .forEach(file -> files.add(file.getFileName().toString()));
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not list files in {}: {}", folderPath, e.getMessage());
        }
        return files;
    }
}