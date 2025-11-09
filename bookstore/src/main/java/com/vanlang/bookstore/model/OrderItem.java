package com.vanlang.bookstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ về Order
    @JsonIgnore   // Ngăn Jackson lặp vô hạn khi serialize JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    // Sản phẩm (book)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_items_book"))
    private Book book;

    // Số lượng
    @Column(nullable = false)
    private Integer quantity;

    // Đơn giá (tại thời điểm đặt, lưu lại để không bị thay đổi theo giá hiện hành)
    @Column(nullable = false)
    private Long price;

    public Long getSubtotal() {
        return price * quantity;
    }

}
