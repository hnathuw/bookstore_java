package com.vanlang.bookstore.repository;

import com.vanlang.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;   // 👈 thêm import này
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username); // dùng cho đăng ký tránh trùng

    // Login bằng email HOẶC username (không phân biệt hoa/thường)
    @Query("""
        SELECT u FROM User u
        WHERE lower(u.email) = lower(:login)
           OR lower(u.username) = lower(:login)
    """)
    Optional<User> findByEmailOrUsername(@Param("login") String login);
}
