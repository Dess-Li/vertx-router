package net.deepwit.vertx

import io.vertx.core.Handler
import io.vertx.ext.web.Route
import kotlin.reflect.full.declaredMemberFunctions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import net.deepwit.vertx.annotation.*
import org.apache.log4j.Logger
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType


class VxRouter(private val obj:Any) {
    val log = Logger.getLogger(VxRouter::class.java)
    fun expression(router:Router) {
        val clazz = obj::class
        var mainUrl = ""
        clazz.annotations.forEach { ann ->
            if (ann is VxAnRouter) {
                mainUrl += ann.url
            }
        }
        clazz.declaredMemberProperties.forEach { prop ->
            prop.annotations.forEach { ann ->
                if (ann is VxAnHandler) {
                    this.vxAnHandlerExpression(router, prop, ann, mainUrl)
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
            false -> {
                val routeUrl ="$mainUrl${ann.url}"
                when (routeUrl.isBlank()) {
                    true -> router.route()
                    false -> router.route(routeUrl)
                }
            }
        }
        route = this.checkMethod(route, ann.method)
        route = this.checkConsumes(route, ann.consumes)
        route = this.checkProduces(route, ann.produces)
        this.checkHandler(prop)
        route.handler { routingContext -> prop.call(obj, routingContext) }
    }

    private fun vxFailureRouterExpression(router:Router, prop: KFunction<*>, ann: VxAnFailureRouter, mainUrl: String) {
        val routeUrl = "$mainUrl${ann.url}"
        var route:Route = when (routeUrl.isBlank()) {
            true -> router.route()
            false -> router.route("$mainUrl${ann.url}")
        }
        route = this.checkMethod(route, ann.method)
        this.checkHandler(prop)
        route.failureHandler { failureRoutingContext -> prop.call(obj, failureRoutingContext) }
    }

    private fun vxAnHandlerExpression(router:Router, prop: KProperty1<*, *>, ann: VxAnHandler, mainUrl: String) {
        val vxProp = try {
            @Suppress("UNCHECKED_CAST")
            prop.call(obj) as Handler<RoutingContext>
        } catch (e:Exception) {
            this.log.warn("Router handler expression error: $prop")
            null
        } ?: return
        if (ann.url.count() == 0) {
            var route:Route = when (mainUrl.isBlank()) {
                true -> router.route()
                false -> router.route(mainUrl)
            }
            route.handler(vxProp)
            return
        }
        for (it in ann.url) {
            val routeUrl = "$mainUrl$it"
            var route:Route
            if (routeUrl.isBlank()) continue else route = router.route(routeUrl)
            route = this.checkMethod(route, ann.method)
            route.handler(vxProp)
        }
    }

    private fun checkMethod(route:Route, method: Array<String>): Route {
        val methodRead = readHttpMethod(method)
        var checkRoute = route
        if (methodRead.count() == 0) return checkRoute
        for (it in methodRead) {
            checkRoute = checkRoute.method(it)
        }
        return checkRoute
    }

    private fun checkConsumes(route:Route, consumes: Array<String>): Route {
        for (it in consumes) {
            if (it.isBlank()) continue
            route.consumes(it)
        }
        return route
    }

    private fun checkProduces(route:Route, produces: Array<String>): Route {
        for (it in produces) {
            if (it.isBlank()) continue
            route.produces(it)
        }
        return route
    }

    private fun checkHandler(prop: KFunction<*>) {
        if (prop.parameters.count() != 2) throw Exception("Router function param count error: $prop")
        if (prop.parameters[1].type.javaType.typeName != "io.vertx.ext.web.RoutingContext") {
            throw Exception("Router function param type error: $prop")
        }
    }
}
