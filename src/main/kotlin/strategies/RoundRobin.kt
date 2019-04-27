package strategies

import connectors.UpstreamNode
import java.util.concurrent.atomic.AtomicLong

class RoundRobin(private val hosts: List<UpstreamNode>) : Strategy {

    private val lastHost = AtomicLong(0)

    override fun next() = hosts[(lastHost.getAndIncrement() % hosts.size).toInt()]
}