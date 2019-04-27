package pipelineVersion

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase

class Proxy(config: Configuration) {

    constructor(providers: List<ProxyProvider>) : this(Configuration(providers))

    constructor() : this(Configuration())

    private var config = config.copy()

    class Configuration(providers: List<ProxyProvider> = emptyList()) {
        internal val providers = ArrayList<ProxyProvider>(providers)

        fun register(provider: ProxyProvider) {
            providers.add(provider)
        }

        fun copy() = Configuration(providers)
    }

    companion object Feature : ApplicationFeature<Application, Configuration, Proxy> {
        override val key = AttributeKey<Proxy>("Proxy")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): Proxy {
            val feature = Proxy().apply {
                config.apply(configure)
            }
            feature.interceptPipeline(pipeline)
            return feature
        }
    }

    fun interceptPipeline(pipeline: ApplicationCallPipeline) {
        val configurations = config.providers

        val proxyPipeline = ProxyPipeline().apply {
            for (provider in configurations) {
                merge(provider.pipeline)
            }
        }

        val phase = PipelinePhase("AfterCall")
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Call, phase)

        pipeline.merge(proxyPipeline)
    }
}