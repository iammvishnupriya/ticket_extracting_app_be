package com.L3Support.TicketEmailExtraction.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

import com.L3Support.TicketEmailExtraction.model.ContributorRequest;
import com.L3Support.TicketEmailExtraction.model.ContributorResponse;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.service.ContributorService;
import com.L3Support.TicketEmailExtraction.service.TicketContributorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/contributors")
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
@Validated
@Slf4j
@Tag(name = "Contributors", description = "Contributor management operations")
public class ContributorController {

    private final ContributorService contributorService;
    private final TicketContributorService ticketContributorService;

    @Operation(summary = "👥 Get all contributors")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contributors"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<ContributorResponse>> getAllContributors() {
        log.info("GET /api/contributors - Fetching all contributors");
        try {
            List<ContributorResponse> contributors = contributorService.getAllContributors();
            log.info("Successfully retrieved {} contributors", contributors.size());
            return ResponseEntity.ok(contributors);
        } catch (Exception e) {
            log.error("Error fetching contributors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "✅ Get active contributors only")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved active contributors"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/active")
    public ResponseEntity<List<ContributorResponse>> getActiveContributors() {
        log.info("GET /api/contributors/active - Fetching active contributors");
        try {
            List<ContributorResponse> contributors = contributorService.getActiveContributors();
            log.info("Successfully retrieved {} active contributors", contributors.size());
            return ResponseEntity.ok(contributors);
        } catch (Exception e) {
            log.error("Error fetching active contributors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "🔍 Get contributor by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contributor"),
        @ApiResponse(responseCode = "404", description = "Contributor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContributorResponse> getContributorById(
            @Parameter(description = "Contributor ID") @PathVariable Long id) {
        log.info("GET /api/contributors/{} - Fetching contributor by ID", id);
        try {
            return contributorService.getContributorById(id)
                    .map(contributor -> {
                        log.info("Successfully retrieved contributor: {}", contributor.getName());
                        return ResponseEntity.ok(contributor);
                    })
                    .orElseGet(() -> {
                        log.warn("Contributor not found with ID: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("Error fetching contributor with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "➕ Create new contributor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Contributor created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Email or Employee ID already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ContributorResponse> createContributor(
            @Parameter(description = "Contributor data") @Valid @RequestBody ContributorRequest request) {
        log.info("POST /api/contributors - Creating contributor: {}", request.getName());
        try {
            ContributorResponse created = contributorService.createContributor(request);
            log.info("Successfully created contributor with ID: {}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid data for contributor creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error creating contributor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "✏️ Update existing contributor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contributor updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Contributor not found"),
        @ApiResponse(responseCode = "409", description = "Email or Employee ID already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ContributorResponse> updateContributor(
            @Parameter(description = "Contributor ID") @PathVariable Long id,
            @Parameter(description = "Updated contributor data") @Valid @RequestBody ContributorRequest request) {
        log.info("PUT /api/contributors/{} - Updating contributor", id);
        try {
            ContributorResponse updated = contributorService.updateContributor(id, request);
            log.info("Successfully updated contributor with ID: {}", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid data for contributor update: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error updating contributor with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "🗑️ Delete contributor (soft delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Contributor deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Contributor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContributor(
            @Parameter(description = "Contributor ID") @PathVariable Long id) {
        log.info("DELETE /api/contributors/{} - Soft deleting contributor", id);
        try {
            contributorService.deleteContributor(id);
            log.info("Successfully soft deleted contributor with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Contributor not found for deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting contributor with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "💀 Permanently delete contributor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Contributor permanently deleted"),
        @ApiResponse(responseCode = "404", description = "Contributor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteContributor(
            @Parameter(description = "Contributor ID") @PathVariable Long id) {
        log.info("DELETE /api/contributors/{}/permanent - Permanently deleting contributor", id);
        try {
            contributorService.permanentlyDeleteContributor(id);
            log.info("Successfully permanently deleted contributor with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Contributor not found for permanent deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error permanently deleting contributor with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "🔄 Activate contributor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contributor activated successfully"),
        @ApiResponse(responseCode = "404", description = "Contributor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/activate")
    public ResponseEntity<ContributorResponse> activateContributor(
            @Parameter(description = "Contributor ID") @PathVariable Long id) {
        log.info("PUT /api/contributors/{}/activate - Activating contributor", id);
        try {
            ContributorResponse activated = contributorService.activateContributor(id);
            log.info("Successfully activated contributor with ID: {}", id);
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            log.warn("Contributor not found for activation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error activating contributor with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "⏸️ Deactivate contributor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contributor deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Contributor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ContributorResponse> deactivateContributor(
            @Parameter(description = "Contributor ID") @PathVariable Long id) {
        log.info("PUT /api/contributors/{}/deactivate - Deactivating contributor", id);
        try {
            ContributorResponse deactivated = contributorService.deactivateContributor(id);
            log.info("Successfully deactivated contributor with ID: {}", id);
            return ResponseEntity.ok(deactivated);
        } catch (IllegalArgumentException e) {
            log.warn("Contributor not found for deactivation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deactivating contributor with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "🔍 Search contributors by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved search results"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ContributorResponse>> searchContributors(
            @Parameter(description = "Name to search for") @RequestParam String name) {
        log.info("GET /api/contributors/search?name={} - Searching contributors", name);
        try {
            List<ContributorResponse> results = contributorService.searchContributorsByName(name);
            log.info("Found {} contributors matching name: {}", results.size(), name);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching contributors by name: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "🏢 Get contributors by department")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contributors by department"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/department/{department}")
    public ResponseEntity<List<ContributorResponse>> getContributorsByDepartment(
            @Parameter(description = "Department name") @PathVariable String department) {
        log.info("GET /api/contributors/department/{} - Fetching contributors by department", department);
        try {
            List<ContributorResponse> contributors = contributorService.getContributorsByDepartment(department);
            log.info("Found {} contributors in department: {}", contributors.size(), department);
            return ResponseEntity.ok(contributors);
        } catch (Exception e) {
            log.error("Error fetching contributors by department: {}", department, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "✅ Check if email exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email check completed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailExists(
            @Parameter(description = "Email to check") @RequestParam String email,
            @Parameter(description = "ID to exclude from check") @RequestParam(required = false) Long excludeId) {
        log.info("GET /api/contributors/check-email?email={}&excludeId={}", email, excludeId);
        try {
            boolean exists = contributorService.emailExists(email, excludeId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("Error checking email existence: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "🆔 Check if employee ID exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employee ID check completed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/check-employee-id")
    public ResponseEntity<Boolean> checkEmployeeIdExists(
            @Parameter(description = "Employee ID to check") @RequestParam String employeeId,
            @Parameter(description = "ID to exclude from check") @RequestParam(required = false) Long excludeId) {
        log.info("GET /api/contributors/check-employee-id?employeeId={}&excludeId={}", employeeId, excludeId);
        try {
            boolean exists = contributorService.employeeIdExists(employeeId, excludeId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("Error checking employee ID existence: {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Contributor-Ticket Relationship Endpoints ====================

    @Operation(summary = "🎫 Get all tickets assigned to contributor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tickets for contributor"),
        @ApiResponse(responseCode = "404", description = "Contributor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{contributorId}/tickets")
    public ResponseEntity<List<Ticket>> getTicketsForContributor(
            @Parameter(description = "Contributor ID") @PathVariable Long contributorId) {
        log.info("GET /api/contributors/{}/tickets - Fetching tickets for contributor", contributorId);
        try {
            List<Ticket> tickets = ticketContributorService.getTicketsForContributor(contributorId);
            log.info("Found {} tickets for contributor {}", tickets.size(), contributorId);
            return ResponseEntity.ok(tickets);
        } catch (IllegalArgumentException e) {
            log.warn("Contributor not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching tickets for contributor {}", contributorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "📊 Get ticket count for contributor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved ticket count"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{contributorId}/tickets/count")
    public ResponseEntity<Long> getTicketCountForContributor(
            @Parameter(description = "Contributor ID") @PathVariable Long contributorId) {
        log.info("GET /api/contributors/{}/tickets/count - Getting ticket count for contributor", contributorId);
        try {
            long count = ticketContributorService.countTicketsForContributor(contributorId);
            log.info("Contributor {} has {} tickets", contributorId, count);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting ticket count for contributor {}", contributorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}