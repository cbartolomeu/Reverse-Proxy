package connectors

import io.ktor.application.ApplicationCall
import io.ktor.util.AttributeKey

interface Connector {
    companion object {
        val HOST = AttributeKey<UpstreamNode>("HOST")
    }

    suspend fun intercept(call: ApplicationCall)

    fun onlyIf(filter: Filter) = object : Connector {
        override suspend fun intercept(call: ApplicationCall) {
            if (filter.test(call)) this@Connector.intercept(call)
        }
    }

    fun andAfter(other: Connector) = object : Connector {
        override suspend fun intercept(call: ApplicationCall) {
            this@Connector.intercept(call)
            other.intercept(call)
        }
    }
}