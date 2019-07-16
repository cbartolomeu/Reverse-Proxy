package appFeatures.throttling

import io.ktor.request.ApplicationRequest
import java.time.Duration

interface ThrottlingStrategy {
    val rateLimit: Int
    val periodLimit: Duration
    val maxPendingRequests: Int

    fun getKey(req: ApplicationRequest): String
    suspend fun acquirePermission(req: ApplicationRequest, timeout: Long = 0): Boolean
    fun release(req: ApplicationRequest)
}

