package org.kosign.chatbotapi.controller;

import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.components.common.Pagination;
import org.kosign.chatbotapi.components.common.api.ChatAIRestController;
import org.kosign.chatbotapi.payload.MultiSortBuilder;
import org.kosign.chatbotapi.service.PageContentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/page-content")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
public class PageContentController extends ChatAIRestController {
    private final PageContentService pageContentService;

    @GetMapping
    public Object getPageContent(
            @RequestParam(name = "search_value", required = false) String searchValue,
            @RequestParam(name = "page_number", defaultValue = "0") Integer pageNumber,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "sort_columns", required = false, defaultValue = "id:desc") String sortColumns
            ) throws Throwable {
        List<Sort.Order> sortBuilder = new MultiSortBuilder().with(sortColumns).build();
        Pageable pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(sortBuilder));
        return ok(pageContentService.getAllPageContents(sortColumns, searchValue, pageRequest));
    }
}
