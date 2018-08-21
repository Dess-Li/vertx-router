package net.deepwit.vertx.annotation

@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class VxAnRouter(
        val method:Array<String> = [""],
        val url:String,
        val consumes:Array<String>  = [""],
        val produces:Array<String>  = [""],
        val getWithRegex:String = ""
)


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class VxAnFailureRouter(
        val method:Array<String> = [""],
        val url:String
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class VxAnBodyHandler(
        val method:Array<String> = [""],
        val url:String = ""
)