package com.mas.gov.bt.mas.primary.utility;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse<T> {
    private String status = "Success";
    private String message;
    private T data;
    private PaginationInfo pagination;

    public SuccessResponse(String message, T data) {
        this.status = "Success";
        this.message = message;
        this.data = data;
        this.pagination = null;
    }

    public SuccessResponse(String message, T data, PaginationInfo pagination) {
        this.status = "Success";
        this.message = message;
        this.data = data;
        this.pagination = pagination;
    }

    /**
     * Create a paginated success response from a Spring Page object.
     */
    public static <T> SuccessResponse<java.util.List<T>> fromPage(String message, Page<T> page) {
        return new SuccessResponse<>(message, page.getContent(), PaginationInfo.fromPage(page));
    }

    public static ResponseEntity<SuccessResponse<Void>> buildSuccessResponse(String message) {
        return ResponseEntity.ok(new SuccessResponse<>(message, null));
    }
}