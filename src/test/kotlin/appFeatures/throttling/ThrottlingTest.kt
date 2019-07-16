package appFeatures.throttling

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.atomicfu.atomic
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals

class ThrottlingTest {

    @Test
    fun `should allow all the requests through`() {
        val strategy = TestStrategy(true)
        val nRequests = 10

        withTestApplication(buildApplication(strategy)) {
            repeat(nRequests) {
                handleRequest(HttpMethod.Get, "/")
            }

            assertEquals(nRequests, strategy.requestsCount.value)
            assertEquals(0, strategy.pendingCount.value)
        }
    }

    @Test
    fun `should respond with Too Many Requests status`() {
        val strategy = TestStrategy(false)

        withTestApplication(buildApplication(strategy)) {
            with(handleRequest(HttpMethod.Get, "/")) {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }

    class TestStrategy(private val allowAcquire: Boolean) : ThrottlingStrategy {
        override val rateLimit: Int = 1
        override val periodLimit: Duration = Duration.ofSeconds(1)
        override val maxPendingRequests: Int = 1

        val requestsCount = atomic(0)
        val pendingCount = atomic(0)

        override fun getKey(req: ApplicationRequest): String = ""

        override suspend fun acquirePermission(req: ApplicationRequest, timeout: Long): Boolean {
            requestsCount.incrementAndGet()
            pendingCount.incrementAndGet()
            return allowAcquire
        }

        override fun release(req: ApplicationRequest) {
            pendingCount.decrementAndGet()
        }

    }

    companion object {
        fun buildApplication(strategy: ThrottlingStrategy): Application.() -> Unit = {
            routing {
                route("/") {
                    install(Throttling) {
                        this.strategy = strategy
                    }
                    handle {
                        respondOk()
                    }
                }
            }
        }
    }
}