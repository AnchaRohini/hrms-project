package com.mentis.hrms.controller;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.EmployeeService;
import com.mentis.hrms.util.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/candidate/auth")
public class CandidateAuthController {

    private static final Logger logger = LoggerFactory.getLogger(CandidateAuthController.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PasswordUtil passwordUtil;

    /* ==================== FORGOT PASSWORD FLOW ==================== */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "candidate/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetLink(@RequestParam String email,
                                @RequestParam(required = false) String employeeId,
                                RedirectAttributes ra) {

        logger.info("Forgot password request for email: {}, employeeId: {}", email, employeeId);

        try {
            Optional<Employee> employeeOpt;

            if (employeeId != null && !employeeId.trim().isEmpty()) {
                // Search by employee ID
                employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId.trim());
            } else {
                // Search by email
                employeeOpt = employeeService.getAllEmployees().stream()
                        .filter(e -> email.equalsIgnoreCase(e.getEmail()) ||
                                email.equalsIgnoreCase(e.getPersonalEmail()))
                        .findFirst();
            }

            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "No account found with provided email/employee ID.");
                return "redirect:/candidate/auth/forgot-password";
            }

            Employee employee = employeeOpt.get();

            // Check if employee is active
            if (!employee.isActive()) {
                ra.addFlashAttribute("error", "Account is deactivated. Please contact HR.");
                return "redirect:/candidate/auth/forgot-password";
            }

            // Check if credentials were created (has password)
            if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                ra.addFlashAttribute("error", "You haven't created your credentials yet. Please use the login page first.");
                return "redirect:/candidate/auth/forgot-password";
            }

            // Generate reset token
            String resetToken = UUID.randomUUID().toString();
            employee.setResetToken(resetToken);
            employee.setTokenExpiry(LocalDateTime.now().plusHours(24));

            employeeService.saveEmployee(employee);

            // Create reset link URL
            String resetLink = "http://localhost:8080/candidate/auth/reset-password?token=" + resetToken;

            // Store reset link in session for display (not in flash attribute)
            ra.addFlashAttribute("resetLink", resetLink);
            ra.addFlashAttribute("successMessage", "Password reset link generated successfully!");

            logger.info("Reset token generated for {}: {}", employee.getEmployeeId(), resetToken);

            return "redirect:/candidate/auth/forgot-password";

        } catch (Exception e) {
            logger.error("Error in forgot password: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An error occurred. Please try again.");
            return "redirect:/candidate/auth/forgot-password";
        }
    }

    /* ==================== CHANGE PASSWORD (LOGGED IN USER) ==================== */
    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session, Model model) {
        String employeeId = (String) session.getAttribute("userId");
        if (employeeId == null) {
            return "redirect:/candidate/auth/login?error=Please+login+first";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return "redirect:/candidate/auth/login?error=Employee+not+found";
        }

        model.addAttribute("employee", employeeOpt.get());
        return "candidate/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {

        String employeeId = (String) session.getAttribute("userId");
        if (employeeId == null) {
            return "redirect:/candidate/auth/login?error=Session+expired";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return "redirect:/candidate/auth/login?error=Employee+not+found";
        }

        Employee employee = employeeOpt.get();

        // Validate current password
        if (!passwordUtil.matchesPassword(currentPassword, employee.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/candidate/auth/change-password";
        }

        // Validate new password
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/candidate/auth/change-password";
        }

        if (newPassword.length() < 8) {
            ra.addFlashAttribute("error", "New password must be at least 8 characters");
            return "redirect:/candidate/auth/change-password";
        }

        // Update password
        employee.setPassword(passwordUtil.encodePassword(newPassword));
        employeeService.saveEmployee(employee);

        ra.addFlashAttribute("success", "Password changed successfully!");
        return "redirect:/candidate/dashboard/" + employeeId;
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token, Model model) {
        try {
            logger.info("Processing reset password request with token: {}", token);

            Optional<Employee> employeeOpt = employeeService.getAllEmployees().stream()
                    .filter(e -> token.equals(e.getResetToken()) &&
                            e.getTokenExpiry() != null &&
                            e.getTokenExpiry().isAfter(LocalDateTime.now()))
                    .findFirst();

            if (employeeOpt.isEmpty()) {
                logger.warn("Invalid or expired reset token: {}", token);
                model.addAttribute("error", "Invalid or expired reset token.");
                return "candidate/reset-password-error";
            }

            Employee employee = employeeOpt.get();
            model.addAttribute("token", token);
            model.addAttribute("employeeId", employee.getEmployeeId());
            model.addAttribute("employeeName", employee.getFirstName() + " " + employee.getLastName());

            logger.info("Reset password page loaded for employee: {}", employee.getEmployeeId());
            return "candidate/reset-password";

        } catch (Exception e) {
            logger.error("Error showing reset password page: {}", e.getMessage(), e);
            model.addAttribute("error", "Invalid reset token.");
            return "candidate/reset-password-error";
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes ra) {

        try {
            // Find employee with valid token
            Optional<Employee> employeeOpt = employeeService.getAllEmployees().stream()
                    .filter(e -> token.equals(e.getResetToken()) &&
                            e.getTokenExpiry() != null &&
                            e.getTokenExpiry().isAfter(LocalDateTime.now()))
                    .findFirst();

            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Invalid or expired reset token.");
                return "redirect:/candidate/login";
            }

            // Validate passwords
            if (!password.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Passwords do not match.");
                return "redirect:/candidate/auth/reset-password?token=" + token;
            }

            if (password.length() < 8) {
                ra.addFlashAttribute("error", "Password must be at least 8 characters long.");
                return "redirect:/candidate/auth/reset-password?token=" + token;
            }

            Employee employee = employeeOpt.get();

            // Update password and clear token
            employee.setPassword(passwordUtil.encodePassword(password));
            employee.setResetToken(null);
            employee.setTokenExpiry(null);

            employeeService.saveEmployee(employee);

            ra.addFlashAttribute("success", "Password reset successfully! You can now login with your new password.");
            return "redirect:/candidate/login";

        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An error occurred. Please try again.");
            return "redirect:/candidate/auth/reset-password?token=" + token;
        }
    }





    public static void main(String[] args) {
        // Use your PasswordUtil bean
        PasswordUtil util = new PasswordUtil();
        String hash = util.encodePassword("Test@123");
        System.out.println("HASHED PASSWORD: " + hash);
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/candidate/login?logout=success";
    }


}