package org.example.database;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_codes")
public class RoleCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean isActive = true;

    public RoleCode() {}

    public RoleCode(String code, UserRole role, User creator, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.code = code;
        this.role = role;
        this.creator = creator;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}