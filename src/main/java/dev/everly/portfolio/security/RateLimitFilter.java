package dev.everly.portfolio.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Profile("prod")
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long MAX_BODY_BYTES = 16_384;
    private static final long TOKENS_PER_MINUTE = 10;
    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final long BURST_TOKENS = 3;
    private static final Duration BURST_WINDOW = Duration.ofSeconds(10);
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/chat");
    }

    private static Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(TOKENS_PER_MINUTE)
                        .refillGreedy(TOKENS_PER_MINUTE, MINUTE))
                .addLimit(limit -> limit
                        .capacity(BURST_TOKENS)
                        .refillGreedy(BURST_TOKENS, BURST_WINDOW))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }
        long len = req.getContentLengthLong();
        if (len > MAX_BODY_BYTES) {
            res.setStatus(413);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"error\":\"Request too large\"}");
            return;
        }

        String clientIp = ClientIpResolver.resolve(req);
        Bucket bucket = buckets.get(clientIp, k -> newBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(
                    1L,
                    (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);

            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}
