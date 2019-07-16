package demo

import appFeatures.simpleLogging.SimpleLogging
import io.ktor.application.install
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.delay

class Node(private val port: Int, private val nodeWindow: NodeWindow) {

    fun start() {
        embeddedServer(Netty, port,
            module = {
                install(SimpleLogging)
                install(Routing) {
                    route("/") {
                        handle {
                            if (nodeWindow.success)
                                context.respond(HttpStatusCode.OK)
                            else {
                                delay(2_000)
                                context.respond(HttpStatusCode.ServiceUnavailable)
                            }
                        }
                    }
                }
            }
        ).start()
    }
}