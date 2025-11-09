package com.vanlang.bookstore.config;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    // Inject CSRF token vào model cho mọi view Thymeleaf
    @ModelAttribute("_csrf")
    public CsrfToken csrfToken(CsrfToken token) {
        return token;
    }
}
