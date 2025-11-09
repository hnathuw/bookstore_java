package com.vanlang.bookstore.service;

import com.vanlang.bookstore.model.Order;
import com.vanlang.bookstore.repository.OrderItemRepository;
import com.vanlang.bookstore.repository.OrderRepository;
import com.vanlang.bookstore.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EntityManager em;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    /* ======== DTOs ======== */
    public record Summary(long totalOrders, long totalCustomers, long totalRevenue,
                          double avgOrderValue) {}
    public record DailyPoint(LocalDate day, long revenue) {}
    public record Monthly(String ym, long revenue) {}
    public record StatusCount(String status, long count) {}
    public record TopProduct(Long bookId, String title, long qty, long revenue) {}
    public record RecentOrder(Long id, String orderNumber, String customerName,
                              String status, long total, LocalDateTime createdAt) {}

    /* ================= Tổng hợp ================= */
    public Summary summary(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // inclusive

        // Tổng số đơn (mọi trạng thái) để nhìn volume
        Long totalOrders = em.createQuery(
                        "select count(o) from Order o where o.createdAt >= :s and o.createdAt < :e", Long.class)
                .setParameter("s", start).setParameter("e", end)
                .getSingleResult();

        // Doanh thu chỉ tính đơn KHÔNG bị hủy
        Long totalRevenue = em.createQuery(
                        "select coalesce(sum(o.total),0) from Order o " +
                                "where o.createdAt >= :s and o.createdAt < :e and o.status <> 'CANCELED'",
                        Long.class)
                .setParameter("s", start).setParameter("e", end)
                .getSingleResult();

        // Số đơn KHÔNG bị hủy để tính AOV sát nghĩa
        Long nonCanceledOrders = em.createQuery(
                        "select count(o) from Order o " +
                                "where o.createdAt >= :s and o.createdAt < :e and o.status <> 'CANCELED'",
                        Long.class)
                .setParameter("s", start).setParameter("e", end)
                .getSingleResult();

        Long totalCustomers = em.createQuery(
                        "select count(distinct o.user.id) from Order o " +
                                "where o.user is not null and o.createdAt >= :s and o.createdAt < :e",
                        Long.class)
                .setParameter("s", start).setParameter("e", end)
                .getSingleResult();

        long rev = (totalRevenue == null ? 0L : totalRevenue);
        long nonCancel = (nonCanceledOrders == null ? 0L : nonCanceledOrders);
        double aov = (nonCancel > 0) ? Math.round((double) rev / nonCancel) : 0.0;

        return new Summary(
                totalOrders == null ? 0 : totalOrders,
                totalCustomers == null ? 0 : totalCustomers,
                rev,
                aov
        );
    }

    /** Doanh thu theo ngày trong khoảng [from, to] (loại CANCELED) */
    public List<DailyPoint> salesByDay(LocalDate from, LocalDate to) {
        String sql = """
            SELECT DATE(o.created_at) AS d, COALESCE(SUM(o.total_amount),0) AS rev
            FROM orders o
            WHERE o.created_at >= :s AND o.created_at < :e
              AND o.status <> 'CANCELED'
            GROUP BY DATE(o.created_at)
            ORDER BY d
        """;
        var q = em.createNativeQuery(sql)
                .setParameter("s", from.atStartOfDay())
                .setParameter("e", to.plusDays(1).atStartOfDay());
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream()
                .map(r -> new DailyPoint(((java.sql.Date) r[0]).toLocalDate(),
                        ((Number) r[1]).longValue()))
                .toList();
    }

    /** Doanh thu theo tháng (yyyy-MM) (loại CANCELED) */
    public List<Monthly> monthly(LocalDate from, LocalDate to) {
        String sql = """
            SELECT DATE_FORMAT(o.created_at, '%Y-%m') AS ym, COALESCE(SUM(o.total_amount),0) AS rev
            FROM orders o
            WHERE o.created_at >= :s AND o.created_at < :e
              AND o.status <> 'CANCELED'
            GROUP BY DATE_FORMAT(o.created_at, '%Y-%m')
            ORDER BY ym
        """;
        var q = em.createNativeQuery(sql)
                .setParameter("s", from.withDayOfMonth(1).atStartOfDay())
                .setParameter("e", to.plusMonths(1).withDayOfMonth(1).atStartOfDay());
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<Monthly> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new Monthly((String) r[0], ((Number) r[1]).longValue()));
        }
        return out;
    }

    /** Đếm đơn theo trạng thái (giữ nguyên, không loại CANCELED) */
    public List<StatusCount> statusCounts(LocalDate from, LocalDate to) {
        String sql = """
            SELECT o.status, COUNT(*) AS cnt
            FROM orders o
            WHERE o.created_at >= :s AND o.created_at < :e
            GROUP BY o.status
        """;
        var q = em.createNativeQuery(sql)
                .setParameter("s", from.atStartOfDay())
                .setParameter("e", to.plusDays(1).atStartOfDay());
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream()
                .map(r -> new StatusCount((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    /** Top sản phẩm theo số lượng bán (loại CANCELED) */
    public List<TopProduct> topProducts(LocalDate from, LocalDate to, int limit) {
        String sql = """
            SELECT oi.book_id, b.title,
                   SUM(oi.quantity) AS qty,
                   SUM(oi.quantity * oi.price) AS revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN books  b ON b.id = oi.book_id
            WHERE o.created_at >= :s AND o.created_at < :e
              AND o.status <> 'CANCELED'
            GROUP BY oi.book_id, b.title
            ORDER BY qty DESC
            LIMIT :lim
        """;
        var q = em.createNativeQuery(sql)
                .setParameter("s", from.atStartOfDay())
                .setParameter("e", to.plusDays(1).atStartOfDay())
                .setParameter("lim", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream()
                .map(r -> new TopProduct(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue(),
                        ((Number) r[3]).longValue()
                ))
                .toList();
    }

    /** 10 đơn gần đây (mọi trạng thái) */
    public List<RecentOrder> recentOrders(int limit) {
        TypedQuery<Order> q = em.createQuery(
                "select o from Order o order by o.createdAt desc", Order.class);
        q.setMaxResults(Math.max(1, limit));
        return q.getResultList().stream()
                .map(o -> new RecentOrder(
                        o.getId(), o.getOrderNumber(), o.getCustomerName(),
                        o.getStatus(), o.getTotal(), o.getCreatedAt()
                ))
                .toList();
    }

    /* Convenience wrappers */
    public Summary summaryLastNDays(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1L));
        return summary(from, to);
    }
    public List<DailyPoint> salesLastNDays(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1L));
        return salesByDay(from, to);
    }
    public List<TopProduct> topProductsLastNDays(int days, int limit) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1L));
        return topProducts(from, to, limit);
    }
    public List<StatusCount> statusLastNDays(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1L));
        return statusCounts(from, to);
    }
}
