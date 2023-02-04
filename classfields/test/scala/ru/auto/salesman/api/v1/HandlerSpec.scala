package ru.auto.salesman.api.v1

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import ru.auto.salesman.api.RoutingSpec

class HandlerSpec extends RoutingSpec {

  val testRoute: Route =
    complete(StatusCodes.OK)

  private val route = new ServiceHandler {

    protected def serviceHandler = testRoute
    override def route: Route = wrapRequest(serviceRoute)
  }.route

  "ANY /service/{service}" should {
    "route to existed service handler" in {
      Get("/service/autoru") ~> Route.seal(route) ~> check {
        status should be(StatusCodes.OK)
      }
      Put("/service/autoru") ~> Route.seal(route) ~> check {
        status should be(StatusCodes.OK)
      }
      Post("/service/autoru") ~> Route.seal(route) ~> check {
        status should be(StatusCodes.OK)
      }
      Delete("/service/autoru") ~> Route.seal(route) ~> check {
        status should be(StatusCodes.OK)
      }
    }
    "not route to non existed service handler" in {
      Get("/service/another") ~> Route.seal(route) ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }
}
