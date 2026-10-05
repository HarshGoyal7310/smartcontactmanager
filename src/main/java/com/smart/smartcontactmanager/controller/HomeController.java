package com.smart.smartcontactmanager.controller;

import com.smart.smartcontactmanager.dao.UserRepository;
import com.smart.smartcontactmanager.entities.User;
import com.smart.smartcontactmanager.helper.Message;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid; // ✅ Fixed: jakarta.validation.Valid

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Home - Smart Contact Manager");
        return "home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About - Smart Contact Manager");
        return "about";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("title", "Register - Smart Contact Manager");
        model.addAttribute("user", new User());
        return "signup";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "Login - Smart Contact Manager");
        return "login";
    }

    // Handler for registering user
    @PostMapping("/do_register")
    public String registerUser(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam(value = "agreement", defaultValue = "false") boolean agreement,
            Model model,
            HttpSession session) {

        try {
            if (!agreement) {
                throw new Exception("Please accept terms and conditions !!");
            }

            if (bindingResult.hasErrors()) {
                System.out.println("Validation errors: " + bindingResult);
                model.addAttribute("title", "Register - Smart Contact Manager");
                model.addAttribute("user", user);
                return "signup";
            }

            // Check if email already exists
            if (userRepository.getUserByUserName(user.getEmail()) != null) {
                throw new Exception("This email is already registered. Please use another email.");
            }

            user.setRole("ROLE_USER");
            user.setEnabled(true);
            user.setImageUrl("default.png");
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // Save user to DB
            this.userRepository.save(user);

            // Fresh blank user for form reset on success
            model.addAttribute("user", new User());
            model.addAttribute("title", "Register - Smart Contact Manager");
            session.setAttribute("message", new Message("Successfully Registered !!", "alert-success"));
            return "signup";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("user", user); // Retain entered data
            model.addAttribute("title", "Register - Smart Contact Manager");
            session.setAttribute("message", new Message("Something went wrong! " + e.getMessage(), "alert-danger"));
            return "signup";
        }
    }
}