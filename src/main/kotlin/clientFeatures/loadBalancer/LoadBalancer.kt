package clientFeatures.loadBalancer

import core.UpstreamNode
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext

class LoadBalancer(private val strategy: LoadBalancerStrategy) {
    class Config {
        var strategy: LoadBalancerStrategy =
            RoundRobinStrategy(emptyList())

        internal fun build(): LoadBalancer =
            LoadBalancer(strategy)
    }

    companion object Feature : HttpClientFeature<Config, LoadBalancer> {
        override val key = AttributeKey<LoadBalancer>("LoadBalancer")
        val forwardForKey = AttributeKey<MutableList<UpstreamNode>>("Forward-For")

        override fun prepare(block: Config.() -> Unit): LoadBalancer = Config().apply(block).build()

        override fun install(feature: LoadBalancer, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                interceptBeforePhase(feature)
            }
        }

        private suspend fun PipelineContext<Any, HttpRequestBuilder>.interceptBeforePhase(feature: LoadBalancer) {
            val requestData = HttpRequestBuilder().apply {
                takeFrom(context)
            }.build()

            val next = feature.strategy.next(requestData)

            context.setAttributes {
                val forwardFor = getOrNull(forwardForKey) ?: mutableListOf()
                forwardFor.add(next)
                if (forwardFor.size == 1) put(forwardForKey, forwardFor)
            }

            context.url.host = next.host
            proceed()
        }
    }
}