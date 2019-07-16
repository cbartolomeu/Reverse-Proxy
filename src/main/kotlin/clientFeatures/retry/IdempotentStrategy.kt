package clientFeatures.retry

import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpMethod

class IdempotentStrategy(override val numberOfRetries: Int) : RetryStrategy {

    override fun canRetry(request: HttpRequest, response: HttpResponse) =
        when (request.method) {
            HttpMethod.Get, HttpMethod.Put, HttpMethod.Head,
            HttpMethod.Delete, HttpMethod.Options -> response.status.value in 500..600
            else -> false
        }

    override fun canRetry(request: HttpRequestBuilder, cause: Throwable) = true
}