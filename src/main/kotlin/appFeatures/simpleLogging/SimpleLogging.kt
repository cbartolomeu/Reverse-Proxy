package appFeatures.simpleLogging

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.currentTimeMillis
import java.util.*

class SimpleLogging(var interval: Long = 3000) {
    val nRequests = atomic(0)
    val success = atomic(0)
    val timesList: MutableList<Long> = Collections.synchronizedList(LinkedList<Long>())

    companion object : ApplicationFeature<Application, SimpleLogging, SimpleLogging> {
        private val durationKey = AttributeKey<Long>("duration")
        override val key: AttributeKey<SimpleLogging> = AttributeKey("SimpleLogging")

        override fun install(pipeline: Application, configure: SimpleLogging.() -> Unit): SimpleLogging {
            val feature = SimpleLogging().apply(configure)

            val simpleLoggingPhase = PipelinePhase("SimpleLoggingPhase")
            pipeline.insertPhaseAfter(ApplicationCallPipeline.Call, simpleLoggingPhase)

            pipeline.launch {
                loggingRoutine(feature)
            }

            pipeline.intercept(ApplicationCallPipeline.Setup) {
                interceptSetupPhase()
            }

            pipeline.intercept(simpleLoggingPhase) {
                interceptSimpleLoggingPhase(feature)
            }

            pipeline.intercept(ApplicationCallPipeline.Fallback) {
                interceptFallbackPhase(feature)
            }

            return feature
        }

        private suspend fun loggingRoutine(feature: SimpleLogging) {
            while (true) {
                val avg =
                    if (feature.timesList.isEmpty()) 0
                    else feature.timesList.reduce(Long::plus) / feature.timesList.size
                feature.timesList.clear()
                val requests = feature.nRequests.value
                val success = feature.success.value
                feature.nRequests.getAndSet(0)
                feature.success.getAndSet(0)
                val successRate = String.format(
                    "%.0f",
                    if (requests == 0) 0.0 else (success.toDouble() / requests) * 100
                )
                println("Requests: $requests, Success: $successRate%, time_avg: ${avg}ms")
                delay(feature.interval)
            }
        }

        private fun PipelineContext<Unit, ApplicationCall>.interceptSetupPhase() {
            context.attributes.put(durationKey, currentTimeMillis())
        }

        private fun PipelineContext<Unit, ApplicationCall>.interceptSimpleLoggingPhase(feature: SimpleLogging) {
            feature.nRequests.incrementAndGet()
            if (context.response.status()?.isSuccess() == true) feature.success.incrementAndGet()
        }

        private fun PipelineContext<Unit, ApplicationCall>.interceptFallbackPhase(feature: SimpleLogging) {
            val duration = currentTimeMillis() - context.attributes[durationKey]
            feature.timesList.add(duration)
        }
    }
}