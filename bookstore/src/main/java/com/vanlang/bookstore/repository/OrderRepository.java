package com.vanlang.bookstore.repository;

import com.vanlang.bookstore.model.Order;
import com.vanlang.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ======= CRUD & lookup thường dùng =======
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);

    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // ======= Dành cho màn detail: fetch full để tránh Lazy =======
    @EntityGraph(attributePaths = {"items", "items.book", "user"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    // ======= Dashboard / thống kê =======
    List<Order> findTop10ByOrderByCreatedAtDesc();

    @Query("""
        select coalesce(sum(o.total),0)
        from Order o
        where o.createdAt >= :start
          and o.createdAt < :end
          and o.status <> 'CANCELED'
    """)
    Long sumRevenueBetweenLong(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
    select b.title, sum(i.quantity) as qty
    from Order o join o.items i join i.book b
    where o.createdAt >= :start and o.createdAt < :end
      and o.status <> 'CANCELED'
    group by b.title
    order by qty desc
""")

    List<Object[]> topBooksBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ======= Tìm kiếm trang quản trị đơn  =======
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN o.user u
        WHERE (:kw IS NULL OR :kw = '' OR
               lower(o.orderNumber) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(u.fullName, '')) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(u.username, '')) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(o.customerPhone, '')) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(o.shippingAddress, '')) LIKE lower(concat('%', :kw, '%')))
          AND (:st IS NULL OR :st = '' OR o.status = :st)
    """)
    Page<Order> search(@Param("kw") String kw,
                       @Param("st") String status,
                       Pageable pageable);

    // ======= Tìm kiếm KHÔNG PHÂN TRANG (trả về List) + hỗ trợ Sort =======
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN o.user u
        WHERE (:kw IS NULL OR :kw = '' OR
               lower(o.orderNumber) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(u.fullName, '')) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(u.username, '')) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(o.customerPhone, '')) LIKE lower(concat('%', :kw, '%')) OR
               lower(coalesce(o.shippingAddress, '')) LIKE lower(concat('%', :kw, '%')))
          AND (:st IS NULL OR :st = '' OR o.status = :st)
    """)
    List<Order> search(@Param("kw") String kw,
                       @Param("st") String status,
                       Sort sort);

    @Query("""
        select o.status as status, count(o) as cnt
        from Order o
        group by o.status
    """)
    List<Object[]> countByStatus();
}
