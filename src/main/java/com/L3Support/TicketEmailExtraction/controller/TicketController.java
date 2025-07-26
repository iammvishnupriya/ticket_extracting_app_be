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
import org.springframework.web.bind.annotation.RestController;

import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.model.TicketEditRequest;
import com.L3Support.TicketEmailExtraction.model.TicketResponse;
import com.L3Support.TicketEmailExtraction.service.TicketService;
import com.L3Support.TicketEmailExtraction.service.TicketContributorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tickets", description = "Ticket management operations")
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

    // ==================== Legacy Single Contributor Support (Backward Compatibility) ====================

    @Operation(summary = "👤 Assign single contributor to ticket by ID (Legacy)")
    @PutMapping("/{ticketId}/contributor/{contributorId}")
    public ResponseEntity<String> assignSingleContributorToTicket(
            @PathVariable Long ticketId, 
            @PathVariable Long contributorId) {
        try {
            ticketContributorService.addContributorToTicket(ticketId, contributorId);
            return ResponseEntity.ok("Contributor assigned successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error assigning contributor");
        }
    }

    @Operation(summary = "🚫 Remove all contributors from ticket (Legacy)")
    @DeleteMapping("/{ticketId}/contributor")
    public ResponseEntity<String> removeAllContributorsLegacy(@PathVariable Long ticketId) {
        try {
            ticketContributorService.removeAllContributorsFromTicket(ticketId);
            return ResponseEntity.ok("Contributors removed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error removing contributors");
        }
    }

    @Operation(summary = "📋 Get contributor names for ticket (Legacy)")
    @GetMapping("/{ticketId}/contributor")
    public ResponseEntity<List<String>> getContributorNamesForTicket(@PathVariable Long ticketId) {
        try {
            List<String> contributorNames = ticketContributorService.getContributorNamesForTicket(ticketId);
            return ResponseEntity.ok(contributorNames);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== Multiple Contributors Support ====================

    @Operation(summary = "👥 Add multiple contributors to ticket")
    @PostMapping("/{ticketId}/contributors")
    public ResponseEntity<String> addContributorsToTicket(
            @PathVariable Long ticketId, 
            @RequestBody List<Long> contributorIds) {
        try {
            ticketContributorService.addContributorsToTicket(ticketId, contributorIds);
            return ResponseEntity.ok("Contributors added successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error adding contributors");
        }
    }

    @Operation(summary = "👤 Add single contributor to ticket")
    @PostMapping("/{ticketId}/contributors/{contributorId}")
    public ResponseEntity<String> addSingleContributorToTicket(
            @PathVariable Long ticketId, 
            @PathVariable Long contributorId) {
        try {
            ticketContributorService.addContributorToTicket(ticketId, contributorId);
            return ResponseEntity.ok("Contributor added successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error adding contributor");
        }
    }

    @Operation(summary = "🔄 Replace all contributors for ticket")
    @PutMapping("/{ticketId}/contributors")
    public ResponseEntity<String> replaceContributorsForTicket(
            @PathVariable Long ticketId, 
            @RequestBody List<Long> contributorIds) {
        try {
            ticketContributorService.replaceContributorsForTicket(ticketId, contributorIds);
            return ResponseEntity.ok("Contributors replaced successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error replacing contributors");
        }
    }

    @Operation(summary = "🗑️ Remove specific contributor from ticket")
    @DeleteMapping("/{ticketId}/contributors/{contributorId}")
    public ResponseEntity<String> removeSpecificContributorFromTicket(
            @PathVariable Long ticketId, 
            @PathVariable Long contributorId) {
        try {
            ticketContributorService.removeContributorFromTicket(ticketId, contributorId);
            return ResponseEntity.ok("Contributor removed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error removing contributor");
        }
    }

    @Operation(summary = "🗑️ Remove all contributors from ticket")
    @DeleteMapping("/{ticketId}/contributors")
    public ResponseEntity<String> removeAllContributorsFromTicket(@PathVariable Long ticketId) {
        try {
            ticketContributorService.removeAllContributorsFromTicket(ticketId);
            return ResponseEntity.ok("All contributors removed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error removing contributors");
        }
    }

    @Operation(summary = "📋 Get all contributors for ticket")
    @GetMapping("/{ticketId}/contributors")
    public ResponseEntity<List<Contributor>> getContributorsForTicket(@PathVariable Long ticketId) {
        try {
            List<Contributor> contributors = ticketContributorService.getContributorsForTicket(ticketId);
            return ResponseEntity.ok(contributors);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "📊 Get contributor count for ticket")
    @GetMapping("/{ticketId}/contributors/count")
    public ResponseEntity<Long> getContributorCountForTicket(@PathVariable Long ticketId) {
        try {
            long count = ticketContributorService.countContributorsForTicket(ticketId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "🔍 Check if contributor is assigned to ticket")
    @GetMapping("/{ticketId}/contributors/{contributorId}/exists")
    public ResponseEntity<Boolean> isContributorAssignedToTicket(
            @PathVariable Long ticketId, 
            @PathVariable Long contributorId) {
        try {
            boolean exists = ticketContributorService.isContributorAssignedToTicket(ticketId, contributorId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
