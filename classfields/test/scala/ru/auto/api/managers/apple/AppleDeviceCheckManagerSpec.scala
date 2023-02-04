package ru.auto.api.managers.apple

import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.pushnoy.DeviceInfo
import ru.yandex.passport.model.common.CommonModel.DomainBan
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.util.Request
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Model.Reason

import scala.jdk.CollectionConverters._

class AppleDeviceCheckManagerSpec extends BaseSpec with MockitoSupport {

  private val userModerationStatus: UserModerationStatus = UserModerationStatus.newBuilder().build()

  private val userModerationStatusForReseller: UserModerationStatus = {
    val bans = Map("CARS" -> DomainBan.newBuilder().addReasons(Reason.USER_RESELLER.toString).build)
    UserModerationStatus
      .newBuilder()
      .putAllBans(bans.asJava)
      .build()
  }

  private val passportClient = mock[PassportClient]
  private val passportClientForReaeller = mock[PassportClient]

  when(passportClient.getUserModeration(?)(?)).thenReturnF(userModerationStatus)
  when(passportClientForReaeller.getUserModeration(?)(?)).thenReturnF(userModerationStatusForReseller)

  private val someToken: Option[String] = Some("TOKEN")

  private case class TestCase(description: String,
                              token: Option[String],
                              passportClient: PassportClient,
                              request: Request,
                              expected: Option[DeviceInfo.IosDeviceCheckBits])

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "Without token",
      token = None,
      passportClient = passportClient,
      request = ModelGenerators.privateRequestGen.next,
      expected = None
    ),
    TestCase(
      description = "Not authorized user",
      token = someToken,
      passportClient = passportClient,
      request = ModelGenerators.notAuthorizedRequestGen.next,
      expected = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = false))
    ),
    TestCase(
      description = "Authorized user",
      token = someToken,
      passportClient = passportClient,
      request = ModelGenerators.privateRequestGen.next,
      expected = Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = false))
    ),
    TestCase(
      description = "Authorized dealer",
      token = someToken,
      passportClient = passportClient,
      request = ModelGenerators.dealerRequestGen.next,
      expected = Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = false))
    ),
    TestCase(
      description = "Reseller",
      token = someToken,
      passportClient = passportClientForReaeller,
      request = ModelGenerators.privateRequestGen.next,
      expected = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true))
    )
  )

  "AppleDeviceCheckManager.iosDeviceCheckBits" should {
    testCases.foreach {
      case TestCase(description, token, passportClient, request, expected) =>
        description in {
          val appleDeviceCheckManager = new AppleDeviceCheckManager(passportClient)
          val actual = appleDeviceCheckManager.iosDeviceCheckBits(token.isDefined)(request).futureValue
          actual shouldBe expected
        }
    }
  }
}
