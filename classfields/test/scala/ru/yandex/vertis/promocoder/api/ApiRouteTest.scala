package ru.yandex.vertis.promocoder.api

import akka.http.scaladsl.server.{Directive, Directive0, Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

trait ApiRouteTest extends Matchers with AnyWordSpecLike with ScalatestRouteTest with Directives {

  def seal(route: Route): Route =
    wrapRequest {
      extractRequestContext { rc =>
        Route.seal(route)(
          rc.settings,
          rc.parserSettings,
          exceptionHandler = DomainExceptionHandler.specificExceptionHandler
        )
      }
    }

  private def wrapRequest: Directive0 = Directive { inner => ctx =>
    val newCtx = PromocoderRequestContext.wrap(ctx)
    inner.apply(())(newCtx)
  }

}
