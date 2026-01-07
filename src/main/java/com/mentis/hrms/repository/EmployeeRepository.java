package com.mentis.hrms.repository;

import com.mentis.hrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.mentis.hrms.model.enums.UserRole;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByPersonalEmail(String personalEmail);
    // ✅ ADD THESE 2 NEW METHODS
    List<Employee> findByDocumentDeadlineIsNotNull();

    List<Employee> findByDocumentDeadlineIsNotNullAndOnboardingStatusIn(List<String> statuses);

}