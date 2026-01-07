

        package com.mentis.hrms.service;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Save employee with all details
     */
    @Transactional
    public Employee saveEmployee(Employee employee) {
        try {
            // Generate employee ID if not set
            if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
                employee.setEmployeeId(generateEmployeeId());
            }

            // Set audit fields
            if (employee.getCreatedDate() == null) {
                employee.setCreatedDate(LocalDateTime.now());
            }
            employee.setUpdatedDate(LocalDateTime.now());

            // Set default status if not set
            if (employee.getStatus() == null || employee.getStatus().isEmpty()) {
                employee.setStatus("Active");
            }

            // Set default onboarding status
            if (employee.getOnboardingStatus() == null || employee.getOnboardingStatus().isEmpty()) {
                employee.setOnboardingStatus("NOT_STARTED");
            }

            // Ensure email fields are consistent
            if (employee.getEmail() == null && employee.getPersonalEmail() != null) {
                employee.setEmail(employee.getPersonalEmail());
            }

            logger.info("Saving employee: {} {} (ID: {})",
                    employee.getFirstName(), employee.getLastName(), employee.getEmployeeId());

            Employee savedEmployee = employeeRepository.save(employee);
            employeeRepository.flush(); // Force immediate save

            logger.info("Employee saved successfully: {}", savedEmployee.getEmployeeId());
            return savedEmployee;

        } catch (Exception e) {
            logger.error("Error saving employee: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save employee: " + e.getMessage(), e);
        }
    }
    @Transactional
    public Employee updateEmployeeColor(String employeeId, String color) {
        Optional<Employee> employeeOpt = getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            employee.setProfileColor(color);
            return updateEmployee(employee);
        }
        throw new RuntimeException("Employee not found: " + employeeId);
    }
    /**
     * Get employee by employee ID with all details
     */
    @Transactional(readOnly = true)
    public Optional<Employee> getEmployeeByEmployeeId(String employeeId) {
        try {
            return employeeRepository.findByEmployeeId(employeeId);
        } catch (Exception e) {
            logger.error("Error fetching employee {}: {}", employeeId, e.getMessage());
            throw new RuntimeException("Failed to fetch employee: " + e.getMessage(), e);
        }
    }

    /**
     * Get all employees
     */
    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    /**
     * Update employee
     */
    @Transactional
    public Employee updateEmployee(Employee employee) {
        if (employee.getId() == null) {
            throw new IllegalArgumentException("Cannot update employee without ID");
        }

        employee.setUpdatedDate(LocalDateTime.now());
        Employee updated = employeeRepository.save(employee);
        employeeRepository.flush();

        logger.info("Employee updated: {}", updated.getEmployeeId());
        return updated;
    }
    /**
     * Delete employee by ID
     */
    @Transactional
    public void deleteEmployee(Long id) {
        try {
            if (!employeeRepository.existsById(id)) {
                throw new IllegalArgumentException("Employee not found with ID: " + id);
            }
            employeeRepository.deleteById(id);
            logger.info("Employee deleted: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting employee {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete employee: " + e.getMessage(), e);
        }
    }
    /**
     * Check if employee exists
     */
    public boolean employeeExists(String employeeId) {
        return employeeRepository.existsByEmployeeId(employeeId);
    }

    /**
     * Generate unique employee ID
     */
    private String generateEmployeeId() {
        String prefix = "MENTI";
        Random random = new Random();
        String employeeId;

        do {
            int randomNum = 1000 + random.nextInt(9000);
            employeeId = prefix + randomNum;
        } while (employeeExists(employeeId));

        logger.info("Generated Employee ID: {}", employeeId);
        return employeeId;
    }


}