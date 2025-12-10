package com.executor.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "authorities")
public class Authority {
    public enum USER_ROLES {
        ROLE_ANALYST
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_authorities")
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private USER_ROLES authority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "username", nullable = false)
    private String username;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public USER_ROLES getAuthority() { return authority; }
    public void setAuthority(USER_ROLES authority) { this.authority = authority; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}