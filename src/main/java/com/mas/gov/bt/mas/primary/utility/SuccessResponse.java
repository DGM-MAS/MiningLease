package com.mas.gov.bt.mas.primary.utility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse<T> {
    private String status = "Success";
    private String message;
    private T data;

    public SuccessResponse(String message, T data) {
        this.status = "Success";
        this.message = message;
        this.data = data;
    }

    public static ResponseEntity<SuccessResponse<Void>> buildSuccessResponse(String message) {
        return ResponseEntity.ok(new SuccessResponse<>("Success", message, null));
    }
}