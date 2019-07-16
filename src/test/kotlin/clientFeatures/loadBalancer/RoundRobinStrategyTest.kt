package clientFeatures.loadBalancer

import core.UpstreamNode
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.invoke
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class RoundRobinStrategyTest {
    @Test
    fun `Should split requests between the two nodes`() {
        val nodeA = UpstreamNode("nodeA")
        val nodeB = UpstreamNode("nodeB")
        val nRequests = 50
        val strategy = RoundRobinStrategy(listOf(nodeA, nodeB))

        val requestsToNodeA = atomic(0)
        val requestsToNodeB = atomic(0)

        runBlocking {
            repeat(nRequests) {
                val nextHost = strategy.next(HttpRequestBuilder.invoke().build())
                when (nextHost.host) {
                    nodeA.host -> requestsToNodeA.incrementAndGet()
                    nodeB.host -> requestsToNodeB.incrementAndGet()
                }
            }
        }

        assertEquals(nRequests / 2, requestsToNodeA.value)
        assertEquals(nRequests / 2, requestsToNodeB.value)
    }
}