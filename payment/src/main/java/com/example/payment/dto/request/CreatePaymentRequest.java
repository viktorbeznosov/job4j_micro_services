package com.example.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaymentRequest {
    @NotBlank(message = "Description cannot be blank")
    private String description;
}
