package pipelineVersion

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.util.pipeline.Pipeline

class ProxyPipeline : Pipeline<Unit, ApplicationCall>(ApplicationCallPipeline.Call)