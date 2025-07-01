package org.kosign.chatbotapi.service;

import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.components.common.Pagination;
import org.kosign.chatbotapi.payload.IGetPageContents;
import org.kosign.chatbotapi.payload.MainResponse;
import org.kosign.chatbotapi.payload.PageContentResponse;
import org.kosign.chatbotapi.repository.PPCBankContentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PageContentServiceImpl implements PageContentService{
    private final PPCBankContentRepository pageContentRepository;


    @Override
    @Transactional(readOnly = true)
    public Object getAllPageContents(String sort, String searchValue, Pageable pageable) throws Throwable {
        Page<IGetPageContents> pageContent = pageContentRepository.findAllContentByStatus(searchValue, pageable);
        var response = pageContent.map(
                content -> PageContentResponse.builder()
                        .no(content.getNo())
                        .url(content.getUrl())
                        .title(content.getTitle())
                        .content(content.getContent())
                        .updatedAt(content.getUpdatedAt())
                        .build()
        ).toList();
        return MainResponse.builder()
                .data(response)
                .pagination(new Pagination(pageContent))
                .build();
    }
}
