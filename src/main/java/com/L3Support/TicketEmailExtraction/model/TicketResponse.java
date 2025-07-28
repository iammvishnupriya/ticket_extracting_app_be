package com.L3Support.TicketEmailExtraction.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

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
    // Legacy single contributor support (backward compatibility)
    private ContributorResponse contributor;
    private Long contributorId;
    private String contributorName;
    
    // Multiple contributors support
    private List<ContributorResponse> contributors;
    private String contributorNames; // Comma-separated names for easy display
    private List<Long> contributorIds; // List of contributor IDs
    private Integer contributorCount; // Count of contributors for quick reference
    
    private BugType bugType;
    private Status status;
    private String review;
    private String impact;
    private String contact;
    private String employeeId;
    private String employeeName;
    private String messageId;
    
    public static TicketResponse fromEntity(Ticket ticket) {
        return fromEntity(ticket, null);
    }
    
    public static TicketResponse fromEntity(Ticket ticket, List<Contributor> multipleContributors) {
        // Parse multiple contributors from contributorIds field
        List<Long> contributorIdsList = parseContributorIds(ticket.getContributorIds());
        List<ContributorResponse> contributorResponses = new ArrayList<>();
        String contributorNamesStr = null;
        
        // If multipleContributors are provided, use them
        if (multipleContributors != null && !multipleContributors.isEmpty()) {
            contributorResponses = multipleContributors.stream()
                    .map(ContributorResponse::fromEntity)
                    .collect(Collectors.toList());
            contributorNamesStr = multipleContributors.stream()
                    .map(Contributor::getName)
                    .collect(Collectors.joining(", "));
        }
        // Otherwise, if we have contributorIds but no contributor objects, create placeholder responses
        else if (!contributorIdsList.isEmpty()) {
            contributorNamesStr = "Contributors: " + contributorIdsList.size() + " assigned";
        }
        
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketSummary(ticket.getTicketSummary())
                .project(ticket.getProject() != null ? ticket.getProject().getDisplayName() : null)
                .issueDescription(ticket.getIssueDescription())
                .receivedDate(ticket.getReceivedDate())
                .priority(ticket.getPriority())
                .ticketOwner(ticket.getTicketOwner())
                // Legacy single contributor support (backward compatibility)
                .contributor(ticket.getContributor() != null ? ContributorResponse.fromEntity(ticket.getContributor()) : null)
                .contributorId(ticket.getContributor() != null ? ticket.getContributor().getId() : null)
                .contributorName(ticket.getContributorName())
                // Multiple contributors support
                .contributors(contributorResponses)
                .contributorNames(contributorNamesStr)
                .contributorIds(contributorIdsList)
                .contributorCount(contributorIdsList.size())
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
    
    /**
     * Utility method: Convert comma-separated string to List of Long IDs
     */
    private static List<Long> parseContributorIds(String contributorIds) {
        if (!StringUtils.hasText(contributorIds)) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(contributorIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}