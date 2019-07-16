package clientFeatures.loadBalancer

import core.UpstreamNode
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class LoadBalancerTest {
    @Test
    fun `Should respond with header host`() {
        val upstreamNode = UpstreamNode("test")
        val client = createClientWithLB(upstreamNode)
        runBlocking {
            val clientCall = client.call("/")
            assertEquals(upstreamNode.host, clientCall.response.content.readUTF8Line())
        }
    }

    class TestStrategy(private val upstreamNode: UpstreamNode) : LoadBalancerStrategy {
        override val hosts: List<UpstreamNode> = listOf(upstreamNode)

        override fun next(req: HttpRequestData): UpstreamNode = upstreamNode
    }

    companion object {
        private fun createClientWithLB(upstreamNode: UpstreamNode) =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        respond(request.url.host)
                    }
                }
                install(LoadBalancer) {
                    strategy = TestStrategy(upstreamNode)
                }
            }
    }
}