package com.trustvault.config;

import com.trustvault.repo.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;

@Configuration
public class UserConfig {
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username).map(u -> User.withUsername(u.getUsername()).password(u.getPasswordHash()).authorities(new SimpleGrantedAuthority("ROLE_"+u.getRole())).build()).orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
    }
}
