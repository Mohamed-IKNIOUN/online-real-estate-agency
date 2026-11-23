package com.agenceimmobiliere.identity_service.dto;

import com.agenceimmobiliere.identity_service.enums.Role;

public class LoginResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;

    private String accessToken;
    private String tokenType;
    
    public LoginResponse() {
    }

    public LoginResponse(
            Long id,
            String fullName,
            String email,
            Role role,
            String accessToken) {

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
    }

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

    // Generate getters and setters using Eclipse
    
}