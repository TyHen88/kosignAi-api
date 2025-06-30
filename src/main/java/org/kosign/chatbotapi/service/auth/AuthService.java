package org.kosign.chatbotapi.service.auth;


import org.kosign.chatbotapi.payload.auth.AuthRequest;
import org.kosign.chatbotapi.payload.auth.LoginRequest;

public interface AuthService {
    void register(AuthRequest request) throws Throwable;
    Object login(LoginRequest request) throws Throwable;
}
