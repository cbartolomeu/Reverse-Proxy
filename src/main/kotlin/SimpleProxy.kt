import connectors.ProxyConnector
import feature.Proxy
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import org.slf4j.event.Level

fun Application.simpleProxyModule() {
    val client = HttpClient {
        followRedirects = false
    }
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }
    install(Proxy) {
        connector = ProxyConnector(client)
    }
}