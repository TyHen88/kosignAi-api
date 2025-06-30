package org.kosign.chatbotapi.service.auth;


import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.components.common.api.StatusCode;
import org.kosign.chatbotapi.config.security.JwtUtil;
import org.kosign.chatbotapi.config.security.UserAuthenticationProvider;
import org.kosign.chatbotapi.domains.SecurityUser;
import org.kosign.chatbotapi.domains.Users;
import org.kosign.chatbotapi.enums.Role;
import org.kosign.chatbotapi.enums.StatusUser;
import org.kosign.chatbotapi.exception.BusinessException;
import org.kosign.chatbotapi.payload.auth.AuthRequest;
import org.kosign.chatbotapi.payload.auth.AuthResponse;
import org.kosign.chatbotapi.payload.auth.LoginRequest;
import org.kosign.chatbotapi.repository.UserRepository;
import org.kosign.chatbotapi.service.password.PasswordEncryption;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImp implements  AuthService {
    private final UserRepository userRepository;
//    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserAuthenticationProvider userAuthenticationProvider;
    private final PasswordEncryption passwordEncryption;

    @Override
    @Transactional
    public void register(AuthRequest request) {
        String rawPassword;
        String confirmPassword;
        try {
            rawPassword = passwordEncryption.getPassword(request.getPassword());
            confirmPassword = passwordEncryption.getPassword(request.getConfirmPassword());
            if (!confirmPassword.equals(rawPassword)) {
                throw new BusinessException(StatusCode.PASSWORD_MUST_MATCH, "Password and confirm password must match");
            }
        } catch (Exception e) {
            throw new BusinessException(StatusCode.PASSWORD_MUST_BE_ENCRYPTED);
        }

        var users = Users.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(rawPassword)
                .confirmPassword(confirmPassword)
                .email(request.getEmail())
                .role(Role.USER)
                .status(StatusUser.ACTIVE)
                .build();
        userRepository.save(users);
    }

    @Override
    @Transactional
    public Object login(LoginRequest request) throws Throwable {

        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Username and password are required");
        }

        Authentication authentication = userAuthenticationProvider.authenticate(
                request.getUsername(),
                request.getPassword()
        );

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (securityUser == null) {
            throw new BusinessException(StatusCode.AUTHENTICATION_FAILED, "Authentication failed");
        }

        if (!securityUser.isEnabled()) {
            throw new BusinessException(StatusCode.USER_DISABLED, "User account is disabled");
        }

        String token = jwtUtil.doGenerateToken(securityUser);
        return new AuthResponse(
                token,
                "Bearer",
                jwtUtil.getExpireIn()
        );
    }
}
