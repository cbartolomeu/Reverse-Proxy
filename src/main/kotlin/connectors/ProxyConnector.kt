package connectors

import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
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

class ProxyConnector(private val client: HttpClient) : Connector {

    override suspend fun intercept(call: ApplicationCall) {
        val exchange = client.call(call.request.uri) {
            method = call.request.httpMethod
            if (call.attributes.contains(Connector.HOST)) {
                host = call.attributes[Connector.HOST].url
                headers.append(HttpHeaders.XForwardedHost, call.request.host())
            } else {
                host = call.request.headers[HttpHeaders.XForwardedHost] ?: call.request.host()
            }
            headers.appendAll(call.request.headers.filter { key, _ ->
                key != HttpHeaders.XForwardedHost
            })
            headers[HttpHeaders.XForwardedFor] =
                call.request.headers[HttpHeaders.XForwardedFor] ?: call.request.origin.remoteHost
        }

        val exchangeHeaders = exchange.response.headers

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val headers: Headers = Headers.build {
                appendAll(exchangeHeaders.filter { key, _ ->
                    !key.equals(
                        HttpHeaders.ContentType,
                        ignoreCase = true
                    ) && !key.equals(
                        HttpHeaders.ContentLength,
                        ignoreCase = true
                    ) && !key.equals(HttpHeaders.TransferEncoding, ignoreCase = true)
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
}