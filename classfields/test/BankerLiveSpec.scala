package auto.dealers.booking.scheduler

import common.zio.sttp.endpoint.Endpoint
import ru.auto.api.price_model.KopeckPrice
import auto.dealers.booking.model.{BankerTransactionId, UserId}
import auto.dealers.booking.scheduler.Banker.Banker
import common.zio.sttp.Sttp
import common.zio.sttp.Sttp.ZioSttpBackendStub
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.{Method, StatusCode}
import zio._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object BankerLiveSpec extends DefaultRunnableSpec {

  val queryParams = Map(
    "amount" -> "100"
  )

  private val successResponseStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r
        if r.uri.path.equals("api/1.x/service/autoru/customer/user:123/refund/payment/12345".split('/').toList) &&
          r.uri.params.toMap == queryParams &&
          r.method == Method.POST =>
      Response.ok(())
  }

  private val conflictResponseStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r
        if r.uri.path.equals("api/1.x/service/autoru/customer/user:123/refund/payment/12345".split('/').toList) &&
          r.uri.params.toMap == queryParams &&
          r.method == Method.POST =>
      Response((), StatusCode(409))
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("BankerLive")(
      testM("success when banker response 200") {
        for {
          response <-
            ZIO.accessM[Banker](_.get.refundPayment(UserId("user:123"), BankerTransactionId("12345"), KopeckPrice(100)))
        } yield assert(response)(isUnit)
      }.provideCustomLayer(createEnvironment(successResponseStub)),
      testM("success when banker response 409") {
        for {
          response <-
            ZIO.accessM[Banker](_.get.refundPayment(UserId("user:123"), BankerTransactionId("12345"), KopeckPrice(100)))
        } yield assert(response)(isUnit)
      }.provideCustomLayer(createEnvironment(conflictResponseStub))
    )
  }

  def createEnvironment(
      sttpBackendStub: ZioSttpBackendStub): ZLayer[Any, Nothing, Banker] = {

    (Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpBackendStub)) >>>
      ZLayer.fromServices[Endpoint, Sttp.Service, Banker.Service](
        new BankerLive(_, _)
      )
  }

}
