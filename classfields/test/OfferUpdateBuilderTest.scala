package ru.yandex.vertis.general.hammer.logic.test

import general.common.seller_model.SellerId
import general.gost.offer_model.OfferStatusEnum.OfferStatus._
import general.gost.offer_model._
import general.hammer.storage.model.{OfferModerationInfo, OfferStatus}
import general.hammer.storage.model.ExactOfferStatus.{OfferStatus => ExactOfferStatus}
import general.users.model.UserView
import ru.yandex.vertis.general.clients.router.{Routable, Route, RouterClient}
import ru.yandex.vertis.general.common.dictionaries.BansDictionaryService
import ru.yandex.vertis.general.common.resources.ban_reasons.BanReasonSnapshot
import ru.yandex.vertis.general.common.resources.ban_reasons.testkit.Data
import ru.yandex.vertis.general.hammer.logic.offers.OfferUpdateBuilder
import ru.yandex.vertis.general.hammer.model.NotificationBatchElem.{EmailBatchElem, PushBatchElem}
import ru.yandex.vertis.general.hammer.model._
import ru.yandex.vertis.general.users.testkit.TestUserService
import zio.test.Assertion.{hasField, _}
import zio.test.{DefaultRunnableSpec, _}
import zio.{Task, UIO, ZIO, ZLayer}

object OfferUpdateBuilderTest extends DefaultRunnableSpec {

  private val nonEditableBan = ModerationInfo(
    Some(BanDescription(reasons = Seq(BanReasonDescription(code = Data.wrongPhoto.code)), offerEditable = false))
  )

  private val editableBan = ModerationInfo(
    Some(BanDescription(reasons = Seq(BanReasonDescription(code = Data.editableReason.code)), offerEditable = true))
  )

  private val inactiveInfo = InactiveInfo(
    reason = InactiveInfo.Reason.ModerationRecall(ModerationRecallInfo(reasonCode = Data.sold.code))
  )

  private val offer =
    OfferView(sellerId = Some(SellerId(SellerId.SellerId.UserId(100))), offerId = "offer", version = 100)
  private val existingInfo = OfferModerationInfo(exactStatus = ExactOfferStatus.ACTIVE, offerEditable = true)

  private object ExpectedBanCodes {
    val offerBan = "OFFER_BAN_BATCHED_SENDING"
    val offerUnban = "OFFER_UNBAN_BATCHED_SENDING"
    val offerExpired = "OFFER_EXPIRED_BATCHED_SENDING"
  }

  private def assertUpdate(
      result: ModerationOfferUpdate,
      eventName: String,
      banTitle: String,
      sellerId: Option[SellerId]): TestResult = {
    assert(result)(
      isSubtype[StatusChangedBatch](
        hasField(
          "email",
          (_: StatusChangedBatch).email,
          isSome[EmailBatchElem](
            hasField("eventName", (_: EmailBatchElem).eventName, equalTo(eventName)) &&
              hasField("banReasonDescription", (_: EmailBatchElem).banReasonTitle, equalTo(banTitle))
          )
        ) &&
          hasField(
            "push",
            (_: StatusChangedBatch).push,
            isSome[PushBatchElem](
              hasField("userId", (_: PushBatchElem).userId, equalTo(sellerId.get.getUserId)) &&
                hasField("eventName", (_: PushBatchElem).eventName, equalTo(eventName))
            )
          )
      )
    )
  }

