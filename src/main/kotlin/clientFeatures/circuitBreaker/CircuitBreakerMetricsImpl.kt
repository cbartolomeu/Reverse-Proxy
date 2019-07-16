package clientFeatures.circuitBreaker

class CircuitBreakerMetricsImpl(ringSize: Int, delta: Long) : CircuitBreakerMetrics {

    private val ring: Ring = Ring(ringSize, delta)

    override fun getNumberOfBufferedCalls() = ring.getLength()

    override fun getMaxNumberOfBufferedCalls() = ring.getSize()

    override fun getFailureRate() = getFailureRate(getNumberOfFailedCalls())

    override fun getNumberOfFailedCalls() = ring.getCardinality()

    override fun getNumberOfSuccessfulCalls() = getNumberOfBufferedCalls() - getNumberOfFailedCalls()

    fun onSuccess(time: Long): Float {
        val currentNumberOfFailedCalls = ring.setNextBit(false, time)
        return getFailureRate(currentNumberOfFailedCalls)
    }

    fun onError(time: Long): Float {
        val currentNumberOfFailedCalls = ring.setNextBit(true, time)
        return getFailureRate(currentNumberOfFailedCalls)
    }

    private fun getFailureRate(numberOfFailedCalls: Int) =
        if (getNumberOfBufferedCalls() == getMaxNumberOfBufferedCalls())
            numberOfFailedCalls * 100.0f / ring.getLength()
        else -1f
}