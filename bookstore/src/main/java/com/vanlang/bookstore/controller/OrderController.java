// src/main/java/com/vanlang/bookstore/controller/OrderController.java
package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.model.User;
import com.vanlang.bookstore.repository.OrderRepository;
import com.vanlang.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // Lịch sử đơn hàng của tôi
    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) return "redirect:/login";
        var u = userRepository.findByEmailOrUsername(principal.getUsername()).orElse(null);
        if (u == null) return "redirect:/login";
        model.addAttribute("orders", orderRepository.findByUserOrderByCreatedAtDesc(u));
        return "orders";
    }

    // Chi tiết đơn
    @GetMapping("/orders/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber,
                              @AuthenticationPrincipal UserDetails principal,
                              Model model,
                              RedirectAttributes ra) {
        var orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/orders";
        }
        var order = orderOpt.get();

        boolean isOwner = false;
        if (order.getUser() != null && principal != null) {
            var p = principal.getUsername();
            isOwner = order.getUser().getEmail().equalsIgnoreCase(p)
                    || (order.getUser().getUsername()!=null
                    && order.getUser().getUsername().equalsIgnoreCase(p));
        }
        boolean isAdmin = principal != null && principal.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!(isOwner || isAdmin)) {
            ra.addFlashAttribute("error", "Bạn không có quyền xem đơn này");
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "order-detail";
    }
}
