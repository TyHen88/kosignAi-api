package org.kosign.chatbotapi.payload;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kosign.chatbotapi.components.common.Pagination;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MainResponse {
    private List<?> data;
    private Pagination pagination;

    @Builder
    public MainResponse(List<?> data, Pagination pagination) {
        this.data = data;
        this.pagination = pagination;
    }
}
