package com.L3Support.TicketEmailExtraction;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.service.TicketParserService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class DateExtractionTest {

    @Autowired
    private TicketParserService ticketParserService;

    @Test
    public void testDateExtractionFromSampleEmail() {
        String sampleEmail = """
            From: Software Support <SoftwareSupport@hepl.com>
            Sent: 12 July 2025 10:26
            To: Arun Prasad S/IT/HEPL <arun.se@hepl.com>
            Cc: Ganagavathy K G V/IT/HEPL <ganaga.v@hepl.com>; Ajaykiran S/IT/HEPL <ajay.s@hepl.com>
            Subject: Fw: Cab request
             
            Dear Arun,

            As per the user request, kindly update the supervisor details for Mrs. Rebecca from Srikanth to Vairam.

            Thanks and Regards,
            Prithika K
            Software Support
            04142 350036
            """;

        log.info("🧪 Testing date extraction from sample email");
        
        Ticket ticket = ticketParserService.parseEmailToTicket(sampleEmail);
        
        if (ticket != null) {
            log.info("✅ Ticket created successfully");
            log.info("📅 Received Date: {}", ticket.getReceivedDate());
            log.info("📧 Subject: {}", ticket.getTicketSummary());
            
            // Expected date should be 2025-07-12
            LocalDate expectedDate = LocalDate.of(2025, 7, 12);
            if (expectedDate.equals(ticket.getReceivedDate())) {
                log.info("✅ Date extraction SUCCESS! Expected: {}, Actual: {}", expectedDate, ticket.getReceivedDate());
            } else {
                log.error("❌ Date extraction FAILED! Expected: {}, Actual: {}", expectedDate, ticket.getReceivedDate());
            }
        } else {
            log.error("❌ Failed to create ticket from email");
        }
    }

    @Test
    public void testDateExtractionFromSecondFormat() {
        String sampleEmail = """
            From: Software Support <SoftwareSupport@hepl.com>
            Sent: Friday, July 11, 2025 11:25 AM
            To: Arun Prasad S/IT/HEPL <arun.se@hepl.com>
            Subject: Test email
             
            Dear Arun,

            This is a test email.

            Thanks and Regards,
            Test User
            """;

        log.info("🧪 Testing date extraction from second format email");
        
        Ticket ticket = ticketParserService.parseEmailToTicket(sampleEmail);
        
        if (ticket != null) {
            log.info("✅ Ticket created successfully");
            log.info("📅 Received Date: {}", ticket.getReceivedDate());
            
            // Expected date should be 2025-07-11
            LocalDate expectedDate = LocalDate.of(2025, 7, 11);
            if (expectedDate.equals(ticket.getReceivedDate())) {
                log.info("✅ Date extraction SUCCESS! Expected: {}, Actual: {}", expectedDate, ticket.getReceivedDate());
            } else {
                log.error("❌ Date extraction FAILED! Expected: {}, Actual: {}", expectedDate, ticket.getReceivedDate());
            }
        } else {
            log.error("❌ Failed to create ticket from email");
        }
    }
}