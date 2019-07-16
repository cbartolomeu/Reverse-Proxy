package clientFeatures.circuitBreaker

class Ring(private val size: Int, private val delta: Long) {
    private val bitSet = BitSetMod(size)
    private val times = LongArray(size)
    private var currIndex = -1
    private var validIndex = 0
    @Volatile
    private var cardinality = 0

    @Synchronized
    fun setNextBit(value: Boolean, time: Long): Int {
        currIndex = (currIndex + 1) % size

        times[currIndex] = time
        bitSet[currIndex] = value

        val currTime = System.currentTimeMillis()

        while (validIndex % size < currIndex && times[validIndex] < currTime - delta)
            validIndex++

        validIndex %= size

        var current = if (value) 1 else 0
        var i = validIndex
        while (i++ % size < currIndex) if (bitSet[i]) current++
        cardinality = current
        return cardinality
    }

    // Number of failed requests
    fun getCardinality() = cardinality

    // Number of valid (failed or successed) requests
    fun getLength() = if (validIndex <= currIndex) currIndex - validIndex + 1 else size - validIndex + currIndex + 1

    // Max number of requests
    fun getSize() = size
}