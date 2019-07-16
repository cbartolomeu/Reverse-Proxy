package clientFeatures.loadBalancer

import core.UpstreamNode
import io.ktor.client.request.HttpRequestData
import kotlinx.atomicfu.atomic

open class RoundRobinStrategy(override val hosts: List<UpstreamNode>) : LoadBalancerStrategy {
    private val lastHost = atomic(0)

    //override fun next(req: HttpRequestData) = hosts[(lastHost.getAndIncrement() % hosts.size)]
    override fun next(req: HttpRequestData): UpstreamNode {
        val forwardedHosts = req.attributes.getOrNull(LoadBalancer.forwardForKey)
        return if (forwardedHosts == null || forwardedHosts.size == hosts.size)
            hosts[(lastHost.getAndIncrement() % hosts.size)]
        else {
            var res = hosts[(lastHost.getAndIncrement() % hosts.size)]
            while (forwardedHosts.contains(res)) {
                res = hosts[(lastHost.getAndIncrement() % hosts.size)]
            }
            res
        }
    }
}