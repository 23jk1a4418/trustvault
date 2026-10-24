package com.trustvault.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="app_users")
public class AppUser {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable=false, unique=true) private String username;
    @Column(nullable=false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role;
    protected AppUser() {}
    public AppUser(String username, String passwordHash, Role role) { this.username=username; this.passwordHash=passwordHash; this.role=role; }
    public UUID getId(){return id;} public String getUsername(){return username;} public String getPasswordHash(){return passwordHash;} public Role getRole(){return role;}
}
