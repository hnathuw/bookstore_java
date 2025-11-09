package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.model.Book;
import com.vanlang.bookstore.service.BookService;
import com.vanlang.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BookService bookService;
    private final CartService cartService;

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        model.addAttribute("title", "Trang Chủ");

        // Lấy dữ liệu, null-safe
        List<Book> featured = safe(bookService.getFeaturedBooks());
        List<Book> newArr   = safe(bookService.getNewArrivals());
        List<Book> best     = safe(bookService.getBestSellers());

        // Fallback nếu rỗng
        if (featured.isEmpty()) featured = safe(bookService.getLatestBooks(8));
        if (newArr.isEmpty())   newArr   = safe(bookService.getLatestBooks(8));
        if (best.isEmpty())     best     = safe(bookService.getLatestBooks(8));

        // Giới hạn top 8
        model.addAttribute("featuredBooks", limit8(featured));
        model.addAttribute("newArrivals",   limit8(newArr));
        model.addAttribute("bestSellers",   limit8(best));

        String username = (principal != null) ? principal.getName() : null;
        model.addAttribute("cartItemCount", cartService.getCartItemCount(username));

        return "index";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            Principal principal
    ) {
        model.addAttribute("title", "Tìm Kiếm: " + keyword);
        model.addAttribute("keyword", keyword);

        var books = bookService.searchBooks(keyword, page, 12);
        model.addAttribute("books", books);
        model.addAttribute("currentPage", page);

        String username = (principal != null) ? principal.getName() : null;
        model.addAttribute("cartItemCount", cartService.getCartItemCount(username));

        return "search-results";
    }

    // --- helpers ---
    private static List<Book> safe(List<Book> src) {
        return (src == null) ? Collections.emptyList() : src;
    }
    private static List<Book> limit8(List<Book> src) {
        return src.size() <= 8 ? src : src.subList(0, 8);
    }
}
