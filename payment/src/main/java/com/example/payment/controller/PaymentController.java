package com.example.payment.controller;

import com.example.payment.dto.request.CreatePaymentRequest;
import com.example.payment.dto.response.PaymentResponse;
import com.example.payment.dto.response.UserResponse;
import com.example.payment.security.CurrentUserDto;
import com.example.payment.security.CurrentUserProvider;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        CurrentUserDto currentUser = currentUserProvider.getCurrentUser(authorizationHeader);
        return ResponseEntity.ok(paymentService.createPayment(request, currentUser));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PaymentResponse> approvePayment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {
        CurrentUserDto currentUser = currentUserProvider.getCurrentUser(authorizationHeader);
        return ResponseEntity.ok(paymentService.approvePayment(id, currentUser));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PaymentResponse> rejectPayment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {
        CurrentUserDto currentUser = currentUserProvider.getCurrentUser(authorizationHeader);
        return ResponseEntity.ok(paymentService.rejectPayment(id, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(
            @RequestHeader("Authorization") String authorizationHeader) {
        CurrentUserDto currentUser = currentUserProvider.getCurrentUser(authorizationHeader);
        return ResponseEntity.ok(paymentService.getAllPayments(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {
        CurrentUserDto currentUser = currentUserProvider.getCurrentUser(authorizationHeader);
        return ResponseEntity.ok(paymentService.getPaymentById(id, currentUser));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {
        CurrentUserDto currentUser = currentUserProvider.getCurrentUser(authorizationHeader);
        return ResponseEntity.ok(paymentService.getUserById(id, currentUser, authorizationHeader));
    }
}
