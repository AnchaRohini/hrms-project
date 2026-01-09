package com.mentis.hrms.service;

import com.mentis.hrms.model.Job;
import com.mentis.hrms.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList; // Add this import

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    public List<Job> getAllJobs() {
        try {
            List<Job> jobs = jobRepository.findAll();
            // Initialize lists for each job
            for (Job job : jobs) {
                initializeJobLists(job);
            }
            return jobs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve jobs: " + e.getMessage(), e);
        }
    }

    public List<Job> getActiveJobs() {
        try {
            List<Job> jobs = jobRepository.findByActiveTrue();
            // Initialize lists for each job
            for (Job job : jobs) {
                initializeJobLists(job);
            }
            return jobs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve active jobs: " + e.getMessage(), e);
        }
    }

    public Job saveJob(Job job) {
        try {
            // Ensure the job has required fields before saving
            if (job.getTitle() == null || job.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Job title is required");
            }
            if (job.getDepartment() == null || job.getDepartment().trim().isEmpty()) {
                throw new IllegalArgumentException("Department is required");
            }

            // Initialize lists if null
            initializeJobLists(job);

            return jobRepository.save(job);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save job: " + e.getMessage(), e);
        }
    }

    public Job getJobById(Long id) {
        try {
            Optional<Job> job = jobRepository.findById(id);
            if (job.isPresent()) {
                initializeJobLists(job.get());
                return job.get();
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve job with ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void deleteJob(Long id) {
        try {
            if (!jobRepository.existsById(id)) {
                throw new RuntimeException("Job with ID " + id + " does not exist");
            }
            jobRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete job with ID " + id + ": " + e.getMessage(), e);
        }
    }

    // Additional method to find jobs by department
    public List<Job> getJobsByDepartment(String department) {
        try {
            List<Job> jobs = jobRepository.findByDepartment(department);
            // Initialize lists for each job
            for (Job job : jobs) {
                initializeJobLists(job);
            }
            return jobs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve jobs for department " + department + ": " + e.getMessage(), e);
        }
    }

    // Helper method to initialize lists
    private void initializeJobLists(Job job) {
        if (job.getRequirementList() == null) {
            job.setRequirementList(new ArrayList<>());
        }
        if (job.getResponsibilities() == null) {
            job.setResponsibilities(new ArrayList<>());
        }
    }
}