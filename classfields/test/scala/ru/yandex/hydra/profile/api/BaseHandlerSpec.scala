package ru.yandex.hydra.profile.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Rejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import common.akka.http.{ApiRequestContext, OpsDirectives, RequestObserver}
import common.zio.logging.Logging
import common.zio.ops.tracing.Tracing
import common.zio.ops.tracing.Tracing.Tracing
import common.zio.ops.tracing.testkit.TestTracing
import io.opentracing.Tracer
import ru.yandex.hydra.profile.dao.SpecBase
import zio._
import zio.clock.Clock

/** @author zvez
  */
trait BaseHandlerSpec extends SpecBase with ScalatestRouteTest {

  def seal(handler: HttpHandler): Route =
    runSync {
      Tracing.tracer.map { tracer =>
        val wrapper = makeWrapper(tracer)
        wrapper(Route.seal(handler.route))
      }
    }

  def seal(handler: (Tracing.Service, Logging.Service) => Runtime[Clock] => HttpHandler): Route = runSync {
    for {
      rt <- ZIO.runtime[Clock]
      tracing <- ZIO.service[Tracing.Service]
      logger <- ZIO.service[Logging.Service]
      wrapper = makeWrapper(tracing.tracer)
    } yield wrapper(Route.seal(handler(tracing, logger)(rt).route))
  }

  private def makeWrapper(tracer: Tracer) = OpsDirectives.makeWrapper(
    tracer,
    NoopRequestObserver,
    System.nanoTime
  )

  private def runSync[A](io: => RIO[ZEnv with Tracing with Logging.Logging, A]): A =
    Runtime.default.unsafeRunTask(
      io.provideLayer(ZEnv.live ++ TestTracing.noOp ++ Logging.live)
    )

  object NoopRequestObserver extends RequestObserver {
    override def observeTimeout(ctx: ApiRequestContext): Unit = ()

    override def observeResponse(ctx: ApiRequestContext, response: HttpResponse): Unit = ()

    override def observeRejection(ctx: ApiRequestContext, rejections: Seq[Rejection]): Unit = ()

    override def observeException(ctx: ApiRequestContext, ex: Throwable): Unit = ()
  }
}
