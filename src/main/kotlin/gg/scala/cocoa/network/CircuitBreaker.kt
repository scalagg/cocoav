package gg.scala.cocoa.network

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import java.time.Duration

/**
 * @author GrowlyX
 * @since 8/9/2024
 */
val vpnDetectorAPICircuitBreakerConfig = CircuitBreakerConfig.custom()
    .failureRateThreshold(50F)
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .permittedNumberOfCallsInHalfOpenState(5) // Number of calls allowed in half-open state
    .slidingWindowSize(10)
    .build()

val vpnDetectorAPICircuitBreakerRegistry = CircuitBreakerRegistry.of(vpnDetectorAPICircuitBreakerConfig)
val vpnDetectorAPICircuitBreaker = vpnDetectorAPICircuitBreakerRegistry.circuitBreaker("vpnDetectorAPI")
