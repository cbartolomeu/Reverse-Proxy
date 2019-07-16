package clientFeatures.circuitBreaker

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import java.time.Clock
import java.time.Instant

class CircuitBreakerStateMachine(
    circuitBreakerConfig: CircuitBreakerConfig
) : CircuitBreakerStrategy {

    private val config = circuitBreakerConfig
    private val stateReference: AtomicRef<CircuitBreakerState> = atomic(ClosedState())
    private val clock: Clock = Clock.systemUTC()


    override fun tryAcquirePermission(): Boolean = stateReference.value.tryAcquirePermission()

    override fun onError() {
        stateReference.value.onError(System.currentTimeMillis())
    }

    override fun onSuccess() {
        stateReference.value.onSuccess(System.currentTimeMillis())
    }

    override fun getState(): CircuitBreakerStrategy.State = stateReference.value.getState()

    override fun getCircuitBreakerConfig(): CircuitBreakerConfig = config

    fun transitionToClosedState() {
        stateReference.getAndSet(ClosedState())
    }

    fun transitionToOpenState() {
        stateReference.getAndSet(OpenState())
    }

    fun transitionToHalfOpenState() {
        stateReference.getAndSet(HalfOpen())
    }

    private inner class ClosedState : CircuitBreakerState {
        private val failureRateThreshold: Float = config.failureRateThreshold
        private val metrics = CircuitBreakerMetricsImpl(config.ringSize, config.delta)

        override fun tryAcquirePermission(): Boolean = true

        override fun onError(time: Long) = checkFailureRate(metrics.onError(time))

        override fun onSuccess(time: Long) = checkFailureRate(metrics.onSuccess(time))

        override fun getState(): CircuitBreakerStrategy.State = CircuitBreakerStrategy.State.CLOSED

        private fun checkFailureRate(currentFailureRate: Float) {
            if (currentFailureRate >= failureRateThreshold)
                transitionToOpenState()
        }
    }

    private inner class OpenState : CircuitBreakerState {
        private val retryAfterWaitDuration: Instant = clock.instant().plus(config.waitDurationInOpenState)

        override fun tryAcquirePermission(): Boolean {
            if (clock.instant().isAfter(retryAfterWaitDuration)) {
                transitionToHalfOpenState()
                return true
            }
            return false
        }

        override fun onError(time: Long) {}

        override fun onSuccess(time: Long) {}

        override fun getState(): CircuitBreakerStrategy.State = CircuitBreakerStrategy.State.OPEN

    }

    private inner class HalfOpen : CircuitBreakerState {
        private val nRequests = atomic(config.numberOfHalfOpenRequests)

        override fun tryAcquirePermission() = nRequests
            .getAndUpdate { curr -> if (curr == 0) curr else curr - 1 } > 0

        override fun onError(time: Long) = transitionToOpenState()

        override fun onSuccess(time: Long) {
            if (nRequests.value == 0) transitionToClosedState()
        }

        override fun getState(): CircuitBreakerStrategy.State = CircuitBreakerStrategy.State.HALF_OPEN
    }
}