  override def spec =
    suite("OfferUpdateBuilder")(
      testM("ban offer if prev is none") {
        val prev = None
        val newOffer = offer.withStatus(BANNED).withModeration(nonEditableBan)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerBan, Data.wrongPhoto.titleLk, offer.sellerId)
        }
      },
      testM("ban offer if prev is ok") {
        val prev = Some(existingInfo)
        val newOffer = offer.withStatus(BANNED).withModeration(nonEditableBan)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerBan, Data.wrongPhoto.titleLk, offer.sellerId)
        }
      },
      testM("ban offer if prev is editable") {
        val prev = Some(existingInfo.withExactStatus(ExactOfferStatus.BANNED).withOfferEditable(true))
        val newOffer = offer.withStatus(BANNED).withModeration(nonEditableBan)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerBan, Data.wrongPhoto.titleLk, offer.sellerId)
        }
      },
      testM("ban offer if prev is inactive") {
        val prev = Some(existingInfo.withExactStatus(ExactOfferStatus.INACTIVE))
        val newOffer = offer.withStatus(BANNED).withModeration(nonEditableBan)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerBan, Data.wrongPhoto.titleLk, offer.sellerId)
        }
      },
      testM("expire offer") {
        val offerExpiredInactiveInfo = InactiveInfo(
          reason = InactiveInfo.Reason.ModerationRecall(ModerationRecallInfo(reasonCode = Data.offerExpired.code))
        )
        val prev = None
        val newOffer = offer.withStatus(INACTIVE).withInactiveInfo(offerExpiredInactiveInfo)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerExpired, Data.offerExpired.titleLk, offer.sellerId)
        }
      },
      testM("unban offer if prev is banned") {
        val prev = Some(existingInfo.withExactStatus(ExactOfferStatus.BANNED))
        val newOffer = offer.withStatus(ACTIVE)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerUnban, Data.offerUnban.titleLk, offer.sellerId)

        }
      },
      testM("unban offer if prev is inactive") {
        val prev = Some(existingInfo.withExactStatus(ExactOfferStatus.INACTIVE))
        val newOffer = offer.withStatus(ACTIVE)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerUnban, Data.offerUnban.titleLk, offer.sellerId)
        }
      },
      testM("deactivate offer") {
        val prev = None
        val newOffer = offer.withStatus(INACTIVE).withInactiveInfo(inactiveInfo)
        OfferUpdateBuilder.build(prev, newOffer).map {
          assertUpdate(_, ExpectedBanCodes.offerBan, Data.sold.titleLk, offer.sellerId)
        }
      },
      testM("update if status is ok, but stored none") {
        val prev = None
        val newOffer = offer
        OfferUpdateBuilder.build(prev, newOffer).map { result =>
          assert(result)(equalTo(StatusChangedBatch(None, None)))
        }
      },
      testM("skip if status is ok and stored ok") {
        val prev = Some(existingInfo)
        val newOffer = offer
        OfferUpdateBuilder.build(prev, newOffer).map { result =>
          assert(result)(equalTo(NotChanged))
        }
      },
      testM("skip if status is banned(non_editable) and stored banned(non_editable)") {
        val prev = Some(existingInfo.withExactStatus(ExactOfferStatus.BANNED).withOfferEditable(false))
        val newOffer = offer.withStatus(BANNED).withModeration(nonEditableBan)
        OfferUpdateBuilder.build(prev, newOffer).map { result =>
          assert(result)(equalTo(NotChanged))
        }
      },
      testM("skip if status is banned(editable) and stored banned(editable)") {
        val prev = Some(existingInfo.withExactStatus(ExactOfferStatus.BANNED).withOfferEditable(true))
        val newOffer = offer.withStatus(BANNED).withModeration(editableBan)
        OfferUpdateBuilder.build(prev, newOffer).map { result =>
          assert(result)(equalTo(NotChanged))
        }
      },
      testM("skip if new version is lower") {
        val prev = Some(existingInfo.withOfferVersion(offer.version + 1000))
        val newOffer = offer.withStatus(BANNED)
        OfferUpdateBuilder.build(prev, newOffer).map { result =>
          assert(result)(equalTo(NotChanged))
        }
      }
    ).provideCustomLayerShared {
      val conf = ZLayer.succeed(HammerConfig("http//o.test.yandex.net"))
      val router = ZLayer.succeed(new RouterClient.Service {
        override def getLink[R: Routable](routable: R, isCanonical: Boolean): Task[String] = ZIO.succeed("link")

        override def getLinks(routes: Seq[Route]): Task[Seq[String]] = ???

        override def parseRoute(link: String): UIO[Route] = ???
      })
      val users = TestUserService.withUsers(Seq(UserView(100, email = Some("email"))))
      val dict = ZLayer.succeed(new BansDictionaryService.Service {
        override def banReasons: UIO[BanReasonSnapshot] = ZIO.succeed(Data.TestSnapshot)
      })

      conf ++ router ++ users ++ dict
    }
}
