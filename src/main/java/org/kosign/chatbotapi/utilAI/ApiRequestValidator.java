package org.kosign.chatbotapi.utilAI;

import org.kosign.chatbotapi.domains.openAPITools.ApiRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ApiRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return ApiRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ApiRequest request = (ApiRequest) target;
        
        if (request.getKeywords() == null || request.getKeywords().isBlank()) {
            errors.rejectValue("keyword", "blank", "Keyword cannot be blank");
        }
        
        if (request.getUrl().contains(" ")) {
            errors.rejectValue("urlTemplate", "invalid", "URL template cannot contain spaces");
        }
    }
}