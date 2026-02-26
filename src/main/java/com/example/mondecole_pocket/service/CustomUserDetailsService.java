package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.repository.UserRepository;
import com.example.mondecole_pocket.security.CustomUserDetails;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {

        Long organizationId = TenantContext.getTenantId();
        if (organizationId == null) {
            throw new IllegalStateException("Tenant not resolved");
        }

        User user = userRepository
                .findByUsernameAndOrganizationId(username, organizationId)
                .orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));

        if (user.isLocked()) throw new LockedException("ACCOUNT_LOCKED");
        if (!user.isActive()) throw new DisabledException("ACCOUNT_DISABLED");

        return new CustomUserDetails(
                user.getId(),
                user.getOrganization().getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                user.isLocked(),
                user.getRole()
        );
    }
}
