package com.notesapp.backend.service;

import com.notesapp.backend.dto.auth.AuthResponse;
import com.notesapp.backend.dto.auth.LoginRequest;
import com.notesapp.backend.dto.auth.RegisterRequest;
import com.notesapp.backend.dto.user.UserResponse;
import com.notesapp.backend.entity.Category;
import com.notesapp.backend.entity.User;
import com.notesapp.backend.exception.BadRequestException;
import com.notesapp.backend.exception.DuplicateEmailException;
import com.notesapp.backend.repository.CategoryRepository;
import com.notesapp.backend.repository.UserRepository;
import com.notesapp.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final List<String> DEFAULT_CATEGORIES = List.of("Personal", "Study", "Work", "Ideas");

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);

        // Every new account starts with the same 4 default categories,
        // matching what the frontend's original mock data assumed.
        for (String name : DEFAULT_CATEGORIES) {
            Category category = Category.builder()
                    .name(name)
                    .user(saved)
                    .build();
            categoryRepository.save(category);
        }

        return toUserResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, toUserResponse(user));
    }

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
