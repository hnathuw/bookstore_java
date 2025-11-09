package com.vanlang.bookstore.controller.admin;

import com.vanlang.bookstore.model.Book;
import com.vanlang.bookstore.repository.BookRepository;
import com.vanlang.bookstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;

@Controller
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    // ================= LIST + SEARCH + FILTER (KHÔNG PHÂN TRANG) =================
    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId) {

        Sort sort = Sort.by(Sort.Direction.DESC, "id"); // mới thêm nằm trên cùng
        List<Book> books;

        if (keyword != null && !keyword.isBlank() && categoryId != null) {
            books = bookRepository.findByTitleContainingIgnoreCaseAndCategoryId(keyword.trim(), categoryId, sort);
        } else if (keyword != null && !keyword.isBlank()) {
            books = bookRepository.findByTitleContainingIgnoreCase(keyword.trim(), sort);
        } else if (categoryId != null) {
            books = bookRepository.findByCategoryId(categoryId, sort);
        } else {
            books = bookRepository.findAll(sort);
        }

        model.addAttribute("books", books);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("title", "Sách");
        return "admin/books/list";
    }

    // ================= ADD =================
    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("title", "Thêm sách");
        return "admin/books/form";
    }

    // ================= EDIT =================
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách id=" + id));
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("title", "Sửa sách");
        return "admin/books/form";
    }

    // ================= SAVE (ADD / EDIT) =================
    @PostMapping("/save")
    public String save(@ModelAttribute("book") Book book,
                       BindingResult bindingResult,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            return "admin/books/form";
        }

        if (book.getPrice() == null) book.setPrice(BigDecimal.ZERO);
        if (book.getStock() == null) book.setStock(0);
        if (book.getEnabled() == null) book.setEnabled(true);

        // Lưu lần 1 để có ID
        book = bookRepository.save(book);

        // Nếu upload ảnh mới
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String original = imageFile.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : ".jpg";
                Path uploadRoot = Path.of("uploads", "books").toAbsolutePath().normalize();
                Files.createDirectories(uploadRoot);
                Path dest = uploadRoot.resolve(book.getId() + ext);
                Files.copy(imageFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                book.setImageUrl("/uploads/books/" + book.getId() + ext);
                bookRepository.save(book);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ra.addFlashAttribute("success", "Lưu sách thành công!");
        return "redirect:/admin/books";
    }

    // ================= DELETE (POST + CSRF) =================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookRepository.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa sách!");
        } catch (Exception e) {
            bookRepository.findById(id).ifPresent(b -> { b.setEnabled(false); bookRepository.save(b); });
            ra.addFlashAttribute("warning", "Sách đang được tham chiếu, đã đặt trạng thái INACTIVE.");
        }
        return "redirect:/admin/books";
    }
}
