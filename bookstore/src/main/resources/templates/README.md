# 🎀 BOOKSTORE COQUETTE - BỘ TEMPLATES HOÀN CHỈNH

## 📦 **TỔNG CỘNG: 26 FILES**

### ✨ **CSS** (1 file)
- `coquette-style.css` - CSS đồng bộ cho toàn bộ project

### 🛍️ **FRONTEND** (13 files)
1. `index.html` - Trang chủ với hero banner
2. `books.html` - Danh sách sách
3. `book-detail.html` - Chi tiết sản phẩm ⭐ MỚI
4. `cart.html` - Giỏ hàng
5. `checkout.html` - Thanh toán ⭐ MỚI
6. `login.html` - Đăng nhập
7. `register.html` - Đăng ký
8. `forgot-password.html` - Quên mật khẩu ⭐ MỚI
9. `orders.html` - Danh sách đơn hàng ⭐ MỚI
10. `order-detail.html` - Chi tiết đơn hàng ⭐ MỚI
11. `order-success.html` - Thành công ⭐ MỚI
12. `profile.html` - Thông tin cá nhân ⭐ MỚI
13. `search-results.html` - Kết quả tìm kiếm ⭐ MỚI

### 📄 **CHÍNH SÁCH** (3 files)
1. `policy/return.html` - Đổi trả (nội dung đầy đủ)
2. `policy/privacy.html` - Bảo mật (nội dung đầy đủ)
3. `policy/terms.html` - Điều khoản (nội dung đầy đủ)

### 👑 **ADMIN** (9 files)
1. `admin/dashboard.html` - Dashboard
2. `admin/books/list.html` - Danh sách sách ⭐ MỚI
3. `admin/books/form.html` - Form thêm/sửa sách ⭐ MỚI
4. `admin/categories/list.html` - Danh sách danh mục ⭐ MỚI
5. `admin/categories/form.html` - Form danh mục ⭐ MỚI
6. `admin/orders/list.html` - Danh sách đơn hàng
7. `admin/orders/detail.html` - Chi tiết đơn hàng ⭐ MỚI
8. `admin/users/list.html` - Danh sách người dùng ⭐ MỚI
9. `admin/users/form.html` - Form người dùng ⭐ MỚI

---

## ⚡ CÀI ĐẶT NHANH

```bash
# 1. Copy CSS
cp coquette-style.css src/main/resources/static/css/

# 2. Copy templates
cp *.html src/main/resources/templates/
cp -r policy/ src/main/resources/templates/
cp -r admin/ src/main/resources/templates/

# 3. Tạo PolicyController (xem bên dưới)

# 4. Run
mvn spring-boot:run
```

---

## 📝 **POLICYT CONTROLLER**

```java
package com.vanlang.bookstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policy")
public class PolicyController {
    
    @GetMapping("/return")
    public String returnPolicy() {
        return "policy/return";
    }
    
    @GetMapping("/privacy")
    public String privacyPolicy() {
        return "policy/privacy";
    }
    
    @GetMapping("/terms")
    public String termsPolicy() {
        return "policy/terms";
    }
}
```

---

## ✅ **ĐẶC ĐIỂM**

- 🎨 Màu sắc coquette đồng bộ 100%
- 📱 Responsive hoàn toàn
- ✨ Hover effects mượt mà
- 🎯 Gradient backgrounds
- 💼 Admin đồng bộ với frontend
- 📄 3 trang chính sách đầy đủ nội dung
- 🔗 Khớp với backend hiện tại

---

## 🎨 **MÀU SẮC**

- **Pink Baby**: #FFB6C1
- **Blue Baby**: #A8D5E2
- **White**: #FFFFFF
- **Soft Pink**: #FFE4E9
- **Soft Blue**: #E0F4F9

---

**Made with 💗 - Coquette Theme**
**Version: 2.0.0 - Complete Edition**
