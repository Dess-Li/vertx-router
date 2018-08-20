package net.deepwit.vertx.annotation

import io.vertx.core.http.HttpMethod

@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class VxRouter(
        val method:Array<String> = [""],
        val url:String,
        val consumes:Array<String>  = [""],
        val produces:Array<String>  = [""],
        val getWithRegex:String = ""
)


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class VxFailureRouter(
        val method:Array<String> = [""],
        val url:String
)