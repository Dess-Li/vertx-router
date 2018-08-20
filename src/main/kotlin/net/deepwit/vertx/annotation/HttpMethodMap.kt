package net.deepwit.vertx.annotation

import io.vertx.core.http.HttpMethod

val methodMap = mapOf<String, HttpMethod>(
        "OPTIONS" to HttpMethod.OPTIONS,
        "GET" to HttpMethod.GET,
        "HEAD" to HttpMethod.HEAD,
        "POST" to HttpMethod.POST,
        "PUT" to HttpMethod.PUT,
        "DELETE" to HttpMethod.DELETE,
        "TRACE" to HttpMethod.TRACE,
        "CONNECT" to HttpMethod.CONNECT,
        "PATCH" to  HttpMethod.PATCH,
        "OTHER" to  HttpMethod.OTHER
)

fun readHttpMethod(method: Array<String>): MutableList<HttpMethod> {
    val methodArray:MutableList<HttpMethod> = mutableListOf()
    for (it in method) {
        val httpMethod = methodMap[it.toUpperCase()]
        if (httpMethod !== null) {
            methodArray.add(httpMethod)
        }
    }
    return methodArray
}