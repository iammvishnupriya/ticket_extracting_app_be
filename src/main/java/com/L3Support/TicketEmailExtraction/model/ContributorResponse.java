package com.L3Support.TicketEmailExtraction.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributorResponse {

    private Long id;
    private String name;
    private String email;
    private String employeeId;
    private String department;
    private String phone;
    private Boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContributorResponse fromEntity(Contributor contributor) {
        return ContributorResponse.builder()
                .id(contributor.getId())
                .name(contributor.getName())
                .email(contributor.getEmail())
                .employeeId(contributor.getEmployeeId())
                .department(contributor.getDepartment())
                .phone(contributor.getPhone())
                .active(contributor.getActive())
                .notes(contributor.getNotes())
                .createdAt(contributor.getCreatedAt())
                .updatedAt(contributor.getUpdatedAt())
                .build();
    }
}