package com.example.payment.dto.response;

import com.example.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long userId;
    private Long managerId;
    private PaymentStatus status;
    private String description;
    private LocalDateTime createdAt;
}
