package clientFeatures.circuitBreaker

import clientFeatures.retry.RequestFailedException
import io.ktor.http.HttpStatusCode

class CircuitBreakerOpenException : RequestFailedException(
    HttpStatusCode.ServiceUnavailable.value,
    "Circuit currently open!"
)