package com.L3Support.TicketEmailExtraction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidateResponse {
    private int sNo;
    private String project;
    private long closedCount;
    private long openCount;
    private long totalBugs; // closedCount + openCount
    
    public ConsolidateResponse(int sNo, String project, long closedCount, long openCount) {
        this.sNo = sNo;
        this.project = project;
        this.closedCount = closedCount;
        this.openCount = openCount;
        this.totalBugs = closedCount + openCount;
    }
}