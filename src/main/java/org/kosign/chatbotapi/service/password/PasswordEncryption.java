package org.kosign.chatbotapi.service.password;

public interface PasswordEncryption {
    String getPassword(String password) throws Exception;
}
