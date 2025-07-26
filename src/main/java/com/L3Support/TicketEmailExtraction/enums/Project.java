package com.L3Support.TicketEmailExtraction.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Project {
    MATERIAL_RECEIPT("Material Receipt"),
    MY_BUDDY("My Buddy"),
    CK_ALUMNI("CK Alumni"),
    HEPL_ALUMNI("HEPL Alumni"),
    HEPL_PORTAL("HEPL Portal"),
    MMW_MODULE_TICKET_TOOL("MMW Module(Ticket tool)"),
    CK_TRENDS("CK Trends"),
    LIVEWIRE("Livewire"),
    MEETING_AGENDA("Meeting Agenda"),
    PRO_HIRE("Pro Hire"),
    E_CAPEX("E-Capex"),
    SOP("SOP"),
    ASSET_MANAGEMENT("Asset Management"),
    MOULD_MAMP("Mould Mamp"),
    E_LIBRARY("E-Library"),
    OUTLET_APPROVAL("Outlet_Approval"),
    RA_TOOL("RA_Tool"),
    CK_BAKERY("CK_Bakery"),
    I_VIEW("I-View"),
    FORM_BUILDER("FormBuilder"),
    CK_TICKETING_TOOL("CK_Ticketing_tool"),
    GENERAL("General");

    private final String displayName;

    Project(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Project fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return GENERAL;
        }
        
        // First try to match by enum constant name (for database compatibility)
        try {
            return Project.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // If that fails, try by display name
        }
        
        // Try exact display name match first
        for (Project project : Project.values()) {
            if (project.displayName.equals(value.trim())) {
                return project;
            }
        }
        
        // Normalize the input string for fuzzy matching
        String normalized = value.trim().toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "") // Remove special characters except spaces
            .replaceAll("\\s+", " "); // Replace multiple spaces with single space
        
        for (Project project : Project.values()) {
            String projectNormalized = project.displayName.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ");
            
            if (projectNormalized.equals(normalized)) {
                return project;
            }
        }
        
        // Additional fuzzy matching for common variations
        switch (normalized) {
            case "material receipt":
            case "material reciept":
            case "materialreceipt":
            case "materialreciept":
                return MATERIAL_RECEIPT;
            case "my buddy":
            case "mybuddy":
                return MY_BUDDY;
            case "ck alumni":
            case "ckalumni":
                return CK_ALUMNI;
            case "hepl alumni":
            case "heplalumni":
                return HEPL_ALUMNI;
            case "hepl portal":  // Direct mapping to HEPL_PORTAL
            case "heplportal":
                return HEPL_PORTAL;
            case "mmw module":
            case "mmw module ticket tool":
            case "mmwmodule":
            case "ticket tool":
            case "tickettool":
                return MMW_MODULE_TICKET_TOOL;
            case "ck trends":
            case "cktrends":
                return CK_TRENDS;
            case "livewire":
                return LIVEWIRE;
            case "meeting agenda":
            case "meetingagenda":
                return MEETING_AGENDA;
            case "pro hire":
            case "prohire":
                return PRO_HIRE;
            case "e capex":
            case "ecapex":
                return E_CAPEX;
            case "sop":
                return SOP;
            case "asset management":
            case "assetmanagement":
            case "assert management":
            case "assertmanagement":
                return ASSET_MANAGEMENT;
            case "mould mamp":
            case "mouldmamp":
                return MOULD_MAMP;
            case "e library":
            case "elibrary":
            case "e-library":
                return E_LIBRARY;
            case "outlet approval":
            case "outletapproval":
            case "outlet_approval":
                return OUTLET_APPROVAL;
            case "ra tool":
            case "ratool":
            case "ra_tool":
                return RA_TOOL;
            case "ck bakery":
            case "ckbakery":
            case "ck_bakery":
                return CK_BAKERY;
            case "i view":
            case "iview":
            case "i-view":
                return I_VIEW;
            case "form builder":
            case "formbuilder":
                return FORM_BUILDER;
            case "ck ticketing tool":
            case "cktickettool":
            case "ck_ticketing_tool":
            case "ticketing tool":
            case "ticketingtool":
                return CK_TICKETING_TOOL;

            default:
                return GENERAL;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}