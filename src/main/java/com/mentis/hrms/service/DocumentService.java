package com.mentis.hrms.service;

import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.OnboardingDocumentRepository;
import com.mentis.hrms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    @Autowired private OnboardingDocumentRepository documentRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private NotificationService notificationService;

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    /* ---------------------------------------------------- */
    /* 1.  NEW – used by DashboardController                */
    /* ---------------------------------------------------- */
    public void initializeDocumentChecklist(Employee employee) {
        List<String> mandatory = List.of("PAN_CARD", "AADHAAR_CARD", "OFFER_LETTER");
        List<String> optional  = List.of("RESUME", "PASSPORT", "EXPERIENCE_LETTERS", "EDUCATIONAL_CERTIFICATES");

        for (String type : mandatory) createDocumentIfAbsent(employee, type, true);
        for (String type : optional)  createDocumentIfAbsent(employee, type, false);

        // ✅ IMPORTANT: Update total document count after initialization
        employee.setTotalDocuments(mandatory.size() + optional.size());
        employeeRepository.save(employee);
    }

    private void createDocumentIfAbsent(Employee emp, String type, boolean mandatory) {
        if (documentRepository.findByEmployeeAndDocumentType(emp, type).isEmpty()) {
            OnboardingDocument doc = new OnboardingDocument();
            doc.setEmployee(emp);
            doc.setDocumentType(type);
            doc.setDocumentName(getDisplayName(type));
            doc.setMandatory(mandatory);
            doc.setStatus("PENDING");
            documentRepository.save(doc);
        }
    }

    /* ---------------------------------------------------- */
    /* 2.  EXISTING – fixed upload + toast                 */
    /* ---------------------------------------------------- */
    public OnboardingDocument uploadDocument(String employeeId, String documentType,
                                             MultipartFile file, String notes) throws Exception {
        Employee emp = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        OnboardingDocument doc = documentRepository
                .findByEmployeeAndDocumentType(emp, documentType)
                .orElseGet(() -> {
                    OnboardingDocument d = new OnboardingDocument();
                    d.setEmployee(emp);
                    d.setDocumentType(documentType);
                    d.setDocumentName(getDisplayName(documentType));
                    d.setMandatory(isMandatory(documentType));
                    d.setStatus("PENDING");
                    return d;
                });

        String savedPath = saveFile(file, employeeId, documentType);
        doc.setFilePath(savedPath);
        doc.setSubmittedDate(LocalDateTime.now());
        doc.setStatus("SUBMITTED");
        OnboardingDocument saved = documentRepository.save(doc);

        notificationService.notifyDocumentUploaded(saved);

        // ✅ NEW: Check if all documents are now uploaded
        checkAllDocumentsUploaded(emp);

        return saved;
    }

    /* ---------------------------------------------------- */
    /* 3.  HR verify – with toast                          */
    /* ---------------------------------------------------- */
    public OnboardingDocument verifyDocumentWithMessage(Long docId, String verifiedBy, String status, String remarks) {
        OnboardingDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        doc.setStatus(status);
        doc.setVerificationNotes(remarks);
        doc.setVerifiedBy(verifiedBy);
        doc.setVerifiedDate(LocalDateTime.now());
        OnboardingDocument saved = documentRepository.save(doc);
        notificationService.notifyDocumentVerified(saved, verifiedBy);
        return saved;
    }

    /* ---------------------------------------------------- */
    /* 4.  HR Upload Document (Auto-Verify)               */
    /* ---------------------------------------------------- */
    public OnboardingDocument hrUploadDocument(String empId, String docType,
                                               MultipartFile file, String notes,
                                               String hrId) throws Exception {
        // Upload the document first
        OnboardingDocument doc = uploadDocument(empId, docType, file, notes);

        // ✅ AUTO-VERIFY: Immediately set to VERIFIED for HR uploads
        doc.setStatus("VERIFIED");
        doc.setVerifiedBy(hrId);
        doc.setVerifiedDate(LocalDateTime.now());
        doc.setVerificationNotes("Document uploaded and verified by HR");

        OnboardingDocument verifiedDoc = documentRepository.save(doc);

        // Send verification notification (this triggers the GREEN toast)
        notificationService.notifyDocumentVerified(verifiedDoc, hrId);

        return verifiedDoc;
    }

    /* ---------------------------------------------------- */
    /* 5.  NEW: Check if all documents are uploaded       */
    /* ---------------------------------------------------- */
    private void checkAllDocumentsUploaded(Employee employee) {
        // Refresh employee object to get latest counts
        Employee emp = employeeRepository.findByEmployeeId(employee.getEmployeeId())
                .orElse(null);

        if (emp == null) return;

        // Check if all required documents are submitted
        if (emp.getSubmittedDocuments() >= emp.getTotalDocuments() &&
                emp.getTotalDocuments() > 0) {

            // Check if already notified to avoid duplicate notifications
            boolean alreadyNotified = notificationService.getNotifications(
                            emp.getEmployeeId(), "EMPLOYEE")
                    .stream()
                    .anyMatch(n -> "ALL_DOCUMENTS_UPLOADED".equals(n.getType()));

            if (!alreadyNotified) {
                notificationService.notifyAllDocumentsUploaded(emp);
            }
        }
    }

    /* ---------------------------------------------------- */
    /* 6.  Utility methods                                */
    /* ---------------------------------------------------- */
    private String saveFile(MultipartFile file, String empId, String docType) throws Exception {
        String ext   = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        String fileName = docType + "_" + UUID.randomUUID() + ext;
        Path dir  = Paths.get(basePath, "documents", empId);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        Files.copy(file.getInputStream(), target);
        return "documents/" + empId + "/" + fileName;
    }

    private boolean isMandatory(String type) {
        return List.of("PAN_CARD", "AADHAAR_CARD", "OFFER_LETTER").contains(type);
    }

    private String getDisplayName(String type) {
        return Map.of(
                "RESUME", "Resume/CV",
                "PAN_CARD", "PAN Card",
                "AADHAAR_CARD", "Aadhaar Card",
                "PASSPORT", "Passport",
                "OFFER_LETTER", "Offer Letter",
                "EXPERIENCE_LETTERS", "Experience Letters",
                "EDUCATIONAL_CERTIFICATES", "Educational Certificates"
        ).getOrDefault(type, type.replace('_', ' '));
    }

    /* ---------------------------------------------------- */
    /* 7.  Repository wrappers                            */
    /* ---------------------------------------------------- */
    public List<OnboardingDocument> getDocumentsByEmployee(Employee emp) {
        return documentRepository.findByEmployee(emp);
    }

    public OnboardingDocument getDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

    public OnboardingDocumentRepository getDocumentRepository() {
        return documentRepository;
    }
}