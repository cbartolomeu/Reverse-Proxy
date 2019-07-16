package demo

import appFeatures.throttling.Throttling
import clientFeatures.circuitBreaker.CircuitBreaker
import clientFeatures.circuitBreaker.CircuitBreakerNode
import clientFeatures.loadBalancer.LoadBalancer
import clientFeatures.loadBalancer.RoundRobinStrategy
import clientFeatures.retry.IdempotentStrategy
import clientFeatures.retry.Retry
import core.UpstreamNode

object Config {

    val loadBalancerConfig: LoadBalancer.Config.() -> Unit = {
        strategy = RoundRobinStrategy(
            listOf(
                UpstreamNode("localhost:8081"),
                UpstreamNode("localhost:8082")
            )
        )
    }

    val retryConfig: Retry.Config.() -> Unit = {
        strategy = IdempotentStrategy(2)
    }

    val circuitBreakerConfig: CircuitBreaker.Config.() -> Unit = {
        nodes = listOf(
            CircuitBreakerNode(UpstreamNode("localhost:8081")),
            CircuitBreakerNode(UpstreamNode("localhost:8082"))
        )
    }

    val throttlingConfig: Throttling.Configuration.() -> Unit = {

    }
}