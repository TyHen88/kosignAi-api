package org.kosign.chatbotapi.payload;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PageContentResponse {
    private Long no;
    private String url;
    private String title;
    private String content;
    private String updatedAt;

    @Builder
    public PageContentResponse(Long no, String url, String title, String content, String updatedAt) {
        this.no = no;
        this.url = url;
        this.title = title;
        this.content = content;
        this.updatedAt = updatedAt;
    }
}
