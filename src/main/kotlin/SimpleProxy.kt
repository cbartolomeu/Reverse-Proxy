import connectors.ProxyConnector
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import org.slf4j.event.Level

fun Application.simpleProxyModule() {
    val client = HttpClient {
        followRedirects = false
    }
    val con = ProxyConnector(client)
    install(DefaultHeaders)
    install(CallLogging){
        level = Level.INFO
    }
    intercept(ApplicationCallPipeline.Call) {
        con.intercept(call)
    }
}