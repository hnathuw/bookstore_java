package com.vanlang.bookstore.admin.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AdminDashboardStats {
    // Range
    private LocalDate from;
    private LocalDate to;

    // Top tiles
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long itemsSold;
    private long totalBooks;
    private long totalUsers;

    // Charts
    private List<String> chartLabels;   // yyyy-MM-dd
    private List<BigDecimal> chartRevenue;
    private List<Long> chartOrders;

    // Tables
    // topBooks: List<Object[]> -> [title(String), qty(Long)]
    private List<Object[]> topBooks;

    // recentOrders: List<Order>
    private List<com.vanlang.bookstore.model.Order> recentOrders;
}

