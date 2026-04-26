package com.example.auth_service.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auth_user_id", nullable = false)
    private UUID authUserId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;
}
