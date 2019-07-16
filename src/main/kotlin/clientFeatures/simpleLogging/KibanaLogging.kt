package clientFeatures.simpleLogging


class KibanaLogging {
    //var logger = LoggerFactory.getLogger(this.javaClass)

    /*companion object : HttpClientFeature<SimpleLogging.Config, SimpleLogging> {
        override val key: AttributeKey<SimpleLogging> = AttributeKey("KibanaLogging")

        override fun install(feature: SimpleLogging, scope: HttpClient) {
            val loggingPhase = PipelinePhase("LoggingPhase")
            scope.requestPipeline.insertPhaseBefore(HttpRequestPipeline.Send, loggingPhase)

            scope.requestPipeline.intercept(loggingPhase) {
            }
        }
    }*/
}