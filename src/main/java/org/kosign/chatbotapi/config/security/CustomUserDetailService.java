package org.kosign.chatbotapi.config.security;//package com.planprostructure.planpro.config;
//
//import com.planprostructure.planpro.domain.security.SecurityUser;
//import com.planprostructure.planpro.domain.users.UserRepository;
//import com.planprostructure.planpro.domain.users.Users;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//
//public class CustomUserDetailService implements UserDetailsService {
//    private final UserRepository userRepository;
////
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        Users user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
//
//        return new SecurityUser(user);
//
//    }
//}
