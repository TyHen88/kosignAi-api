package org.kosign.chatbotapi.service;

import org.springframework.data.domain.Pageable;

public interface PageContentService {
    Object getAllPageContents(String sort, String searchValue, Pageable pageable) throws Throwable;
}
