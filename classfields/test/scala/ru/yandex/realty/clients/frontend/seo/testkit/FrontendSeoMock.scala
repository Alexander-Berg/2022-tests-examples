package ru.yandex.realty.clients.frontend.seo.testkit

import ru.yandex.realty.clients.frontend.seo.model.method.SeoMethod
import ru.yandex.realty.clients.frontend.seo.service.FrontendSeo
import ru.yandex.realty.clients.frontend.seo.service.FrontendSeo.FrontendSeo
import ru.yandex.realty.tracing.Traced
import zio.test.Assertion._
import zio.test.{mock, Assertion}
import zio._
import zio.test.mock._
import Expectation._

import scala.annotation.nowarn

object FrontendSeoMock extends Mock[FrontendSeo] {

  case object Send extends Poly.Effect.InputOutput[Throwable]

  override val compose: URLayer[Has[mock.Proxy], FrontendSeo] =
    ZLayer.fromServiceM { proxy =>
      withRuntime.map { //noinspection ScalaUnusedSymbol
        _ =>
          new FrontendSeo.Service {
            override def send[I: Tag, O: Tag](request: I)(implicit method: SeoMethod[I, O]): RIO[Has[Traced], O] =
              proxy(Send.of[I, O], request)
          }
      }
    }

  final class PartiallySend[I: Tag](input: Assertion[I]) {

    @nowarn("msg=parameter value ev in method returns is never used")
    def returns[O: Tag](o: O)(implicit ev: SeoMethod[I, O]): Expectation[FrontendSeo] =
      Send.of[I, O](input, value(o))

  }

  def onSend[I: Tag](e: I): PartiallySend[I] = new PartiallySend[I](equalTo(e))
  def onSend[I: Tag](a: Assertion[I]): PartiallySend[I] = new PartiallySend[I](a)
}
