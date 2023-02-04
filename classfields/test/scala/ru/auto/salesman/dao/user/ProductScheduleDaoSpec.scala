package ru.auto.salesman.dao.user

import ru.auto.salesman.dao.user.ProductScheduleDao.Patch
import ru.auto.salesman.dao.user.ProductScheduleDao.ScheduleFilter._
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.schedule.AllowMultipleRescheduleUpsert.{
  False,
  SameOrTrue
}
import ru.auto.salesman.model.user.schedule.{IsVisible, ProductSchedule, ScheduleSource}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig, TestException}
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.{now, DateTimeOrdering}

import scala.collection.mutable
import scala.util.{Failure, Success}

trait ProductScheduleDaoSpec
    extends BaseSpec
    with ProductScheduleModelGenerators
    with IntegrationPropertyCheckConfig {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  def newDao(
      data: Iterable[ProductSchedule]
  ): ProductScheduleDao with ProductScheduleInsertDao

  "ProductScheduleDao" should {

    "delete entries" in {
      val schedule =
        listNUnique[ProductSchedule, Long](1, ProductScheduleGen)(
          _.id
        ).next.head
      val dao = newDao(Seq(schedule))
      dao.delete(
        ForUserRef(schedule.user),
        ForProduct(schedule.product),
        ForOfferId(schedule.offerId)
      )
      dao.get(IsDeleted(false)).get shouldBe empty
    }

    "get and scan all schedules" in {
      val schedules =
        listNUnique[ProductSchedule, Long](50, ProductScheduleGen)(_.id).next
      val dao = newDao(schedules)

      dao.get().get should contain theSameElementsAs schedules

      val first = schedules.head
      dao.get(ForId(first.id)).get should (have size 1 and contain(first))

      val scanned = mutable.ArrayBuffer.empty[ProductSchedule]
      dao.scan(ForId(first.id))(s => Success(scanned += s)).get
      scanned should (have size 1 and contain(first))

      val midUpdateTime = {
        val sorted = schedules.map(_.epoch).sorted
        sorted(sorted.size / 2)
      }
      val updateSinceMillis = midUpdateTime.getMillis

      val expectedModified = schedules.filter(_.epoch.isAfter(midUpdateTime))
      dao
        .get(UpdatedSince(updateSinceMillis))
        .get should contain theSameElementsAs expectedModified

      scanned.clear()
      dao.scan(UpdatedSince(updateSinceMillis))(s => Success(scanned += s)).get
      scanned should contain theSameElementsAs expectedModified

      val (deleted, notDeleted) = schedules.partition(_.isDeleted)
      dao.get(IsDeleted(true)).get should contain theSameElementsAs deleted
      dao.get(IsDeleted(false)).get should contain theSameElementsAs notDeleted

      scanned.clear()
      dao.scan(IsDeleted(true))(s => Success(scanned += s)).get
      scanned should contain theSameElementsAs deleted

      scanned.clear()
      dao.scan(IsDeleted(false))(s => Success(scanned += s)).get
      scanned should contain theSameElementsAs notDeleted

      val (updatedDeleted, updatedNotDeleted) =
        expectedModified.partition(_.isDeleted)
      dao
        .get(UpdatedSince(updateSinceMillis), IsDeleted(true))
        .get should contain theSameElementsAs updatedDeleted
      dao
        .get(UpdatedSince(updateSinceMillis), IsDeleted(false))
        .get should contain theSameElementsAs updatedNotDeleted
    }

    "fail scan on callback failure" in {
      forAll(ProductScheduleGen) { schedule =>
        val dao = newDao(List(schedule))
        val exception = new TestException
        dao
          .scan(UpdatedSince(0))(_ => Failure(exception))
          .failure
          .exception shouldBe exception
      }
    }

    "update schedule's is_deleted by id" in {
      val schedules =
        listUnique[ProductSchedule, Long](5, 10, ProductScheduleGen)(_.id)
          .suchThat(_.exists(!_.isDeleted))
          .next
      val dao = newDao(schedules)

      intercept[IllegalArgumentException] {
        dao.update(Patch.Delete).get
      }

      val firstNotDeleted = schedules.find(!_.isDeleted).get
      dao.update(Patch.Delete, ForId(firstNotDeleted.id)).get

      val afterDelete = dao.get().get
      afterDelete.filterNot(_.id == firstNotDeleted.id) should
        contain theSameElementsAs schedules.filterNot(
          _.id == firstNotDeleted.id
        )

      val deleted = afterDelete.find(_.id == firstNotDeleted.id).get
      deleted.id shouldBe firstNotDeleted.id
      deleted.isDeleted shouldBe true
    }
    "insert schedule, get inserted" in {
      forAll(ProductScheduleGen) { productSchedule =>
        val dao = newDao(None)
        dao.get().get.size shouldBe 0

        dao.insert(List(productSchedule))
        dao.get().success.value.size shouldBe 1
        val inserted = dao.get().get.iterator.next()

        shouldEqualProductSchedule(inserted, productSchedule)
      }
    }

    "replace, should insert if not exist in storage" in {
      forAll(visibleScheduleSourceGen) { ss =>
        forAll(
          listUnique[ProductSchedule, Long](5, 10, ProductScheduleGen)(_.id)
            .map(_.filter(_.offerId != ss.offerId))
        ) { schedules =>
          val dao = newDao(schedules)
          dao.replace(List(ss))

          dao.get(ForOfferId(ss.offerId)).get.toList.size shouldBe 1
          fieldsShouldEqual(ss, dao.get(ForOfferId(ss.offerId)).get.toList.last)
        }
      }
    }
    "replace, should insert if storage is empty" in {
      forAll(visibleScheduleSourceGen) { ss =>
        val dao = newDao(None)
        dao.replace(List(ss))
        dao.get(ForOfferId(ss.offerId)).get.toList.size shouldBe 1
        fieldsShouldEqual(ss, dao.get(ForOfferId(ss.offerId)).get.toList.last)
      }
    }
    "replace, should insert if exist in storage and isDeleted = true" in {
      forAll(productScheduleGen(IsVisible(true), isDeletedGen = true)) { p =>
        forAll(
          listUnique[ProductSchedule, Long](5, 10, ProductScheduleGen)(_.id)
            .map(_.filter(_.offerId != p.offerId))
        ) { schedules =>
          val dao = newDao(schedules)
          dao.insert(List(p))

          val oldSize = dao.get().get.size

          val ss = scheduleToScheduleSource(p)
          dao.replace(List(ss))

          val schedulesInDao = dao.get(ForOfferId(p.offerId)).get.toList
          schedulesInDao.size shouldBe 2
          schedulesInDao.count(_.isDeleted == false) shouldBe 1

          fieldsShouldEqual(
            ss,
            schedulesInDao.filter(_.isDeleted == false).head
          )

          val afterInsertSize = dao.get().get.size
          oldSize shouldBe afterInsertSize - 1
        }
      }
    }
    "replace, shouldn't insert if exist in storage and isDeleted = false" in {
      forAll(productScheduleGen(IsVisible(true), isDeletedGen = false)) { p =>
        forAll(
          listUnique[ProductSchedule, Long](5, 10, ProductScheduleGen)(_.id)
            .map(_.filter(_.offerId != p.offerId))
        ) { schedules =>
          val dao = newDao(schedules)

          dao.insert(List(p))
          val oldSize = dao.get().get.size

          dao.get(ForOfferId(p.offerId)).get.toList.size shouldBe 1
          dao
            .get(ForOfferId(p.offerId), IsDeleted(false))
            .get
            .toList
            .size shouldBe 1

          val newP = p.copy(scheduleParameters = ScheduleOnceAtTimeGen.next)

          val ss = scheduleToScheduleSource(newP)
          dao.replace(List(ss))

          dao.get(ForOfferId(p.offerId)).get.toList.size shouldBe 2
          dao
            .get(ForOfferId(p.offerId), IsDeleted(false))
            .get
            .toList
            .size shouldBe 1

          dao.replace(List(ss))
          dao.get(ForOfferId(p.offerId)).get.toList.size shouldBe 2
          dao
            .get(ForOfferId(p.offerId), IsDeleted(false))
            .get
            .toList
            .size shouldBe 1

          val afterInsertSize = dao.get().get.size
          afterInsertSize shouldBe oldSize + 1

          fieldsShouldEqual(ss, dao.get(ForOfferId(p.offerId)).get.toList.last)
        }
      }
    }

    "leave non-visible schedule unchanged on replacing" in {
      forAll(
        productScheduleGen(IsVisible(false), isDeletedGen = false),
        ScheduleParametersGen
      ) { (nonVisible, newParams) =>
        val dao = newDao(List(nonVisible))
        val visible =
          scheduleToScheduleSource(
            nonVisible
              .copy(scheduleParameters = newParams, isVisible = IsVisible(true))
          )
        dao.replace(Seq(visible)).success
        dao
          .get(IsDeleted(false))
          .success
          .value should contain allElementsOf List(nonVisible)
      }
    }

    "get only visible schedule" in {
      forAll(
        productScheduleGen(IsVisible(true)),
        productScheduleGen(IsVisible(false))
      ) { (visible, nonVisible) =>
        val dao = newDao(List(visible, nonVisible))
        dao.get(Visible).success.value should contain only visible
      }
    }

    "delete only visible schedule" in {
      forAll(
        productScheduleGen(IsVisible(true), isDeletedGen = false),
        productScheduleGen(IsVisible(false), isDeletedGen = false)
      ) { (visible, nonVisible) =>
        val dao = newDao(List(visible, nonVisible))
        dao.delete(Visible).success
        dao.get(IsDeleted(false)).success.value should contain only nonVisible
      }
    }

    "get only expired schedule" in {
      forAll(
        productScheduleGen(expireDateGen = Some(now().minusSeconds(5))),
        productScheduleGen(expireDateGen = Some(now().plusSeconds(20))),
        productScheduleGen(expireDateGen = None)
      ) { (expired, notExpiredYet, neverExpired) =>
        val dao = newDao(List(expired, notExpiredYet, neverExpired))
        dao.get(Expired).success.value should contain only expired
      }
    }

    "get only non-expired schedules" in {
      forAll(
        productScheduleGen(expireDateGen = Some(now().minusSeconds(5))),
        productScheduleGen(expireDateGen = Some(now().plusSeconds(20))),
        productScheduleGen(expireDateGen = None)
      ) { (expired, notExpiredYet, neverExpired) =>
        val dao = newDao(List(expired, notExpiredYet, neverExpired))
        dao
          .get(NonExpired)
          .success
          .value should contain only (notExpiredYet, neverExpired)
      }
    }

    "get only non-expired schedule with given offer id" in {
      def notExpiredScheduleGen(offerId: AutoruOfferId) =
        productScheduleGen(
          expireDateGen = Some(now().plusSeconds(20)),
          offerIdGen = offerId
        )

      forAll(
        notExpiredScheduleGen(AutoruOfferId("1-a")),
        notExpiredScheduleGen(AutoruOfferId("2-b"))
      ) { (notExpired, notExpiredAnotherOffer) =>
        val dao = newDao(List(notExpired, notExpiredAnotherOffer))
        dao
          .get(ForOfferId(AutoruOfferId("1-a")), NonExpired)
          .success
          .value should contain only notExpired
      }
    }

    "insert schedule using insertIfAbsent()" in {
      forAll(scheduleSourceGen()) { source =>
        val dao = newDao(Nil)
        dao.insertIfAbsent(source).success
        val inserted = dao.get().success.value
        inserted should have size 1
        scheduleToScheduleSource(inserted.headOption.value) shouldBe source
      }
    }

    "don't insert if schedule already exists" in {
      forAll(scheduleSourceGen()) { source =>
        val dao = newDao(Nil)
        dao.insertIfAbsent(source).success
        dao.insertIfAbsent(source).success
        val inserted = dao.get().success.value
        inserted should have size 1
        scheduleToScheduleSource(inserted.headOption.value) shouldBe source
      }
    }
  }

  def scheduleToScheduleSource(p: ProductSchedule): ScheduleSource = {
    val allowMultipleReschedule =
      if (p.allowMultipleReschedule) SameOrTrue else False
    ScheduleSource(
      p.offerId,
      p.user,
      p.product,
      p.scheduleParameters,
      p.isVisible,
      p.expireDate,
      p.customPrice,
      allowMultipleReschedule,
      p.prevScheduleId
    )
  }

  def shouldEqualProductSchedule(
      a: ProductSchedule,
      b: ProductSchedule
  ): Unit = {
    a.offerId shouldBe b.offerId
    a.product shouldBe b.product
    a.isDeleted shouldBe b.isDeleted
    a.epoch shouldBe b.epoch
    a.user shouldBe b.user
    a.scheduleParameters.timezone
      .getOffset(a.epoch) shouldBe b.scheduleParameters.timezone
      .getOffset(b.epoch)
    a.updatedAt shouldBe b.updatedAt
  }

  def fieldsShouldEqual(ss: ScheduleSource, p: ProductSchedule): Unit = {
    ss.offerId shouldBe p.offerId
    ss.user shouldBe p.user
    ss.product shouldBe p.product
    val now = DateTimeUtil.now
    ss.scheduleParameters.timezone
      .getOffset(now) shouldBe p.scheduleParameters.timezone.getOffset(now)
  }
}
