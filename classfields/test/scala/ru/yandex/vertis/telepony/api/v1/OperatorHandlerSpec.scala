package ru.yandex.vertis.telepony.api.v1

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.BasicDirectives
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.telepony.api.v1.OperatorHandlerSpec.{BlockAuthenticationService, PassAuthenticationService}
import ru.yandex.vertis.telepony.api.{DomainExceptionHandler, DomainMarshalling, RouteTest}
import ru.yandex.vertis.telepony.exception.ForbiddenException
import ru.yandex.vertis.telepony.model.{Credentials, TypedDomain, TypedDomains}
import ru.yandex.vertis.telepony.service.AuthenticationService

import scala.concurrent.Future

/**
  * @author evans
  */
class OperatorHandlerSpec extends RouteTest with Eventually with DomainExceptionHandler with DomainMarshalling {

  trait Test extends BasicDirectives {

    def authenticationService: AuthenticationService

    @volatile
    var unmatchedPath: Path = _

    private val pathExtraction = extractUnmatchedPath { path => http =>
      unmatchedPath = path
      http.complete(StatusCodes.OK)
    }

    val route: Route = new OperatorHandler {
      override def authService: AuthenticationService = authenticationService

      override protected def beelineHandlerRoute: Route = pathExtraction

      override protected def mttHandlerRoute: Route = pathExtraction

      override protected def voxCallbackHandlerRoute: Route = pathExtraction

      override protected def voxCallbackConfirmationHandlerRoute: Route = pathExtraction

      override protected def voxApp2AppHandlerRoute: Route = pathExtraction

      override protected def voxAppBackHandlerRoute: Route = pathExtraction

      override protected def voxHandlerRoute: TypedDomain => Option[Route] = _ => None

      override protected def mtsHandlerRoute: TypedDomain => Option[Route] = {
        case TypedDomains.billing_realty => Some(pathExtraction)
        case _ => None
      }
    }.route

    val credentials = Credentials("billing_realty-testing", "billing_realty")

    val authorization =
      Authorization(BasicHttpCredentials(credentials.username, credentials.password))
  }

  private val PathAndRest =
    (Seq("testing", "stable", "anything_else").map(x => s"/$x/${TypedDomains.billing_realty}/") ++
      Seq("beeline", "mtt", "vox/callback").map(x => s"/operators/$x/")).zipWithIndex.map {
      case (p, i) => s"$p$i" -> i.toString
    }

  "Handler" should {

    PathAndRest.foreach {
      case (path, rest) =>
        s"handle $path and rest = $rest" in new Test with PassAuthenticationService {
          Get(path).withHeaders(authorization) ~> route ~> check {
            status shouldEqual StatusCodes.OK
          }
          eventually {
            unmatchedPath = Path./(rest)
          }
        }
    }

    "fail" in new Test with BlockAuthenticationService {
      Get(s"/testing/${TypedDomains.billing_realty}") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "fail if wrong domain" in new Test with PassAuthenticationService {
      Get("/testing/bad/abc").withHeaders(authorization) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}

object OperatorHandlerSpec {

  trait PassAuthenticationService {

    def authenticationService: AuthenticationService = new AuthenticationService {
      override def validateBeeline(credentials: Credentials): Future[Unit] = Future.unit
      override def validateMtt(credentials: Credentials): Future[Unit] = Future.unit
      override def validateVoxCallback(credentials: Credentials): Future[Unit] = Future.unit
      override def validateVoxApp2App(credentials: Credentials): Future[Unit] = Future.unit
      override def validateMts(domain: TypedDomain, credentials: Credentials): Future[Unit] = Future.unit
      override def validateVox(domain: TypedDomain, credentials: Credentials): Future[Unit] = Future.unit
    }
  }

  trait BlockAuthenticationService {

    def authenticationService: AuthenticationService = new AuthenticationService {

      override def validateBeeline(credentials: Credentials): Future[Unit] =
        Future.failed(ForbiddenException(credentials.username))

      override def validateMtt(credentials: Credentials): Future[Unit] =
        Future.failed(ForbiddenException(credentials.username))

      override def validateMts(domain: TypedDomain, credentials: Credentials): Future[Unit] =
        Future.failed(ForbiddenException(credentials.username))

      override def validateVox(domain: TypedDomain, credentials: Credentials): Future[Unit] =
        Future.failed(ForbiddenException(credentials.username))

      override def validateVoxCallback(credentials: Credentials): Future[Unit] =
        Future.failed(ForbiddenException(credentials.username))

      override def validateVoxApp2App(credentials: Credentials): Future[Unit] =
        Future.failed(ForbiddenException(credentials.username))
    }
  }
}
