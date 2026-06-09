package com.yogiraj.campusconnect.repository;

import com.yogiraj.campusconnect.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, String> {

    List<Complaint> findByComplaintIdContainingIgnoreCase(String complaintId);

    List<Complaint> findByNameContainingIgnoreCase(String name);

    List<Complaint> findByEmailContainingIgnoreCase(String email);

    List<Complaint> findByStatus(String status);

    List<Complaint> findByCategory(String category);

    List<Complaint> findByStatusAndCategory(String status, String category);
}