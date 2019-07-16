package clientFeatures.retry

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.delay
import org.apache.http.client.HttpResponseException

class Retry(private val strategy: RetryStrategy, private val delayInMillis: Long) {
    class Config {
        var strategy: RetryStrategy = IdempotentStrategy(2)
        var delayInMillis: Long = 0

        internal fun build(): Retry = Retry(strategy, delayInMillis)
    }

    companion object Feature : HttpClientFeature<Config, Retry> {
        override val key = AttributeKey<Retry>("Retry")
        private val nRetriesKey = AttributeKey<Int>("NumberOfRetries")
        private val requestFailed = RequestFailedException(
            HttpStatusCode.ServiceUnavailable.value,
            HttpStatusCode.ServiceUnavailable.description
        )
        private val maxRetriesException = HttpResponseException(
            HttpStatusCode.ServiceUnavailable.value,
            "Maximum retries reached"
        )

        private fun shouldRetry(feature: Retry, clientCall: HttpClientCall): Boolean {
            return if (feature.strategy.canRetry(clientCall.request, clientCall.response)) {
                if (clientCall.request.attributes[nRetriesKey] <= 0) throw maxRetriesException
                else true
            } else false
        }

        override fun prepare(block: Config.() -> Unit): Retry = Config().apply(block).build()

        override fun install(feature: Retry, scope: HttpClient) {
            val retryPhase = PipelinePhase("RetryPhase")
            scope.requestPipeline.insertPhaseAfter(HttpRequestPipeline.Render, retryPhase)

            scope.requestPipeline.intercept(retryPhase) {
                interceptRetryPhase(scope, feature.delayInMillis)
            }

            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                interceptBeforePhase(feature)
            }
        }

        private suspend fun PipelineContext<Any, HttpRequestBuilder>.interceptRetryPhase(
            scope: HttpClient,
            delayInMillis: Long
        ) {
            try {
                proceed()
            } catch (e: RequestFailedException) {
                delay(delayInMillis)
                val clientCall = scope.execute(context)
                proceedWith(clientCall)
            }
        }

        private suspend fun PipelineContext<Any, HttpRequestBuilder>.interceptBeforePhase(feature: Retry) {
            context.setAttributes {
                getOrNull(nRetriesKey) ?: put(nRetriesKey, feature.strategy.numberOfRetries)
            }

            try {
                val clientCall = proceedWith(subject) as HttpClientCall
                context.setAttributes {
                    put(nRetriesKey, get(nRetriesKey).dec())
                }

                if (shouldRetry(feature, clientCall))
                    throw requestFailed

            } catch (cause: Throwable) {
                if (cause != requestFailed
                    && cause != maxRetriesException
                    && feature.strategy.canRetry(context, cause)
                ) {


                    val requestData = HttpRequestBuilder().apply {
                        takeFrom(context)
                    }.build()

                    context.setAttributes {
                        put(nRetriesKey, get(nRetriesKey).dec())
                    }

                    throw if (requestData.attributes[nRetriesKey] <= 0)
                        maxRetriesException else requestFailed
                } else throw cause
            }
        }
    }
}