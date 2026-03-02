package com.mas.gov.bt.mas.primary.utility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInfo {
    private int currentPage;
    private long totalItems;
    private int totalPages;
    private int pageSize;
    private boolean isFirst;
    private boolean isLast;

    /**
     * Create PaginationInfo from Spring's Page object.
     * Converts 0-based page number to 1-based for the response.
     */
    public static PaginationInfo fromPage(Page<?> page) {
        return PaginationInfo.builder()
                .currentPage(page.getNumber() + 1) // Convert to 1-based
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
