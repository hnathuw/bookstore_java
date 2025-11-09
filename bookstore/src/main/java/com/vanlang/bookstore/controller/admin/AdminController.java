package com.vanlang.bookstore.controller.admin;

import com.vanlang.bookstore.repository.OrderRepository;
import com.vanlang.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String dashboard(
            Model model,
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        // ====== Chuẩn hoá input ngày ======
        LocalDate today = LocalDate.now();
        if (start == null && end == null) {
            // mặc định: đầu tháng đến hôm nay
            start = today.withDayOfMonth(1);
            end   = today;
        } else if (start == null) {
            start = end;
        } else if (end == null) {
            end = start;
        }

        boolean invalidRange = start.isAfter(end);
        if (invalidRange) {
            model.addAttribute("dateError", "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
        }

        // ====== KPI cố định (không phụ thuộc range) ======
        Long revenueToday = orderRepository.sumRevenueBetweenLong(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        long totalOrders = orderRepository.count();
        long totalUsers  = userRepository.count();
        var recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc();

        // ====== Dữ liệu phụ thuộc range ======
        List<String> labels = new ArrayList<>();
        List<Long> values   = new ArrayList<>();
        List<String> topBookTitles = new ArrayList<>();
        List<Long>   topBookQty    = new ArrayList<>();
        Long revenueRange = 0L;

        if (!invalidRange) {
            LocalDateTime from = start.atStartOfDay();
            LocalDateTime toExclusive = end.plusDays(1).atStartOfDay();

            // revenue trong khoảng
            revenueRange = orderRepository.sumRevenueBetweenLong(from, toExclusive);
            if (revenueRange == null) revenueRange = 0L;

            // chart theo ngày
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                LocalDateTime dStart = cursor.atStartOfDay();
                LocalDateTime dEnd   = cursor.plusDays(1).atStartOfDay();
                Long rev = orderRepository.sumRevenueBetweenLong(dStart, dEnd);
                labels.add(cursor.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")));
                values.add(rev == null ? 0L : rev);
                cursor = cursor.plusDays(1);
            }

            // top sách bán chạy trong khoảng
            var topRows = orderRepository.topBooksBetween(from, toExclusive);
            for (int i = 0; i < Math.min(10, topRows.size()); i++) {
                Object[] row = topRows.get(i);
                topBookTitles.add(Objects.toString(row[0], ""));
                topBookQty.add(row[1] == null ? 0L : ((Number) row[1]).longValue());
            }
        }

        // ====== Đưa lên model ======
        model.addAttribute("start", start);
        model.addAttribute("end", end);

        model.addAttribute("revenueToday", revenueToday == null ? 0L : revenueToday);
        model.addAttribute("revenueMonth", revenueRange == null ? 0L : revenueRange); // “trong khoảng”

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalUsers", totalUsers);

        model.addAttribute("recentOrders", recentOrders);

        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartValues", values);

        model.addAttribute("topBookTitles", topBookTitles);
        model.addAttribute("topBookQty", topBookQty);

        return "admin/dashboard";
    }
}
