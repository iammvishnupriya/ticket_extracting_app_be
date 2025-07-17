package com.L3Support.TicketEmailExtraction.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Project {
    MATERIAL_RECEIPT("Material Receipt"),
    MY_BUDDY("My Buddy"),
    CK_ALUMNI("CK Alumni"),
    HEPL_ALUMNI("HEPL Alumni"),
    HEPL_PORTAL("HEPL Portal"),
    MMW_MODULE_TICKET_TOOL("MMW Module (Ticket Tool)"),
    CK_TRENDS("CK Trends"),
    LIVEWIRE("Livewire"),
    MEETING_AGENDA("Meeting Agenda"),
    PRO_HIRE("Pro Hire"),
    E_CAPEX("E-Capex"),
    SOP("SOP"),
    ASSET_MANAGEMENT("Asset Management"),
    MOULD_MAMP("Mould Mamp"),
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
        
        // Normalize the input string
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
            default:
                return GENERAL;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}