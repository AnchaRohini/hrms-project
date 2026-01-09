package com.mentis.hrms.service;

import com.mentis.hrms.model.Department;
import com.mentis.hrms.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> getAllDepartments() {
        try {
            return departmentRepository.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve departments: " + e.getMessage(), e);
        }
    }

    public Department saveDepartment(Department department) {
        try {
            // Validate department before saving
            if (department.getName() == null || department.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Department name is required");
            }

            return departmentRepository.save(department);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save department: " + e.getMessage(), e);
        }
    }

    public Department getDepartmentByName(String name) {
        try {
            return departmentRepository.findByName(name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve department with name " + name + ": " + e.getMessage(), e);
        }
    }

    // Additional method to check if department exists
    public boolean departmentExists(String name) {
        try {
            return departmentRepository.findByName(name) != null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to check department existence: " + e.getMessage(), e);
        }
    }

    // Additional method to delete department
    public void deleteDepartment(Long id) {
        try {
            if (!departmentRepository.existsById(id)) {
                throw new RuntimeException("Department with ID " + id + " does not exist");
            }
            departmentRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete department with ID " + id + ": " + e.getMessage(), e);
        }
    }
}