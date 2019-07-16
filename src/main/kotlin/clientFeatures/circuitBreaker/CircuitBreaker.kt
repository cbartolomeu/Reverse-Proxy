package clientFeatures.circuitBreaker

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase

class CircuitBreaker(private val nodes: List<CircuitBreakerNode>) {
    class Config {
        var nodes: List<CircuitBreakerNode> = emptyList()

        internal fun build(): CircuitBreaker = CircuitBreaker(nodes)
    }

    companion object : HttpClientFeature<Config, CircuitBreaker> {
        override val key: AttributeKey<CircuitBreaker> = AttributeKey("CircuitBreaker")
        private val circuitBreakerException = CircuitBreakerOpenException()

        private val map = HashMap<String, CircuitBreakerStrategy>()

        override fun prepare(block: Config.() -> Unit) = Config().apply(block).build()

        override fun install(feature: CircuitBreaker, scope: HttpClient) {
            feature.nodes.forEach { map[it.node.host] = it.circuitBreakerStrategy }

            val circuitBreakerPhase = PipelinePhase("CircuitBreakerPhase")
            scope.requestPipeline.insertPhaseBefore(HttpRequestPipeline.Send, circuitBreakerPhase)

            scope.requestPipeline.intercept(circuitBreakerPhase) {
                interceptCircuitBreakerPhase()
            }
        }

        private suspend fun PipelineContext<Any, HttpRequestBuilder>.interceptCircuitBreakerPhase() {
            val cb = map[context.url.host]
            if (cb != null && !cb.tryAcquirePermission())
                throw circuitBreakerException
            try {
                proceed()
                cb?.onSuccess()
            } catch (e: Throwable) {
                cb?.onError()
                throw e
            }
        }
    }
}