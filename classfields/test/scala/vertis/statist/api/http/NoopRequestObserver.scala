package vertis.statist.api.http

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Rejection
import common.akka.http.{ApiRequestContext, RequestObserver}

/** @author kusaeva
  */
object NoopRequestObserver extends RequestObserver {
  override def observeTimeout(ctx: ApiRequestContext): Unit = ()

  override def observeResponse(ctx: ApiRequestContext, response: HttpResponse): Unit = ()

  override def observeRejection(ctx: ApiRequestContext, rejections: Seq[Rejection]): Unit = ()

  override def observeException(ctx: ApiRequestContext, ex: Throwable): Unit = ()
}
