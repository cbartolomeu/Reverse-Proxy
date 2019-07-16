package clientFeatures.retry

import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.response.HttpResponse

interface RetryStrategy {
    val numberOfRetries: Int

    fun canRetry(request: HttpRequest, response: HttpResponse): Boolean
    fun canRetry(request: HttpRequestBuilder, cause: Throwable): Boolean
}