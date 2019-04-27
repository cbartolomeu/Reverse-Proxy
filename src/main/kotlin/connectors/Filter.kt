package connectors

import io.ktor.application.ApplicationCall

interface Filter {
    suspend fun test(call: ApplicationCall): Boolean
}