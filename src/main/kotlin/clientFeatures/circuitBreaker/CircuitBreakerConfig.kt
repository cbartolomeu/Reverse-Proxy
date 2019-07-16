package clientFeatures.circuitBreaker

import java.time.Duration

data class CircuitBreakerConfig(
    val failureRateThreshold: Float = 50f,
    val waitDurationInOpenState: Duration = Duration.ofSeconds(30),
    val delta: Long = 1_800_000, // only consider requests made in the last 30 mins
    val ringSize: Int = 6,
    val numberOfHalfOpenRequests: Int = 10
)