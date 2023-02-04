package ru.yandex.vertis.safe_deal.spec.stages

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.safe_deal.application.stages.Stage.DealChangedStageSource
import ru.yandex.vertis.safe_deal.application.stages.{OverdueStage, Stage}
import ru.yandex.vertis.safe_deal.dictionary.NotificationTemplateDictionary
import ru.yandex.vertis.safe_deal.model.Deal.{DealFlags, UpdateInfo}
import ru.yandex.vertis.safe_deal.model.event.DealChangedEvent
import ru.yandex.vertis.safe_deal.model.{AutoruDeal, AutoruSubject, Buyer, Deal, Escrow, Seller, Tag}
import ru.yandex.vertis.safe_deal.model.TestSyntax._
import ru.yandex.vertis.safe_deal.notification.NotificationCreator
import ru.yandex.vertis.safe_deal.proto.common.{BuyerStep, DealState, DealStep, SellerStep}
import ru.yandex.vertis.safe_deal.util.RichAutoruModel.RichAutoruDeal
import ru.yandex.vertis.safe_deal.util.mock.NotificationTemplateDictionaryMock
import ru.yandex.vertis.zio_baker.model.User
import zio.clock.Clock
import zio.duration.durationInt
import zio.test.Assertion.isTrue
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio._

import java.time.Instant

object OverdueStageSpec extends DefaultRunnableSpec {

  private lazy val notificationTemplateDictionaryLayer: ULayer[NotificationTemplateDictionary] =
    ZLayer.succeed(new NotificationTemplateDictionaryMock())

  private lazy val notificationCreatorLayer: ULayer[NotificationCreator] = notificationTemplateDictionaryLayer >>>
    NotificationCreator.live

  private lazy val overdueStageLayer = ZLayer.succeed[Domain](Domain.DOMAIN_AUTO) ++ notificationCreatorLayer ++
    Clock.any >>> OverdueStage.live

  private val now: Instant = Instant.ofEpochMilli(0)

