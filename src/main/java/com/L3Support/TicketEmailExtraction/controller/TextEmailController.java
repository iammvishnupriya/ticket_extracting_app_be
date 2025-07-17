package com.L3Support.TicketEmailExtraction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.repository.TicketRepository;
import com.L3Support.TicketEmailExtraction.service.TextEmailParserService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/emails")
@CrossOrigin(origins = {
    "http://localhost:3000", 
    "http://localhost:3001", 
    "http://localhost:4200", 
    "http://localhost:5173",
    "http://127.0.0.1:3000",
    "http://127.0.0.1:3001",
    "http://127.0.0.1:4200",
    "http://127.0.0.1:5173"
})
@Slf4j
public class TextEmailController {

    @Autowired
    private TextEmailParserService textEmailParserService;
    
    @Autowired
    private TicketRepository ticketRepository;

    @PostMapping("/process-text")
    public ResponseEntity<Ticket> processTextEmail(@RequestBody String emailContent) {
        try {
            log.info("📧 Processing text email...");
            log.info("📄 Email content length: {} characters", emailContent.length());

            // Parse the email content to extract ticket information
            Ticket ticket = textEmailParserService.parseEmailToTicket(emailContent);
            
            // Save the ticket to the database
            Ticket savedTicket = ticketRepository.save(ticket);
            log.info("💾 Ticket saved to database with ID: {}", savedTicket.getId());
            
            log.info("✅ Email processed successfully");
            log.info("🎫 Ticket details: {}", savedTicket);
            
            return ResponseEntity.ok(savedTicket);
            
        } catch (Exception e) {
            log.error("❌ Error processing email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Ticket.builder()
                    .ticketSummary("Error processing email")
                    .issueDescription("Failed to process email: " + e.getMessage())
                    .build());
        }
    }
}