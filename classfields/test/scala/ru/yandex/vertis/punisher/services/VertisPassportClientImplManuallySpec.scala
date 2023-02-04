package ru.yandex.vertis.punisher.services

import cats.effect.{IO, Timer}
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.services.impl.VertisPassportClientImpl
import ru.yandex.vertis.punisher.{AutoruStagesBuilder, BaseSpec}

import scala.jdk.CollectionConverters._

@Ignore
@RunWith(classOf[JUnitRunner])
class VertisPassportClientImplManuallySpec extends BaseSpec {

  implicit val timer: Timer[F] = IO.timer(SameThreadExecutionContext)

  private val client = new VertisPassportClientImpl(AutoruStagesBuilder.vertisPassportConfig)

  "PassportClientImpl" should {
    "getUser" in {

      val user = client.getUser("5").await.get
      val dealer = client.getUser("21714804").await.get

      user.getUser.getProfile.getAutoru.getClientId.nonEmpty shouldBe false
      dealer.getUser.getProfile.getAutoru.getClientId.nonEmpty shouldBe true

      user.getUser.getEmailsList.asScala
        .filter(_.getConfirmed)
        .map(_.getEmail)
        .head shouldBe "slider5@yandex-team.ru"
      dealer.getUser.getEmailsList.asScala
        .filter(_.getConfirmed)
        .map(_.getEmail)
        .head shouldBe "entuziastov@lada-avtogermes.ru"

      client.getUser("555").await shouldBe None
    }

    "moderation" in {
      val reseller = client.getModeration("5").await.get
      val banned = client.getModeration("30707916").await.get

      (banned.containsBans("CARS") &&
        banned.getBansMap.get("CARS").getReasonsList.asScala.contains("WRONG_YEAR")) shouldBe true

      reseller.getReseller shouldBe true

      reseller.hasResellerFlagUpdated shouldBe true

      // TODO: Moderation passport handler does not currently support the NotFoundException
      // client.getUser("555").await shouldBe None shouldBe None
    }
  }
}
