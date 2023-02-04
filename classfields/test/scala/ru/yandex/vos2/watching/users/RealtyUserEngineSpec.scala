package ru.yandex.vos2.watching.users

import java.net.URL
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.application.ng.s3.ExtendedS3Client
import ru.yandex.realty.clients.social.SocialClient
import ru.yandex.realty.util.CryptoUtils.Crypto
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Model.InstanceSource
import ru.yandex.vos2.moderation.{
  AgencyProfileModerationClient,
  UserModerationTransportDecider,
  UserRealtyModerationClient
}
import ru.yandex.vos2.UserModel.{AgencyProfile, User}
import ru.yandex.realty.util.getNow
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.moderation.proto.RealtyLight.AgencyCardRealtyEssentials.AgencyCardServiceResolution
import ru.yandex.vos2.dao.offers.OfferDao
import ru.yandex.vos2.dao.users.UserDao
import ru.yandex.vos2.dao.offers.ExactDelay
import ru.yandex.vos2.realty.services.moderation.{AgencyProfileModerationTransportDecider, NotChanged, ShouldBeSent}
import ru.yandex.vos2.services.interfax.InterfaxClient
import ru.yandex.vos2.watching.users.stages.UserProcessingStage

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class RealtyUserEngineSpec extends WordSpec with Matchers with MockitoSupport {

  abstract private class Fixture {
    val userModerationClient = mock[UserRealtyModerationClient]
    val userModerationDecider = mock[UserModerationTransportDecider]
    val agencyProfileModerationClient = mock[AgencyProfileModerationClient]
    val agencyProfileModerationTransportDecider = mock[AgencyProfileModerationTransportDecider]
    val interfaxClient = mock[InterfaxClient]
    val s3Client = mock[ExtendedS3Client]
    val socialClient = mock[SocialClient]
    val brokerClient = mock[BrokerClient]
    val crypto = mock[Crypto]
    val userDao = mock[UserDao]
    val offerDao = mock[OfferDao]

    lazy val engine = new RealtyUserEngine(
      userModerationClient,
      userModerationDecider,
      agencyProfileModerationClient,
      agencyProfileModerationTransportDecider,
      interfaxClient,
      s3Client,
      "realty",
      new URL("http://s3.mdst.yandex.net"),
      socialClient,
      brokerClient,
      crypto,
      offerDao
    ) {
      override protected def pipeline: List[UserProcessingStage] =
        List(userModerationStage, sparkInfoStage, agencyProfileStage)
    }

    when(userModerationDecider.apply(any(), any())).thenReturn(Success(NotChanged))
  }

  private val user = User
    .newBuilder()
    .setUserRef("uid_123")
    .setCurrentAgencyProfile(AgencyProfile.newBuilder().setName("foo"))
    .setHashAgencyProfileModeration("aaabbb")
    .setAgencyProfileEnabled(true)
    .build()

  private val instance = InstanceSource.newBuilder().setVersion(1).build()

  "RealtyUserEngine" should {
    "schedule agency profile sending first time" in new Fixture {
      when(agencyProfileModerationTransportDecider.apply(any()))
        .thenReturn(Success(ShouldBeSent(instance, "cccddd")))

      val update = engine.process(user)
      update.getVisitDelay.get.isFinite shouldBe true
      update.getUpdate.get.hasTimestampAgencyModerationSyncAfter shouldBe true
      update.getUpdate.get.getTimestampAgencyModerationSyncAfter shouldBe >(getNow)
      update.getUpdate.get.hasApprovedAgencyProfile shouldBe false

      verify(agencyProfileModerationClient, never()).push(any())
    }

    "ignore disabled agency profile" in new Fixture {
      val userBuilder = user.toBuilder
      userBuilder.setAgencyProfileEnabled(false)

      val update = engine.process(userBuilder.build())
      update.getVisitDelay shouldBe empty
      update.getUpdate shouldBe empty

      verify(agencyProfileModerationClient, never()).push(any())
    }

    "not schedule if no changes" in new Fixture {
      when(agencyProfileModerationTransportDecider.apply(any())).thenReturn(Success(NotChanged))

      val update = engine.process(user)
      update.getVisitDelay shouldBe empty
      update.getUpdate shouldBe empty

      verify(agencyProfileModerationClient, never()).push(any())
    }

    "reschedule after error" in new Fixture {
      when(agencyProfileModerationTransportDecider.apply(any()))
        .thenReturn(Failure(new RuntimeException("TEST")))

      val update = engine.process(user)
      update.getVisitDelay.get shouldEqual ExactDelay(30.minutes)
      update.getUpdate shouldBe empty

      verify(agencyProfileModerationClient, never()).push(any())
    }

    "reschedule if time not come" in new Fixture {
      when(agencyProfileModerationTransportDecider.apply(any()))
        .thenReturn(Success(ShouldBeSent(instance, "cccddd")))

      val update =
        engine.process(user.toBuilder.setTimestampAgencyModerationSyncAfter(getNow + 1.minute.toMillis).build())
      update.getVisitDelay.get.isFinite shouldBe true
      update.getUpdate shouldBe empty

      verify(agencyProfileModerationClient, never()).push(any())
    }

    "actually send agency profile second time" in new Fixture {
      when(agencyProfileModerationTransportDecider.apply(any()))
        .thenReturn(Success(ShouldBeSent(instance, "cccddd")))
      doNothing().when(agencyProfileModerationClient).push(any())

      val update = engine.process(user.toBuilder.setTimestampAgencyModerationSyncAfter(getNow).build())
      update.getVisitDelay.get.isFinite shouldBe false
      update.getUpdate.get.getHashAgencyProfileModeration shouldEqual "cccddd"
      update.getUpdate.get.hasTimestampAgencyModerationSyncAfter shouldBe false
      update.getUpdate.get.hasApprovedAgencyProfile shouldBe false

      verify(agencyProfileModerationClient).push(any())
    }

    "not send if no changes" in new Fixture {
      when(agencyProfileModerationTransportDecider.apply(any())).thenReturn(Success(NotChanged))

      val update =
        engine.process(user.toBuilder.setTimestampAgencyModerationSyncAfter(getNow).build())
      update.getVisitDelay.get.isFinite shouldBe false
      update.getUpdate.get.hasTimestampAgencyModerationSyncAfter shouldBe false
      update.getUpdate.get.hasApprovedAgencyProfile shouldBe false

      verify(agencyProfileModerationClient, never()).push(any())
    }

    "resend after error" in new Fixture {
      when(agencyProfileModerationTransportDecider.apply(any()))
        .thenReturn(Success(ShouldBeSent(instance, "cccddd")))
      when(agencyProfileModerationClient.push(any())).thenThrow(new RuntimeException("TEST"))

      val update = engine.process(user.toBuilder.setTimestampAgencyModerationSyncAfter(getNow).build())
      update.getVisitDelay.get shouldEqual ExactDelay(5.minutes)
      update.getUpdate shouldBe empty

      verify(agencyProfileModerationClient).push(any())
    }

    "approve without send if only non-verifiable field changed" in new Fixture {
      override val agencyProfileModerationTransportDecider: AgencyProfileModerationTransportDecider =
        AgencyProfileModerationTransportDecider.Default

      val schedule = AgencyProfile.WorkSchedule.newBuilder().setMinutesFrom(1).setMinutesTo(2).build()
      val userBuilder = user.toBuilder
      userBuilder.setApprovedAgencyProfile(userBuilder.getCurrentAgencyProfile)
      userBuilder.getCurrentAgencyProfileBuilder.addWorkSchedule(schedule)
      userBuilder.setTimestampAgencyModerationSyncAfter(getNow)
      val currentProfileHash = agencyProfileModerationTransportDecider
        .profileToHash(userBuilder.build(), userBuilder.getCurrentAgencyProfileBuilder.build())
        .get
      val approvedProfileHash = agencyProfileModerationTransportDecider
        .profileToHash(userBuilder.build(), userBuilder.getApprovedAgencyProfileBuilder.build())
        .get
      currentProfileHash shouldEqual approvedProfileHash
      userBuilder.setHashAgencyProfileModeration(currentProfileHash)

      val update = engine.process(userBuilder.build())
      update.getVisitDelay.get.isFinite shouldBe false
      update.getUpdate.get.hasTimestampAgencyModerationSyncAfter shouldBe false
      update.getUpdate.get.hasApprovedAgencyProfile shouldBe true
      update.getUpdate.get.getApprovedAgencyProfile.getWorkScheduleList.asScala.toList shouldEqual List(schedule)
      update.getUpdate.get.hasCurrentAgencyProfile shouldBe false

      verify(agencyProfileModerationClient, never()).push(any())
    }

    "not approve without send if not only non-verifiable field changed" in new Fixture {
      override val agencyProfileModerationTransportDecider: AgencyProfileModerationTransportDecider =
        AgencyProfileModerationTransportDecider.Default

      val schedule = AgencyProfile.WorkSchedule.newBuilder().setMinutesFrom(1).setMinutesTo(2).build()
      val userBuilder = user.toBuilder
      userBuilder.setApprovedAgencyProfile(userBuilder.getCurrentAgencyProfile)
      userBuilder.getCurrentAgencyProfileBuilder.addWorkSchedule(schedule).setOgrn("12345")
      userBuilder.setTimestampAgencyModerationSyncAfter(getNow)
      val currentProfileHash = agencyProfileModerationTransportDecider
        .profileToHash(userBuilder.build(), userBuilder.getCurrentAgencyProfileBuilder.build())
        .get
      val approvedProfileHash = agencyProfileModerationTransportDecider
        .profileToHash(userBuilder.build(), userBuilder.getApprovedAgencyProfileBuilder.build())
        .get
      currentProfileHash should not be equal(approvedProfileHash)
      userBuilder.setHashAgencyProfileModeration(currentProfileHash)

      val update = engine.process(userBuilder.build())
      update.getVisitDelay.get.isFinite shouldBe false
      update.getUpdate.get.hasTimestampAgencyModerationSyncAfter shouldBe false
      update.getUpdate.get.hasApprovedAgencyProfile shouldBe true
      update.getUpdate.get.getApprovedAgencyProfile.getOgrn shouldEqual ""
      update.getUpdate.get.hasCurrentAgencyProfile shouldBe true

      verify(agencyProfileModerationClient, never()).push(any())
    }
  }
}
