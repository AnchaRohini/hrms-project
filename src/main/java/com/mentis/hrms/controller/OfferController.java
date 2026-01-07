package com.mentis.hrms.controller;

import com.mentis.hrms.dto.OfferRequest;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.model.OfferLetter;
import com.mentis.hrms.service.EmployeeService;
import com.mentis.hrms.service.JobApplicationService;
import com.mentis.hrms.service.OfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/dashboard")
public class OfferController {

    private static final Logger logger = LoggerFactory.getLogger(OfferController.class);

    @Autowired
    private OfferService offerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JobApplicationService jobApplicationService;

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    @GetMapping("/offers/create/{id}")
    public String showCreateOfferForm(@PathVariable String id,
                                      @RequestParam(required = false) String type,
                                      Model model) {
        try {
            OfferRequest offerRequest = new OfferRequest();

            if ("EMPLOYEE".equals(type)) {
                // Use only the methods that actually exist
                Employee employee = findEmployeeSafely(id);
                if (employee != null) {
                    offerRequest.setEmployeeId(id);
                    offerRequest.setCandidateName(employee.getFirstName() + " " + employee.getLastName());
                    offerRequest.setCandidateEmail(employee.getEmail() != null ? employee.getEmail() : "employee@company.com");
                    offerRequest.setDesignation(employee.getDesignation() != null ? employee.getDesignation() : "Software Engineer");
                    offerRequest.setDepartment(employee.getDepartment() != null ? employee.getDepartment() : "IT");
                } else {
                    // Fallback values
                    offerRequest.setEmployeeId(id);
                    offerRequest.setCandidateName("Employee " + id);
                    offerRequest.setCandidateEmail("employee" + id + "@company.com");
                    offerRequest.setDesignation("Software Engineer");
                    offerRequest.setDepartment("IT");
                }
            } else {
                // Get application details
                JobApplication application = jobApplicationService.getApplicationById(Long.parseLong(id));
                if (application != null) {
                    offerRequest.setApplicationId(application.getId());
                    offerRequest.setCandidateName(application.getFirstName() + " " + application.getLastName());
                    offerRequest.setCandidateEmail(application.getEmail());
                    offerRequest.setDesignation(application.getJobTitle() != null ? application.getJobTitle() : "Software Engineer");
                    offerRequest.setDepartment(application.getJobDepartment() != null ? application.getJobDepartment() : "IT");
                }
            }

            // Set default values
            offerRequest.setEmploymentType("Full-time");
            offerRequest.setWorkLocation("Hyderabad Office");
            offerRequest.setCurrency("USD");
            offerRequest.setProbationPeriod("3 months");
            offerRequest.setOfferType("OFFER");
            offerRequest.setJoiningDate(java.time.LocalDate.now().plusDays(14).toString());
            offerRequest.setReportingManager("To be assigned");

            model.addAttribute("offerRequest", offerRequest);
            return "templates/offer/create-offer";

        } catch (Exception e) {
            logger.error("Error loading offer form: {}", e.getMessage());
            model.addAttribute("error", "Failed to load offer form: " + e.getMessage());
            return "redirect:/dashboard/employees";
        }
    }

    @PostMapping("/save-offer-letter")
    public String saveOffer(@ModelAttribute OfferRequest offerRequest,
                            @RequestParam(required = false) MultipartFile signatureFile,
                            Model model) {
        try {
            offerRequest.setSignatureFile(signatureFile);

            // Create offer using the unified method
            OfferLetter offer = offerService.createOffer(offerRequest);

            // Redirect with success parameters
            return "redirect:/dashboard/offer-candidates?success=Offer+generated+successfully&offerId=" + offer.getId();

        } catch (Exception e) {
            logger.error("Error saving offer: {}", e.getMessage());
            model.addAttribute("error", "Failed to generate offer: " + e.getMessage());
            model.addAttribute("offerRequest", offerRequest);
            return "templates/offer/create-offer";
        }
    }

    @GetMapping("/offers/download/{offerId}")
    public ResponseEntity<byte[]> downloadOffer(@PathVariable Long offerId) {
        try {
            OfferLetter offer = offerService.getOfferById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offer not found"));

            String filePath = basePath + "/" + offer.getOfferFilePath();
            Path path = Paths.get(filePath);

            byte[] pdfBytes = Files.readAllBytes(path);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("offer-letter-" + offerId + ".pdf").build());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error downloading offer: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/offers/candidates")
    public String showOfferCandidates(Model model) {
        try {
            List<OfferLetter> offers = offerService.getAllOfferLetters();
            model.addAttribute("offerCandidates", offers);
            return "templates/offer/offer-candidates";
        } catch (Exception e) {
            logger.error("Error loading offer candidates: {}", e.getMessage());
            model.addAttribute("error", "Failed to load offers: " + e.getMessage());
            return "templates/offer/offer-candidates";
        }
    }

    @PostMapping("/offers/send/{offerId}")
    @ResponseBody
    public Map<String, Object> sendOffer(@PathVariable Long offerId) {
        Map<String, Object> response = new HashMap<>();
        try {
            OfferLetter offer = offerService.sendOfferToCandidate(offerId);
            response.put("success", true);
            response.put("message", "Offer sent successfully to " + offer.getCandidateEmail());
        } catch (Exception e) {
            logger.error("Error sending offer: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @GetMapping("/offers/details/{offerId}")
    @ResponseBody
    public Map<String, Object> getOfferDetails(@PathVariable Long offerId) {
        Map<String, Object> response = new HashMap<>();
        try {
            OfferLetter offer = offerService.getOfferById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offer not found"));
            response.put("success", true);
            response.put("offer", offer);
        } catch (Exception e) {
            logger.error("Error loading offer details: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    // Safe method to find employee without depending on specific method names
    private Employee findEmployeeSafely(String employeeId) {
        try {
            // Get all employees and find by ID
            List<Employee> allEmployees = employeeService.getAllEmployees();
            for (Employee emp : allEmployees) {
                if (employeeId.equals(emp.getEmployeeId())) {
                    return emp;
                }
            }
            return null;
        } catch (Exception e) {
            logger.warn("Error finding employee with ID {}: {}", employeeId, e.getMessage());
            return null;
        }
    }
}