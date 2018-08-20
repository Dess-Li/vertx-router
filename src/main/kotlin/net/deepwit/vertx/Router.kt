package net.deepwit.vertx

import io.vertx.ext.web.Route
import kotlin.reflect.full.declaredMemberFunctions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import net.deepwit.vertx.annotation.VxFailureRouter
import net.deepwit.vertx.annotation.VxRouter
import net.deepwit.vertx.annotation.readHttpMethod
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType


class Router(private val obj:Any) {
    fun expression(router:Router) {
        val clazz = obj::class
        var mainUrl: String = ""
        clazz.annotations.forEach { ann ->
            if (ann is VxRouter) {
                mainUrl += ann.url
            }
        }
        clazz.declaredMemberFunctions.forEach { prop ->
            prop.annotations.forEach { ann ->
                when (ann) {
                    is VxRouter -> this.vxRouterExpression(router, prop, ann, mainUrl)
                    is VxFailureRouter -> this.vxFailureRouter(router, prop, ann, mainUrl)
                }
            }
        }
    }

    private fun vxRouterExpression(router:Router, prop: KFunction<*>, ann: VxRouter, mainUrl: String) {
        var route:Route = when (ann.getWithRegex.isBlank()) {
            true -> router.getWithRegex("$mainUrl${ann.getWithRegex}")
            false -> router.route("$mainUrl${ann.url}")
        }
        route = this.checkMethod(route, ann.method)
        route = this.checkConsumes(route, ann.consumes)
        route = this.checkProduces(route, ann.produces)
        this.checkHandler(prop)
        route.handler { routingContext -> prop.call(obj, routingContext) }
    }

    private fun vxFailureRouter(router:Router, prop: KFunction<*>, ann: VxFailureRouter, mainUrl: String) {
        var route:Route = router.route("$mainUrl${ann.url}")
        route = this.checkMethod(route, ann.method)
        this.checkHandler(prop)
        route.failureHandler { failureRoutingContext -> prop.call(obj, failureRoutingContext) }
    }

    private fun checkMethod(route:Route, method: Array<String>): Route {
        val methodRead = readHttpMethod(method)
        var checkRoute = route
        for (it in methodRead) {
            checkRoute = checkRoute.method(it)
        }
        return checkRoute
    }

    private fun checkConsumes(route:Route, consumes: Array<String>): Route {
        var checkRoute = route
        for (it in consumes) {
            checkRoute.consumes(it)
        }
        return checkRoute
    }

    private fun checkProduces(route:Route, produces: Array<String>): Route {
        var checkRoute = route
        for (it in produces) {
            checkRoute.produces(it)
        }
        return checkRoute
    }

    private fun checkHandler(prop: KFunction<*>) {
        if (prop.parameters.count() != 2) throw Exception("Router function param count error: $prop")
        if (prop.parameters[1].type.javaType.typeName != "io.vertx.ext.web.RoutingContext") throw Exception("Router function param type error: $prop")
    }
}