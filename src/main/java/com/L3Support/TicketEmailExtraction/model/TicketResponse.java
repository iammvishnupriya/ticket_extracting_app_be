package com.L3Support.TicketEmailExtraction.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    
    private Long id;
    private String ticketSummary;
    private String project;
    private String issueDescription;
    private LocalDate receivedDate;
    private Priority priority;
    private String ticketOwner;
    private ContributorResponse contributor;
    private Long contributorId;
    private String contributorName;
    private BugType bugType;
    private Status status;
    private String review;
    private String impact;
    private String contact;
    private String employeeId;
    private String employeeName;
    private String messageId;
    
    public static TicketResponse fromEntity(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketSummary(ticket.getTicketSummary())
                .project(ticket.getProject() != null ? ticket.getProject().getDisplayName() : null)
                .issueDescription(ticket.getIssueDescription())
                .receivedDate(ticket.getReceivedDate())
                .priority(ticket.getPriority())
                .ticketOwner(ticket.getTicketOwner())
                .contributor(ticket.getContributor() != null ? ContributorResponse.fromEntity(ticket.getContributor()) : null)
                .contributorId(ticket.getContributor() != null ? ticket.getContributor().getId() : null)
                .contributorName(ticket.getContributorName())
                .bugType(ticket.getBugType())
                .status(ticket.getStatus())
                .review(ticket.getReview())
                .impact(ticket.getImpact())
                .contact(ticket.getContact())
                .employeeId(ticket.getEmployeeId())
                .employeeName(ticket.getEmployeeName())
                .messageId(ticket.getMessageId())
                .build();
    }
}