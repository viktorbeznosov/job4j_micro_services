package ru.job4j.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.payment.dto.request.CreatePaymentRequest;
import ru.job4j.payment.dto.response.PaymentResponse;
import ru.job4j.payment.dto.response.UserResponse;
import ru.job4j.payment.entity.PaymentEntity;
import ru.job4j.payment.entity.PaymentStatus;
import ru.job4j.payment.exception.AccessDeniedException;
import ru.job4j.payment.exception.BadRequestException;
import ru.job4j.payment.exception.ResourceNotFoundException;
import ru.job4j.payment.repository.PaymentRepository;
import ru.job4j.payment.security.AuthClient;
import ru.job4j.payment.security.CurrentUserDto;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private PaymentService paymentService;

    private CurrentUserDto managerUser;
    private CurrentUserDto regularUser;
    private CurrentUserDto adminUser;
    private PaymentEntity paymentEntity;

    @BeforeEach
    void setUp() {
        managerUser = CurrentUserDto.builder()
                .id(1L)
                .username("manager")
                .email("manager@example.com")
                .fullName("Manager User")
                .roles(new HashSet<>(Set.of("ROLE_MANAGER")))
                .build();

        regularUser = CurrentUserDto.builder()
                .id(2L)
                .username("user")
                .email("user@example.com")
                .fullName("Regular User")
                .roles(new HashSet<>(Set.of("ROLE_USER")))
                .build();

        adminUser = CurrentUserDto.builder()
                .id(3L)
                .username("admin")
                .email("admin@example.com")
                .fullName("Admin User")
                .roles(new HashSet<>(Set.of("ROLE_ADMIN")))
                .build();

        paymentEntity = PaymentEntity.builder()
                .id(1L)
                .userId(2L)
                .status(PaymentStatus.PENDING)
                .description("Test payment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createPaymentSuccess() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setDescription("Test payment");

        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);

        PaymentResponse result = paymentService.createPayment(request, regularUser);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals("Test payment", result.getDescription());
        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void approvePaymentSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);

        PaymentResponse result = paymentService.approvePayment(1L, managerUser);

        assertNotNull(result);
        assertEquals(PaymentStatus.APPROVED, paymentEntity.getStatus());
        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void approvePaymentOnlyManagerAllowed() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> paymentService.approvePayment(1L, regularUser));

        assertEquals("Only managers can approve payments", exception.getMessage());
    }

    @Test
    void approvePaymentNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.approvePayment(999L, managerUser));

        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void approvePaymentAlreadyApproved() {
        paymentEntity.setStatus(PaymentStatus.APPROVED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.approvePayment(1L, managerUser));

        assertTrue(exception.getMessage().contains("APPROVED"));
    }

    @Test
    void rejectPaymentSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);

        PaymentResponse result = paymentService.rejectPayment(1L, managerUser);

        assertNotNull(result);
        assertEquals(PaymentStatus.REJECTED, paymentEntity.getStatus());
        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void rejectPaymentOnlyManagerAllowed() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> paymentService.rejectPayment(1L, regularUser));

        assertEquals("Only managers can reject payments", exception.getMessage());
    }

    @Test
    void getAllPaymentsAsManagerSuccess() {
        List<PaymentEntity> payments = Arrays.asList(paymentEntity);
        when(paymentRepository.findAll()).thenReturn(payments);

        List<PaymentResponse> result = paymentService.getAllPayments(managerUser);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllPaymentsAsAdminSuccess() {
        List<PaymentEntity> payments = Arrays.asList(paymentEntity);
        when(paymentRepository.findAll()).thenReturn(payments);

        List<PaymentResponse> result = paymentService.getAllPayments(adminUser);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllPaymentsRegularUserDenied() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> paymentService.getAllPayments(regularUser));

        assertTrue(exception.getMessage().contains("managers or admins"));
    }

    @Test
    void getPaymentByIdAsOwnerSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));

        PaymentResponse result = paymentService.getPaymentById(1L, regularUser);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getPaymentByIdAsManagerSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));

        PaymentResponse result = paymentService.getPaymentById(1L, managerUser);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getPaymentByIdAccessDenied() {
        CurrentUserDto otherUser = CurrentUserDto.builder()
                .id(999L)
                .username("other")
                .email("other@example.com")
                .fullName("Other User")
                .roles(new HashSet<>(Set.of("ROLE_USER")))
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> paymentService.getPaymentById(1L, otherUser));

        assertTrue(exception.getMessage().contains("permission"));
    }

    @Test
    void getPaymentByIdNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentById(999L, managerUser));

        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void getUserByIdAsAdminSuccess() {
        UserResponse userResponse = UserResponse.builder()
                .id(2L)
                .username("user")
                .email("user@example.com")
                .fullName("Regular User")
                .roles(new HashSet<>())
                .build();

        when(authClient.getUserById(eq(2L), any())).thenReturn(userResponse);

        UserResponse result = paymentService.getUserById(2L, adminUser, "Bearer token");

        assertNotNull(result);
        assertEquals("user", result.getUsername());
    }

    @Test
    void getUserByIdAsSelfSuccess() {
        UserResponse userResponse = UserResponse.builder()
                .id(2L)
                .username("user")
                .email("user@example.com")
                .fullName("Regular User")
                .roles(new HashSet<>())
                .build();

        when(authClient.getUserById(eq(2L), any())).thenReturn(userResponse);

        UserResponse result = paymentService.getUserById(2L, regularUser, "Bearer token");

        assertNotNull(result);
        assertEquals("user", result.getUsername());
    }

    @Test
    void getUserByIdAccessDenied() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> paymentService.getUserById(3L, regularUser, "Bearer token"));

        assertTrue(exception.getMessage().contains("own profile"));
    }
}