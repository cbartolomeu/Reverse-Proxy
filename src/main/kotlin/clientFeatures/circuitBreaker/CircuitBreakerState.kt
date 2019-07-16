package clientFeatures.circuitBreaker

interface CircuitBreakerState {
    fun tryAcquirePermission(): Boolean

    fun onError(time: Long)

    fun onSuccess(time: Long)

    fun getState(): CircuitBreakerStrategy.State

}