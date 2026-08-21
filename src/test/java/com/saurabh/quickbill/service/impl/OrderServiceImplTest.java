package com.saurabh.quickbill.service.impl;

import com.saurabh.quickbill.entity.ItemEntity;
import com.saurabh.quickbill.entity.OrderEntity;
import com.saurabh.quickbill.entity.UserEntity;
import com.saurabh.quickbill.exception.AccessDeniedException;
import com.saurabh.quickbill.exception.PaymentVerificationException;
import com.saurabh.quickbill.exception.ResourceNotFoundException;
import com.saurabh.quickbill.io.OrderRequest;
import com.saurabh.quickbill.io.OrderResponse;
import com.saurabh.quickbill.io.PaymentDetails;
import com.saurabh.quickbill.io.PaymentVerificationRequest;
import com.saurabh.quickbill.repository.ItemRepository;
import com.saurabh.quickbill.repository.OrderEntityRepository;
import com.saurabh.quickbill.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl — the highest-risk class in the app,
 * since it owns money calculations and order-ownership security checks.
 *
 * These are pure Mockito unit tests: no Spring context, no database, no
 * network. Every collaborator (the repositories) is a mock we control, so
 * each test is fast (milliseconds) and only exercises OrderServiceImpl's
 * own logic — not JPA, not Hibernate, not a real DB.
 *
 * Test naming follows methodUnderTest_condition_expectedResult, so a
 * failing test name alone tells you what broke without opening the file.
 *
 * Mocks are created by hand with Mockito.mock(...) rather than the
 * @Mock/@InjectMocks annotations. Both approaches are equally valid and
 * you'll see both in real codebases — doing it by hand here makes it
 * obvious exactly which dependencies OrderServiceImpl needs.
 */
class OrderServiceImplTest {

    private final OrderEntityRepository orderEntityRepository = mock(OrderEntityRepository.class);
    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final OrderServiceImpl orderService =
            new OrderServiceImpl(orderEntityRepository, itemRepository, userRepository);

    @BeforeEach
    void setUp() {
        // taxRate and razorpayKeySecret are populated by Spring from
        // application.properties via @Value in the real app. Outside a
        // Spring context nothing sets them, so we set them ourselves.
        ReflectionTestUtils.setField(orderService, "taxRate", new BigDecimal("0.18"));
        ReflectionTestUtils.setField(orderService, "razorpayKeySecret", "test_dummy_secret");
    }

    @AfterEach
    void clearSecurityContext() {
        // SecurityContextHolder is a static, thread-local holder — if we
        // don't clear it, state from one test can leak into the next.
        SecurityContextHolder.clearContext();
    }

