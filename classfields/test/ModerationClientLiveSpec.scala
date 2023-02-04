package common.clients.moderation.test

import common.clients.moderation.ModerationClient
import common.clients.moderation.impl.ModerationClientLive
import common.clients.moderation.model._
import common.clients.moderation.test.resources.TestData.instancesJson
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import ru.yandex.vertis.moderation.proto.model.{Reason, Service}
import ru.yandex.vertis.moderation.proto.model.Signal.SignalType
import sttp.client3.Response
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.TestAspect.sequential

import java.time.Instant

object ModerationClientLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("ModerationClientLive")(
      testM("parse offers") {
        for {
          res <- ModerationClient.getInstances(Service.AUTORU, Seq("1514693565-d49dca0c", "1514693564-7b48a2c0"))
        } yield assertTrue(
          res.instances.head == Instance(
            "1514693565-d49dca0c",
            "auto_ru_71413434",
            Opinion(OpinionType.Unknown),
            Context(Visibility.VISIBLE)
          ) &&
            res.instances(1) == Instance(
              "1514693564-7b48a2c0",
              "dealer_20101",
              Opinion(OpinionType.Unknown),
              Context(Visibility.DELETED)
            )
        )
      },
      testM("encode signals") {
        val signal = SignalSourceView(
          `type` = SignalType.WARN,
          domain = DomainView(`type` = DomainType.Autoru, value = DomainValue.DefaultAutoru),
          source = SourceView(`type` = SourceType.Manual, application = None, userId = Some("me")),
          detailedReasons = Some(Seq(DetailedReasonView(reason = Reason.REJECT))),
          weight = Some(0.5),
          timestamp = Some(Instant.now.toEpochMilli)
        )
        for {
          _ <- ModerationClient.appendSignals(Service.AUTORU, "obj1", Seq(signal))
        } yield assertTrue(true)
      }

      /** проверила json в дебагере и в запросе в тестинге:
       * [
       * {
       * "type" : "warn",
       * "domain" : {
       * "type" : "autoru",
       * "value" : "DEFAULT_AUTORU"
       * },
       * "source" : {
       * "type" : "manual",
       * "application" : null,
       * "userId" : "me"
       * },
       * "detailedReasons" : [
       * {
       * "reason" : "REJECT"
       * }
       * ],
       * "weight" : 0.5,
       * "timestamp" : 1650375287574
       * }
       * ]
       */

    ) @@ sequential)
      .provideCustomLayerShared((Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpStub) >>> ModerationClientLive.layer))
  }

  private val sttpStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial { case _ =>
    Response.ok(instancesJson)
  }

}
