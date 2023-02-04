package ru.yandex.realty.seller.processing.schedule

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.model.user.{PassportUser, UserRef}
import ru.yandex.realty.proto.offer.CampaignType
import ru.yandex.realty.proto.offer.vos.Offer.{Vas, VosOfferSource}
import ru.yandex.realty.proto.offer.vos.OfferResponse.VosOfferResponse
import ru.yandex.realty.seller.dao.ProductScheduleStateDao
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.seller.model.schedule._
import ru.yandex.realty.seller.model.{PersonPaymentTypes, ProductType}
import ru.yandex.realty.seller.service.util.ScheduleUtils
import ru.yandex.realty.seller.service.util.ScheduleUtils._
import ru.yandex.realty.seller.watchers._
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class MigrateToScheduleStageSpec extends AsyncSpecBase with SellerModelGenerators {

  private val features: Features = new SimpleFeatures
  private val vosClient: VosClientNG = mock[VosClientNG]
  private val scheduleStateDao: ProductScheduleStateDao = mock[ProductScheduleStateDao]

  private val stage = new MigrateToSchedulesStage(features, scheduleStateDao, vosClient)
  implicit val traced: Traced = Traced.empty

  implicit private def oneToSeq[A](elem: A): Seq[A] =
    Seq(elem)

  private def raisingVasWithStartTime(implicit state: ProcessingState[PurchasedProduct]): Vas = {
    Vas
      .newBuilder()
      .setType(CampaignType.CAMPAIGN_TYPE_RAISING)
      .setStartTimeMs(state.entry.startTime.get.minusDays(3).getMillis)
      .build()
  }

  private def raisingVasWithoutStartTime(implicit state: ProcessingState[PurchasedProduct]): Vas =
    raisingVasWithStartTime.toBuilder.setStartTimeMs(0).build()

  private def vosReturnsOfferWithVases(vases: Vas*)(implicit state: ProcessingState[PurchasedProduct]) = {

    val PassportUser(uid) = state.entry.owner
    val OfferTarget(offerId) = state.entry.target

    (vosClient
      .getOffer(_: String, _: String)(_: Traced))
      .expects(uid.toString, offerId, *)
      .returns(
        Future.successful(
          Some(
            VosOfferResponse
              .newBuilder()
              .setContent(
                VosOfferSource.newBuilder().addAllRequestedVas(vases.asJava).build()
              )
              .build()
          )
        )
      )
  }

  private def scheduleDaoReturnItems(
    ctx: ScheduleOnceContext
  )(implicit state: ProcessingState[PurchasedProduct]) = {

    val OfferTarget(offerId) = state.entry.target

    val scheduleState = scheduleStateGen.next.copy(
      owner = state.entry.owner,
      offerId = offerId,
      scheduleContext = ProductScheduleContext(ctx)
    )

    (scheduleStateDao
      .getOrDefault(_: UserRef, _: Set[String]))
      .expects(state.entry.owner, Set(offerId))
      .returns(
        Future.successful(
          Seq(scheduleState)
        )
      )
  }

  private def scheduleDaoReturnManualItems(
    items: Seq[ProductScheduleItem]
  )(implicit state: ProcessingState[PurchasedProduct]) =
    scheduleDaoReturnItems(
      ScheduleOnceContext(
        manualSchedule = items
      )
    )

  private def scheduleDaoReturnFeedItems(
    items: Seq[ProductScheduleItem]
  )(implicit state: ProcessingState[PurchasedProduct]) =
    scheduleDaoReturnItems(
      ScheduleOnceContext(
        feedSchedule = items
      )
    )

  private def expectScheduleOnceUpdate(
    update: ScheduleOncePatch
  )(
    implicit state: ProcessingState[PurchasedProduct]
  ) = {

    val expectedPatch = ScheduleMultiPatch(
      ScheduleEnabledPatch(true),
      update,
      ScheduleVisitTimePatch(
        ScheduleUtils.getNextScheduleAfterNow(update.items).map(_.withSecondOfMinute(0))
      )
    )

    (scheduleStateDao
      .updateSchedule(_: UserRef, _: String, _: ProductType, _: ProductSchedulePatch))
      .expects(
        state.entry.owner,
        state.entry.target.asInstanceOf[OfferTarget].offerId,
        ProductTypes.Raising,
        expectedPatch
      )
      .returns(
        Future.successful(scheduleStateGen.next)
      )
  }

  override def beforeAll(): Unit = {
    features.UseNewRaisingScheduling.setNewState(false)
    features.MigrateRaisingToSchedules.setNewState(true)
  }

  private def nextState(p: ProductType, source: PurchaseSource): ProcessingState[PurchasedProduct] = {
    val newStartTime = Some(DateTimeUtil.now().minusMillis(100).minusDays(1))

    val product =
      purchasedProductGen.next.copy(
        product = p,
        startTime = newStartTime,
        endTime = newStartTime.map(_.plusDays(1)),
        paymentType = PersonPaymentTypes.JuridicalPerson,
        source = source,
        owner = passportUserGen.next,
        target = offerTargetGen.next
      )

    ProcessingState.apply(product).withToMigrate(product)
  }

  private def getProductStartTime(implicit state: ProcessingState[PurchasedProduct]) = {
    state.entry.startTime.get
  }

  private def withZeroDate(t: DateTime): DateTime =
    ZeroDate
      .withMillisOfDay(t.getMillisOfDay)
      .withMillisOfSecond(0)

  "MigrateToScheduleStage" should {
    "correctly migrate manual schedules" when {
      "come juridical manual raising" in {
        implicit val state = nextState(ProductTypes.Raising, ManualSource)
        scheduleDaoReturnManualItems(Seq())
        expectScheduleOnceUpdate(
          ManualSchedulePatch(
            everyDaySchedule(withZeroDate(getProductStartTime))
          )
        )
        stage.process(state).futureValue
      }
    }

    "don't migrate manual schedule" when {
      "schedule exist" in {
        implicit val state = nextState(ProductTypes.Raising, ManualSource)
        // миграция уже прошла
        scheduleDaoReturnManualItems(
          everyDaySchedule(
            withZeroDate(getProductStartTime.minusDays(1))
          )
        )
        stage.process(state).futureValue
      }
    }

    "correctly migrate feed schedule" when {
      "offer contain requested vas with time" in {
        implicit val state = nextState(ProductTypes.Raising, FeedSource)

        scheduleDaoReturnFeedItems(Seq())
        vosReturnsOfferWithVases(raisingVasWithStartTime)

        val time = DateTimeUtil
          .fromMillis(raisingVasWithStartTime.getStartTimeMs)
          .withMillisOfSecond(0)

        expectScheduleOnceUpdate(
          FeedSchedulePatch(
            everyDaySchedule(time)
          )
        )

        stage.process(state).futureValue
      }

      "offer contain vas without startTime" in {
        implicit val state = nextState(ProductTypes.Raising, FeedSource)

        scheduleDaoReturnFeedItems(Seq())
        vosReturnsOfferWithVases(raisingVasWithoutStartTime)

        val time = getProductStartTime
          .withMillisOfSecond(0)

        (scheduleStateDao
          .updateSchedule(_: UserRef, _: String, _: ProductType, _: ProductSchedulePatch))
          .expects(where { (owner, offer, pt, patch) =>
            val base = owner == state.entry.owner &&
              OfferTarget(offer) == state.entry.target &&
              pt == ProductTypes.Raising &&
              patch.isInstanceOf[ScheduleMultiPatch]

            val schedulePatchItems = patch
              .asInstanceOf[ScheduleMultiPatch]
              .patches
              .find(_.isInstanceOf[FeedSchedulePatch])
              .map(_.asInstanceOf[FeedSchedulePatch].items)
              .get

            val hours = Seq(9, 12, 16)
            val itemTime = schedulePatchItems.head.startTime

            val res = base &&
              schedulePatchItems.size == 1 &&
              hours.contains(itemTime.getHourOfDay) &&
              itemTime.getYear == ZeroDate.getYear &&
              itemTime.getDayOfYear == ZeroDate.getDayOfYear

            res
          })
          .returns(
            Future.successful(scheduleStateGen.next)
          )

        stage.process(state).futureValue
      }
    }

    "don't migrate feed schedule" when {
      "product not in requested vas" in {
        implicit val state = nextState(ProductTypes.Raising, FeedSource)

        val items = scheduleItemGen.next(4)
        scheduleDaoReturnFeedItems(items.toSeq)

        vosReturnsOfferWithVases() // empty

        stage.process(state).futureValue
      }

      "current schedules contains schedule with 1970 date" in {
        implicit val state = nextState(ProductTypes.Raising, FeedSource)

        val item = scheduleItemGen.next
          .copy(startTime = ZeroDate.withHourOfDay(12))
          .copy(feedWithZeroTime = true)
        scheduleDaoReturnFeedItems(Seq(item))

        vosReturnsOfferWithVases(raisingVasWithoutStartTime) // empty

        stage.process(state).futureValue
      }
    }
  }
}
