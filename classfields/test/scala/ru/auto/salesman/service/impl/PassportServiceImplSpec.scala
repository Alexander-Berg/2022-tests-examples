package ru.auto.salesman.service.impl

import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.salesman.client.PassportClient
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  UserSellerType
}
import ru.auto.salesman.service.PassportService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.util.offer._
import ru.yandex.passport.model.common.CommonModel.{DomainBan, UserModerationStatus}
import ru.yandex.vertis.moderation.proto.Model.Domain.{UsersAutoru => ModerationCategory}

import scala.collection.JavaConverters._

class PassportServiceImplSpec extends BaseSpec with ServiceModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val passportClient = mock[PassportClient]
  val passportService = new PassportServiceImpl(passportClient)

  "PassportClient.userType" should {
    "return reseller type" in {
      forAll(genOffer()) { offer =>
        val bans = Map(
          "CARS" -> DomainBan.newBuilder
            .addAllReasons(List("USER_RESELLER").asJava)
            .build
        ).asJava

        val userModerationStatus =
          UserModerationStatus.newBuilder
            .putAllBans(bans)
            .build

        PassportService.userType(
          Some(userModerationStatus),
          offer.moderationCategory
        ) shouldBe UserSellerType.Reseller
      }
    }

    "return usual type" in {
      forAll(genOffer()) { offer =>
        val bans = Map(
          "NOTCARS" -> DomainBan.newBuilder
            .addAllReasons(List("USER_RESELLER").asJava)
            .build
        ).asJava

        val userModerationStatus =
          UserModerationStatus.newBuilder
            .putAllBans(bans)
            .build

        PassportService.userType(
          Some(userModerationStatus),
          offer.moderationCategory
        ) shouldBe UserSellerType.Usual
      }
    }
  }

  "PassportService.userType" should {

    "return reseller in this category" in {
      mockModerationStatus(
        AutoruUser(33108624),
        Some("CARS" -> "USER_RESELLER")
      )

      passportService
        .userType(AutoruUser(33108624), Some(ModerationCategory.CARS))
        .success
        .value shouldBe UserSellerType.Reseller
    }

    "return usual if user is reseller in another category" in {
      mockModerationStatus(
        AutoruUser(33108624),
        Some("CARS" -> "USER_RESELLER")
      )

      passportService
        .userType(AutoruUser(33108624), Some(ModerationCategory.TRUCK))
        .success
        .value shouldBe UserSellerType.Usual
    }

    "return usual if user isn't reseller at all" in {
      mockModerationStatus(AutoruUser(33108624), status = None)
      passportService
        .userType(AutoruUser(33108624), Some(ModerationCategory.CARS))
        .success
        .value shouldBe UserSellerType.Usual
    }

    "return usual if user's moderation category is unknown" in {
      passportService
        .userType(AutoruUser(33108624), None)
        .success
        .value shouldBe UserSellerType.Usual
    }
  }

  private type BanCategory = String
  private type BanReason = String

  private def mockModerationStatus(
      user: AutoruUser,
      status: Option[(BanCategory, BanReason)]
  ): Unit =
    (passportClient.userModeration _)
      .expects(user.id)
      .returningZ {
        val b = UserModerationStatus
          .newBuilder()
        status.foreach { case (category, reason) =>
          b.putBans(category, DomainBan.newBuilder().addReasons(reason).build())
        }
        b.build()
      }

  private def genOffer(): ApiOfferModel.Offer =
    offerGen(offerCategoryGen = CARS).next

}
