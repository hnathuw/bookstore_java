package com.vanlang.bookstore.service;

import com.vanlang.bookstore.model.Book;
import com.vanlang.bookstore.model.Category;
import com.vanlang.bookstore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /** Mặc định sort theo createdAt desc*/
    public Page<Book> getBooks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return bookRepository.findAll(pageable);
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Page<Book> searchBooks(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    public Page<Book> getBooksByCategory(Category category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepository.findByCategory(category, pageable);
    }

    public List<Book> getFeaturedBooks() {
        return bookRepository.findByFeaturedTrue();
    }

    public List<Book> getNewArrivals() {
        return bookRepository.findByNewArrivalTrue();
    }

    public List<Book> getBestSellers() {
        return bookRepository.findByBestSellerTrue();
    }

    public List<Book> getLatestBooks(int limit) {
        // lùi về trang 0, sort theo createdAt desc nếu repo đã có query sắp sẵn
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findLatestBooks(pageable);
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    public boolean updateStock(Long bookId, int quantity) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            if (book.getStock() >= quantity) {
                book.setStock(book.getStock() - quantity);
                bookRepository.save(book);
                return true;
            }
        }
        return false;
    }

    /* ====== API mới: hỗ trợ Sort linh hoạt cho list / category / search ====== */

    /** Danh sách tất cả sách với Sort tuỳ biến */
    public Page<Book> getBooks(int page, int size, Sort sort) {
        // Nếu field sort không tồn tại (VD thiếu createdAt), có thể fallback sang id
        Sort safeSort = (sort == null) ? Sort.by("createdAt").descending() : sort;
        Pageable pageable = PageRequest.of(page, size, safeSort);
        return bookRepository.findAll(pageable);
    }

    /** Tìm theo danh mục + Sort */
    public Page<Book> getBooksByCategory(Category category, int page, int size, Sort sort) {
        Sort safeSort = (sort == null) ? Sort.by("createdAt").descending() : sort;
        Pageable pageable = PageRequest.of(page, size, safeSort);
        return bookRepository.findByCategory(category, pageable);
    }

    /** Search theo tiêu đề + Sort */
    public Page<Book> searchBooks(String keyword, int page, int size, Sort sort) {
        Sort safeSort = (sort == null) ? Sort.by("createdAt").descending() : sort;
        Pageable pageable = PageRequest.of(page, size, safeSort);
        return bookRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    // === paging cho featured/new/best ===
    public Page<Book> getFeaturedBooks(int page, int size, Sort sort) {
        Sort s = (sort == null) ? Sort.by("createdAt").descending() : sort;
        return bookRepository.findByFeaturedTrue(PageRequest.of(page, size, s));
    }

    public Page<Book> getNewArrivals(int page, int size, Sort sort) {
        Sort s = (sort == null) ? Sort.by("createdAt").descending() : sort;
        return bookRepository.findByNewArrivalTrue(PageRequest.of(page, size, s));
    }

    public Page<Book> getBestSellers(int page, int size, Sort sort) {
        Sort s = (sort == null) ? Sort.by("createdAt").descending() : sort;
        return bookRepository.findByBestSellerTrue(PageRequest.of(page, size, s));
    }

}
