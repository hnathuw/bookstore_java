// src/main/java/com/vanlang/bookstore/controller/CategoryController.java
package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.model.Book;
import com.vanlang.bookstore.model.Category;
import com.vanlang.bookstore.repository.BookRepository;
import com.vanlang.bookstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    @GetMapping("/{slugOrId}")
    public String byCategory(
            @PathVariable String slugOrId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "q", required = false) String keyword,
            Model model
    ) {
        // 1) Lấy danh mục theo slug hoặc id
        Category category = findCategory(slugOrId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục: " + slugOrId));

        // 2) Phân trang + sắp xếp
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // 3) Lấy sách theo danh mục (có/không có keyword)
        Page<Book> bookPage;
        if (keyword != null && !keyword.isBlank()) {
            bookPage = bookRepository.findByCategoryAndTitleContainingIgnoreCase(category, keyword.trim(), pageable);
        } else {
            bookPage = bookRepository.findByCategory(category, pageable);
        }

        // 4) Đổ dữ liệu ra view
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

        model.addAttribute("title", "Danh mục: " + category.getName());
        model.addAttribute("categories", categories);           // sidebar
        model.addAttribute("selectedCategory", category);       // để bôi đậm đang chọn
        model.addAttribute("bookPage", bookPage);               // danh sách sách
        model.addAttribute("keyword", (keyword != null) ? keyword.trim() : "");
        model.addAttribute("currentPath", "/categories/" + slugOrId); // để build link phân trang giữ ngữ cảnh

        return "books";
    }

    /**
     * Tìm category theo slug trước, nếu không có thì thử parse id dạng số.
     */
    private Optional<Category> findCategory(String slugOrId) {
        Optional<Category> bySlug = categoryRepository.findBySlug(slugOrId);
        if (bySlug.isPresent()) return bySlug;

        try {
            Long id = Long.valueOf(slugOrId);
            return categoryRepository.findById(id);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}

