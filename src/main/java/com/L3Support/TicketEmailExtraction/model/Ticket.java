package com.L3Support.TicketEmailExtraction.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    @Column(length = 100)
    private String project;
    
    @Column(length = 2000)
    private String issueDescription;
    
    private LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(length = 100)
    private String ticketOwner;
    
    @Column(length = 500)
    private String contributor;

    @Enumerated(EnumType.STRING)
    private BugType bugType;

    @Enumerated(EnumType.STRING)
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
