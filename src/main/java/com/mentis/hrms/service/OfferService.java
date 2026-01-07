package com.mentis.hrms.service;

import com.mentis.hrms.dto.OfferRequest;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.model.OfferLetter;
import com.mentis.hrms.repository.OfferLetterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OfferService {
    private static final Logger logger = LoggerFactory.getLogger(OfferService.class);

    private final OfferLetterRepository offerLetterRepository;
    private final JobApplicationService jobApplicationService;
    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;
    private final PdfGenerationService pdfGenerationService;

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    @Autowired
    public OfferService(OfferLetterRepository offerLetterRepository,
                        JobApplicationService jobApplicationService,
                        SpringTemplateEngine templateEngine,
                        EmailService emailService,
                        PdfGenerationService pdfGenerationService) {
        this.offerLetterRepository = offerLetterRepository;
        this.jobApplicationService = jobApplicationService;
        this.templateEngine = templateEngine;
        this.emailService = emailService;
        this.pdfGenerationService = pdfGenerationService;
    }

    // === EXISTING METHODS (Keep all your current methods) ===

    public List<OfferLetter> getAllOfferLetters() {
        try {
            return offerLetterRepository.findAllByOrderByCreatedDateDesc();
        } catch (Exception e) {
            logger.error("Error getting all offer letters: {}", e.getMessage());
            return List.of();
        }
    }

    public long getOfferCount() {
        try {
            return offerLetterRepository.count();
        } catch (Exception e) {
            logger.error("Error getting offer count: {}", e.getMessage());
            return 0;
        }
    }

    public long getSentOfferCount() {
        try {
            return offerLetterRepository.countByStatus("SENT");
        } catch (Exception e) {
            logger.error("Error getting sent offer count: {}", e.getMessage());
            return 0;
        }
    }

    public OfferLetter createAndGenerateOffer(OfferRequest request) throws Exception {
        logger.info("Creating offer letter for application ID: {}", request.getApplicationId());

        try {
            JobApplication application = jobApplicationService.getApplicationById(request.getApplicationId());
            if (application == null) {
                throw new RuntimeException("Application not found with ID: " + request.getApplicationId());
            }

            OfferLetter offer = new OfferLetter(application);
            populateOfferData(offer, request);

            offer = offerLetterRepository.save(offer);
            logger.info("Offer letter created with ID: {}", offer.getId());

            String signaturePath = handleSignatureUpload(offer.getId(), request.getSignatureFile());
            String filePath = generateOfferDocument(offer, signaturePath);
            offer.setOfferFilePath(filePath);
            offer.setStatus("GENERATED");

            return offerLetterRepository.save(offer);

        } catch (Exception e) {
            logger.error("Error creating offer: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create offer: " + e.getMessage());
        }
    }

    private void populateOfferData(OfferLetter offer, OfferRequest request) {
        offer.setDesignation(safeValue(request.getDesignation(), "Not Specified"));
        offer.setDepartment(safeValue(request.getDepartment(), "Not Specified"));
        offer.setJoiningDate(safeValue(request.getJoiningDate(), java.time.LocalDate.now().plusDays(14).toString()));
        offer.setWorkLocation(safeValue(request.getWorkLocation(), "Not Specified"));
        offer.setEmploymentType(safeValue(request.getEmploymentType(), "Full-time"));
        offer.setAnnualSalary(safeValue(request.getAnnualSalary(), "To be discussed"));
        offer.setCurrency(safeValue(request.getCurrency(), "USD"));
        offer.setReportingManager(safeValue(request.getReportingManager(), "Not Specified"));
        offer.setProbationPeriod(safeValue(request.getProbationPeriod(), "3 months"));
        offer.setAdditionalNotes(safeValue(request.getAdditionalNotes(), ""));
        offer.setOfferType(safeValue(request.getOfferType(), "OFFER"));
        offer.setStatus("DRAFT");

        // FIX: Prevent duplicate names by checking for commas or duplicates
        String candidateName = safeValue(request.getCandidateName(), "Candidate");
        if (candidateName != null && candidateName.contains(",")) {
            // Data is corrupted like "Arjun N,Arjun N" - take only first part
            candidateName = candidateName.split(",")[0].trim();
        }
        // Also check if name appears twice (no comma but repeated)
        if (candidateName != null && candidateName.split(" ").length > 4) {
            String[] parts = candidateName.split(" ");
            candidateName = parts[0] + " " + parts[parts.length - 1];
        }
        offer.setCandidateName(candidateName);

        // FIX: Prevent duplicate emails by checking for commas
        String candidateEmail = safeValue(request.getCandidateEmail(), "candidate@example.com");
        if (candidateEmail != null && candidateEmail.contains(",")) {
            // Data is corrupted like "email@example.com,email@example.com" - take only first part
            candidateEmail = candidateEmail.split(",")[0].trim();
        }
        offer.setCandidateEmail(candidateEmail);
    }

    private String safeValue(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    private String handleSignatureUpload(Long offerId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Path signaturesDir = Paths.get(basePath, "signatures");
        Files.createDirectories(signaturesDir);

        String fileName = "signature-" + offerId + ".png";
        Path targetPath = signaturesDir.resolve(fileName);

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Signature saved: {}", targetPath);
        return "/signatures/" + fileName;
    }

    private String generateOfferDocument(OfferLetter offer, String signaturePath) throws Exception {
        logger.info("Generating offer document for ID: {}", offer.getId());

        Context context = new Context();
        context.setVariable("offer", offer);
        context.setVariable("today", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        context.setVariable("currentYear", LocalDateTime.now().getYear());

        if (signaturePath != null) {
            try {
                Path signatureFilePath = Paths.get(basePath).resolve(signaturePath.substring(1));
                String signatureUrl = "file:///" + signatureFilePath.toString().replace("\\", "/");
                context.setVariable("signature", signatureUrl);
                context.setVariable("showSignature", true);
            } catch (Exception e) {
                logger.warn("Signature processing failed: {}", e.getMessage());
                context.setVariable("showSignature", false);
            }
        } else {
            context.setVariable("showSignature", false);
        }

        String templateName = "OFFER".equalsIgnoreCase(offer.getOfferType()) ?
                "offer-letter-template" : "appointment-letter-template";

        String htmlContent = templateEngine.process(templateName, context);
        logger.info("Template processed successfully");

        return pdfGenerationService.generateOfferPdf(
                htmlContent,
                offer.getCandidateName(),
                offer.getOfferType(),
                offer.getId()
        );
    }

    public OfferLetter createAndGenerateOfferForEmployee(OfferRequest request) throws Exception {
        logger.info("Creating offer for employee ID: {}", request.getEmployeeId());

        OfferLetter offer = new OfferLetter();
        offer.setEmployeeId(request.getEmployeeId());
        populateOfferData(offer, request);

        offer = offerLetterRepository.save(offer);
        logger.info("Employee offer created with ID: {}", offer.getId());

        String signaturePath = handleSignatureUpload(offer.getId(), request.getSignatureFile());
        String filePath = generateOfferDocument(offer, signaturePath);
        offer.setOfferFilePath(filePath);
        offer.setStatus("GENERATED");

        return offerLetterRepository.save(offer);
    }

    public Optional<OfferLetter> getOfferById(Long id) {
        return offerLetterRepository.findById(id);
    }

    public List<OfferLetter> getOffersByApplication(Long applicationId) {
        return offerLetterRepository.findByApplicationId(applicationId);
    }

    // === FIXES FOR COMPILATION ERRORS ===
    // Add these two methods that OfferController expects

    /**
     * Unified method to create offer for both application and employee
     * This is called from OfferController.createOffer()
     */
    public OfferLetter createOffer(OfferRequest request) throws Exception {
        logger.info("Unified createOffer called for type: {}", request.getType());

        if ("EMPLOYEE".equalsIgnoreCase(request.getType())) {
            if (request.getEmployeeId() == null) {
                throw new IllegalArgumentException("Employee ID is required for employee offers");
            }
            return createAndGenerateOfferForEmployee(request);
        } else {
            if (request.getApplicationId() == null) {
                throw new IllegalArgumentException("Application ID is required for application offers");
            }
            return createAndGenerateOffer(request);
        }
    }

    /**
     * Send offer letter to candidate via email
     * This is called from OfferController.sendOffer()
     */
    public OfferLetter sendOfferToCandidate(Long offerId) throws Exception {
        logger.info("Sending offer ID: {}", offerId);

        OfferLetter offer = offerLetterRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer letter not found: " + offerId));

        if (!"GENERATED".equals(offer.getStatus())) {
            throw new RuntimeException("Offer must be in GENERATED status before sending. Current status: " + offer.getStatus());
        }

        // Send email
        emailService.sendOfferLetter(
                offer.getCandidateEmail(),
                offer.getCandidateName(),
                offer.getOfferFilePath(),
                offer.getOfferType()
        );

        // Update status
        offer.setStatus("SENT");
        offer.setSentDate(LocalDateTime.now());

        return offerLetterRepository.save(offer);
    }
}