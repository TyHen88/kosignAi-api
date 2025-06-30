package org.kosign.chatbotapi.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatAILoggingService extends  LoggingServiceImpl{
    public void logRequest(HttpServletRequest httpServletRequest, Object body) {
        log.info(handleLoggingRequest(httpServletRequest, body));
    }

    public void logResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object body) {
        log.info(handleLoggingResponse(httpServletRequest, httpServletResponse, body));
    }
}
