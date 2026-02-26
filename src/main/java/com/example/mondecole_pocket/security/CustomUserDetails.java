package com.example.mondecole_pocket.security;

import com.example.mondecole_pocket.entity.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final Long organizationId;  // ✅ AJOUT
    private final String username;
    private final String password;
    private final boolean active;
    private final boolean locked;
    private final UserRole role;

    public CustomUserDetails(Long id, Long organizationId, String username,
                             String password, boolean active, boolean locked, UserRole role) {
        this.id = id;
        this.organizationId = organizationId;  // ✅ AJOUT
        this.username = username;
        this.password = password;
        this.active = active;
        this.locked = locked;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}