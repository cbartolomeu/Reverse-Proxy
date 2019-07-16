package appFeatures.throttling

import io.ktor.request.ApplicationRequest
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import java.time.Duration


class RedisStrategy(
    redisUri: RedisURI,
    private val scriptHash: String,
    val toKey: (ApplicationRequest) -> String,
    override val rateLimit: Int,
    override val periodLimit: Duration,
    override val maxPendingRequests: Int
) : ThrottlingStrategy {
    private var asyncCommands: RedisAsyncCommands<String, String>

    init {
        val redisClient = RedisClient.create(redisUri)
        val connection = redisClient.connect()
        asyncCommands = connection.async()
    }

    override fun getKey(req: ApplicationRequest): String = toKey(req)

    override suspend fun acquirePermission(req: ApplicationRequest, timeout: Long): Boolean {
        // Timeout is ignored
        if (timeout != 0L) throw IllegalArgumentException("This strategy does not support timeout")

        val rateLimitKey = getKey(req)
        val totalReqKey = "$rateLimitKey.concurrent"

        val result = asyncCommands.evalsha<String>(
            scriptHash, ScriptOutputType.STATUS,
            arrayOf(rateLimitKey, totalReqKey),
            rateLimit.toString(), periodLimit.toMillis().toString(), maxPendingRequests.toString()
        ).await()

        return result == "OK"
    }

    override fun release(req: ApplicationRequest) {
        asyncCommands.decr("${getKey(req)}.concurrent")
    }
}
