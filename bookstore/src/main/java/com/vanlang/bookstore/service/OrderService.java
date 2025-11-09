package com.vanlang.bookstore.service;

import com.vanlang.bookstore.model.Order;
import com.vanlang.bookstore.model.OrderItem;
import com.vanlang.bookstore.model.User;
import com.vanlang.bookstore.repository.OrderRepository;
import com.vanlang.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final BookService bookService;

    private String genOrderNumberOnce() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        return "ORD-" + date + "-" + rand;
    }

    private String newOrderNumber() {
        for (int i = 0; i < 5; i++) {
            String candidate = genOrderNumberOnce();
            if (!orderRepository.existsByOrderNumber(candidate)) return candidate;
        }
        return genOrderNumberOnce();
    }

    @Transactional
    public Order placeOrder(String loginEmailOrUsername, Order form, List<Long> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            throw new IllegalStateException("Bạn chưa chọn sản phẩm nào để thanh toán.");
        }
        Set<Long> chosen = new HashSet<>(selectedIds);

        User user = (loginEmailOrUsername == null)
                ? null
                : userRepository.findByEmailOrUsername(loginEmailOrUsername).orElse(null);

        var cartItems = cartService.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng.");
        }

        var filtered = cartItems.stream()
                .filter(ci -> ci.getBook()!=null && ci.getBook().getId()!=null
                        && chosen.contains(ci.getBook().getId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new IllegalStateException("Danh sách sản phẩm chọn thanh toán không hợp lệ.");
        }

        Order o = new Order();
        o.setOrderNumber(newOrderNumber());
        o.setUser(user);

        o.setCustomerName(form.getCustomerName());
        o.setCustomerPhone(form.getCustomerPhone());
        o.setCustomerEmail(form.getCustomerEmail());
        o.setShippingAddress(form.getShippingAddress());
        o.setNotes(form.getNotes());
        o.setPaymentMethod(form.getPaymentMethod() == null ? "cod" : form.getPaymentMethod());
        o.setStatus("PLACED");
        o.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (var ci : filtered) {
            var price = ci.getUnitPrice();
            if (price == null) {
                throw new IllegalStateException("Sản phẩm không có giá hợp lệ: " + ci.getBook().getTitle());
            }

            // trừ kho
            boolean stockUpdated = bookService.updateStock(ci.getBook().getId(), ci.getQuantity());
            if (!stockUpdated) {
                throw new IllegalStateException("Không đủ hàng trong kho cho sản phẩm: " + ci.getBook().getTitle());
            }

            OrderItem it = new OrderItem();
            it.setBook(ci.getBook());
            it.setQuantity(ci.getQuantity());
            it.setPrice(price.setScale(0, RoundingMode.HALF_UP).longValue());
            o.addItem(it);
            total = total.add(price.multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        o.setTotal(total.setScale(0, RoundingMode.HALF_UP).longValue());

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Order saved = orderRepository.save(o);
                cartService.removeByBookIds(chosen);
                return saved;
            } catch (DataIntegrityViolationException ex) {
                o.setOrderNumber(genOrderNumberOnce());
                if (attempt == 2) throw ex;
            }
        }
        throw new IllegalStateException("Không thể tạo đơn hàng.");
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    // ✅ QUAN TRỌNG: cập nhật qua service để kho + doanh thu chuẩn
    @Transactional
    public void updateStatus(Long orderId, String newStatus) {
        // Lấy kèm items để thao tác trong 1 transaction
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String oldStatus = order.getStatus();
        if (Objects.equals(oldStatus, newStatus)) return;

        // Hủy: hoàn kho + total = 0
        if (!"CANCELED".equals(oldStatus) && "CANCELED".equals(newStatus)) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    var book = item.getBook();
                    if (book != null) {
                        book.setStock(book.getStock() + item.getQuantity());
                        bookService.saveBook(book);
                    }
                }
            }
            order.setStatus("CANCELED");
            order.setTotal(0L); // 💥 để dashboard/report tự trừ doanh thu
            orderRepository.save(order);
            return;
        }

        // Khôi phục: trừ kho lại + tính lại total
        if ("CANCELED".equals(oldStatus) && !"CANCELED".equals(newStatus)) {
            long recomputed = 0L;
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    var book = item.getBook();
                    if (book != null) {
                        if (book.getStock() >= item.getQuantity()) {
                            book.setStock(book.getStock() - item.getQuantity());
                            bookService.saveBook(book);
                        } else {
                            throw new IllegalStateException("Không đủ tồn kho để khôi phục đơn #" + order.getId());
                        }
                        recomputed += (long) item.getQuantity() * (item.getPrice() == null ? 0L : item.getPrice());
                    }
                }
            }
            order.setStatus(newStatus);
            order.setTotal(recomputed); // 💥 cộng lại doanh thu
            orderRepository.save(order);
            return;
        }

        // Các chuyển trạng thái khác
        order.setStatus(newStatus);
        orderRepository.save(order);
    }
}
