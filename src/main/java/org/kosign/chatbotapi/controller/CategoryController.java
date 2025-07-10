package org.kosign.chatbotapi.controller;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.components.common.api.ChatAIRestController;
import org.kosign.chatbotapi.payload.category.CategoryRequest;
import org.kosign.chatbotapi.service.category.CategoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController extends ChatAIRestController {
    private final CategoryService categoryService;

    @GetMapping
    public Object getALLCategory() throws Throwable{
        return ok(categoryService.getAllCategories());
    }

    @PatchMapping("/{id}")
    public Object updateCategoryId(@PathVariable("id") Long id, @Valid @RequestBody CategoryRequest request) throws Throwable{
        categoryService.updateCategoryId(id, request);
        return ok();
    }

    @PostMapping
    public Object createCategory(@Valid @RequestBody CategoryRequest request) throws Throwable{
        categoryService.createCategory(request);
        return ok();
    }

    @DeleteMapping("/{id}")
    public Object deleteCategoryId(@PathVariable("id") Long id) throws Throwable{
        categoryService.deleteCategoryId(id);
        return ok();
    }

}
