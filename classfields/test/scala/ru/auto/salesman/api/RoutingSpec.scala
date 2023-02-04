package ru.auto.salesman.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directive, Directive0, Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import ru.auto.salesman.api.akkahttp.{SalesmanExceptionHandler, SalesmanRequestContext}
import ru.auto.salesman.api.v1.RequiredHeaders
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.HandlerModelGenerators
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import spray.json.DefaultJsonProtocol

abstract class RoutingSpec
    extends BaseSpec
    // provides implicit exception handler for Route.seal
    with SalesmanExceptionHandler
    with ScalatestRouteTest
    with SprayJsonSupport
    with DefaultJsonProtocol
    with Directives
    with HandlerModelGenerators
    with RequiredHeaders
    with ProtobufSupport {

  def seal(route: Route): Route =
    Route.seal(wrapRequest(route))

  /*
   * SalesmanRequestContext прокидывается из Handler`a на несколько уровней выше,
   * потому нужна обертка
   */
  private def wrapRequest: Directive0 = Directive { inner => ctx =>
    val newCtx = SalesmanRequestContext.wrap(ctx)
    inner.apply(Unit)(newCtx)
  }

  override def testConfig: Config = ConfigFactory.empty()
}
