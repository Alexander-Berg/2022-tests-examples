package ru.yandex.realty.admin.api

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.akka.http.ProtobufSupport

import scala.concurrent.duration._

trait ApiHandlerSpecBase extends WordSpec with ProtobufSupport with ScalatestRouteTest with Matchers {

  import ru.yandex.realty.admin.TestComponent._

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(30.seconds.dilated(system))

  def basePath: String

  protected def url(remainPath: String, query: (String, String)*): Uri =
    Uri(s"$basePath$remainPath").withQuery(Uri.Query(query.toMap))

  protected lazy val route: Route = Route.seal(appRoute)

}
