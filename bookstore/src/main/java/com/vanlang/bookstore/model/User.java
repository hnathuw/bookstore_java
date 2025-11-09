package com.vanlang.bookstore.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_username", columnNames = "username")
        })
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String fullName;

    @Column(length = 150, nullable = false)
    private String email;

    @Column(nullable = false, length = 200)
    private String password;

    @Column(length = 100)
    private String username;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role;

    // hết cảnh báo @Builder nhờ @Builder.Default
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    public enum Role { USER, ADMIN }
    public boolean getEnabled() { return enabled; }

    // ====== địa chỉ giao hàng mặc định ======
    @Column(length = 150)
    private String shippingFullName;

    @Column(length = 20)
    private String shippingPhone;

    @Column(length = 255)
    private String shippingAddress;

}
