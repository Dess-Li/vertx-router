package net.deepwit.vertx

import io.vertx.ext.web.Route
import kotlin.reflect.full.declaredMemberFunctions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import net.deepwit.vertx.annotation.VxAnBodyHandler
import net.deepwit.vertx.annotation.VxAnFailureRouter
import net.deepwit.vertx.annotation.VxAnRouter
import net.deepwit.vertx.annotation.readHttpMethod
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType


class VxRouter(private val obj:Any) {
    fun expression(router:Router) {
        println("start")
        val clazz = obj::class
        var mainUrl = ""
        clazz.annotations.forEach { ann ->
            if (ann is VxAnRouter) {
                mainUrl += ann.url
            }
        }
        clazz.declaredMemberProperties.forEach { prop ->
            prop.annotations.forEach { ann ->
                if (prop.returnType.javaType.typeName == "io.vertx.ext.web.handler.BodyHandler") {
                    val mutableProp = prop.call(obj) as BodyHandler
                    when (ann) {
                        is VxAnBodyHandler -> this.vxAnBodyHandlerExpression(router, mutableProp, ann, mainUrl)
                    }
                }
            }
        }
        clazz.declaredMemberFunctions.forEach { prop ->
            prop.annotations.forEach { ann ->
                when (ann) {
                    is VxAnRouter -> this.vxRouterExpression(router, prop, ann, mainUrl)
                    is VxAnFailureRouter -> this.vxFailureRouterExpression(router, prop, ann, mainUrl)
                }
            }
        }
    }

    private fun vxRouterExpression(router:Router, prop: KFunction<*>, ann: VxAnRouter, mainUrl: String) {
        var route:Route = when (ann.getWithRegex.isNotBlank()) {
            true -> router.getWithRegex("$mainUrl${ann.getWithRegex}")
            false -> router.route("$mainUrl${ann.url}")
        }
        route = this.checkMethod(route, ann.method)
        route = this.checkConsumes(route, ann.consumes)
        route = this.checkProduces(route, ann.produces)
        this.checkHandler(prop)
        route.handler { routingContext -> prop.call(obj, routingContext) }
    }

    private fun vxFailureRouterExpression(router:Router, prop: KFunction<*>, ann: VxAnFailureRouter, mainUrl: String) {
        var route:Route = router.route("$mainUrl${ann.url}")
        route = this.checkMethod(route, ann.method)
        this.checkHandler(prop)
        route.failureHandler { failureRoutingContext -> prop.call(obj, failureRoutingContext) }
    }

    private fun vxAnBodyHandlerExpression(router:Router, prop: BodyHandler, ann: VxAnBodyHandler, mainUrl: String) {
        var route:Route = router.route("$mainUrl${ann.url}")
        route = this.checkMethod(route, ann.method)
        route.handler(prop)
    }

    private fun checkMethod(route:Route, method: Array<String>): Route {
        val methodRead = readHttpMethod(method)
        var checkRoute = route
        if (methodRead == null) return checkRoute
        for (it in methodRead) {
            checkRoute = checkRoute.method(it)
        }
        return checkRoute
    }

    private fun checkConsumes(route:Route, consumes: Array<String>): Route {
        var checkRoute = route
        for (it in consumes) {
            if (it.isBlank()) continue
            checkRoute.consumes(it)
        }
        return checkRoute
    }

    private fun checkProduces(route:Route, produces: Array<String>): Route {
        var checkRoute = route
        for (it in produces) {
            if (it.isBlank()) continue
            checkRoute.produces(it)
        }
        return checkRoute
    }

    private fun checkHandler(prop: KFunction<*>) {
        if (prop.parameters.count() != 2) throw Exception("Router function param count error: $prop")
        if (prop.parameters[1].type.javaType.typeName != "io.vertx.ext.web.RoutingContext")
            throw Exception("Router function param type error: $prop")
    }
}