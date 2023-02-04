package ru.yandex.vertis.punisher.services

import java.net.URL
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru
import ru.yandex.vertis.moderation.proto.Model.Reason
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.config.ServiceConfig
import ru.yandex.vertis.punisher.convert.Reasons
import ru.yandex.vertis.punisher.services.ModerationService._
import ru.yandex.vertis.punisher.services.impl.ModerationServiceImpl

import scala.concurrent.duration._

@Ignore
@RunWith(classOf[JUnitRunner])
class ModerationServiceManuallySpec extends BaseSpec {

  private val serviceConfig: ServiceConfig =
    ServiceConfig(
      url = new URL("http://moderation-push-api-01-sas.test.vertis.yandex.net:37158"),
      service = "USERS_AUTORU",
      requestTimeout = 10.seconds,
      readTimeout = 10.seconds,
      maxConnections = 5,
      throttleRequests = 5,
      threadPoolName = "autoru-moderation-client"
    )

  private val service: ModerationService[F] = new ModerationServiceImpl(serviceConfig)
  private val userId = "30798330"
  private val categories = Set(UsersAutoru.CARS)

  "ModerationService" should {
    "user warn signal - reseller" in {
      service.send {
        AutoruSignal(
          signalType = SignalTypes.Warn(),
          externalId = AutoruExternalId(userId),
          detailedReason = Reasons.toDetailedReason(Reason.USER_RESELLER),
          info = Some("some warn info"),
          tag = None,
          categories = categories,
          task = None
        )
      }.await shouldBe Some(SuccessSendResult)
    }

    "user ban signal - reseller" in {
      service.send {
        AutoruSignal(
          signalType = SignalTypes.Ban,
          externalId = AutoruExternalId(userId),
          detailedReason = Reasons.toDetailedReason(Reason.USER_RESELLER),
          info = Some("some ban reseller info"),
          tag = None,
          categories = categories,
          task = None
        )
      }.await shouldBe Some(SuccessSendResult)
    }

    "user ban signal - ban" in {
      service.send {
        AutoruSignal(
          signalType = SignalTypes.Ban,
          externalId = AutoruExternalId(userId),
          detailedReason = Reasons.toDetailedReason(Reason.DO_NOT_EXIST),
          info = Some("some ban do_not_exist info"),
          tag = None,
          categories = categories,
          task = None
        )
      }.await shouldBe Some(SuccessSendResult)
    }
  }
}
