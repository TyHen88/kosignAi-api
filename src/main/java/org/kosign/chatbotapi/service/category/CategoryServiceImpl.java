package org.kosign.chatbotapi.service.category;

import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.domains.Category;
import org.kosign.chatbotapi.enums.Status;
import org.kosign.chatbotapi.payload.category.CategoryRequest;
import org.kosign.chatbotapi.payload.category.CategoryResponse;
import org.kosign.chatbotapi.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Object getAllCategories() throws Throwable {
        List<Category> categories = categoryRepository.findAllActive();

        return categories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }
    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    @Override
    @Transactional
    public Object createCategory(CategoryRequest request) throws Throwable {
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setStatus(Status.ACTIVE);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Object deleteCategoryId(Long id) throws Throwable {
        var cateId = categoryRepository.findById(id);
        if (cateId.isEmpty()) {
            throw new IllegalArgumentException("Category not found");
        }
        Category category = cateId.get();
        category.setStatus(Status.DELETED);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Object updateCategoryId(Long id, CategoryRequest request) throws Throwable {
        var cateId = categoryRepository.findById(id);
        if (cateId.isEmpty()) {
            throw new IllegalArgumentException("Category not found");
        }
        Category category = cateId.get();
        category.setName(request.getName());
        category.setStatus(Status.ACTIVE);
        return categoryRepository.save(category);
    }
}
