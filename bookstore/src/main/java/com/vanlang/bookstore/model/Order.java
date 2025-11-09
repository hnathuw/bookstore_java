package com.vanlang.bookstore.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã đơn hàng (unique) */
    @Column(name = "order_number", length = 40, unique = true, nullable = false)
    private String orderNumber;

    /** Chủ đơn */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_orders_user"))
    private User user;

    /** Thông tin từ form checkout */
    @Column(name = "customer_name", length = 150, nullable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 30, nullable = false)
    private String customerPhone;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "shipping_address", length = 500, nullable = false)
    private String shippingAddress;

    @Column(length = 500)
    private String notes;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // "cod", "bank", "vnpay", ...

    /** Tổng tiền (map đúng cột DB: total_amount) */
    @Column(name = "total_amount", nullable = false)
    private Long total = 0L;

    @Column(length = 30, nullable = false)
    private String status = "PLACED"; // PLACED, PAID, SHIPPED, DONE, CANCELED,...

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Dòng hàng */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /** đảm bảo createdAt & orderNumber trước khi insert */
    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            // đơn giản: thời gian + id tạm (id có sau insert, nên dùng timestamp là đủ unique cho demo)
            orderNumber = "ORD-" + System.currentTimeMillis();
        }
    }

    /** tiện ích để gắn liên kết 2 chiều */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
