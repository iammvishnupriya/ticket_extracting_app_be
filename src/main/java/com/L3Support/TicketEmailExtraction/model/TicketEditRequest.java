package com.L3Support.TicketEmailExtraction.model;

import java.time.LocalDate;

import com.L3Support.TicketEmailExtraction.enums.Project;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEditRequest {
    
    private Long id;
    private String ticketSummary;
    private Project project;
    private String issueDescription;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receivedDate;
    
    private Priority priority;
    private String ticketOwner;
    
    // Contributor can be provided in multiple ways
    private ContributorResponse contributor; // Full contributor object
    private Long contributorId; // Just the ID (most common from frontend)
    private String contributorName; // Just the name (fallback)
    
    private BugType bugType;
    private Status status;
    private String review;
    private String impact;
    private String contact;
    private String employeeId;
    private String employeeName;
    private String messageId;
    
    /**
     * Convert this DTO to a Ticket entity for processing
     */
    public Ticket toTicketEntity() {
        Ticket ticket = Ticket.builder()
                .id(this.id)
                .ticketSummary(this.ticketSummary)
                .project(this.project)
                .issueDescription(this.issueDescription)
                .receivedDate(this.receivedDate)
                .priority(this.priority)
                .ticketOwner(this.ticketOwner)
                .contributorId(this.contributorId)
                .contributorName(this.contributorName)
                .bugType(this.bugType)
                .status(this.status)
                .review(this.review)
                .impact(this.impact)
                .contact(this.contact)
                .employeeId(this.employeeId)
                .employeeName(this.employeeName)
                .messageId(this.messageId)
                .build();
        
        // Handle contributor object if provided
        if (this.contributor != null) {
            Contributor contributorEntity = Contributor.builder()
                    .id(this.contributor.getId())
                    .name(this.contributor.getName())
                    .email(this.contributor.getEmail())
                    .employeeId(this.contributor.getEmployeeId())
                    .department(this.contributor.getDepartment())
                    .phone(this.contributor.getPhone())
                    .active(this.contributor.getActive())
                    .notes(this.contributor.getNotes())
                    .build();
            ticket.setContributor(contributorEntity);
        }
        
        return ticket;
    }
}