  private def makeDeal(now: Instant): Deal = {
    val buyer: Buyer = Buyer.forTest(User.fromUid("12345"))
    val updateInfo: UpdateInfo = UpdateInfo(now, now, None, now, now, now)
    val meetingInfo: Deal.MeetingInfo = Deal.MeetingInfo(None, None, None)
    val priceInfo: Deal.PriceInfo = Deal.PriceInfo(None, None, None)
    val escrows: List[Escrow] = List()
    val seller: Seller = Seller.forTest(User.fromUid("67890"))
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
      totalProvidedRub = 0L,
      safeDealAccount = None,
      escrows = escrows,
      buyer = buyer,
      seller = seller.some,
      notifications = Seq(),
      subject = subject,
      documents = Seq(),
      flags = DealFlags.empty,
      isRollback = false,
      lastOverdueNotification = None
    )
  }

  def spec: ZSpec[TestEnvironment, Any] =
    suite("OverdueStage")(
      testM("noOverdue - no changes") {
        val now = Instant.ofEpochMilli(0)
        val deal = makeDeal(now)
        val event = DealChangedStageSource(
          deal,
          DealChangedEvent(deal.id, now, Domain.DOMAIN_AUTO, DealChangedEvent.Event.Plain())
        )
        assertM(ZIO.accessM[Stage[DealChangedStageSource, Deal]](s => s.get.process(event)).map { x =>
          x.result.isEmpty
        })(isTrue).provideLayer(overdueStageLayer)
      },
      testM("Bad state - no changes (with overdue)") {
        val minusDays = now.minus(2.days)
        val deal = makeDeal(now)
          .withDealStep(DealStep.DEAL_CONFIRMED)
          .withUpdateInfo(UpdateInfo(minusDays, minusDays, None, minusDays, minusDays, minusDays))
        val event = DealChangedStageSource(
          deal,
          DealChangedEvent(deal.id, now, Domain.DOMAIN_AUTO, DealChangedEvent.Event.Plain())
        )
        assertM(ZIO.accessM[Stage[DealChangedStageSource, Deal]](s => s.get.process(event)).map { x =>
          x.result.isEmpty
        })(isTrue).provideLayer(overdueStageLayer)
      },
      testM("withOverdueNoTs") {
        val minusDays = now.minus(5.days)
        val deal = makeDeal(now)
          .withUpdateInfo(UpdateInfo(minusDays, minusDays, None, minusDays, minusDays, minusDays))
        val event = DealChangedStageSource(
          deal,
          DealChangedEvent(deal.id, now, Domain.DOMAIN_AUTO, DealChangedEvent.Event.Plain())
        )
        assertM(ZIO.accessM[Stage[DealChangedStageSource, Deal]](s => s.get.process(event)).map { x =>
          x.result.nonEmpty
        })(isTrue).provideLayer(overdueStageLayer)
      },
      testM("withOverdueWithOldTs") {
        val minusDays = now.minus(5.days)
        val deal = makeDeal(now)
          .withUpdateInfo(UpdateInfo(minusDays, minusDays, None, minusDays, minusDays, minusDays))
          .withLastOverdueNotification(minusDays)
        val event = DealChangedStageSource(
          deal,
          DealChangedEvent(deal.id, now, Domain.DOMAIN_AUTO, DealChangedEvent.Event.Plain())
        )
        assertM(ZIO.accessM[Stage[DealChangedStageSource, Deal]](s => s.get.process(event)).map { x =>
          x.result.nonEmpty
        })(isTrue).provideLayer(overdueStageLayer)
      },
      testM("withOverdueAvoidDuplication") {
        val minusDays = now.minus(5.days)
        val deal = makeDeal(now)
          .withUpdateInfo(UpdateInfo(minusDays, minusDays, None, minusDays, minusDays, minusDays))
          .withLastOverdueNotification(now)
        val event = DealChangedStageSource(
          deal,
          DealChangedEvent(deal.id, now, Domain.DOMAIN_AUTO, DealChangedEvent.Event.Plain())
        )
        assertM(ZIO.accessM[Stage[DealChangedStageSource, Deal]](s => s.get.process(event)).map { x =>
          x.result.isEmpty
        })(isTrue).provideLayer(overdueStageLayer)
      },
      testM("withOverdueNeedToCancelNoMoney") {
        val minusDays = now.minus(12.days)
        val deal = makeDeal(now)
          .withUpdateInfo(UpdateInfo(minusDays, minusDays, None, minusDays, minusDays, minusDays))
        val event = DealChangedStageSource(
          deal,
          DealChangedEvent(deal.id, now, Domain.DOMAIN_AUTO, DealChangedEvent.Event.Plain())
        )
        assertM(ZIO.accessM[Stage[DealChangedStageSource, Deal]](s => s.get.process(event)).map { x =>
          x.result.nonEmpty && x.result.get.dealStep == DealStep.DEAL_CANCELLED
        })(isTrue).provideLayer(overdueStageLayer)
      },
      testM("withOverdueNeedToCancelHaveMoney") {
        val minusDays = now.minus(12.days)
        val deal = makeDeal(now)
          .withUpdateInfo(UpdateInfo(minusDays, minusDays, None, minusDays, minusDays, minusDays))
          .autoru
          .get
          .copy(totalProvidedRub = 1000L)
        val event = DealChangedStageSource(
          deal,
          DealChangedEvent(deal.id, now, Domain.DOMAIN_AUTO, DealChangedEvent.Event.Plain())
        )
        assertM(ZIO.accessM[Stage[DealChangedStageSource, Deal]](s => s.get.process(event)).map { x =>
          x.result.nonEmpty && x.result.get.dealStep == DealStep.DEAL_CANCELLING
        })(isTrue).provideLayer(overdueStageLayer)
      }
    )
}
