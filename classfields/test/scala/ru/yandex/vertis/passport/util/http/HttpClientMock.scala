package ru.yandex.vertis.passport.util.http

import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.vertis.passport.util.tracing.TracingContext

import scala.concurrent.Future

trait HttpClientMock extends ScalaFutures {

  type Handler = Function[HttpRequest, HttpResponse]

  def materializer: ActorMaterializer

  val http = new HttpClient {
    implicit override def materializer: ActorMaterializer = HttpClientMock.this.materializer

    override def singleRequest(req: HttpRequest)(implicit traced: TracingContext) = Future {
      val result = handler(req)
      handler = emptyHandler
      result
    }
  }

  def onRequest(h: Handler): Any = {
    handler = h
  }

  def withEntity[T, A](entity: HttpEntity, um: FromEntityUnmarshaller[T])(f: T => A) = {
    val t = um.apply(entity)(materializer.executionContext, materializer).futureValue
    f(t)
  }

  private def emptyHandler: Handler = {
    case _ => throw new IllegalStateException("Request handler should be set")
  }

  private var handler: Handler = emptyHandler
}
