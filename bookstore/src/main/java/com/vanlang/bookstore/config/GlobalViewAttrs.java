package com.vanlang.bookstore.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttrs {
    @ModelAttribute("activePath")
    public String activePath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}

