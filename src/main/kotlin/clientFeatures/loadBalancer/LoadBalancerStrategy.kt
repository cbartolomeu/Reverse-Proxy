package clientFeatures.loadBalancer

import core.UpstreamNode
import io.ktor.client.request.HttpRequestData

interface LoadBalancerStrategy {
    val hosts: List<UpstreamNode>
    fun next(req: HttpRequestData): UpstreamNode
}