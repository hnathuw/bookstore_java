package com.vanlang.bookstore.service;

import com.vanlang.bookstore.model.Category;
import com.vanlang.bookstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repo;

    public List<Category> findAll() {
        return repo.findAll();
    }

    public Category get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
    }

    @Transactional
    public Category create(Category c) {
        // Chống trùng tên (case-insensitive) bằng findByName rồi so sánh bỏ qua hoa/thường
        repo.findByName(c.getName()).ifPresent(existing -> {
            if (existing.getName() != null &&
                    existing.getName().equalsIgnoreCase(c.getName())) {
                throw new IllegalArgumentException("Tên danh mục đã tồn tại");
            }
        });

        if (c.getSlug() == null || c.getSlug().isBlank()) {
            c.setSlug(makeSlug(c.getName()));
        }
        return repo.save(c);
    }

    @Transactional
    public Category update(Long id, Category form) {
        Category c = get(id);

        // Nếu đổi tên -> kiểm tra trùng
        if (form.getName() != null && !form.getName().equalsIgnoreCase(c.getName())) {
            repo.findByName(form.getName()).ifPresent(existing -> {
                // khác id thì mới coi là trùng
                if (!existing.getId().equals(id) &&
                        existing.getName().equalsIgnoreCase(form.getName())) {
                    throw new IllegalArgumentException("Tên danh mục đã tồn tại");
                }
            });
        }

        c.setName(form.getName());
        c.setDescription(form.getDescription());
        // Nếu không nhập slug thì auto từ name; nếu nhập thì dùng slug đã nhập (trim)
        c.setSlug((form.getSlug() == null || form.getSlug().isBlank())
                ? makeSlug(form.getName())
                : form.getSlug().trim());

        return repo.save(c);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private String makeSlug(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+","-")
                .replaceAll("(^-|-$)","");
        return n;
    }
}

