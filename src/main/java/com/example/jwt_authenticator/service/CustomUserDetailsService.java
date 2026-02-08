package com.example.jwt_authenticator.service;

import com.example.jwt_authenticator.entity.User;
import com.example.jwt_authenticator.repository.UserRepository;
import com.example.jwt_authenticator.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));

        if (user.isLocked()) throw new LockedException("ACCOUNT_LOCKED");
        if (!user.isActive()) throw new DisabledException("ACCOUNT_DISABLED");

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                user.isLocked(),
                user.getRole()
        );
    }
}
