package com.vanlang.bookstore.controller.admin;

import com.vanlang.bookstore.model.Order;
import com.vanlang.bookstore.repository.OrderRepository;
import com.vanlang.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService; // dùng service để cập nhật trạng thái

    // KHÔNG PHÂN TRANG: list tất cả + search đơn giản
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       Model model) {

        // Lấy hết, rồi lọc theo keyword/status (đơn giản, tránh thêm query phức tạp)
        List<Order> orders = orderRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            orders = orders.stream().filter(o ->
                    (o.getOrderNumber() != null && o.getOrderNumber().toLowerCase().contains(kw)) ||
                            (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(kw)) ||
                            (o.getCustomerPhone() != null && o.getCustomerPhone().toLowerCase().contains(kw)) ||
                            (o.getShippingAddress() != null && o.getShippingAddress().toLowerCase().contains(kw)) ||
                            (o.getUser() != null && (
                                    (o.getUser().getUsername() != null && o.getUser().getUsername().toLowerCase().contains(kw)) ||
                                            (o.getUser().getFullName() != null && o.getUser().getFullName().toLowerCase().contains(kw)) ||
                                            (o.getUser().getEmail() != null && o.getUser().getEmail().toLowerCase().contains(kw))
                            ))
            ).toList();
        }

        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(o -> status.equalsIgnoreCase(o.getStatus()))
                    .toList();
        }

        // Sắp xếp mới nhất trước
        orders = orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        // Tổng tiền hiển thị: loại CANCELED cho chắc (kể cả khi total chưa = 0)
        long totalAmount = orders.stream()
                .filter(o -> !"CANCELED".equalsIgnoreCase(o.getStatus()))
                .mapToLong(o -> o.getTotal() == null ? 0L : o.getTotal())
                .sum();

        model.addAttribute("orders", orders);
        model.addAttribute("totalCount", orders.size());
        model.addAttribute("totalAmount", totalAmount);

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Order order = orderRepository.findByIdWithItems(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/admin/orders";
        }

        long itemsTotalQty = order.getItems() == null ? 0 :
                order.getItems().stream().mapToLong(it -> it.getQuantity() == null ? 0 : it.getQuantity()).sum();
        long itemsTotalMoney = order.getItems() == null ? 0 :
                order.getItems().stream().mapToLong(it ->
                        (it.getPrice() == null ? 0 : it.getPrice()) *
                                (it.getQuantity() == null ? 0 : it.getQuantity())
                ).sum();

        model.addAttribute("order", order);
        model.addAttribute("items", order.getItems());
        model.addAttribute("itemsTotalQty", itemsTotalQty);
        model.addAttribute("itemsTotalMoney", itemsTotalMoney);
        return "admin/orders/detail";
    }

    // Cập nhật trạng thái qua Service để áp dụng logic kho + doanh thu
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes ra) {
        try {
            orderService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái đơn #" + id + " → " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        if (!orderRepository.existsById(id)) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/admin/orders";
        }
        orderRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa đơn hàng #" + id);
        return "redirect:/admin/orders";
    }
}
