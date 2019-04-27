import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.request.host
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.util.filter
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import org.slf4j.event.Level

fun Application.simpleProxyModule() {
    val client = HttpClient {
        followRedirects = false
    }
    install(DefaultHeaders)
    install(CallLogging){
        level = Level.INFO
    }
    intercept(ApplicationCallPipeline.Call) {
        val exchange = client.call(call.request.uri) {
            method = call.request.httpMethod
            host = call.request.host()
            headers.appendAll(call.request.headers)
        }

        val exchangeHeaders = exchange.response.headers
        val location = exchangeHeaders[HttpHeaders.Location]

        println(exchange.response.status)

        if (location != null) call.response.header(HttpHeaders.Location, location)

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
            override val contentLength: Long? = exchange.response.contentLength()
            override val contentType: ContentType? = exchange.response.contentType()
            override suspend fun writeTo(channel: ByteWriteChannel) {
                exchange.response.content.copyAndClose(channel)
            }
        })
    }
}