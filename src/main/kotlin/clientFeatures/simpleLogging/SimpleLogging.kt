package clientFeatures.simpleLogging

import clientFeatures.circuitBreaker.CircuitBreakerOpenException
import clientFeatures.retry.RequestFailedException
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.currentTimeMillis
import java.util.*

class SimpleLogging(val interval: Long) {
    class Config {
        var interval: Long = 3000

        internal fun build(): SimpleLogging =
            SimpleLogging(interval)
    }

    private val nRequests = atomic(0)
    private val success = atomic(0)
    private val timesList = Collections.synchronizedList(LinkedList<Long>())

    private val mon = Object()

    companion object : HttpClientFeature<Config, SimpleLogging> {
        override val key: AttributeKey<SimpleLogging> = AttributeKey("SimpleLogging")

        override fun prepare(block: Config.() -> Unit) = Config().apply(block).build()

        override fun install(feature: SimpleLogging, scope: HttpClient) {
            val loggingPhase = PipelinePhase("LoggingPhase")
            scope.requestPipeline.insertPhaseBefore(HttpRequestPipeline.Send, loggingPhase)

            scope.launch {
                loggingRoutine(feature)
            }

            scope.requestPipeline.intercept(loggingPhase) {
                interceptLoggingPhase(feature)
            }
        }

        private suspend fun loggingRoutine(feature: SimpleLogging) {
            while (true) {
                var percentile: Long
                var avg: Long
                var requests: Int
                var success: Int
                synchronized(feature.mon) {
                    requests = feature.nRequests.value
                    success = feature.success.value
                    feature.nRequests.getAndSet(0)
                    feature.success.getAndSet(0)

                    val sortedTimes = if (feature.timesList.isEmpty()) feature.timesList
                    else feature.timesList.sorted()
                    percentile = if (feature.timesList.isEmpty()) 0
                    else sortedTimes[Math.ceil(0.95 * sortedTimes.size).toInt() - 1]
                    avg = if (feature.timesList.isEmpty()) 0
                    else feature.timesList.reduce(Long::plus) / feature.timesList.size
                    feature.timesList.clear()
                }

                val successRate = String.format(
                    "%.0f",
                    if (requests == 0) 0.0 else (success.toDouble() / requests) * 100
                )
                println("Requests: $requests, SuccessRate: $successRate%, time_avg: ${avg}ms, time_p95: ${percentile}ms")
                delay(feature.interval)
            }
        }

        private suspend fun PipelineContext<Any, HttpRequestBuilder>.interceptLoggingPhase(feature: SimpleLogging) {
            val start = currentTimeMillis()
            try {
                val call = proceed() as HttpClientCall
                feature.nRequests.incrementAndGet()

                if (call.response.status.value in 200..300)
                    feature.success.incrementAndGet()
            } catch (ex: RequestFailedException) {
                if (ex !is CircuitBreakerOpenException) feature.nRequests.incrementAndGet()
                throw ex
            } finally {
                feature.timesList.add(currentTimeMillis() - start)
            }
        }
    }
}