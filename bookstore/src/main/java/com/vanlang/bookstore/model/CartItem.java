package com.vanlang.bookstore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {

    private Book book;
    private int quantity;

    public BigDecimal getSubtotal() {
        BigDecimal price = book.getDiscountPrice() != null ?
                book.getDiscountPrice() : book.getPrice();
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getUnitPrice() {
        return book.getDiscountPrice() != null ?
                book.getDiscountPrice() : book.getPrice();
    }
}