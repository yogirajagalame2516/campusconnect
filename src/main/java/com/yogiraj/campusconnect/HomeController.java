package com.yogiraj.campusconnect;

import com.yogiraj.campusconnect.model.Complaint;
import com.yogiraj.campusconnect.repository.ComplaintRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class HomeController {

    private final ComplaintRepository complaintRepository;

    public HomeController(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/admin-login")
    public String adminLogin() {
        return "admin-login";
    }

    @PostMapping("/admin-login")
    public String checkAdminPassword(
            @RequestParam String password,
            HttpSession session,
            Model model) {

        if ("admin123".equals(password)) {
            session.setAttribute("adminLoggedIn", true);
            return "redirect:/admin";
        }

        model.addAttribute("error", "Invalid password");
        return "admin-login";
    }

    @GetMapping("/admin-logout")
    public String adminLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin-login";
    }

    @GetMapping("/submit")
    public String submitPage(Model model) {
        model.addAttribute("complaint", new Complaint());
        return "submit";
    }

    @PostMapping("/submit")
    public String submitComplaint(@ModelAttribute Complaint complaint) {
        if (complaint.getName() == null || complaint.getName().trim().isEmpty()
                || complaint.getEmail() == null || complaint.getEmail().trim().isEmpty()
                || complaint.getMessage() == null || complaint.getMessage().trim().isEmpty()) {
            return "redirect:/submit";
        }

        String complaintId = "CC-" + System.currentTimeMillis();

        complaint.setComplaintId(complaintId);
        complaint.setPriority("Medium");
        complaint.setStatus("Pending");
        complaint.setCreatedDate(LocalDate.now().toString());

        complaintRepository.save(complaint);

        return "redirect:/list";
    }

    @GetMapping("/list")
    public String complaintList(Model model) {
        List<Complaint> complaints = complaintRepository.findAll();
        Collections.reverse(complaints);

        model.addAttribute("complaints", complaints);
        return "list";
    }

    @GetMapping("/complaints")
    public String viewComplaints(Model model) {
        List<Complaint> complaints = complaintRepository.findAll();
        Collections.reverse(complaints);

        model.addAttribute("complaints", complaints);
        return "list";
    }

    @GetMapping("/admin")
    public String adminDashboard(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            HttpSession session,
            Model model) {

        Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");

        if (adminLoggedIn == null || !adminLoggedIn) {
            return "redirect:/admin-login";
        }

        List<Complaint> complaints;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty();
        boolean hasCategory = category != null && !category.trim().isEmpty();

        if (hasSearch) {
            String keyword = search.trim();

            complaints = complaintRepository.findByComplaintIdContainingIgnoreCase(keyword);

            if (complaints.isEmpty()) {
                complaints = complaintRepository.findByNameContainingIgnoreCase(keyword);
            }

            if (complaints.isEmpty()) {
                complaints = complaintRepository.findByEmailContainingIgnoreCase(keyword);
            }
        } else if (hasStatus && hasCategory) {
            complaints = complaintRepository.findByStatusAndCategory(status, category);
        } else if (hasStatus) {
            complaints = complaintRepository.findByStatus(status);
        } else if (hasCategory) {
            complaints = complaintRepository.findByCategory(category);
        } else {
            complaints = complaintRepository.findAll();
        }

        Collections.reverse(complaints);

        int total = complaints.size();
        int pending = 0;
        int inProgress = 0;
        int resolved = 0;

        for (Complaint complaint : complaints) {
            if ("Pending".equals(complaint.getStatus())) {
                pending++;
            } else if ("In Progress".equals(complaint.getStatus())) {
                inProgress++;
            } else if ("Resolved".equals(complaint.getStatus())) {
                resolved++;
            }
        }

        model.addAttribute("complaints", complaints);
        model.addAttribute("total", total);
        model.addAttribute("pending", pending);
        model.addAttribute("inProgress", inProgress);
        model.addAttribute("resolved", resolved);
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);

        return "admin";
    }

    @PostMapping("/update")
    public String updateComplaint(
            @RequestParam("complaintId") String complaintId,
            @RequestParam String priority,
            @RequestParam String status,
            @RequestParam(required = false) String adminNote,
            HttpSession session) {

        Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");

        if (adminLoggedIn == null || !adminLoggedIn) {
            return "redirect:/admin-login";
        }

        Optional<Complaint> optionalComplaint =
                complaintRepository.findById(complaintId.trim());

        if (optionalComplaint.isPresent()) {
            Complaint complaint = optionalComplaint.get();

            complaint.setPriority(priority);
            complaint.setStatus(status);
            complaint.setAdminNote(adminNote == null ? "" : adminNote);

            complaintRepository.save(complaint);
        }

        return "redirect:/admin";
    }

    @GetMapping("/track")
    public String trackPage() {
        return "track";
    }

    @PostMapping("/track")
    public String trackComplaint(
            @RequestParam(value = "complaintId", required = false) String complaintId,
            Model model) {

        if (complaintId == null || complaintId.trim().isEmpty()) {
            model.addAttribute("error", "Please enter Complaint ID");
            return "track";
        }

        Optional<Complaint> optionalComplaint =
                complaintRepository.findById(complaintId.trim());

        if (optionalComplaint.isPresent()) {
            model.addAttribute("complaint", optionalComplaint.get());
            return "track";
        }

        model.addAttribute("error", "Complaint not found");

        return "track";
    }

    @GetMapping("/admin/complaint/{complaintId}")
    public String complaintDetails(
                @PathVariable String complaintId,
                HttpSession session,
                Model model) {

            Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");

            if (adminLoggedIn == null || !adminLoggedIn) {
                return "redirect:/admin-login";
            }

            Optional<Complaint> optionalComplaint =
                    complaintRepository.findById(complaintId.trim());

            if (optionalComplaint.isPresent()) {
                model.addAttribute("complaint", optionalComplaint.get());
                return "complaint-details";
            }

            return "redirect:/admin";
    }

    @GetMapping("/admin/complaint/pdf/{complaintId}")
    public void downloadSingleComplaintPdf(
            @PathVariable String complaintId,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");

        if (adminLoggedIn == null || !adminLoggedIn) {
            response.sendRedirect("/admin-login");
            return;
        }

        Optional<Complaint> optionalComplaint =
                complaintRepository.findById(complaintId.trim());

        if (optionalComplaint.isEmpty()) {
            response.sendRedirect("/admin");
            return;
        }

        Complaint complaint = optionalComplaint.get();

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=" + complaint.getComplaintId() + ".pdf"
        );

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        document.add(new Paragraph("CampusConnect Complaint Report"));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Complaint ID: " + complaint.getComplaintId()));
        document.add(new Paragraph("Student Name: " + complaint.getName()));
        document.add(new Paragraph("Email: " + complaint.getEmail()));
        document.add(new Paragraph("Category: " + complaint.getCategory()));
        document.add(new Paragraph("Priority: " + complaint.getPriority()));
        document.add(new Paragraph("Status: " + complaint.getStatus()));
        document.add(new Paragraph("Date Submitted: " + complaint.getCreatedDate()));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Complaint Message:"));
        document.add(new Paragraph(complaint.getMessage()));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Admin Note:"));
        document.add(new Paragraph(
                complaint.getAdminNote() == null || complaint.getAdminNote().trim().isEmpty()
                        ? "-"
                        : complaint.getAdminNote()
        ));

        document.close();
    }
}