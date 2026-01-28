package com.mas.gov.bt.mas.primary.utility;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class for 1-based pagination
 * Converts 1-based page numbers to Spring's 0-based pagination
 */
public class PageRequest1Based {

    /**
     * Create a 1-based PageRequest
     * @param page Page number (1-based, minimum 1)
     * @param size Page size
     * @return Pageable with 0-based page number
     */
    public static Pageable of(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be >= 1");
        }
        return PageRequest.of(page - 1, size);
    }

    /**
     * Create a 1-based PageRequest with sorting
     * @param page Page number (1-based, minimum 1)
     * @param size Page size
     * @param sort Sort specification
     * @return Pageable with 0-based page number and sorting
     */
    public static Pageable of(int page, int size, Sort sort) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be >= 1");
        }
        return PageRequest.of(page - 1, size, sort);
    }

    /**
     * Create a 1-based PageRequest with sorting by property
     * @param page Page number (1-based, minimum 1)
     * @param size Page size
     * @param direction Sort direction
     * @param properties Properties to sort by
     * @return Pageable with 0-based page number and sorting
     */
    public static Pageable of(int page, int size, Sort.Direction direction, String... properties) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be >= 1");
        }
        return PageRequest.of(page - 1, size, direction, properties);
    }
}
