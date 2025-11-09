package com.vanlang.bookstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policy")
public class PolicyController {

    @GetMapping("/return")
    public String returnPolicy(Model model) {
        model.addAttribute("title", "Chính Sách Đổi Trả");
        return "policy/return";
    }

    @GetMapping("/privacy")
    public String privacyPolicy(Model model) {
        model.addAttribute("title", "Chính Sách Bảo Mật");
        return "policy/privacy";
    }

    @GetMapping("/terms")
    public String termsPolicy(Model model) {
        model.addAttribute("title", "Điều Khoản Sử Dụng");
        return "policy/terms";
    }
}
