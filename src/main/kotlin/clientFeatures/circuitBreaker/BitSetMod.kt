package clientFeatures.circuitBreaker

internal class BitSetMod(capacity: Int) {
    private val size: Int
    private val words: LongArray

    init {
        val countOfWordsRequired = wordIndex(capacity - 1) + 1
        size = countOfWordsRequired shl ADDRESS_BITS_PER_WORD
        words = LongArray(countOfWordsRequired)
    }

    operator fun set(bitIndex: Int, value: Boolean) {
        val wordIndex = wordIndex(bitIndex)
        val bitMask = 1L shl bitIndex
        words[wordIndex] = if (value) words[wordIndex] or bitMask else words[wordIndex] and bitMask.inv()
    }

    operator fun get(bitIndex: Int): Boolean {
        val wordIndex = wordIndex(bitIndex)
        val bitMask = 1L shl bitIndex
        return words[wordIndex] and bitMask != 0L
    }

    companion object {

        private const val ADDRESS_BITS_PER_WORD = 6

        private fun wordIndex(bitIndex: Int) = bitIndex shr ADDRESS_BITS_PER_WORD
    }
}