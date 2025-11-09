package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.model.User;
import com.vanlang.bookstore.service.CartService;
import com.vanlang.bookstore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CartService cartService;

    // Dùng để reload principal khi username/email thay đổi
    private final UserDetailsService userDetailsService;

    // ===================== ĐĂNG NHẬP =====================
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "login";
    }

    // ===================== ĐĂNG KÝ =====================
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                               @RequestParam(value = "avatar", required = false) MultipartFile avatar, // nếu muốn dùng sau
                               RedirectAttributes ra,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("cartItemCount", cartService.getTotalItems());
            return "register";
        }
        if (confirmPassword != null && user.getPassword() != null
                && !user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("cartItemCount", cartService.getTotalItems());
            return "register";
        }
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email đã được sử dụng!");
            model.addAttribute("cartItemCount", cartService.getTotalItems());
            return "register";
        }

        try {
            userService.registerUser(user);
            ra.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Đăng ký thất bại: " + e.getMessage());
            model.addAttribute("cartItemCount", cartService.getTotalItems());
            return "register";
        }
    }

    // ===================== PROFILE: Xem =====================
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) return "redirect:/login";
        var userOpt = userService.findByLogin(principal.getUsername()); // login có thể là email hoặc username
        if (userOpt.isEmpty()) return "redirect:/login";

        model.addAttribute("user", userOpt.get());
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "profile";
    }

    // ===================== PROFILE-EDIT: Hiển thị form chỉnh sửa =====================
    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) return "redirect:/login";
        var userOpt = userService.findByLogin(principal.getUsername());
        if (userOpt.isEmpty()) return "redirect:/login";

        model.addAttribute("user", userOpt.get());
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "profile-edit"; // đảm bảo có file templates/profile-edit.html
    }

    // ===================== PROFILE-EDIT: Submit cập nhật (info + avatar + mật khẩu) =====================
    @PostMapping("/profile/update")
    public String updateProfileForm(@AuthenticationPrincipal UserDetails principal,
                                    @Valid @ModelAttribute("user") User form,
                                    BindingResult result,
                                    @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                    @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                    @RequestParam(value = "newPassword", required = false) String newPassword,
                                    @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                    RedirectAttributes ra,
                                    Model model) {
        if (principal == null) return "redirect:/login";

        if (result.hasErrors()) {
            model.addAttribute("cartItemCount", cartService.getTotalItems());
            return "profile-edit";
        }

        try {
            // Lấy user trước khi cập nhật để kiểm tra có đổi username/email hay không
            var before = userService.findByLogin(principal.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

            // 1) Cập nhật thông tin + avatar
            var updated = userService.updateProfile(principal.getUsername(), form, avatarFile);

            // 2) Đổi mật khẩu nếu người dùng có nhập các field liên quan
            boolean wantChangePw =
                    (currentPassword != null && !currentPassword.isBlank()) ||
                            (newPassword != null && !newPassword.isBlank()) ||
                            (confirmPassword != null && !confirmPassword.isBlank());

            if (wantChangePw) {
                if (newPassword == null || newPassword.isBlank()
                        || confirmPassword == null || !newPassword.equals(confirmPassword)) {
                    ra.addFlashAttribute("error", "Mật khẩu mới không khớp hoặc trống!");
                    return "redirect:/profile/edit";
                }
                userService.changePassword(before.getUsername(), currentPassword, newPassword);
            }

            // 3) Nếu username hoặc email thay đổi → reload principal
            boolean principalChanged =
                    !updated.getUsername().equals(before.getUsername()) ||
                            !updated.getEmail().equalsIgnoreCase(before.getEmail());

            if (principalChanged) {
                var newDetails = userDetailsService.loadUserByUsername(updated.getUsername());
                var auth = new UsernamePasswordAuthenticationToken(
                        newDetails, newDetails.getPassword(), newDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            if (wantChangePw) {
                ra.addFlashAttribute("success", "Cập nhật hồ sơ & đổi mật khẩu thành công!");
            } else {
                ra.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
            }
            return "redirect:/profile";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile/edit";
        }
    }
}
