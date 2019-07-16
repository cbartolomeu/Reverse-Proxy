package appFeatures.throttling

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext

class Throttling(private val strategy: ThrottlingStrategy, private val timeout: Long) {

    class Configuration {
        var strategy: ThrottlingStrategy = DefaultStrategy()
        var timeout: Long = 0
    }

    companion object : ApplicationFeature<ApplicationCallPipeline, Configuration, Throttling> {
        override val key: AttributeKey<Throttling> = AttributeKey("Throttling")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): Throttling {
            val config = Configuration().apply(configure)
            val feature = Throttling(config.strategy, config.timeout)

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                interceptMonitoringPhase(feature)
            }

            return feature
        }

        private suspend fun PipelineContext<Unit, ApplicationCall>.interceptMonitoringPhase(feature: Throttling) {
            if (!feature.strategy.acquirePermission(context.request, feature.timeout)) {
                call.respond(HttpStatusCode.TooManyRequests)
                return finish()
            }
            proceed()
            feature.strategy.release(context.request)
        }
    }
}