package com.example.payment.service;

import com.example.payment.dto.request.CreatePaymentRequest;
import com.example.payment.dto.response.PaymentResponse;
import com.example.payment.dto.response.UserResponse;
import com.example.payment.entity.PaymentEntity;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.exception.AccessDeniedException;
import com.example.payment.exception.BadRequestException;
import com.example.payment.exception.ResourceNotFoundException;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.security.AuthClient;
import com.example.payment.security.CurrentUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AuthClient authClient;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, CurrentUserDto currentUser) {
        PaymentEntity payment = PaymentEntity.builder()
                .userId(currentUser.getId())
                .status(PaymentStatus.PENDING)
                .description(request.getDescription())
                .build();
        
        PaymentEntity saved = paymentRepository.save(payment);
        return mapToPaymentResponse(saved);
    }

    @Transactional
    public PaymentResponse approvePayment(Long id, CurrentUserDto currentUser) {
        if (!hasManagerRole(currentUser)) {
            throw new AccessDeniedException("Only managers can approve payments");
        }

        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Cannot approve a payment that is already " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.APPROVED);
        payment.setManagerId(currentUser.getId());
        
        return mapToPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse rejectPayment(Long id, CurrentUserDto currentUser) {
        if (!hasManagerRole(currentUser)) {
            throw new AccessDeniedException("Only managers can reject payments");
        }

        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Cannot reject a payment that is already " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REJECTED);
        payment.setManagerId(currentUser.getId());
        
        return mapToPaymentResponse(paymentRepository.save(payment));
    }

    public List<PaymentResponse> getAllPayments(CurrentUserDto currentUser) {
        if (!hasManagerRole(currentUser) && !hasAdminRole(currentUser)) {
            throw new AccessDeniedException("Only managers or admins can view all payments");
        }

        return paymentRepository.findAll().stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse getPaymentById(Long id, CurrentUserDto currentUser) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        boolean isManager = hasManagerRole(currentUser);
        boolean isAdmin = hasAdminRole(currentUser);
        boolean isOwner = payment.getUserId().equals(currentUser.getId());

        if (!isManager && !isAdmin && !isOwner) {
            throw new AccessDeniedException("You don't have permission to view this payment");
        }

        return mapToPaymentResponse(payment);
    }

    public UserResponse getUserById(Long id, CurrentUserDto currentUser, String authorizationHeader) {
        boolean isAdmin = hasAdminRole(currentUser);
        boolean isSelf = currentUser.getId().equals(id);

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("You can only view your own profile");
        }

        return authClient.getUserById(id, authorizationHeader);
    }

    private boolean hasManagerRole(CurrentUserDto user) {
        return user.getRoles().contains("ROLE_MANAGER");
    }

    private boolean hasAdminRole(CurrentUserDto user) {
        return user.getRoles().contains("ROLE_ADMIN");
    }

    private PaymentResponse mapToPaymentResponse(PaymentEntity payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .managerId(payment.getManagerId())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
