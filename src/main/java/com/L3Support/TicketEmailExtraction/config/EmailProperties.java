package com.L3Support.TicketEmailExtraction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import com.L3Support.TicketEmailExtraction.utils.CommonConstant;

@Component
@ConfigurationProperties(prefix = "app.email")
@Data
public class EmailProperties {
    
    private Fetch fetch = new Fetch();
    private List<String> allowedSenders;
    private List<String> allowedProjects = CommonConstant.L3_ALLOWED_PROJECTS;
    private Filter filter = new Filter();
    
    @Data
    public static class Fetch {
        private boolean enabled = true;
        private long interval = 300000; // 5 minutes
    }
    
    @Data
    public static class Filter {
        private String startDate;
        private String endDate;
    }
}