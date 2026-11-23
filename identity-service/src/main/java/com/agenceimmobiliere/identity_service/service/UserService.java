package com.agenceimmobiliere.identity_service.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenceimmobiliere.identity_service.dto.RegisterRequest;
import com.agenceimmobiliere.identity_service.dto.RegisterResponse;
import com.agenceimmobiliere.identity_service.entity.User;
import com.agenceimmobiliere.identity_service.enums.Role;
import com.agenceimmobiliere.identity_service.repository.UserRepository;
import com.agenceimmobiliere.identity_service.security.JwtService;
import com.agenceimmobiliere.identity_service.dto.LoginRequest;
import com.agenceimmobiliere.identity_service.dto.LoginResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
    
    
    
    public LoginResponse login(LoginRequest request) {

        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or password"));

        // 2. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // 3. Check whether account is enabled
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }
        
        String accessToken = jwtService.generateToken(user);

        // 4. Return user information

		return new LoginResponse(
		        user.getId(),
		        user.getFullName(),
		        user.getEmail(),
		        user.getRole(),
		        accessToken
		);	
    }
    
    
    
    
    
}