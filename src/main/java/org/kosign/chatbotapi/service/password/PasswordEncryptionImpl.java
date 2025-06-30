package org.kosign.chatbotapi.service.password;

import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.utils.PasswordUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordEncryptionImpl implements PasswordEncryption {
    private final PasswordEncoder passwordEncoder;

    @Override
    public String getPassword(String password) throws Exception {
        var rawPassword = PasswordUtils.decrypt(password);
        return passwordEncoder.encode(rawPassword);
    }
}
