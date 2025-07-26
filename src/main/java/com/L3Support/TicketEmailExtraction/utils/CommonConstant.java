package com.L3Support.TicketEmailExtraction.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Common constants used across the application
 */
public final class CommonConstant {
    
    // Private constructor to prevent instantiation
    private CommonConstant() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * List of allowed L3 Support projects
     * Consolidated from application.properties and TicketParserServiceImpl
     */
    public static final List<String> L3_ALLOWED_PROJECTS = Arrays.asList(
        "Material Receipt",
        "My Buddy", 
        "CK Alumni",
        "HEPL Alumni",
        "MMW Module(Ticket tool)",
        "CK Trends",
        "Livewire",
        "Meeting Agenda",
        "Pro Hire",
        "E-Capex",
        "SOP",
        "Asset Management",
        "Mould Mamp",
        "E-Library",
        "Outlet_Approval",
        "RA_Tool",
        "CK_Bakery",
        "I-View",
        "FormBuilder",
        "CK_Ticketing_tool"
    );
    
    /**
     * Comma-separated string of allowed L3 Support projects (for backward compatibility)
     */
    public static final String L3_ALLOWED_PROJECTS_STRING = String.join(",", L3_ALLOWED_PROJECTS);
}