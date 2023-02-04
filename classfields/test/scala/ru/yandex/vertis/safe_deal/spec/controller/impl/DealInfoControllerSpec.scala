package ru.yandex.vertis.safe_deal.spec.controller.impl

import java.time.Instant
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.safe_deal.controller.{DealController, DealInfoController}
import ru.yandex.vertis.safe_deal.controller.DealController.Deals
import ru.yandex.vertis.safe_deal.model.Deal.{DealFlags, UpdateInfo}
import ru.yandex.vertis.safe_deal.model.PartyAction.PartyProfileUpdateAction
import ru.yandex.vertis.safe_deal.model.{
  Arbitraries,
  AutoruDeal,
  AutoruSubject,
  Buyer,
  Deal,
  Escrow,
  PersonProfile,
  Seller,
  Tag
}
import ru.yandex.vertis.safe_deal.model.TestSyntax._
import ru.yandex.vertis.safe_deal.proto.common.{BuyerStep, DealState, DealStep, SellerStep}
import ru.yandex.vertis.zio_baker.model.User
import zio.test.Assertion.{anything, equalTo}
import zio.test.environment.TestEnvironment
import zio.test.mock.{mockable, Expectation}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.ZIO

object DealInfoControllerSpec extends DefaultRunnableSpec {

  @mockable[DealController.Service]
  object DealControllerMock

  private def dealControllerMock(deals: Seq[Deal]) =
    DealControllerMock
      .List(
        anything,
        Expectation.value(Deals(deals, deals.size.toLong.some))
      )
      .optional

  private def dealControllerLayer(deals: Seq[Deal]) =
    dealControllerMock(deals) >>> DealInfoController.live

  private def profile(): Option[PersonProfile] = {
    Arbitraries.PersonProfileArb.arbitrary.sample
  }

  private def buyer(buyerId: String): Buyer = {
    Buyer.forTest(User.fromUid(buyerId), profile(), isPassportProvided = false)
  }

  private def seller(sellerId: String): Seller = {
    Seller.forTest(User.fromUid(sellerId), profile(), isPassportProvided = false)
  }

  private val now: Instant = Instant.now()

  private def makeDeal(buyer: Buyer, seller: Seller, now: Instant): Deal = {
    val updateInfo: UpdateInfo = UpdateInfo(now, now, None, now, now, now)
    val meetingInfo: Deal.MeetingInfo = Deal.MeetingInfo(None, None, None)
    val priceInfo: Deal.PriceInfo = Deal.PriceInfo(None, None, None)
    val escrows: List[Escrow] = List()
    val subject: AutoruSubject = AutoruSubject(None, None, None, None, None, None, None)
    AutoruDeal.forTest(
      id = "dummy".taggedWith[Tag.DealId],
      dealNumber = 666L.taggedWith[Tag.DealNumber],
      updateInfo = updateInfo,
      scheduledAt = None,
      schedulerLastUpdate = None,
      state = DealState.IN_PROGRESS,
      dealStep = DealStep.DEAL_CREATED,
      sellerStep = SellerStep.SELLER_INTRODUCING_ACCOUNT_DETAILS,
      buyerStep = BuyerStep.BUYER_AWAITING_ACCEPT,
      cancelInfo = None,
      meetingInfo = meetingInfo,
      priceInfo = priceInfo,
      safeDealAccount = None,
      escrows = escrows,
      buyer = buyer,
      seller = seller.some,
      notifications = Seq(),
      subject = subject,
      documents = Seq(),
      flags = DealFlags.empty,
      lastOverdueNotification = None
    )
  }

  def spec: ZSpec[TestEnvironment, Any] =
    suite("DealInfoController")(
      testM("getUserProfile - get profile") {
        val updateAction = for {
          service <- ZIO.service[DealInfoController.Service]
          result <- service.getUserProfile(Domain.DOMAIN_AUTO, None)
        } yield result
        val buyerA = buyer("123")
        val sellerA = seller("456")
        val res: Option[PartyProfileUpdateAction] = None
        assertM(updateAction)(equalTo(res))
          .provideLayer(dealControllerLayer(Seq(makeDeal(buyerA, sellerA, now.minusSeconds(10)))))
      }
    )
}
