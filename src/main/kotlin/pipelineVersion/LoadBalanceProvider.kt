package pipelineVersion

import connectors.UpstreamNode
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.call
import io.ktor.client.request.host
import io.ktor.features.origin
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.util.filter
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import strategies.RoundRobin
import strategies.Strategy

class LoadBalanceProvider : ProxyProvider() {
    var strategy: Strategy = RoundRobin(emptyList())

    private val client = HttpClient {
        followRedirects = false
    }

    fun run() {
        pipeline.intercept(ApplicationCallPipeline.Call) {
            //do {
            try {
                val nextHost = strategy.next()
                val exchange = exchange(call, nextHost)
                respond(call, exchange)
            } catch (e: Exception) {
                //ignored
            }
            //} while(true);
        }
    }

    private suspend fun respond(call: ApplicationCall, exchange: HttpClientCall) {
        val exchangeHeaders = exchange.response.headers
        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val headers: Headers = Headers.build {
                appendAll(exchangeHeaders.filter { key, _ ->
                    key != HttpHeaders.ContentType
                            && key != HttpHeaders.ContentLength
                            && key != HttpHeaders.TransferEncoding
                })
            }
            override val status: HttpStatusCode? = exchange.response.status
            override val contentLength = exchange.response.contentLength()
            override val contentType = exchange.response.contentType()
            override suspend fun writeTo(channel: ByteWriteChannel) {
                exchange.response.content.copyAndClose(channel)
            }
        })
    }

    private suspend fun exchange(call: ApplicationCall, nextHost: UpstreamNode) =
        client.call(call.request.uri) {
            method = call.request.httpMethod
            host = nextHost.url
            headers.append(HttpHeaders.XForwardedHost, call.request.host())
            headers.appendAll(call.request.headers.filter { key, _ ->
                key != HttpHeaders.XForwardedHost
            })
            headers[HttpHeaders.XForwardedFor] =
                call.request.headers[HttpHeaders.XForwardedFor] ?: call.request.origin.remoteHost
        }
}

fun Proxy.Configuration.loadBalance(configure: LoadBalanceProvider.() -> Unit) {
    val provider = LoadBalanceProvider().apply(configure)
    provider.run()
    register(provider)
}