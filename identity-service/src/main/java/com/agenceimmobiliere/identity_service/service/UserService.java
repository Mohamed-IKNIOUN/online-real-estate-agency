package com.agenceimmobiliere.identity_service.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenceimmobiliere.identity_service.dto.RegisterRequest;
import com.agenceimmobiliere.identity_service.dto.RegisterResponse;
import com.agenceimmobiliere.identity_service.entity.User;
import com.agenceimmobiliere.identity_service.enums.Role;
import com.agenceimmobiliere.identity_service.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // 1. Check whether the email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // 2. Prevent public ADMIN registration
        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException(
                    "You cannot register as an administrator");
        }

        // 3. Create the user
        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        // 4. Save the user
        User savedUser = userRepository.save(user);

        // 5. Return a safe response
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }
}