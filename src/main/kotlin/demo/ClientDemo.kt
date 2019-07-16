package demo

import clientFeatures.simpleLogging.SimpleLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main() {
    val client = HttpClient {
        install(SimpleLogging)
    }

    while (true) {
        GlobalScope.launch { client.call("http://localhost:8080") }
        delay(100)
    }
}

/**
 * 1- Teste normal APP-RP-Node1
 * 2- Teste lb APP-RP-Node1/2
 * 3- Teste lb APP-RP-Node1/2(falhar)
 * 4- Teste retry APP-RP-Node1/2(falhar)
 * 5- Teste cb APP-RP-Node1/2(falhar)
 * 6- Teste cb APP-RP-Node1(falhar)/2(falhar)
 */
