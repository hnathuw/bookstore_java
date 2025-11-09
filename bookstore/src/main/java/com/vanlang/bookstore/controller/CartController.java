package com.vanlang.bookstore.controller;

import com.vanlang.bookstore.service.BookService;
import com.vanlang.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final BookService bookService;

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        model.addAttribute("cartItemCount", cartService.getTotalItems());
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes) {

        var book = bookService.getBookById(bookId);
        if (book.isPresent()) {
            if (book.get().getStock() >= quantity) {
                cartService.addItem(book.get(), quantity);
                redirectAttributes.addFlashAttribute("success",
                        "Đã thêm sách vào giỏ hàng!");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Không đủ số lượng trong kho!");
            }
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Không tìm thấy sách!");
        }

        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateCart(
            @RequestParam Long bookId,
            @RequestParam int quantity,
            RedirectAttributes redirectAttributes) {

        var book = bookService.getBookById(bookId);
        if (book.isPresent() && book.get().getStock() >= quantity) {
            cartService.updateQuantity(bookId, quantity);
            redirectAttributes.addFlashAttribute("success",
                    "Đã cập nhật giỏ hàng!");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Không đủ số lượng trong kho!");
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam Long bookId,
            RedirectAttributes redirectAttributes) {

        cartService.removeItem(bookId);
        redirectAttributes.addFlashAttribute("success",
                "Đã xóa sách khỏi giỏ hàng!");
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        cartService.clear();
        redirectAttributes.addFlashAttribute("success",
                "Đã xóa toàn bộ giỏ hàng!");
        return "redirect:/cart";
    }
}
