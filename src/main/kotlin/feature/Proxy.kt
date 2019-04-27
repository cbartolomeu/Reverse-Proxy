package feature

import connectors.Connector
import connectors.ProxyConnector
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.util.AttributeKey

class Proxy(private val connector: Connector) {

    class Configuration {
        var connector: Connector? = null
    }

    /**
     * Installable feature for [Proxy].
     */
    companion object Feature : ApplicationFeature<Application, Configuration, Proxy> {

        override val key = AttributeKey<Proxy>("Proxy")

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit
        ): Proxy {
            val config = Configuration().apply(configure)
            val feature = Proxy(config.connector ?: ProxyConnector(HttpClient()))

            pipeline.intercept(ApplicationCallPipeline.Call) {
                feature.connector.intercept(call)
            }

            return feature
        }
    }
}