// src/main/java/com/vanlang/bookstore/service/CustomUserDetailsService.java
package com.vanlang.bookstore.service;

import com.vanlang.bookstore.model.User;
import com.vanlang.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Cho phép đăng nhập bằng email hoặc username
        User user = userRepository.findByEmailOrUsername(login)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại: " + login));

        // Nếu username null, fallback về email để tránh NPE
        String principal = (user.getUsername() != null && !user.getUsername().isBlank())
                ? user.getUsername()
                : user.getEmail();

        // Boolean -> boolean an toàn (null coi như true để không khoá nhầm)
        boolean enabled = Boolean.TRUE.equals(user.getEnabled());

        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new org.springframework.security.core.userdetails.User(
                principal,               // username dùng để đăng nhập
                user.getPassword(),      // đã mã hoá BCrypt
                enabled,                 // enabled
                true,                    // accountNonExpired
                true,                    // credentialsNonExpired
                true,                    // accountNonLocked
                authorities
        );
    }
}
