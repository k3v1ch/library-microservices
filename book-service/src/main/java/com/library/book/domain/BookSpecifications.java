package com.library.book.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** Поиск: подстрока в названии или авторе плюс фильтр по доступности. */
public final class BookSpecifications {

    public static Specification<Book> matching(String query, Boolean onlyAvailable) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query)) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like)));
            }
            if (Boolean.TRUE.equals(onlyAvailable)) {
                predicates.add(cb.isTrue(root.get("available")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BookSpecifications() {
    }
}
