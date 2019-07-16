package clientFeatures.retry

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking
import kotlinx.html.currentTimeMillis
import org.apache.http.client.HttpResponseException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RetryTest {
    @Test
    fun `Should retry once with success after an invalid response`() {
        var retried = false
        val client = createClientWithRetry(1) {
            if (retried) respondOk()
            else {
                retried = true
                respondError(HttpStatusCode.ServiceUnavailable)
            }
        }

        runBlocking {
            val clientCall = client.call("/")
            assertEquals(clientCall.response.status, HttpStatusCode.OK)
        }
        assertTrue(retried)
    }

    @Test
    fun `Should retry once with success after an exception`() {
        var retried = false
        val client = createClientWithRetry(1) {
            if (retried) respondOk()
            else {
                retried = true
                throw Exception()
            }
        }

        runBlocking {
            val clientCall = client.call("/")
            assertEquals(clientCall.response.status, HttpStatusCode.OK)
        }
        assertTrue(retried)
    }

    @Test
    fun `Should retry n times and fail`() {
        val nRetries = 2
        val retried = atomic(0)
        val client = createClientWithRetry(nRetries) {
            retried.incrementAndGet()
            respondError(HttpStatusCode.ServiceUnavailable)
        }

        val ex = assertFailsWith<HttpResponseException> {
            runBlocking {
                client.call("/")
            }
        }

        assertEquals("Maximum retries reached", ex.message)
        // First request plus number of retries
        assertEquals(nRetries + 1, retried.value)
    }

    @Test
    fun `Should retry once after time and succeed`() {
        var retried = false
        val delayBetweenRetries: Long = 2000
        val client = createClientWithRetry(1, delayBetweenRetries) {
            if (retried) respondOk()
            else {
                retried = true
                respondError(HttpStatusCode.ServiceUnavailable)
            }
        }

        val beforeRequest = currentTimeMillis()
        runBlocking {
            val clientCall = client.call("/")
            assertEquals(clientCall.response.status, HttpStatusCode.OK)
        }
        val operationTime = currentTimeMillis() - beforeRequest
        assertTrue(operationTime > delayBetweenRetries)
        assertTrue(retried)
    }

    private class TestStrategy(override val numberOfRetries: Int) : RetryStrategy {
        override fun canRetry(request: HttpRequest, response: HttpResponse) = response.status != HttpStatusCode.OK

        override fun canRetry(request: HttpRequestBuilder, cause: Throwable) = true
    }

    companion object {
        private fun createClientWithRetry(
            nRetries: Int,
            time: Long = 0,
            handler: suspend (HttpRequestData) -> HttpResponseData
        ) =
            HttpClient(MockEngine) {
                engine {
                    addHandler(handler)
                }
                install(Retry) {
                    strategy = TestStrategy(nRetries)
                    delayInMillis = time
                }
            }
    }
}
