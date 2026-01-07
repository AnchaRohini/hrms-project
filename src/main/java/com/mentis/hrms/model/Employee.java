package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.mentis.hrms.model.enums.UserRole;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Information
    private String employeeId;
    private String employeeName;
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String email;
    private String phone;
    private LocalDateTime dateOfBirth;
    private String gender;

    // Legacy Address Fields (for backward compatibility)
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Permanent Address Fields
    @Column(name = "permanent_address")
    private String permanentAddress;

    @Column(name = "permanent_city")
    private String permanentCity;

    @Column(name = "permanent_state")
    private String permanentState;

    @Column(name = "permanent_zip_code")
    private String permanentZipCode;

    @Column(name = "permanent_country")
    private String permanentCountry;
    // ✅ REPLACE THIS SECTION (remove @NotBlank and any other annotations)
    @Column(name = "document_deadline")
    private LocalDateTime documentDeadline;

    @Column(name = "deadline_warning_sent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean deadlineWarningSent = false;

    @Column(name = "deadline_final_sent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean deadlineFinalSent = false;
    // Residential Address Fields
    @Column(name = "residential_address")
    private String residentialAddress;

    @Column(name = "residential_city")
    private String residentialCity;

    @Column(name = "residential_state")
    private String residentialState;

    @Column(name = "residential_zip_code")
    private String residentialZipCode;

    @Column(name = "residential_country")
    private String residentialCountry;
    @Column(name = "presence_status", length = 20)
    private String presenceStatus = "OFFLINE";   // OFFLINE / ACTIVE / BREAK

    @Column(name = "last_presence_update")
    private java.time.LocalDateTime lastPresenceUpdate;

    @Column(name = "role", nullable = false, length = 20)

    // Add to com.mentis.hrms.model.Employee


    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.EMPLOYEE;

    // Add getter and setter
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
    public String getPresenceStatus(){ return presenceStatus; }
    public void setPresenceStatus(String s){ this.presenceStatus=s; }

    public java.time.LocalDateTime getLastPresenceUpdate(){ return lastPresenceUpdate; }
    public void setLastPresenceUpdate(java.time.LocalDateTime t){ this.lastPresenceUpdate=t; }
    // Job Information
    private String department;
    private String designation;
    // profile-color support
    private String profileColor;


    // Getters and Setters
    public LocalDateTime getDocumentDeadline() { return documentDeadline; }
    public void setDocumentDeadline(LocalDateTime documentDeadline) { this.documentDeadline = documentDeadline; }

    public boolean isDeadlineWarningSent() { return deadlineWarningSent; }
    public void setDeadlineWarningSent(boolean deadlineWarningSent) { this.deadlineWarningSent = deadlineWarningSent; }

    public boolean isDeadlineFinalSent() { return deadlineFinalSent; }
    public void setDeadlineFinalSent(boolean deadlineFinalSent) { this.deadlineFinalSent = deadlineFinalSent; }


    public String getProfileColor() { return profileColor; }
    public void setProfileColor(String profileColor) { this.profileColor = profileColor; }
    private String workLocation;
    private String employmentType;
    private String workType;
    private LocalDateTime startDate;
    private String manager;

    // Compensation
    private String salary;
    private String payFrequency;
    private String currency;

    // Benefits
    @ElementCollection
    private List<String> benefits = new ArrayList<>();

    // Profile
    private String profilePicture;
    private String status = "Active";
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime updatedDate = LocalDateTime.now();

    // Contact Information
    private String emergencyContact;

    // Onboarding Fields
    private String onboardingStatus = "NOT_STARTED";
    private LocalDateTime onboardingStartDate;
    private LocalDateTime onboardingCompletedDate;
    private Integer totalDocuments = 0;
    private Integer submittedDocuments = 0;
    private Integer verifiedDocuments = 0;

    // Login Credentials
    private String password;
    private String resetToken;
    private LocalDateTime tokenExpiry;
    private boolean credentialsCreated = false;
    private boolean active = true;

    // Onboarding Documents
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<OnboardingDocument> documents = new ArrayList<>();

    // Transient helper methods
    @Transient
    public String getInitials() {
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) {
            initials += firstName.substring(0, 1).toUpperCase();
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials += lastName.substring(0, 1).toUpperCase();
        }
        return !initials.isEmpty() ? initials : "U";
    }

    @Transient
    public boolean hasProfilePicture() {
        return profilePicture != null && !profilePicture.trim().isEmpty();
    }

    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Constructors
    public Employee() {}

    public Employee(String firstName, String lastName, String personalEmail) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalEmail = personalEmail;
        this.email = personalEmail;
        this.employeeName = firstName + " " + lastName;
    }

    // Getters and Setters - Basic Info
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    // Ensure dateOfBirth is properly handled
    public LocalDateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDateTime dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    // Helper method for Thymeleaf to handle null dates
    @Transient
    public String getFormattedDateOfBirth() {
        if (this.dateOfBirth == null) {
            return "Not provided";
        }
        return this.dateOfBirth.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    // Getters and Setters - Legacy Address (for backward compatibility)
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    // Getters and Setters - Permanent Address
    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public String getPermanentCity() { return permanentCity; }
    public void setPermanentCity(String permanentCity) { this.permanentCity = permanentCity; }

    public String getPermanentState() { return permanentState; }
    public void setPermanentState(String permanentState) { this.permanentState = permanentState; }

    public String getPermanentZipCode() { return permanentZipCode; }
    public void setPermanentZipCode(String permanentZipCode) { this.permanentZipCode = permanentZipCode; }

    public String getPermanentCountry() { return permanentCountry; }
    public void setPermanentCountry(String permanentCountry) { this.permanentCountry = permanentCountry; }

    // Getters and Setters - Residential Address
    public String getResidentialAddress() { return residentialAddress; }
    public void setResidentialAddress(String residentialAddress) { this.residentialAddress = residentialAddress; }

    public String getResidentialCity() { return residentialCity; }
    public void setResidentialCity(String residentialCity) { this.residentialCity = residentialCity; }

    public String getResidentialState() { return residentialState; }
    public void setResidentialState(String residentialState) { this.residentialState = residentialState; }

    public String getResidentialZipCode() { return residentialZipCode; }
    public void setResidentialZipCode(String residentialZipCode) { this.residentialZipCode = residentialZipCode; }

    public String getResidentialCountry() { return residentialCountry; }
    public void setResidentialCountry(String residentialCountry) { this.residentialCountry = residentialCountry; }

    // Getters and Setters - Job Info
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }

    // Getters and Setters - Compensation
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getPayFrequency() { return payFrequency; }
    public void setPayFrequency(String payFrequency) { this.payFrequency = payFrequency; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }

    // Getters and Setters - Profile
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    // Getters and Setters - Contact
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    // Getters and Setters - Onboarding
    public String getOnboardingStatus() { return onboardingStatus != null ? onboardingStatus : "NOT_STARTED"; }
    public void setOnboardingStatus(String onboardingStatus) { this.onboardingStatus = onboardingStatus; }

    public LocalDateTime getOnboardingStartDate() { return onboardingStartDate; }
    public void setOnboardingStartDate(LocalDateTime onboardingStartDate) { this.onboardingStartDate = onboardingStartDate; }

    public LocalDateTime getOnboardingCompletedDate() { return onboardingCompletedDate; }
    public void setOnboardingCompletedDate(LocalDateTime onboardingCompletedDate) { this.onboardingCompletedDate = onboardingCompletedDate; }

    public Integer getTotalDocuments() { return totalDocuments != null ? totalDocuments : 0; }
    public void setTotalDocuments(Integer totalDocuments) { this.totalDocuments = totalDocuments; }

    public Integer getSubmittedDocuments() { return submittedDocuments != null ? submittedDocuments : 0; }
    public void setSubmittedDocuments(Integer submittedDocuments) { this.submittedDocuments = submittedDocuments; }

    public Integer getVerifiedDocuments() { return verifiedDocuments != null ? verifiedDocuments : 0; }
    public void setVerifiedDocuments(Integer verifiedDocuments) { this.verifiedDocuments = verifiedDocuments; }

    // Getters and Setters - Login Credentials
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getTokenExpiry() { return tokenExpiry; }
    public void setTokenExpiry(LocalDateTime tokenExpiry) { this.tokenExpiry = tokenExpiry; }

    public boolean isCredentialsCreated() { return credentialsCreated; }
    public void setCredentialsCreated(boolean credentialsCreated) { this.credentialsCreated = credentialsCreated; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Getters and Setters - Documents
    public List<OnboardingDocument> getDocuments() { return documents; }
    public void setDocuments(List<OnboardingDocument> documents) { this.documents = documents; }
}