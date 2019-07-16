package appFeatures.throttling

import io.ktor.request.ApplicationRequest
import io.ktor.request.host
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import util.orElse
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

class DefaultStrategy(
    val toKey: (ApplicationRequest) -> String = { it.host() },
    override val rateLimit: Int = 50,
    override val periodLimit: Duration = Duration.ofMillis(1_000),
    override val maxPendingRequests: Int = 10
) : ThrottlingStrategy {
    private val map: MutableMap<String, State> = HashMap()
    private val mon = Object()

    override fun getKey(req: ApplicationRequest) = toKey(req)

    override suspend fun acquirePermission(req: ApplicationRequest, timeout: Long): Boolean {
        val key = getKey(req)
        var cf: CompletableFuture<Boolean>
        synchronized(mon) {
            // Happy path
            if (canAcquireNow(key)) return true

            // Check if we can wait
            if (timeout <= 0) return false

            // Wait
            cf = CompletableFuture()
            map[key]?.list?.add(cf)
        }

        return withTimeoutOrNull(timeout) {
            cf.await()
        }.orElse {
            unregister(key, cf)
            false
        }
    }

    override fun release(req: ApplicationRequest) {
        val key = getKey(req)
        synchronized(mon) {
            val state = map[key] ?: return
            with(state) {
                pendingRequests -= 1
                while (list.size > 0) {
                    val cf = list.removeAt(0)
                    if (!cf.isCompletedExceptionally) {
                        cf.complete(true)
                        pendingRequests += 1
                        val currInstant = Instant.now()
                        if (currInstant.isAfter(lastInstant.plus(periodLimit))) {
                            currRate = 1
                            lastInstant = currInstant
                        }
                        return
                    }
                }
            }
        }
    }

    // Remove from waiting queue
    private fun unregister(key: String, cf: CompletableFuture<Boolean>) {
        synchronized(mon) {
            val state = map[key] ?: return
            state.list.remove(cf)
        }
    }

    // Try to acquire without waiting
    private fun canAcquireNow(key: String): Boolean {
        val state = map[key]
        if (state == null) {
            map[key] = State(1, Instant.now(), 1, mutableListOf())
            return true
        }

        val currInstant = Instant.now()
        if (state.pendingRequests < maxPendingRequests) {
            if (currInstant.isAfter(state.lastInstant.plus(periodLimit))) {
                with(state) {
                    currRate = 1
                    pendingRequests += 1
                    lastInstant = currInstant
                }
                return true
            } else if (state.currRate < rateLimit) {
                with(state) {
                    currRate += 1
                    pendingRequests += 1
                }
                return true
            }
        }
        return false
    }

    private data class State(
        var currRate: Int,
        var lastInstant: Instant,
        var pendingRequests: Int,
        val list: MutableList<CompletableFuture<Boolean>>
    )
}