package com.elegance.salon.dto;

import java.util.List;

public class UserInfoDto {
    private String subject;
    private String name;
    private String email;
    private String preferredUsername;
    private String issuer;
    private List<String> roles;
    private Boolean authenticated;

    public UserInfoDto() {
    }

    public UserInfoDto(String subject, String name, String email, String preferredUsername, String issuer, List<String> roles, Boolean authenticated) {
        this.subject = subject;
        this.name = name;
        this.email = email;
        this.preferredUsername = preferredUsername;
        this.issuer = issuer;
        this.roles = roles;
        this.authenticated = authenticated;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPreferredUsername() { return preferredUsername; }
    public void setPreferredUsername(String preferredUsername) { this.preferredUsername = preferredUsername; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public Boolean getAuthenticated() { return authenticated; }
    public void setAuthenticated(Boolean authenticated) { this.authenticated = authenticated; }
}
