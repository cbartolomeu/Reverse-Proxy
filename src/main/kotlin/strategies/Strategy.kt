package strategies

import connectors.UpstreamNode

interface Strategy {
    fun next(): UpstreamNode
}