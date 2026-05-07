package com.example.payment.repository;

import com.example.payment.entity.PaymentEntity;
import com.example.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByUserId(Long userId);
    List<PaymentEntity> findByStatus(PaymentStatus status);
}
