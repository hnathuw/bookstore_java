package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.model.User;
import com.vanlang.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgot(@RequestParam("email") String email, RedirectAttributes ra) {
        // TODO: generate token + gửi email (sau)
        ra.addFlashAttribute("msg", "Nếu email tồn tại, đường dẫn đặt lại mật khẩu đã được gửi.");
        return "redirect:/login";
    }

}

