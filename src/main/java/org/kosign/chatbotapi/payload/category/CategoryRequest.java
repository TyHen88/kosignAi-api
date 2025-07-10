package org.kosign.chatbotapi.payload.category;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryRequest {

    private String name;

    @Builder
    public CategoryRequest(String name) {
        this.name = name;
    }
}
