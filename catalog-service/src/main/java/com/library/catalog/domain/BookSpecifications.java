package com.library.catalog.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** Поиск по каталогу: подстрока в названии/авторе/ISBN + фильтры по жанру и наличию. */
public final class BookSpecifications {

    public static Specification<Book> matching(String query, String genre, Boolean onlyAvailable) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query)) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like),
                        cb.like(cb.lower(root.get("isbn")), like)));
            }
            if (StringUtils.hasText(genre)) {
                predicates.add(cb.equal(root.get("genre"), genre));
            }
            if (Boolean.TRUE.equals(onlyAvailable)) {
                predicates.add(cb.greaterThan(root.get("availableCopies"), 0));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BookSpecifications() {
    }
}
