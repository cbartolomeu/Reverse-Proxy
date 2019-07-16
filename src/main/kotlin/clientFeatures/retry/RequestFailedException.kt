package clientFeatures.retry

import org.apache.http.client.HttpResponseException

open class RequestFailedException(status: Int, description: String) : HttpResponseException(status, description)