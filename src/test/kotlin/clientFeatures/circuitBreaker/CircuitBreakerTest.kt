package clientFeatures.circuitBreaker

import core.UpstreamNode
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CircuitBreakerTest {
    @Test
    fun `Should always succeed`() {
        val totalRequests = 10
        val strategy = TestStrategy(CircuitBreakerStrategy.State.CLOSED)
        val client = createClientWithCB(strategy) {
            respondOk()
        }

        runBlocking {
            repeat(totalRequests) {
                val clientCall = client.call(url)
                assertEquals(clientCall.response.status, HttpStatusCode.OK)
            }
        }

        assertEquals(totalRequests, strategy.success.value)
        assertEquals(0, strategy.errors.value)
    }

    @Test
    fun `Should always get error`() {
        val totalRequests = 10
        val strategy = TestStrategy(CircuitBreakerStrategy.State.CLOSED)
        val client = createClientWithCB(strategy) {
            throw TestException()
        }

        runBlocking {
            repeat(totalRequests) {
                assertFailsWith<TestException> {
                    client.call(url)
                }
            }
        }

        assertEquals(totalRequests, strategy.errors.value)
        assertEquals(0, strategy.success.value)
    }

    @Test
    fun `Should get a CircuitBreakerOpenException`() {
        val strategy = TestStrategy(CircuitBreakerStrategy.State.OPEN)
        val client = createClientWithCB(strategy) {
            throw TestException()
        }

        assertFailsWith<CircuitBreakerOpenException> {
            runBlocking {
                client.call(url)
            }
        }

        assertEquals(0, strategy.errors.value)
        assertEquals(0, strategy.success.value)
    }

    /**
     * Strategy just for testing purposes.
     * Strategy receives the state that never changes and
     * register the number of requests and how many of them were successful.
     */
    private class TestStrategy(private val state: CircuitBreakerStrategy.State) : CircuitBreakerStrategy {
        val success = atomic(0)
        val errors = atomic(0)

        override fun tryAcquirePermission(): Boolean = state != CircuitBreakerStrategy.State.OPEN

        override fun onError() {
            errors.incrementAndGet()
        }

        override fun onSuccess() {
            success.incrementAndGet()
        }

        override fun getState(): CircuitBreakerStrategy.State = state

        override fun getCircuitBreakerConfig(): CircuitBreakerConfig = CircuitBreakerConfig()
    }

    private class TestException : Exception()

    companion object {
        private fun createClientWithCB(
            strategy: CircuitBreakerStrategy,
            handler: suspend (HttpRequestData) -> HttpResponseData
        ) =
            HttpClient(MockEngine) {
                engine {
                    addHandler(handler)
                }
                install(CircuitBreaker) {
                    nodes = listOf(CircuitBreakerNode(UpstreamNode("test"), strategy))
                }
            }

        private val url = URL("http", "test", 80, "")
    }
}