package com.vanlang.bookstore.repository;

import com.vanlang.bookstore.model.Book;
import com.vanlang.bookstore.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // Giữ nguyên các hàm cũ
    Page<Book> findByCategory(Category category, Pageable pageable);
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    List<Book> findByFeaturedTrue();
    List<Book> findByNewArrivalTrue();
    List<Book> findByBestSellerTrue();
    Page<Book> findByCategoryAndTitleContainingIgnoreCase(Category category, String title, Pageable pageable);

    // Bổ sung bản paging cho 3 nhóm
    Page<Book> findByFeaturedTrue(Pageable pageable);
    Page<Book> findByNewArrivalTrue(Pageable pageable);
    Page<Book> findByBestSellerTrue(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.stock > 0 ORDER BY b.createdAt DESC")
    List<Book> findLatestBooks(Pageable pageable);

    // Các hàm LIST + SORT nếu nơi khác còn dùng
    List<Book> findAll(Sort sort);
    List<Book> findByTitleContainingIgnoreCase(String title, Sort sort);
    List<Book> findByCategoryId(Long categoryId, Sort sort);
    List<Book> findByTitleContainingIgnoreCaseAndCategoryId(String title, Long categoryId, Sort sort);
}
