package com.saurabh.quickbill.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ─────────────────────────────────────────────────────────────────────────────
// How Bucket4j token-bucket works:
//
//   Imagine each IP gets a bucket that holds N tokens.
//   Every request consumes 1 token.
//   Tokens refill at a fixed rate (e.g. 5 tokens per 1 minute).
//   When the bucket is empty the request is rejected with 429.
//
//   This is better than a simple counter because it handles bursts naturally:
//   if nobody hit /login for 3 minutes, the bucket is full — a user can make
//   5 quick attempts. But they can't make 50 attempts in a row.
// ─────────────────────────────────────────────────────────────────────────────
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // One ConcurrentHashMap per endpoint group.
    // Key = client IP address. Value = their Bucket.
    // ConcurrentHashMap is thread-safe — multiple requests can arrive
    // simultaneously from different IPs without data races.
    private final ConcurrentHashMap<String, Bucket> loginBuckets   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> paymentBuckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Bucket factories ─────────────────────────────────────────────────────

    // /login: 5 attempts per minute per IP.
    // Tight because a brute-force attack tries thousands of passwords — even
    // slowing it to 5/min makes the attack take centuries.
    private Bucket newLoginBucket() {
        Bandwidth limit = Bandwidth.classic(
                5,                               // capacity: 5 tokens max
                Refill.greedy(5, Duration.ofMinutes(1)) // refill 5 tokens every 1 min
        );
        return Bucket.builder().addLimit(limit).build();
    }

    // /payments: 10 requests per minute per IP.
    // More generous — a real user could legitimately retry a payment a few
    // times. Still stops bulk hammering of the Razorpay API.
    private Bucket newPaymentBucket() {
        Bandwidth limit = Bandwidth.classic(
                10,
                Refill.greedy(10, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    // ── Filter logic ─────────────────────────────────────────────────────────
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String ip   = resolveClientIp(request);

        // Only apply to the three sensitive endpoints.
        // All other paths pass through with no overhead.
        if (path.equals("/login")) {
            Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> newLoginBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, "/login: too many attempts. Try again in 1 minute.");
                return;
            }

        } else if (path.equals("/payments/create-order") || path.equals("/payments/verify")) {
            Bucket bucket = paymentBuckets.computeIfAbsent(ip, k -> newPaymentBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, "Payment endpoint: too many requests. Try again in 1 minute.");
                return;
            }
        }

        // Token consumed — let the request proceed normally.
        filterChain.doFilter(request, response);
    }

    // ── Write 429 response ────────────────────────────────────────────────────
    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());   // 429
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Match the existing ErrorResponse shape used by GlobalExceptionHandler
        // so the frontend sees a consistent { "status": 429, "message": "..." }
        Map<String, Object> body = Map.of(
                "status",  429,
                "message", message
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    // ── Resolve real client IP ────────────────────────────────────────────────
    // Behind a reverse proxy (Nginx, AWS ALB), the real client IP is in the
    // X-Forwarded-For header. getRemoteAddr() would return the proxy's IP —
    // which is the same for every request, defeating per-IP rate limiting.
    //
    // X-Forwarded-For can contain a chain: "client, proxy1, proxy2"
    // We take only the first value (the original client).
    //
    // Note: X-Forwarded-For can be spoofed by clients directly hitting your
    // server. If you're behind a trusted proxy (ALB/Nginx), configure your
    // proxy to always overwrite this header rather than append to it.
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
