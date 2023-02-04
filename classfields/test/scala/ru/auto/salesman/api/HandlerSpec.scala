package ru.auto.salesman.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import ru.auto.salesman.api.v1.ApiSwaggerInfoHandler
import ru.yandex.vertis.akka.http.swagger.SwaggerHandler

class HandlerSpec extends RoutingSpec {

  private val testRoute = complete(StatusCodes.OK)

  private val route = new RootHandler {
    override protected val v1HandlerRoute: Route = testRoute

    override protected lazy val apiInfoHandler: ApiSwaggerInfoHandler =
      new ApiSwaggerInfoHandler

    override protected lazy val swaggerHandler: SwaggerHandler =
      new SwaggerHandler
  }.route

  "/api/1.x" should {
    "route to api version 1.x" in {
      Get("/api/1.x") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "/api/1.x/" should {
    "route to api version 1.x" in {
      Get("/api/1.x/") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "/api/unknown" should {
    "not route to unknown resource" in {
      Get("/api/unknown/") ~> Route.seal(route) ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

  "/swagger/" should {
    "provide Swagger UI" in {
      Get("/swagger/") ~> Route.seal(route) ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  implicit def actorRefFactory = system
}
