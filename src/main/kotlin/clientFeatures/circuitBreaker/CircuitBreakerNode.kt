package clientFeatures.circuitBreaker

import core.UpstreamNode

data class CircuitBreakerNode(val node: UpstreamNode, val circuitBreakerStrategy: CircuitBreakerStrategy) {
    constructor(node: UpstreamNode) :
            this(node, CircuitBreakerStateMachine(CircuitBreakerConfig()))
}