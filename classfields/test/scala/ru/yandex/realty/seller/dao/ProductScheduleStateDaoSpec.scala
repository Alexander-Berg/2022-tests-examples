package ru.yandex.realty.seller.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.DbSpecBase
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.ProductTypes
import ru.yandex.realty.seller.model.schedule.{
  FeedSchedulePatch,
  ManualSchedulePatch,
  ProductScheduleContext,
  ProductScheduleItem,
  ProductScheduleState,
  ScheduleContextPatch,
  ScheduleEnabledPatch,
  ScheduleHasBoughtProductPatch,
  ScheduleMultiPatch,
  ScheduleOnceContext,
  ScheduleVisitTimePatch
}
import ru.yandex.realty.seller.service.util.ScheduleUtils
import ru.yandex.realty.sharding.Shard
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.realty.util.Mappings._

import scala.concurrent.Future
import ScheduleUtils._

trait ProductScheduleStateDaoSpec extends AsyncSpecBase with DbSpecBase with SellerModelGenerators with PropertyChecks {

  def dao: ProductScheduleStateDao
  def actions: ProductScheduleStateDbActions

  private def actualInBase(offerId: String) =
    actions.get(Set(offerId)).databaseValue.futureValue

  "ProductScheduleStateDao" should {

    "return default value and not affect base" in {
      val uid = passportUserGen.next
      val offerId = readableString.next

      val gotTypes = dao
        .getOrDefault(uid, Set(offerId))
        .futureValue
        .filter(_.offerId == offerId)
        .map(_.productType)

      gotTypes shouldBe Seq(ProductTypes.Raising)

      actualInBase(offerId) shouldBe empty
    }

    "return default value and not create in base" in {
      val uid = passportUserGen.next
      val offerId = readableString.next

      val gotTypes = dao
        .getOrCreate(uid, Set(offerId))
        .futureValue
        .map(_.productType)

      gotTypes should contain theSameElementsAs ProductScheduleStateDao.AllowableProductTypes

      val inBaseTypes =
        actualInBase(offerId)
          .map(_.productType)

      inBaseTypes should contain theSameElementsAs ProductScheduleStateDao.AllowableProductTypes
    }

    "correctly create non-existing states" in {
      val offers = readableString.next(2).toSet
      val uid = passportUserGen.next

      actions.get(offers).databaseValue.futureValue shouldBe empty

      dao.getOrCreate(uid, offers).futureValue

      val expectedOfferToType = offers.flatMap(offer => ProductScheduleStateDao.AllowableProductTypes.map(offer -> _))

      val actualOfferToType =
        actions
          .get(offers)
          .databaseValue
          .futureValue
          .flatMap(offer => ProductScheduleStateDao.AllowableProductTypes.map(offer.offerId -> _))

      actualOfferToType should contain theSameElementsAs expectedOfferToType
    }

    "update context on existing state" in {
      val uid = passportUserGen.next
      val offerId = readableString.next

      val state = dao.getOrCreate(uid, Set(offerId)).futureValue.head

      val newContext =
        ProductScheduleContext(
          ScheduleOnceContext(
            scheduleItemGen.next(3).toSeq,
            scheduleItemGen.next(3).toSeq
          )
        )

      dao
        .updateSchedule(
          uid,
          state.offerId,
          state.productType,
          ScheduleContextPatch(newContext)
        )
        .futureValue

      val newStub = dao.getOrDefault(uid, Set(offerId)).futureValue.find(_.productType == ProductTypes.Raising).get

      newStub shouldBe state.copy(scheduleContext = newContext)
    }

    "update status on existing state" in {
      val uid = passportUserGen.next
      val offerId = readableString.next
      val state = dao.getOrCreate(uid, Set(offerId)).futureValue.head

      val newStatus = !state.turnedOn
      dao
        .updateSchedule(
          uid,
          state.offerId,
          state.productType,
          ScheduleEnabledPatch(newStatus)
        )
        .futureValue

      val newStub = dao.getOrDefault(uid, Set(offerId)).futureValue.find(_.productType == ProductTypes.Raising).get

      newStub shouldBe state.copy(turnedOn = newStatus)
    }

    "update visitTime on existing state" in {
      val uid = passportUserGen.next
      val offerId = readableString.next
      val state = dao.getOrCreate(uid, Set(offerId)).futureValue.head

      val newTime = DateTimeUtil.now().plusHours(3)
      dao
        .updateSchedule(
          uid,
          state.offerId,
          state.productType,
          ScheduleVisitTimePatch(Some(newTime))
        )
        .futureValue

      val newStub = dao.getOrDefault(uid, Set(offerId)).futureValue.find(_.productType == ProductTypes.Raising).get

      newStub shouldBe state.copy(visitTime = Some(newTime))
    }

    "correctly update has_bought_flag" in {
      val uid = passportUserGen.next
      val offerId = readableString.next
      val state = dao.getOrCreate(uid, Set(offerId)).futureValue.head

      val initial = scheduleItemGen.next(2).toSeq
      dao.updateSchedule(uid, offerId, ProductTypes.Raising, ManualSchedulePatch(initial)).futureValue

      val updateDate = initial.head.startTime
      val manualPatch = ScheduleHasBoughtProductPatch(updateDate)
      val expectedManual: Seq[ProductScheduleItem] = Seq(initial.head.copy(hasBoughtProduct = true)) ++ Seq(
        initial.last
      )

      dao
        .updateSchedule(
          uid,
          state.offerId,
          state.productType,
          manualPatch
        )
        .futureValue

      val inBaseContext =
        actualInBase(offerId)
          .map(_.scheduleContext.context.asInstanceOf[ScheduleOnceContext])
          .toSeq
          .head

      inBaseContext
        .applySideEffect(_.feedSchedule shouldBe empty)
        .applySideEffect(_.manualSchedule should have size 2)
        .applySideEffect(_.manualSchedule should contain theSameElementsAs expectedManual)

      val feedInitial = scheduleItemGen.next(1).map(_.copy(startTime = updateDate)).toSeq
      dao
        .updateSchedule(
          uid,
          offerId,
          ProductTypes.Raising,
          ScheduleMultiPatch(
            ManualSchedulePatch(feedInitial),
            FeedSchedulePatch(feedInitial),
            ScheduleHasBoughtProductPatch(updateDate)
          )
        )
        .futureValue

      actualInBase(offerId)
        .map(_.scheduleContext.context.asInstanceOf[ScheduleOnceContext])
        .toSeq
        .head
        .applySideEffect { ctx =>
          ctx.manualSchedule should have size 1
          ctx.feedSchedule should have size 1
        }
        .applySideEffect(
          _.feedSchedule should contain theSameElementsAs feedInitial.map(_.copy(hasBoughtProduct = true))
        )
        .applySideEffect(_.manualSchedule should contain theSameElementsAs feedInitial)
    }

    "correctly update by specified patch" in {
      val time = DateTimeUtil.now().withMillisOfSecond(0).withSecondOfMinute(0)
      val singleManual = scheduleItemGen.next.copy(feedWithZeroTime = false, hasBoughtProduct = false)

      val state = scheduleStateGen.next.copy(
        visitTime = Some(time.minusHours(13)),
        scheduleContext = ProductScheduleContext(ScheduleOnceContext(manualSchedule = Seq(singleManual)))
      )

      actions.create(Iterable(state)).databaseValue.futureValue
      val patch = ScheduleMultiPatch(
        ScheduleVisitTimePatch(Some(time)),
        ScheduleHasBoughtProductPatch(singleManual.startTime)
      )
      dao.updateSchedule(state.owner, state.offerId, state.productType, patch).futureValue

      val updated = actualInBase(state.offerId).head

      updated.visitTime shouldBe Some(time)

      val hasBoughtFlag = updated.getScheduleOnceContext.manualSchedule.head.hasBoughtProduct

      hasBoughtFlag shouldBe true

      val dt = DateTime
        .now()
        .withYear(2020)
        .withMonthOfYear(6)
        .withDayOfMonth(4)
        .withHourOfDay(9)
        .withMinuteOfHour(0)
        .withSecondOfMinute(0)
        .withMillisOfSecond(0)

      val checkTime = dt.withDayOfMonth(8)

      val item = ProductScheduleItem(
        startTime = dt,
        daysOfWeek = ScheduleUtils.AllDays
      )
      val ctx = ScheduleOnceContext(
        feedSchedule = Seq(item)
      )
      val s2 = scheduleStateGen.next.copy(scheduleContext = ProductScheduleContext(ctx))

      val patch2 = ScheduleHasBoughtProductPatch(checkTime)

      patch2.patch(s2).getScheduleOnceContext.feedSchedule shouldBe Seq(item.copy(hasBoughtProduct = true))

    }

    "correctly update batch" in {
      forAll(passportUserGen, Gen.listOfN(3, offerManualRaisingScheduleUpdateGen)) { (user, updates) =>
        val offerIds = updates.map(_.offerId).toSet
        actions.get(offerIds).databaseValue.futureValue shouldBe empty

        dao.updateOffers(user, updates.toSet).futureValue

        def checkProductUpdated(patch: ManualSchedulePatch, current: ProductScheduleState) = {
          current.scheduleContext.context
            .asInstanceOf[ScheduleOnceContext]
            .manualSchedule should contain theSameElementsAs patch.items
        }

        dao.getOrDefault(user, offerIds).futureValue.foreach { state =>
          val patch = updates.find(u => u.offerId == state.offerId).get

          checkProductUpdated(patch.patches(ProductTypes.Raising).asInstanceOf[ManualSchedulePatch], state)
        }
      }
    }

    "correctly return watchList" in {
      val statesWithVisitTime =
        scheduleStateGen.next(3).map(_.copy(turnedOn = true, visitTime = Some(DateTime.now().minusDays(1)))).toSet
      val woTimeState = scheduleStateGen.next.copy(visitTime = None)

      val states = statesWithVisitTime ++ Seq(woTimeState)
      actions.create(states).databaseValue.futureValue

      val shard = Shard(0, 1)
      val watch = dao.watchStates(states.size, shard)(Future(_)).futureValue

      watch.totalCount shouldBe statesWithVisitTime.size
    }
  }
}
