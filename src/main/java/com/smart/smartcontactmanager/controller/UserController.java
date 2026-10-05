package com.smart.smartcontactmanager.controller;

import com.smart.smartcontactmanager.dao.ContactRepository;
import com.smart.smartcontactmanager.dao.UserRepository;
import com.smart.smartcontactmanager.entities.Contact;
import com.smart.smartcontactmanager.entities.User;
import com.smart.smartcontactmanager.helper.Message;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Har request se pehle chalega: Logged-in user ka data model me daal dega
    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        if (principal != null) {
            String userName = principal.getName(); // Logged-in user ka email
            User user = userRepository.getUserByUserName(userName);
            model.addAttribute("user", user);
        }
    }

    // User Dashboard Home
    @GetMapping("/index")
    public String dashboard(Model model) {
        model.addAttribute("title", "User Dashboard - Smart Contact Manager");
        return "normal/user_dashboard";
    }

    // Open Add Contact Form Handler
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model) {
        model.addAttribute("title", "Add Contact - Smart Contact Manager");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    // Processing Add Contact Form
    @PostMapping("/process-contact")
    public String processContact(
            @ModelAttribute Contact contact,
            @RequestParam("imageFile") MultipartFile file,
            Principal principal,
            HttpSession session) {

        try {
            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);

            // Processing and uploading image file
            if (file.isEmpty()) {
                // agar user ne koi image upload nahi ki toh default set karenge
                contact.setImage("contact.png");
            } else {
                // Unique image filename banane ke liye timestamp use karte hain
                String originalFilename = file.getOriginalFilename();
                String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;
                contact.setImage(uniqueFilename);

                // Save file to static/images folder in target/classes and src
                File saveFile = new ClassPathResource("static/images").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + uniqueFilename);

                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image is uploaded to: " + path);
            }

            // Link contact with user
            contact.setUser(user);
            user.getContacts().add(contact);

            // Save user (CascadeType.ALL contact ko bhi save kar dega)
            this.userRepository.save(user);

            // Success message
            session.setAttribute("message", new Message("Your contact is added successfully! Add more..", "alert-success"));

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("message", new Message("Something went wrong! Try again..", "alert-danger"));
        }

        return "normal/add_contact_form";
    }

    // Show Contacts Handler (All + Starred Filter Support)
    @GetMapping("/show-contacts/{page}")
    public String showContacts(
            @PathVariable("page") Integer page,
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            Model model,
            Principal principal) {

        model.addAttribute("title", "View Contacts - Smart Contact Manager");

        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);

        Pageable pageable = PageRequest.of(page, 5);
        Page<Contact> contacts;

        if ("favorites".equalsIgnoreCase(filter)) {
            contacts = this.contactRepository.findFavoriteContactsByUser(user.getId(), pageable);
            model.addAttribute("filter", "favorites");
        } else {
            contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
            model.addAttribute("filter", "all");
        }

        model.addAttribute("contacts", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contacts.getTotalPages());

        return "normal/show-contacts";
    }

    // ==========================================
    // 1. SPECIFIC CONTACT DETAIL HANDLER
    // ==========================================
    @GetMapping("/contact/{cId}")
    public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
        java.util.Optional<Contact> contactOptional = this.contactRepository.findById(cId);

        if (contactOptional.isPresent()) {
            Contact contact = contactOptional.get();

            // Security Check: Kya ye contact isi logged-in user ka hai?
            String userName = principal.getName();
            User user = this.userRepository.getUserByUserName(userName);

            if (user.getId() == contact.getUser().getId()) {
                model.addAttribute("contact", contact);
                model.addAttribute("title", contact.getName() + " - Smart Contact Manager");
            } else {
                model.addAttribute("title", "Unauthorized Access");
                return "redirect:/user/show-contacts/0";
            }
        }

        return "normal/contact_detail";
    }

    // ==========================================
    // 2. DELETE CONTACT HANDLER
    // ==========================================
    @GetMapping("/delete/{cId}")
    public String deleteContact(@PathVariable("cId") Integer cId, Principal principal, HttpSession session) {
        java.util.Optional<Contact> contactOptional = this.contactRepository.findById(cId);

        if (contactOptional.isPresent()) {
            Contact contact = contactOptional.get();
            User user = this.userRepository.getUserByUserName(principal.getName());

            // Security Check
            if (user.getId() == contact.getUser().getId()) {
                // Image cleanup (agar default image nahi hai)
                try {
                    if (contact.getImage() != null && !contact.getImage().equals("contact.png")) {
                        File deleteFile = new ClassPathResource("static/images").getFile();
                        File file = new File(deleteFile, contact.getImage());
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Unlink contact from user and delete
                contact.setUser(null);
                this.contactRepository.delete(contact);

                session.setAttribute("message", new Message("Contact deleted successfully..", "alert-success"));
            } else {
                session.setAttribute("message", new Message("You are not authorized to delete this contact!", "alert-danger"));
            }
        }

        return "redirect:/user/--contacts/0";
    }

    // ==========================================
    // 3. OPEN UPDATE CONTACT FORM
    // ==========================================
    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid, Model model) {
        model.addAttribute("title", "Update Contact - Smart Contact Manager");
        Contact contact = this.contactRepository.findById(cid).get();
        model.addAttribute("contact", contact);
        return "normal/update_form";
    }

    // ==========================================
    // 4. PROCESS UPDATE CONTACT
    // ==========================================
    @PostMapping("/process-update")
    public String processUpdate(
            @ModelAttribute Contact contact,
            @RequestParam("imageFile") MultipartFile file,
            Principal principal,
            HttpSession session) {

        try {
            // Fetch old contact to preserve existing image if new one isn't uploaded
            Contact oldContact = this.contactRepository.findById(contact.getcId()).get();

            if (!file.isEmpty()) {
                // Delete old image if not default
                if (oldContact.getImage() != null && !oldContact.getImage().equals("contact.png")) {
                    File oldFile = new ClassPathResource("static/images").getFile();
                    File deleteOld = new File(oldFile, oldContact.getImage());
                    if (deleteOld.exists()) {
                        deleteOld.delete();
                    }
                }

                // Upload new image
                String uniqueFilename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File saveFile = new ClassPathResource("static/images").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + uniqueFilename);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                contact.setImage(uniqueFilename);
            } else {
                // Keep old image
                contact.setImage(oldContact.getImage());
            }

            User user = this.userRepository.getUserByUserName(principal.getName());
            contact.setUser(user);

            this.contactRepository.save(contact);
            session.setAttribute("message", new Message("Contact details updated successfully!", "alert-success"));

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("message", new Message("Failed to update contact! Try again.", "alert-danger"));
        }

        return "redirect:/user/contact/" + contact.getcId();
    }

    // ==========================================
    // 5. USER PROFILE VIEW HANDLER
    // ==========================================
    @GetMapping("/Profile")
    public String yourProfile(Model model) {
        model.addAttribute("title", "Profile - Smart Contact Manager");
        // Note: 'user' object pehle se hi @ModelAttribute addCommonData ke zariye model me available hai!
        return "normal/Profile";
    }

    // ==========================================
    // 6. SETTINGS (CHANGE PASSWORD) VIEW HANDLER
    // ==========================================
    @GetMapping("/settings")
    public String openSettings(Model model) {
        model.addAttribute("title", "Settings - Smart Contact Manager");
        return "normal/settings";
    }

    // ==========================================
    // 7. PROCESS CHANGE PASSWORD
    // ==========================================
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            Principal principal,
            HttpSession session) {

        String userName = principal.getName();
        User currentUser = this.userRepository.getUserByUserName(userName);

        // Check if old password matches with encrypted DB password
        if (this.passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            // Encode and save new password
            currentUser.setPassword(this.passwordEncoder.encode(newPassword));
            this.userRepository.save(currentUser);

            session.setAttribute("message", new Message("Your password has been successfully updated!", "alert-success"));
            return "redirect:/user/index";
        } else {
            // Old password incorrect
            session.setAttribute("message", new Message("Wrong old password entered! Please try again.", "alert-danger"));
            return "redirect:/user/settings";
        }
    }
        // ==========================================
        // DELETE ENTIRE USER ACCOUNT HANDLER
        // ==========================================
        @GetMapping("/delete-account")
        public String deleteUserAccount (
                Principal principal,
                jakarta.servlet.http.HttpServletRequest request,
                HttpSession session){

            try {
                String userName = principal.getName();
                User user = this.userRepository.getUserByUserName(userName);

                if (user != null) {
                    // 1. Delete associated contact images from filesystem (optional cleanup)
                    try {
                        File imgFolder = new ClassPathResource("static/images").getFile();
                        for (Contact c : user.getContacts()) {
                            if (c.getImage() != null && !c.getImage().equals("contact.png")) {
                                File f = new File(imgFolder, c.getImage());
                                if (f.exists()) f.delete();
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    // 2. Delete user (CascadeType.ALL saare contacts DB se khud delete kar dega)
                    this.userRepository.delete(user);

                    // 3. Clear Spring Security Context & Session Logout
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    if (session != null) {
                        session.invalidate();
                    }

                    return "redirect:/login?logout=true";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return "redirect:/user/profile";
        }
    // ==========================================
    // 1. EXPORT CONTACTS TO CSV / EXCEL
    // ==========================================
    @GetMapping("/export-csv")
    public void exportToCsv(Principal principal, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"contacts.csv\"");

        User user = this.userRepository.getUserByUserName(principal.getName());
        java.util.List<Contact> contacts = user.getContacts();

        java.io.PrintWriter writer = response.getWriter();
        // CSV Header
        writer.println("Name,SecondName,Email,Phone,Work,Description");

        for (Contact c : contacts) {
            writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    escapeCsv(c.getName()),
                    escapeCsv(c.getSecondName()),
                    escapeCsv(c.getEmail()),
                    escapeCsv(c.getPhone()),
                    escapeCsv(c.getWork()),
                    escapeCsv(c.getDescription())
            ));
        }
        writer.flush();
    }

    // ==========================================
    // 2. DOWNLOAD SAMPLE CSV TEMPLATE
    // ==========================================
    @GetMapping("/sample-csv")
    public void downloadSampleCsv(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"sample_contacts_template.csv\"");

        java.io.PrintWriter writer = response.getWriter();
        writer.println("Name,SecondName,Email,Phone,Work,Description");
        writer.println("\"Rahul Sharma\",\"Sharma Ji\",\"rahul@gmail.com\",\"9876543210\",\"Software Engineer\",\"College Friend\"");
        writer.println("\"Priya Patel\",\"\",\"priya@gmail.com\",\"9123456780\",\"Product Manager\",\"Colleague\"");
        writer.flush();
    }

    // ==========================================
    // 3. BULK CONTACT IMPORT (CSV FILE UPLOAD)
    // ==========================================
    @PostMapping("/import-contacts")
    public String importContacts(
            @RequestParam("csvFile") MultipartFile file,
            Principal principal,
            HttpSession session) {

        if (file.isEmpty()) {
            session.setAttribute("message", new Message("Please select a CSV file to upload!", "alert-danger"));
            return "redirect:/user/show-contacts/0";
        }

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;
            User user = this.userRepository.getUserByUserName(principal.getName());
            java.util.List<Contact> contactsToSave = new java.util.ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isFirstLine) {
                    isFirstLine = false; // Skip header line
                    continue;
                }

                // Handles comma separated values with quotes
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (data.length >= 4 && !cleanCsv(data[0]).isEmpty()) {
                    Contact c = new Contact();
                    c.setName(cleanCsv(data[0]));
                    if (data.length > 1) c.setSecondName(cleanCsv(data[1]));
                    if (data.length > 2) c.setEmail(cleanCsv(data[2]));
                    if (data.length > 3) c.setPhone(cleanCsv(data[3]));
                    if (data.length > 4) c.setWork(cleanCsv(data[4]));
                    if (data.length > 5) c.setDescription(cleanCsv(data[5]));
                    c.setImage("contact.png");
                    c.setUser(user);
                    contactsToSave.add(c);
                }
            }

            if (!contactsToSave.isEmpty()) {
                this.contactRepository.saveAll(contactsToSave);
                session.setAttribute("message", new Message(contactsToSave.size() + " contacts imported successfully!", "alert-success"));
            } else {
                session.setAttribute("message", new Message("No valid contacts found in the file.", "alert-danger"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("message", new Message("Error processing file: " + e.getMessage(), "alert-danger"));
        }

        return "redirect:/user/show-contacts/0";
    }

    // Helper functions for CSV format
    private String escapeCsv(String val) {
        return (val == null) ? "" : val.replace("\"", "\"\"");
    }

    private String cleanCsv(String val) {
        if (val == null) return "";
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1).replace("\"\"", "\"");
        }
        return val;
    }

    // AJAX 1-Click Toggle Favorite
    @PostMapping("/toggle-favorite/{cId}")
    @ResponseBody
    public java.util.Map<String, Object> toggleFavorite(@PathVariable("cId") Integer cId, Principal principal) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        java.util.Optional<Contact> contactOpt = this.contactRepository.findById(cId);

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            User user = this.userRepository.getUserByUserName(principal.getName());

            if (user.getId() == contact.getUser().getId()) {
                contact.setFavorite(!contact.isFavorite());
                this.contactRepository.save(contact);

                response.put("status", "success");
                response.put("isFavorite", contact.isFavorite());
                return response;
            }
        }

        response.put("status", "error");
        return response;
    }
    }
