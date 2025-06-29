package org.kosign.chatbotapi.service;

import org.kosign.chatbotapi.payload.PageContentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PageContentService {
    Object getAllPageContents(String sort, String searchValue, Pageable pageable) throws Throwable;
}
