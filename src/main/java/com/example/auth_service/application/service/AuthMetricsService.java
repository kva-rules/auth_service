package com.example.auth_service.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class AuthMetricsService {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter registrationCounter;
    private final Counter tokenRefreshCounter;
    private final AtomicLong activeSessionsGauge;

    public AuthMetricsService(MeterRegistry meterRegistry) {
        this.loginSuccessCounter = Counter.builder("auth.login.success")
                .description("Number of successful logins")
                .register(meterRegistry);

        this.loginFailureCounter = Counter.builder("auth.login.failure")
                .description("Number of failed login attempts")
                .register(meterRegistry);

        this.registrationCounter = Counter.builder("auth.registration")
                .description("Number of user registrations")
                .register(meterRegistry);

        this.tokenRefreshCounter = Counter.builder("auth.token.refresh")
                .description("Number of token refreshes")
                .register(meterRegistry);

        this.activeSessionsGauge = new AtomicLong(0);
        meterRegistry.gauge("auth.sessions.active", activeSessionsGauge);
    }

    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
        activeSessionsGauge.incrementAndGet();
        log.debug("Login success metric incremented");
    }

    public void incrementLoginFailure() {
        loginFailureCounter.increment();
        log.debug("Login failure metric incremented");
    }

    public void incrementRegistrations() {
        registrationCounter.increment();
        log.debug("Registration metric incremented");
    }

    public void incrementTokenRefresh() {
        tokenRefreshCounter.increment();
        log.debug("Token refresh metric incremented");
    }

    public void decrementActiveSessions() {
        activeSessionsGauge.decrementAndGet();
        log.debug("Active sessions decremented");
    }

    public double getLoginSuccessRate() {
        double total = loginSuccessCounter.count() + loginFailureCounter.count();
        if (total == 0) return 0.0;
        return loginSuccessCounter.count() / total * 100;
    }

    public long getActiveSessionCount() {
        return activeSessionsGauge.get();
    }

    public double getTotalLoginAttempts() {
        return loginSuccessCounter.count() + loginFailureCounter.count();
    }

    public double getFailedLoginAttempts() {
        return loginFailureCounter.count();
    }

    public double getTokenRefreshCount() {
        return tokenRefreshCounter.count();
    }
}
