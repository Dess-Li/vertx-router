package net.deepwit.vertx

import io.vertx.ext.web.Route
import kotlin.reflect.full.declaredMemberFunctions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.StaticHandler
import net.deepwit.vertx.annotation.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType


class VxRouter(private val obj:Any) {
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
                when (ann) {
                    is VxAnBodyHandler -> {
                        if (prop.returnType.javaType.typeName == "io.vertx.ext.web.handler.BodyHandler") {
                            val mutableProp = prop.call(obj) as BodyHandler
                            this.vxAnBodyHandlerExpression(router, mutableProp, ann, mainUrl)
                        }
                    }
                    is VxAnStaticHandler -> {
                        if (prop.returnType.javaType.typeName == "io.vertx.ext.web.handler.StaticHandler") {
                            val mutableProp = prop.call(obj) as StaticHandler
                            this.vxAnStaticHandlerExpression(router, mutableProp, ann, mainUrl)
                        }
                    }
                    is VxAnCookieHandler -> {
                        if (prop.returnType.javaType.typeName == "io.vertx.ext.web.handler.CookieHandler") {
                            val mutableProp = prop.call(obj) as CookieHandler
                            this.vxAnCookieHandlerExpression(router, mutableProp, ann, mainUrl)
                        }
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

    private fun vxAnBodyHandlerExpression(router:Router, prop: BodyHandler, ann: VxAnBodyHandler, mainUrl: String) {
        if (ann.url.count() == 0) {
            var route:Route = when (mainUrl.isBlank()) {
                true -> router.route()
                false -> router.route("$mainUrl")
            }
            route = this.checkMethod(route, ann.method)
            route.handler(prop)
            return
        }
        for (it in ann.url) {
            val routeUrl = "$mainUrl$it"
            var route:Route = when (routeUrl.isBlank()) {
                true -> router.route()
                false ->router.route(routeUrl)
            }
            route = this.checkMethod(route, ann.method)
            route.handler(prop)
        }
    }

    private fun vxAnStaticHandlerExpression(router:Router, prop: StaticHandler, ann: VxAnStaticHandler, mainUrl: String) {
        val routeUrl = "$mainUrl${ann.url}"
        var route:Route = when (routeUrl.isBlank()) {
            true -> router.route()
            false -> router.route("$mainUrl${ann.url}")
        }
        route.handler(prop)
    }

    private fun vxAnCookieHandlerExpression(router:Router, prop: CookieHandler, ann: VxAnCookieHandler, mainUrl: String) {
        if (ann.url.count() == 0) {
            var route:Route = when (mainUrl.isBlank()) {
                true -> router.route()
                false -> router.route("$mainUrl")
            }
            route.handler(prop)
            return
        }
        for (it in ann.url) {
            val routeUrl = "$mainUrl$it"
            var route:Route = when (routeUrl.isBlank()) {
                true -> router.route()
                false ->router.route(routeUrl)
            }
            route.handler(prop)
        }
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