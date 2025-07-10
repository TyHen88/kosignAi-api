package org.kosign.chatbotapi.service.category;

import org.kosign.chatbotapi.payload.category.CategoryRequest;

public interface CategoryService {
    Object getAllCategories() throws Throwable;

    Object createCategory(CategoryRequest request) throws Throwable;

    Object deleteCategoryId(Long id) throws Throwable;

    Object updateCategoryId(Long id, CategoryRequest request) throws Throwable;
}
