package clientFeatures.loadBalancer

import core.UpstreamNode
import io.ktor.client.request.HttpRequestData
import util.orElse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

fun LoadBalancerStrategy.composeWithAffinity(toKey: (HttpRequestData) -> String): LoadBalancerStrategy {
    val map: ConcurrentMap<String, UpstreamNode> = ConcurrentHashMap()

    return object : LoadBalancerStrategy {
        override val hosts: List<UpstreamNode> = this@composeWithAffinity.hosts

        override fun next(req: HttpRequestData): UpstreamNode {
            val key = toKey(req)

            return map[key].orElse {
                val node = this@composeWithAffinity.next(req)
                map[key] = node
                node
            }
        }
    }
}