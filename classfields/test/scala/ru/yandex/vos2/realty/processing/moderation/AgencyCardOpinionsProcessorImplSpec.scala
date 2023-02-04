package ru.yandex.vos2.realty.processing.moderation

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers, WordSpec}
import ru.yandex.vertis.moderation.client.ModerationClient
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Opinion.Details.ReasonInfo
import ru.yandex.vertis.moderation.proto.ModerationCommon.EssentialsVersion
import ru.yandex.vos2.UserModel.{AgencyProfile, User}
import ru.yandex.vos2.features.Feature
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.realty.dao.users.RealtyUserDao
import ru.yandex.vos2.realty.processing.Response
import ru.yandex.vos2.realty.services.moderation.AgencyProfileModerationTransportDecider

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import collection.JavaConverters._

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class AgencyCardOpinionsProcessorImplSpec extends WordSpec with MockFactory with ScalaFutures with Matchers {

  abstract class Fixture {
    val feature = mock[Feature]
    val decider = mock[AgencyProfileModerationTransportDecider]
    val userDao = mock[RealtyUserDao]
    val moderationClient = mock[ModerationClient]

    val externalId = Model.ExternalId
      .newBuilder()
      .setVersion(1)
      .setUser(Model.User.newBuilder().setVersion(1).setYandexUser("4040968942"))
      .setObjectId("yandex_uid_4040968942")
      .build()

    val opinions = Model.Opinions
      .newBuilder()
      .setVersion(1)
      .addEntries(
        Model.Opinions.Entry
          .newBuilder()
          .setVersion(1)
          .setDomain(
            Model.Domain
              .newBuilder()
              .setVersion(1)
              .setAgencyCardRealty(Model.Domain.AgencyCardRealty.DEFAULT_AGENCY_CARD_REALTY)
          )
          .setOpinion(
            Model.Opinion
              .newBuilder()
              .setVersion(2)
              .setType(Model.Opinion.Type.OK)
              .setDetails(
                Model.Opinion.Details
                  .newBuilder()
                  .setAgencyCardRealty(
                    Model.Opinion.Details.AgencyCardRealty
                      .newBuilder()
                      .setEssentialsVersion(EssentialsVersion.newBuilder().setHash("aabbcc"))
                  )
              )
          )
      )
      .build()

    val user = User
      .newBuilder()
      .setUserRef("uid_4040968942")
      .setCurrentAgencyProfile(AgencyProfile.newBuilder().setName("unapproved"))
      .addCurrentAgencyProfileReasons(Model.Reason.LOGO_LOW_DEFINITION)
      .build()

    (userDao.useIfExist(_: UserRef)(_: User => Option[AnyRef])).expects(UserRef(4040968942L), *).onCall {
      (ref, action) =>
        Some(action(user))
    }

    lazy val processor = new AgencyCardOpinionsProcessorImpl(
      Model.Service.AGENCY_CARD_REALTY,
      userDao,
      feature,
      decider,
      moderationClient
    ) {

      def processOpinionsPublic(externalId: Model.ExternalId, opinions: OpinionsWrapper): Future[Response] =
        processOpinions(externalId, opinions)
    }

    (feature.isEnabled _).expects().returns(true)
    (decider.profileToHash _).expects(*, *).returns(Some("aabbcc"))
  }

  "AgencyCardOpinionsProcessorImpl" should {
    "handle OK opinions" in new Fixture {
      (userDao.update _).expects(*, *).once().onCall { (user, _) =>
        user.hasCurrentAgencyProfile shouldBe false
        user.hasApprovedAgencyProfile shouldBe true
        user.getCurrentAgencyProfileReasonsList shouldBe empty
      }
      (moderationClient.setContext _).expects(*).returns(Future.successful(()))

      val response = processor.processOpinionsPublic(externalId, OpinionsWrapper(opinions, 1587146469392L)).futureValue
    }

    "handle FAILED opinions" in new Fixture {
      (userDao.update _).expects(*, *).once().onCall { (user, _) =>
        user.hasCurrentAgencyProfile shouldBe true
        user.hasApprovedAgencyProfile shouldBe false
        user.getCurrentAgencyProfileReasonsList.asScala.toSet shouldEqual Set(
          Model.Reason.LOGO_LOW_DEFINITION, // should preserve preceding reasons
          Model.Reason.NAME_NOT_REGISTERED
        )
      }
      (moderationClient.setContext _).expects(*).returns(Future.successful(()))

      val failedOpinion = opinions.toBuilder
      failedOpinion.getEntriesBuilder(0).getOpinionBuilder.setType(Model.Opinion.Type.FAILED)
      failedOpinion
        .getEntriesBuilder(0)
        .getOpinionBuilder
        .getDetailsBuilder
        .getAgencyCardRealtyBuilder
        .addReasonInfo(
          ReasonInfo
            .newBuilder()
            .setEssentialsVersion(EssentialsVersion.newBuilder().setHash("aabbcc").build())
            .setReason(Model.Reason.NAME_NOT_REGISTERED)
            .setIsBan(true)
        )
        .addReasonInfo(
          ReasonInfo
            .newBuilder()
            .setEssentialsVersion(EssentialsVersion.newBuilder().setHash("aabbcc").build())
            .setReason(Model.Reason.LOGO_INAPPROPRIATE)
            .setIsBan(false) // should skip because non-ban reason
        )
        .addReasonInfo(
          ReasonInfo
            .newBuilder()
            .setEssentialsVersion(EssentialsVersion.newBuilder().setHash("outdated").build()) // should skip because autdated hash
            .setReason(Model.Reason.WRONG_CREATE_TIME)
            .setIsBan(true)
        )

      val response =
        processor.processOpinionsPublic(externalId, OpinionsWrapper(failedOpinion.build(), 1587146469392L)).futureValue
    }

    "skip outdated OK opinion" in new Fixture {
      (userDao.update _).expects(*, *).never()

      val outdatedOkOpinion = opinions.toBuilder
      outdatedOkOpinion
        .getEntriesBuilder(0)
        .getOpinionBuilder
        .getDetailsBuilder
        .getAgencyCardRealtyBuilder
        .getEssentialsVersionBuilder
        .setHash("outdated")

      val response = processor
        .processOpinionsPublic(externalId, OpinionsWrapper(outdatedOkOpinion.build(), 1587146469392L))
        .futureValue
    }
  }
}
