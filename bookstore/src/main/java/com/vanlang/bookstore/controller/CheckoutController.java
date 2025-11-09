package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.model.CartItem;
import com.vanlang.bookstore.model.Order;
import com.vanlang.bookstore.service.CartService;
import com.vanlang.bookstore.service.OrderService;
import com.vanlang.bookstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    /** /cart tick -> submit -> GET /checkout?selectedIds=... */
    @GetMapping
    public String checkout(
            @RequestParam(name = "selectedIds", required = false) List<Long> selectedIds,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes ra
    ) {
        if (principal == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán.");
            return "redirect:/login";
        }
        if (selectedIds == null || selectedIds.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán.");
            return "redirect:/cart";
        }

        List<CartItem> selectedItems = cartService.getItemsByBookIds(selectedIds);
        if (selectedItems.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy sản phẩm đã chọn trong giỏ.");
            return "redirect:/cart";
        }

        BigDecimal subtotal = cartService.calculateSubtotal(selectedItems);
        BigDecimal shipping = cartService.calculateShipping(selectedItems);
        BigDecimal total = subtotal.add(shipping);

        model.addAttribute("selectedItems", selectedItems);
        model.addAttribute("selectedIds", selectedIds);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shipping", shipping);
        model.addAttribute("total", total);
        model.addAttribute("cartItemCount", cartService.getTotalItems());

        // Prefill: ưu tiên shipping-default; fallback sang profile info
        userService.findByLogin(principal.getUsername()).ifPresent(u -> {
            model.addAttribute("prefName",
                    u.getShippingFullName() != null ? u.getShippingFullName() : u.getFullName());
            model.addAttribute("prefPhone",
                    u.getShippingPhone() != null ? u.getShippingPhone() : u.getPhone());
            model.addAttribute("prefEmail", u.getEmail()); // email dùng luôn email tài khoản
            model.addAttribute("prefAddress",
                    u.getShippingAddress() != null ? u.getShippingAddress() : u.getAddress());
        });

        return "checkout";
    }

    /** POST /checkout/confirm: tạo đơn chỉ từ các bookId đã chọn */
    @PostMapping("/confirm")
    public String confirm(
            @RequestParam("selectedIds") List<Long> selectedIds,
            @AuthenticationPrincipal UserDetails principal,
            @ModelAttribute Order form, // customerName/Phone/Email/Address/notes/paymentMethod
            @RequestParam(value = "saveAsDefault", required = false) Boolean saveAsDefault,
            RedirectAttributes ra,
            Model model
    ) {
        if (principal == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán.");
            return "redirect:/login";
        }
        if (selectedIds == null || selectedIds.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán.");
            return "redirect:/cart";
        }

        try {
            // lưu mặc định nếu được tick
            if (Boolean.TRUE.equals(saveAsDefault)) {
                userService.updateDefaultShipping(
                        principal.getUsername(),
                        form.getCustomerName(),
                        form.getCustomerPhone(),
                        form.getShippingAddress()
                );
            }

            Order order = orderService.placeOrder(principal.getUsername(), form, selectedIds);
            cartService.removeItemsByBookIds(selectedIds);

            model.addAttribute("orderId", order.getId());
            model.addAttribute("orderCode", order.getOrderNumber());
            model.addAttribute("cartItemCount", cartService.getTotalItems());
            return "order-success";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Thanh toán thất bại: " + e.getMessage());
            return "redirect:/cart";
        }
    }
}
