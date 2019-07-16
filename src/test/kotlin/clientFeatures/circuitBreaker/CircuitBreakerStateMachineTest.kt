package clientFeatures.circuitBreaker

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.html.currentTimeMillis
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircuitBreakerStateMachineTest {
    @Test
    fun `Should stay in state closed`() {
        val config = CircuitBreakerConfig(failureRateThreshold = 1f)
        val strategy = CircuitBreakerStateMachine(config)

        repeat(config.ringSize) {
            strategy.onSuccess()
        }

        assertTrue(strategy.tryAcquirePermission())
        assertEquals(CircuitBreakerStrategy.State.CLOSED, strategy.getState())
    }

    @Test
    fun `Should transit to state open because failure rate was exceeded`() {
        val config = CircuitBreakerConfig(failureRateThreshold = 1f)
        val strategy = CircuitBreakerStateMachine(config)

        repeat(config.ringSize - 1) {
            strategy.onSuccess()
        }
        strategy.onError()

        assertFalse(strategy.tryAcquirePermission())
        assertEquals(CircuitBreakerStrategy.State.OPEN, strategy.getState())
    }

    @Test
    fun `Should stay in state closed until the number of requests needed is reached`() {
        val config = CircuitBreakerConfig(failureRateThreshold = 1f, ringSize = 10)
        val strategy = CircuitBreakerStateMachine(config)

        repeat(config.ringSize - 1) {
            strategy.onError()
            assertTrue(strategy.tryAcquirePermission())
            assertEquals(CircuitBreakerStrategy.State.CLOSED, strategy.getState())
        }

        strategy.onError()
        assertFalse(strategy.tryAcquirePermission())
        assertEquals(CircuitBreakerStrategy.State.OPEN, strategy.getState())
    }

    @Test
    fun `Should wait x time in open state before transit to half open state`() {
        val config = CircuitBreakerConfig(waitDurationInOpenState = Duration.ofSeconds(2))
        val strategy = CircuitBreakerStateMachine(config)

        val prev = currentTimeMillis()
        strategy.transitionToOpenState()
        while (strategy.getState() == CircuitBreakerStrategy.State.OPEN) {
            strategy.tryAcquirePermission()
        }
        val waitTime = currentTimeMillis() - prev
        assertTrue(waitTime > config.waitDurationInOpenState.toMillis())
        assertEquals(CircuitBreakerStrategy.State.HALF_OPEN, strategy.getState())
    }

    @Test
    fun `Should transit to closed state when its current state is half open`() {
        val config = CircuitBreakerConfig(numberOfHalfOpenRequests = 10)
        val strategy = CircuitBreakerStateMachine(config)

        strategy.transitionToHalfOpenState()
        repeat(config.numberOfHalfOpenRequests) {
            assertEquals(CircuitBreakerStrategy.State.HALF_OPEN, strategy.getState())
            strategy.tryAcquirePermission()
            strategy.onSuccess()
        }
        assertEquals(CircuitBreakerStrategy.State.CLOSED, strategy.getState())
    }

    @Test
    fun `Should transit to open state when its current state is half open`() {
        val config = CircuitBreakerConfig(numberOfHalfOpenRequests = 100)
        val strategy = CircuitBreakerStateMachine(config)

        strategy.transitionToHalfOpenState()
        assertTrue(strategy.tryAcquirePermission())
        strategy.onError()

        assertEquals(CircuitBreakerStrategy.State.OPEN, strategy.getState())
    }

    @Test
    fun `Should wait again for x requests if the first one exceeded the limit time`() {
        val config = CircuitBreakerConfig(failureRateThreshold = 1f, delta = 2_000, ringSize = 6)
        val strategy = CircuitBreakerStateMachine(config)

        strategy.tryAcquirePermission()
        strategy.onError()

        runBlocking {
            delay(config.delta)

            repeat(config.ringSize - 1) {
                strategy.tryAcquirePermission()
                strategy.onError()
            }
        }

        assertEquals(CircuitBreakerStrategy.State.CLOSED, strategy.getState())
        strategy.tryAcquirePermission()
        strategy.onError()
        assertEquals(CircuitBreakerStrategy.State.OPEN, strategy.getState())
    }
}