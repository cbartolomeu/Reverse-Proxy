package clientFeatures.circuitBreaker

interface CircuitBreakerMetrics {
    fun getFailureRate(): Float
    fun getNumberOfBufferedCalls(): Int
    fun getNumberOfFailedCalls(): Int
    fun getMaxNumberOfBufferedCalls(): Int
    fun getNumberOfSuccessfulCalls(): Int
}