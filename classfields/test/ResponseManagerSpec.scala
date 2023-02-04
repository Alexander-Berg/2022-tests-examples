package amogus.logic

import amogus.model.ValueTypes.RequestId
import common.zio.clients.kv.KvClient._
import common.zio.clients.kv.testkit.KvClientMock
import ru.yandex.vertis.amogus.amo_response.AmoResponse
import zio.clock.Clock
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion.equalTo
import zio.test.mock.Expectation._

import java.util.UUID

object ResponseManagerSpec extends DefaultRunnableSpec {
  val responseId = RequestId(UUID.randomUUID())
  val amoResponse = AmoResponse.defaultInstance

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("ResponseManagerSpec")(
    testM("get should retry NoKeyError") {
      val kvClient = KvClientMock.PolyOutput.of[AmoResponse](
        equalTo("response:" + responseId.value.toString),
        failure(NoKeyError(responseId.value.toString, None))
      ) ++ KvClientMock.PolyOutput.of[AmoResponse](
        equalTo("response:" + responseId.value.toString),
        value(amoResponse)
      )

      val responseDao =
        Clock.live ++
          kvClient >>>
          ResponseManagerLive.layer

      assertM(ResponseManager(_.get(responseId)))(equalTo(Some(amoResponse)))
        .provideCustomLayer(responseDao)
    },
    testM("get should fail on other errors") {
      val kvClient = KvClientMock.PolyOutput.of[AmoResponse](
        equalTo("response:" + responseId.value.toString),
        failure(GeneralError(new Throwable("yo")))
      )

      val responseDao =
        Clock.live ++
          kvClient >>>
          ResponseManagerLive.layer

      assertM(ResponseManager(_.get(responseId)))(equalTo(None))
        .provideCustomLayer(responseDao)
    }
  )
}
