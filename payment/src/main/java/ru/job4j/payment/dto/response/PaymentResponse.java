package ru.job4j.payment.dto.response;

import ru.job4j.payment.entity.PaymentStatus;
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
