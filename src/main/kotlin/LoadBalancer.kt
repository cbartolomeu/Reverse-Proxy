import connectors.LoadBalanceConnector
import connectors.ProxyConnector
import connectors.UpstreamNode
import feature.Proxy
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import org.slf4j.event.Level
import strategies.RoundRobin
import java.util.concurrent.atomic.AtomicLong

val hosts = listOf(UpstreamNode("localhost:8081"), UpstreamNode("localhost:8082"))
val next = AtomicLong(0)
fun next(): Int = (next.getAndIncrement() % 2).toInt()

fun Application.loadBalanceModule() {
    val client = HttpClient {
        followRedirects = false
    }
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }
    install(Proxy) {
        connector = LoadBalanceConnector(RoundRobin(hosts)).andAfter(ProxyConnector(client))
    }
}