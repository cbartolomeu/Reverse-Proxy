package connectors

import io.ktor.application.ApplicationCall
import strategies.Strategy

class LoadBalanceConnector(private val strategy: Strategy) : Connector {
    override suspend fun intercept(call: ApplicationCall) {
        call.attributes.put(Connector.HOST, strategy.next())
    }
}