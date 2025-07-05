package org.kosign.chatbotapi.payload;


import org.springframework.beans.factory.annotation.Value;

public interface IGetPageContents {
    @Value("#{target.no}")
    Long getNo();

    @Value("#{target.url}")
    String getUrl();

    @Value("#{target.title}")
    String getTitle();

    @Value("#{target.content}")
    String getContent();

    @Value("#{target.updatedAt}")
    String getUpdatedAt();
}
