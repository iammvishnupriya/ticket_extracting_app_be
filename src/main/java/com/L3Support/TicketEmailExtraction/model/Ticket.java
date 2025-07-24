package com.L3Support.TicketEmailExtraction.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.L3Support.TicketEmailExtraction.converter.ProjectConverter;
import com.L3Support.TicketEmailExtraction.enums.Project;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String ticketSummary;
    
    @Convert(converter = ProjectConverter.class)
    private Project project;
    
    @Column(length = 2000)
    private String issueDescription;
    
    private LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Priority priority;

    @Column(length = 100)
    private String ticketOwner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Contributor contributor;

    // Transient field to accept contributor ID from frontend
    @jakarta.persistence.Transient
    private Long contributorId;

    // Keep the old contributor field for backward compatibility during migration
    @Column(length = 500)
    private String contributorName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BugType bugType;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Status status;

    @Column(length = 1000)
    private String review;
    
    @Column(length = 500)
    private String impact;
    
    @Column(length = 100)
    private String contact;
    
    @Column(length = 50)
    private String employeeId;
    
    @Column(length = 100)
    private String employeeName;

    @Column(unique = true)
    private String messageId;
}