    /** Stubs SecurityContextHolder so the service sees "callerEmail" as logged in. */
    private void authenticateAs(String callerEmail, String... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(callerEmail, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ── createOrder ─────────────────────────────────────────────────────

    @Test
    void createOrder_computesTotalsFromDbPrice_ignoringAnyClientSuppliedPrice() {
        // Arrange
        OrderRequest.OrderItemRequest cartLine =
                OrderRequest.OrderItemRequest.builder().itemId("ITM1").quantity(2).build();
        OrderRequest request = OrderRequest.builder()
                .customerName("Test Customer")
                .phoneNumber("9876543210")
                .paymentMethod("CASH")
                .cartItems(List.of(cartLine))
                .build();

        // The DB price is 100.00 — note OrderRequest.OrderItemRequest has no
        // price field at all, so there is no client price to even smuggle in.
        ItemEntity dbItem = ItemEntity.builder()
                .itemId("ITM1").name("Burger").price(new BigDecimal("100.00")).build();
        when(itemRepository.findByItemId("ITM1")).thenReturn(Optional.of(dbItem));

        authenticateAs("customer@test.com");
        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(UserEntity.builder().userId("USR1").build()));

        // save() just echoes back whatever entity it was given, like a real
        // repository would after persisting (minus the @PrePersist orderId).
        when(orderEntityRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert — 2 × ₹100.00 = ₹200.00 subtotal, 18% GST = ₹36.00 tax
        assertThat(response.getSubTotal()).isEqualByComparingTo("200.00");
        assertThat(response.getTax()).isEqualByComparingTo("36.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("236.00");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getPrice()).isEqualByComparingTo("100.00");

        // Prove the item was actually looked up in the DB, not trusted from the request.
        verify(itemRepository).findByItemId("ITM1");
    }

    @Test
    void createOrder_throwsResourceNotFound_whenCartItemDoesNotExistInDb() {
        OrderRequest.OrderItemRequest cartLine =
                OrderRequest.OrderItemRequest.builder().itemId("MISSING").quantity(1).build();
        OrderRequest request = OrderRequest.builder()
                .customerName("Test Customer")
                .phoneNumber("9876543210")
                .paymentMethod("CASH")
                .cartItems(List.of(cartLine))
                .build();

        when(itemRepository.findByItemId("MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderEntityRepository, never()).save(any());
    }

    // ── deleteOrder — ownership checks ─────────────────────────────────

    @Test
    void deleteOrder_ownerCanDeleteTheirOwnOrder() {
        OrderEntity existingOrder = OrderEntity.builder()
                .orderId("ORD-1").createdByUserId("USR1").build();
        when(orderEntityRepository.findByOrderId("ORD-1")).thenReturn(Optional.of(existingOrder));

        authenticateAs("owner@test.com", "ROLE_USER");
        when(userRepository.findByEmail("owner@test.com"))
                .thenReturn(Optional.of(UserEntity.builder().userId("USR1").build()));

        orderService.deleteOrder("ORD-1");

        verify(orderEntityRepository).delete(existingOrder);
    }

    @Test
    void deleteOrder_nonOwnerGetsAccessDenied_andOrderIsNotDeleted() {
        OrderEntity existingOrder = OrderEntity.builder()
                .orderId("ORD-1").createdByUserId("USR1").build();
        when(orderEntityRepository.findByOrderId("ORD-1")).thenReturn(Optional.of(existingOrder));

        authenticateAs("someone-else@test.com", "ROLE_USER");
        when(userRepository.findByEmail("someone-else@test.com"))
                .thenReturn(Optional.of(UserEntity.builder().userId("USR2").build()));

        assertThrows(AccessDeniedException.class, () -> orderService.deleteOrder("ORD-1"));
        verify(orderEntityRepository, never()).delete(any());
    }

    @Test
    void deleteOrder_adminCanDeleteAnyOrder_regardlessOfOwnership() {
        OrderEntity existingOrder = OrderEntity.builder()
                .orderId("ORD-1").createdByUserId("USR1").build();
        when(orderEntityRepository.findByOrderId("ORD-1")).thenReturn(Optional.of(existingOrder));

        // Admin bypasses the ownership check entirely — no userRepository
        // lookup should even happen, since the caller is never compared
        // against createdByUserId when they hold ROLE_ADMIN.
        authenticateAs("admin@test.com", "ROLE_ADMIN");

        orderService.deleteOrder("ORD-1");

        verify(orderEntityRepository).delete(existingOrder);
        verifyNoInteractions(userRepository);
    }

    @Test
    void deleteOrder_throwsResourceNotFound_whenOrderIdDoesNotExist() {
        when(orderEntityRepository.findByOrderId("GHOST")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder("GHOST"));
    }

    // ── verifyPayment ───────────────────────────────────────────────────

    @Test
    void verifyPayment_rejectsInvalidSignature_andNeverMarksOrderAsPaid() {
        // This test originally caught a real bug: verifyRazorpaySignature()
        // called Utils.verifyPaymentSignature() but discarded its boolean
        // return value, and only caught RazorpayException — which that SDK
        // method does NOT throw for a simple signature mismatch (only for
        // malformed input). The result: any signature, including this
        // obviously-fake one, was silently accepted as valid. Fixed in
        // OrderServiceImpl by checking the returned boolean explicitly.
        //
        // This test still does NOT cover the success path (a genuinely
        // valid signature) — that would need either real Razorpay test
        // credentials, or wrapping Utils in an interface we own so it can
        // be mocked. See TESTING_GUIDE.md.
        OrderEntity order = OrderEntity.builder()
                .orderId("ORD-1")
                .paymentDetails(PaymentDetails.builder()
                        .status(PaymentDetails.PaymentStatus.PENDING)
                        .build())
                .build();
        when(orderEntityRepository.findByOrderId("ORD-1")).thenReturn(Optional.of(order));

        PaymentVerificationRequest request = new PaymentVerificationRequest(
                "order_fake", "pay_fake", "definitely_not_a_valid_signature", "ORD-1");

        assertThrows(PaymentVerificationException.class, () -> orderService.verifyPayment(request));
        verify(orderEntityRepository, never()).save(any());
    }
}
