package org.kosign.chatbotapi.payload;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiSortBuilder {
    private final List<Sort.Order> orders;

    public MultiSortBuilder() {
        this.orders = new ArrayList<>();
    }

    public MultiSortBuilder with(String sortExpression) {
        if (sortExpression == null || sortExpression.trim().isEmpty()) {
            return this;
        }

        String[] sorts = sortExpression.split(",");
        Arrays.stream(sorts).forEach(sort -> {
            String[] parts = sort.trim().split(":");
            String property = parts[0];
            Sort.Direction direction = parts.length > 1
                    ? ("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC)
                    : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        });

        return this;
    }

    public List<Sort.Order> build() {
        return orders.isEmpty()
                ? List.of(Sort.Order.asc("id"))  // default sort
                : orders;
    }
}
