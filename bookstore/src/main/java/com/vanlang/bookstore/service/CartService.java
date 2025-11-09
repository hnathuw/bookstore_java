// src/main/java/com/vanlang/bookstore/service/CartService.java
package com.vanlang.bookstore.service;

import com.vanlang.bookstore.model.Book;
import com.vanlang.bookstore.model.CartItem; // <-- dùng CartItem riêng
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SessionScope
public class CartService implements Serializable {

    private final List<CartItem> items = new ArrayList<>();

    public void addItem(Book book, int quantity) {
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getBook() != null && i.getBook().getId().equals(book.getId()))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            items.add(new CartItem(book, quantity));
        }
    }

    public void updateQuantity(Long bookId, int quantity) {
        items.stream()
                .filter(i -> i.getBook()!=null && i.getBook().getId().equals(bookId))
                .findFirst()
                .ifPresent(i -> {
                    if (quantity <= 0) items.remove(i);
                    else i.setQuantity(quantity);
                });
    }

    public void removeItem(Long bookId) {
        items.removeIf(i -> i.getBook()!=null && i.getBook().getId().equals(bookId));
    }

    /** Xoá các món có id đã thanh toán */
    public void removeByBookIds(Set<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return;
        items.removeIf(i -> i.getBook()!=null && bookIds.contains(i.getBook().getId()));
    }

    /** Bọc tiện dụng cho controller (nhận List) */
    public void removeItemsByBookIds(List<Long> bookIds) {
        if (bookIds == null) return;
        removeByBookIds(new HashSet<>(bookIds));
    }

    public void clear() { items.clear(); }

    public List<CartItem> getItems() { return new ArrayList<>(items); }

    public int getTotalItems() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() { return items.isEmpty(); }

    // ====== Helpers cho flow “tick chọn ở giỏ” ======
    public List<CartItem> getItemsByBookIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return List.of();
        Set<Long> set = new HashSet<>(bookIds);
        return items.stream()
                .filter(i -> i.getBook()!=null && set.contains(i.getBook().getId()))
                .collect(Collectors.toList());
    }

    public BigDecimal calculateSubtotal(List<CartItem> selected) {
        if (selected == null) return BigDecimal.ZERO;
        return selected.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Tuỳ bài: hiện cho 0 để đơn giản */
    public BigDecimal calculateShipping(List<CartItem> selected) {
        return BigDecimal.ZERO;
    }

    // Giữ API cũ mà vài nơi đang gọi
    public int getCartItemCount(String ignoredUsername) { return getTotalItems(); }
    public int getCartItemCount() { return getTotalItems(); }
}
