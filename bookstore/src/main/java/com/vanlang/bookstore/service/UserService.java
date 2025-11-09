package com.vanlang.bookstore.service;

import com.vanlang.bookstore.model.User;
import com.vanlang.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // lấy user bằng email HOẶC username
    public Optional<User> findByLogin(String login) {
        if (login == null || login.isBlank()) return Optional.empty();
        return userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login));
    }

    @Transactional
    public User registerUser(User user) {
        if (existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (user.getRole() == null) user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            String base = user.getEmail().split("@")[0];
            user.setUsername(base);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(String login, User form, MultipartFile avatarFile) {
        User u = findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        // đổi username (chống trùng)
        if (form.getUsername() != null && !form.getUsername().isBlank()) {
            String newUsername = form.getUsername().trim();
            if (!newUsername.equals(u.getUsername())) {
                userRepository.findByUsername(newUsername).ifPresent(other -> {
                    if (!other.getId().equals(u.getId())) {
                        throw new IllegalArgumentException("Username đã tồn tại");
                    }
                });
            }
            u.setUsername(newUsername);
        }

        // đổi email (chống trùng)
        if (form.getEmail() != null && !form.getEmail().isBlank()) {
            String newEmail = form.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(u.getEmail())) {
                userRepository.findByEmail(newEmail).ifPresent(other -> {
                    if (!other.getId().equals(u.getId())) {
                        throw new IllegalArgumentException("Email đã tồn tại");
                    }
                });
            }
            u.setEmail(newEmail);
        }

        // các field khác
        u.setFullName(trimOrNull(form.getFullName()));
        u.setPhone(trimOrNull(form.getPhone()));
        u.setAddress(trimOrNull(form.getAddress()));

        // upload avatar
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String original = avatarFile.getOriginalFilename();
            String ext = getExtensionSafe(original);
            String fileName = "avatar-" + u.getId() + ext;

            Path uploadDir = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
            Path dest = uploadDir.resolve(fileName);

            try {
                Files.createDirectories(uploadDir);
                Files.copy(avatarFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                u.setAvatarUrl("/uploads/avatars/" + fileName);
            } catch (Exception e) {
                throw new RuntimeException("Upload avatar lỗi: " + e.getMessage(), e);
            }
        } else if (form.getAvatarUrl() != null && !form.getAvatarUrl().isBlank()) {
            u.setAvatarUrl(form.getAvatarUrl().trim());
        }

        return userRepository.save(u);
    }

    private String sanitize(String name) {
        if (name == null) return "noname";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getExtensionSafe(String originalName) {
        String safe = sanitize(originalName);
        int dot = safe.lastIndexOf('.');
        if (dot >= 0 && dot < safe.length() - 1) {
            return safe.substring(dot);
        }
        return ".png";
    }

    private String trimOrNull(String s) {
        return (s == null) ? null : s.trim();
    }

    @Transactional
    public void changePassword(String login, String currentPassword, String newPassword) {
        User user = findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateDefaultShipping(String login,
                                      String fullName,
                                      String phone,
                                      String address) {
        User u = findByLogin(login).orElseThrow();
        u.setShippingFullName(fullName);
        u.setShippingPhone(phone);
        u.setShippingAddress(address);
        userRepository.save(u);
    }
}
