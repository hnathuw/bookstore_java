package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.model.Book;
import com.vanlang.bookstore.model.Category;
import com.vanlang.bookstore.repository.CategoryRepository;
import com.vanlang.bookstore.service.BookService;
import com.vanlang.bookstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final CartService cartService;

    // ===== Helper: sort an toàn =====
    private Sort buildSort(String sort) {
        return switch (sort) {
            case "priceAsc"  -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "title"     -> Sort.by("title").ascending();
            default          -> Sort.by("id").descending(); // an toàn, luôn có
        };
    }

    // ===== Helper: nếu page vượt tổng trang -> điều hướng về trang cuối =====
    private String redirectIfOutOfRange(Page<?> pageObj, String basePath, int size, String sort) {
        if (pageObj.getTotalPages() > 0 && pageObj.getNumber() >= pageObj.getTotalPages()) {
            return basePath + "?page=" + (pageObj.getTotalPages() - 1) + "&size=" + size + "&sort=" + sort;
        }
        return null;
    }

    // TẤT CẢ SÁCH (có phân trang)
    @GetMapping
    public String listBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            Model model,
            RedirectAttributes ra) {

        Sort sortSpec = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        Page<Book> bookPage;

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category == null) {
                ra.addFlashAttribute("error", "Danh mục không tồn tại hoặc đã bị xóa.");
                return "redirect:/books";
            }
            bookPage = bookService.getBooksByCategory(category, page, size, sortSpec);
            model.addAttribute("selectedCategory", category);
        } else {
            bookPage = bookService.getBooks(page, size, sortSpec);
        }

        String redirect = redirectIfOutOfRange(bookPage, "/books", size, sort);
        if (redirect != null) return "redirect:" + redirect;

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pageSizes", new int[]{6, 12, 24, 48});
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "books";
    }

    // CHI TIẾT SÁCH
    @GetMapping("/{id}")
    public String bookDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Book book = bookService.getBookById(id).orElse(null);
        if (book == null) {
            ra.addFlashAttribute("error", "Không tìm thấy sách!");
            return "redirect:/books";
        }

        List<Book> relatedBooks = bookService.getBooksByCategory(book.getCategory(), 0, 4).getContent();

        model.addAttribute("book", book);
        model.addAttribute("relatedBooks", relatedBooks);
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "book-detail";
    }

    // TÌM KIẾM
    @GetMapping("/search")
    public String searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        Page<Book> searchResults = bookService.searchBooks(keyword, page, size);

        // nếu vượt tổng trang -> về trang cuối
        if (searchResults.getTotalPages() > 0 && page >= searchResults.getTotalPages()) {
            return "redirect:/books/search?keyword=" + keyword
                    + "&page=" + (searchResults.getTotalPages() - 1)
                    + "&size=" + size;
        }

        model.addAttribute("bookPage", searchResults);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", searchResults.getTotalPages());
        model.addAttribute("currentSort", "latest");
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("pageSizes", new int[]{6, 12, 24, 48});
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "search-results";
    }

    // DANH MỤC (slug) - phân trang + sort + size
    @GetMapping("/c/{slug}")
    public String booksByCategorySlug(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sort,
            Model model,
            RedirectAttributes ra) {

        Category category = categoryRepository.findBySlug(slug).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return "redirect:/books";
        }

        Sort sortSpec = buildSort(sort);
        Page<Book> bookPage = bookService.getBooksByCategory(category, page, size, sortSpec);

        if (bookPage.getTotalPages() > 0 && page >= bookPage.getTotalPages()) {
            return "redirect:/books/c/" + slug
                    + "?page=" + (bookPage.getTotalPages() - 1)
                    + "&size=" + size
                    + "&sort=" + sort;
        }

        model.addAttribute("selectedCategory", category);
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("pageSizes", new int[]{6, 12, 24, 48});
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "books";
    }

    // Redirect URL cũ -> URL slug
    @GetMapping("/category/{categoryId}")
    public String redirectOldCategory(@PathVariable Long categoryId) {
        Category cat = categoryRepository.findById(categoryId).orElse(null);
        if (cat == null) return "redirect:/books";
        return "redirect:/books/c/" + cat.getSlug();
    }

    // ==================== ADMIN ====================
    @GetMapping("/admin/manage")
    public String manageBooks(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Book> bookPage = bookService.getBooks(page, 20);
        if (bookPage.getTotalPages() > 0 && page >= bookPage.getTotalPages()) {
            return "redirect:/books/admin/manage?page=" + (bookPage.getTotalPages() - 1);
        }
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        return "admin/manage-books";
    }

    @GetMapping("/admin/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/add-book";
    }

    @PostMapping("/admin/add")
    public String addBook(@Valid @ModelAttribute("book") Book book,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại dữ liệu.");
            return "redirect:/books/admin/add";
        }
        try {
            bookService.saveBook(book);
            redirectAttributes.addFlashAttribute("success", "Thêm sách thành công!");
            return "redirect:/books/admin/manage";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Thêm sách thất bại: " + e.getMessage());
            return "redirect:/books/admin/add";
        }
    }

    @GetMapping("/admin/edit/{id}")
    public String showEditBookForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Book book = bookService.getBookById(id).orElse(null);
        if (book == null) {
            ra.addFlashAttribute("error", "Không tìm thấy sách!");
            return "redirect:/books/admin/manage";
        }
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/edit-book";
    }

    @PostMapping("/admin/edit/{id}")
    public String updateBook(@PathVariable Long id,
                             @Valid @ModelAttribute("book") Book book,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại dữ liệu.");
            return "redirect:/books/admin/edit/" + id;
        }
        try {
            book.setId(id);
            bookService.saveBook(book);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sách thành công!");
            return "redirect:/books/admin/manage";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cập nhật sách thất bại: " + e.getMessage());
            return "redirect:/books/admin/edit/" + id;
        }
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Xóa sách thất bại: " + e.getMessage());
        }
        return "redirect:/books/admin/manage";
    }

    // NHÓM SÁCH THEO TAG: /books/tag/featured | /books/tag/new | /books/tag/best
    @GetMapping("/tag/{tag}")
    public String booksByTag(@PathVariable String tag,
                             @RequestParam(defaultValue = "12") int size,
                             Model model) {

        String title = switch (tag.toLowerCase()) {
            case "featured" -> "Sách nổi bật";
            case "new"      -> "Sách mới về";
            case "best"     -> "Bán chạy nhất";
            default         -> "Tất cả sách";
        };

        List<Book> books = switch (tag.toLowerCase()) {
            case "featured" -> bookService.getFeaturedBooks();
            case "new"      -> bookService.getNewArrivals();
            case "best"     -> bookService.getBestSellers();
            default         -> bookService.getLatestBooks(size);
        };

        books = books.stream().limit(size).toList();

        model.addAttribute("title", title);
        model.addAttribute("bookPage", null);   // dùng nhánh list
        model.addAttribute("books", books);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentSort", "latest");
        model.addAttribute("pageSizes", new int[]{6, 12, 24, 48});
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "books";
    }
}
