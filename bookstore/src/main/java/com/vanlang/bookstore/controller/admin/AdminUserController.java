package com.vanlang.bookstore.controller.admin;

import com.vanlang.bookstore.model.User;
import com.vanlang.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        User u = new User();
        u.setEnabled(true);
        u.setRole(User.Role.USER);
        model.addAttribute("user", u);
        return "admin/users/form";
    }

    @PostMapping
    public String create(@ModelAttribute("user") User user,
                         @RequestParam(value = "rawPassword", required = false) String rawPassword) {
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userRepository.findById(id).orElseThrow());
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("user") User form,
                         @RequestParam(value = "rawPassword", required = false) String rawPassword) {
        User u = userRepository.findById(id).orElseThrow();
        u.setFullName(form.getFullName());
        u.setUsername(form.getUsername());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setAddress(form.getAddress());
        u.setRole(form.getRole());
        u.setEnabled(form.getEnabled());
        if (rawPassword != null && !rawPassword.isBlank()) {
            u.setPassword(passwordEncoder.encode(rawPassword));
        }
        userRepository.save(u);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        User u = userRepository.findById(id).orElseThrow();
        u.setEnabled(!Boolean.TRUE.equals(u.getEnabled()));  // đảo trạng thái true/false
        userRepository.save(u);
        return "redirect:/admin/users";
    }

}

