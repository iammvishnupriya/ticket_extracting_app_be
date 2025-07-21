package com.L3Support.TicketEmailExtraction.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.model.TicketEditRequest;
import com.L3Support.TicketEmailExtraction.model.TicketResponse;
import com.L3Support.TicketEmailExtraction.service.TicketService;
import com.L3Support.TicketEmailExtraction.service.TicketContributorService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/tickets")
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
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final TicketContributorService ticketContributorService;

    @Operation(summary = "🔍 Get all tickets")
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        try {
            List<TicketResponse> tickets = ticketService.getAllTickets();
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "📄 Get ticket by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        try {
            Optional<TicketResponse> ticket = ticketService.getTicketById(id);
            return ticket.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "📝 Add ticket manually (optional)")
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody Ticket ticket) {
        try {
            TicketResponse created = ticketService.createTicket(ticket);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "✏️ Update existing ticket")
    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long id, @RequestBody Ticket updatedTicket) {
        try {
            TicketResponse updated = ticketService.updateTicket(id, updatedTicket);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "📝 Edit all ticket details comprehensively (Flexible Format)")
    @PutMapping("/{id}/edit-details")
    public ResponseEntity<TicketResponse> editTicketDetails(@PathVariable Long id, @RequestBody TicketEditRequest editRequest) {
        try {
            log.debug("Editing all details for ticket ID: {} with summary: {}", id, editRequest.getTicketSummary());
            
            // Convert DTO to entity
            Ticket updatedTicket = editRequest.toTicketEntity();
            updatedTicket.setId(id); // Ensure the ID matches the path parameter
            
            // Use the existing update service with enhanced contributor handling
            TicketResponse updated = ticketService.updateTicket(id, updatedTicket);
            
            log.info("Successfully updated all details for ticket ID: {}", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Ticket not found for editing: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error editing ticket details for ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "📝 Edit all ticket details using direct JSON format")
    @PutMapping("/{id}/edit-json")
    public ResponseEntity<TicketResponse> editTicketDetailsJson(@PathVariable Long id, @RequestBody Ticket ticketData) {
        try {
            log.debug("Editing ticket ID: {} using direct JSON format", id);
            
            // Ensure the ID matches the path parameter
            ticketData.setId(id);
            
            // Use the existing update service with enhanced contributor handling
            TicketResponse updated = ticketService.updateTicket(id, ticketData);
            
            log.info("Successfully updated ticket ID: {} using JSON format", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Ticket not found for editing: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error editing ticket using JSON format for ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "❌ Delete a ticket")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTicket(@PathVariable Long id) {
        try {
            ticketService.deleteTicket(id);
            return ResponseEntity.ok("Ticket deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== Ticket-Contributor Relationship Endpoints ====================

    @Operation(summary = "👤 Assign contributor to ticket by ID")
    @PutMapping("/{ticketId}/contributor/{contributorId}")
    public ResponseEntity<String> assignContributorToTicket(
            @PathVariable Long ticketId, 
            @PathVariable Long contributorId) {
        try {
            ticketContributorService.assignContributorToTicket(ticketId, contributorId);
            return ResponseEntity.ok("Contributor assigned successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error assigning contributor");
        }
    }

    @Operation(summary = "👤 Assign contributor to ticket by name")
    @PutMapping("/{ticketId}/contributor")
    public ResponseEntity<String> assignContributorToTicketByName(
            @PathVariable Long ticketId, 
            @RequestParam String contributorName) {
        try {
            ticketContributorService.assignContributorToTicketByName(ticketId, contributorName);
            return ResponseEntity.ok("Contributor assigned successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error assigning contributor");
        }
    }

    @Operation(summary = "🚫 Remove contributor from ticket")
    @DeleteMapping("/{ticketId}/contributor")
    public ResponseEntity<String> removeContributorFromTicket(@PathVariable Long ticketId) {
        try {
            ticketContributorService.removeContributorFromTicket(ticketId);
            return ResponseEntity.ok("Contributor removed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error removing contributor");
        }
    }

    @Operation(summary = "📋 Get contributor name for ticket")
    @GetMapping("/{ticketId}/contributor")
    public ResponseEntity<String> getContributorForTicket(@PathVariable Long ticketId) {
        try {
            String contributorName = ticketContributorService.getContributorNameForTicket(ticketId);
            return ResponseEntity.ok(contributorName);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching contributor");
        }
    }

    // ==================== Additional Ticket Query Endpoints ====================
    
    @Operation(summary = "📊 Get tickets by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TicketResponse>> getTicketsByStatus(@PathVariable String status) {
        try {
            List<TicketResponse> tickets = ticketService.getTicketsByStatus(status);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "🔥 Get tickets by priority")
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TicketResponse>> getTicketsByPriority(@PathVariable String priority) {
        try {
            List<TicketResponse> tickets = ticketService.getTicketsByPriority(priority);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
