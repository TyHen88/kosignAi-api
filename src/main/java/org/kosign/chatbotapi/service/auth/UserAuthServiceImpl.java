package org.kosign.chatbotapi.service.auth;

import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.domains.SecurityUser;
import org.kosign.chatbotapi.domains.Users;
import org.kosign.chatbotapi.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SecurityUser loadUserByUsername(String username) {
        List<Users> users = userRepository.findByUsername(username);

        Users user = users.get(0);

        return new SecurityUser(
                user
        );

    }
}
