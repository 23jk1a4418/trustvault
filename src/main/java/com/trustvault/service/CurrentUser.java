package com.trustvault.service;
import com.trustvault.model.AppUser; import com.trustvault.repo.UserRepository; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Component;
@Component public class CurrentUser { private final UserRepository users; public CurrentUser(UserRepository users){this.users=users;} public AppUser get(Authentication auth){return users.findByUsername(auth.getName()).orElseThrow();} }
