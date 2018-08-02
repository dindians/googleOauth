package com.example

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respond

internal suspend fun PipelineContext<*, ApplicationCall>.failWithBadRequest(throwable: Throwable) = failWithStatusCode(throwable, HttpStatusCode.BadRequest)

internal suspend fun <R> PipelineContext<*, ApplicationCall>.tryFailWithStatusCode(block: suspend () -> R, httpStatusCode: HttpStatusCode): R? {
    return try {
        block()
    } catch (throwable: Throwable) {
        failWithStatusCode(throwable, httpStatusCode)
        null
    }
}

private suspend fun PipelineContext<*, ApplicationCall>.failWithStatusCode(throwable: Throwable, httpStatusCode:HttpStatusCode) {
    with(call) {
        response.status(httpStatusCode)
        respond(mapOf("exceptionType" to throwable::class.qualifiedName, "exceptionDetails" to throwable, "request" to "${request.httpMethod.value} ${request.uri}"))
    }
}