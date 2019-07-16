package clientFeatures.circuitBreaker

interface CircuitBreakerStrategy {
    fun tryAcquirePermission(): Boolean
    fun onError()
    fun onSuccess()
    fun getState(): State
    fun getCircuitBreakerConfig(): CircuitBreakerConfig

    enum class State {
        CLOSED, OPEN, HALF_OPEN
    }

}

