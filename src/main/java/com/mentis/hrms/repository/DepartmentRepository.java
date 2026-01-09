package com.mentis.hrms.repository;

import com.mentis.hrms.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Department findByName(String name);

    // Add this method if it doesn't exist
    boolean existsById(Long id);
}