package com.L3Support.TicketEmailExtraction.converter;

import com.L3Support.TicketEmailExtraction.enums.Project;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectConverter implements AttributeConverter<Project, String> {

    @Override
    public String convertToDatabaseColumn(Project project) {
        if (project == null) {
            return null;
        }
        // Store as display name for consistency with existing data
        return project.getDisplayName();
    }

    @Override
    public Project convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        // Use the fromString method which handles both enum names and display names
        return Project.fromString(dbData);
    }
}