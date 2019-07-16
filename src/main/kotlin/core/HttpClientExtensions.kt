package core

import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.contentType
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.util.filter
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import org.apache.http.client.HttpResponseException

suspend fun HttpClient.forward(call: ApplicationCall, host: String = "") {
    try {
        val exchange = this.call(call.request.uri) {
            method = call.request.httpMethod
            headers.appendAll(getValidHeaders(call.request.headers))
            url.host = host
            body = object : OutgoingContent.WriteChannelContent() {
                override val contentType = call.request.contentType()

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    call.request.receiveChannel().copyAndClose(channel)
                }
            }
        }

        val exchangeHeaders = exchange.response.headers

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val headers: Headers = Headers.build {
                appendAll(getValidHeaders(exchangeHeaders))
            }
            override val status: HttpStatusCode? = exchange.response.status
            override val contentLength = exchange.response.contentLength()
            override val contentType = exchange.response.contentType()
            override suspend fun writeTo(channel: ByteWriteChannel) {
                exchange.response.content.copyAndClose(channel)
            }
        })
    } catch (cause: HttpResponseException) {
        call.respond(HttpStatusCode(cause.statusCode, cause.message ?: ""))
    }
}

private fun getValidHeaders(headers: Headers) =
    headers.filter { key, _ ->
        !key.equals(HttpHeaders.ContentType, true)
                && !key.equals(HttpHeaders.ContentLength, true)
                && !key.equals(HttpHeaders.TransferEncoding, true)
    }