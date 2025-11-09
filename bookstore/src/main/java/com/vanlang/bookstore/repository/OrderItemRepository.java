package com.vanlang.bookstore.repository;

import com.vanlang.bookstore.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        select coalesce(sum(i.quantity), 0)
        from OrderItem i
        where i.order.createdAt >= :start and i.order.createdAt < :end
    """)
    Long sumItemsBetween(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);
}
