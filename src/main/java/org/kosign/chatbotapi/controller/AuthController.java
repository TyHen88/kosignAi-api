package org.kosign.chatbotapi.controller;



import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.components.common.api.ChatAIRestController;
import org.kosign.chatbotapi.components.common.api.Common;
import org.kosign.chatbotapi.payload.auth.AuthRequest;
import org.kosign.chatbotapi.payload.auth.LoginRequest;
import org.kosign.chatbotapi.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wb/v1/auth")
@RequiredArgsConstructor
public class AuthController extends ChatAIRestController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity signup(@RequestHeader Map<String, String> headers,@Valid @RequestBody AuthRequest payload) throws Throwable{
        authService.register(payload);
        return ok(new Common(headers));
    }

    @GetMapping("/test")
    public String test() {
        return "Test successful";
    }

    @PostMapping("/login")
    public Object login(@RequestHeader Map<String, String> headers, @RequestBody @Valid LoginRequest payload) throws Throwable{
        return ok(authService.login(payload),new Common(headers));
    }
}
