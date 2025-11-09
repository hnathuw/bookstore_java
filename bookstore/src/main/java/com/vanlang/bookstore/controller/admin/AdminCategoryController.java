package com.vanlang.bookstore.controller.admin;

import com.vanlang.bookstore.model.Category;
import com.vanlang.bookstore.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", service.findAll());
        model.addAttribute("title", "Danh mục");
        return "admin/categories/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("title", "Thêm danh mục");
        return "admin/categories/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", service.get(id));
        model.addAttribute("title", "Sửa danh mục");
        return "admin/categories/form";
    }

    @PostMapping("/save") // ✅ form.html post tới /save
    public String save(@ModelAttribute("category") Category form, RedirectAttributes ra) {
        try {
            if (form.getId() == null) {
                service.create(form);
                ra.addFlashAttribute("success", "Tạo danh mục thành công");
            } else {
                service.update(form.getId(), form);
                ra.addFlashAttribute("success", "Cập nhật danh mục thành công");
            }
            return "redirect:/admin/categories";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            // quay lại đúng form đang nhập
            return (form.getId() == null) ? "redirect:/admin/categories/add"
                    : "redirect:/admin/categories/edit/" + form.getId();
        }
    }

    @GetMapping("/delete/{id}") // khớp nút delete trong list.html
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("success", "Đã xóa danh mục");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
