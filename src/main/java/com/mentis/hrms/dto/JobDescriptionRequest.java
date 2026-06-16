package com.mentis.hrms.dto;

public class JobDescriptionRequest {
    private String jobTitle;
    private String department;
    private String level; // Junior, Mid, Senior, Lead, Manager
    private String skills; // Optional: comma-separated skills
    private String companyName;
    private String location;

    // Constructors
    public JobDescriptionRequest() {}

    public JobDescriptionRequest(String jobTitle, String department, String level) {
        this.jobTitle = jobTitle;
        this.department = department;
        this.level = level;
    }

    // Getters and Setters
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
