package com.L3Support.TicketEmailExtraction.enums;

/**
 * Enum representing different project types in the ticket system.
 * This enum is expandable - new projects can be added as needed.
 */
public enum ProjectType {
    MATERIAL_RECEIPT("Material receipt"),
    MY_BUDDY("My buddy"),
    CK_ALUMNI("CK_Alumni"),
    HEPL_ALUMNI("HEPL_Alumni"),
    MMW_MODULE("MMW Module(Ticket tool)"),
    CK_TRENDS("CK Trends"),
    LIVEWIRE("Livewire"),
    MEETING_AGENDA("Meeting agenda"),
    PRO_HIRE("Pro Hire"),
    E_CAPEX("E-Capex"),
    SOP("SOP"),
    ASSERT_MANAGEMENT("Assert Management"),
    MOULD_MAMP("Mould Mamp"),
    GENERAL("General"); // Fallback option

    private final String displayName;

    ProjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get ProjectType by display name (case-insensitive)
     * @param displayName The display name to search for
     * @return ProjectType if found, GENERAL as fallback
     */
    public static ProjectType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return GENERAL;
        }
        
        for (ProjectType project : ProjectType.values()) {
            if (project.getDisplayName().equalsIgnoreCase(displayName.trim())) {
                return project;
            }
        }
        
        // If no exact match found, return GENERAL as fallback
        return GENERAL;
    }

    /**
     * Get all available project display names
     * @return Array of all project display names
     */
    public static String[] getAllDisplayNames() {
        ProjectType[] values = ProjectType.values();
        String[] displayNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            displayNames[i] = values[i].getDisplayName();
        }
        return displayNames;
    }

    @Override
    public String toString() {
        return displayName;
    }
}