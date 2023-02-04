package ru.auto.salesman.dao

import java.time.{LocalDate, ZoneOffset}
import org.scalatest.concurrent.IntegrationPatience
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.dao.GoodsDao.Filter._
import ru.auto.salesman.dao.GoodsDao._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model._
import ru.auto.salesman.tasks.Partition
import ru.auto.salesman.test.BaseSpec
import zio.ZIO

import scala.concurrent.duration._

trait CategorizedGoodsDaoSpec extends BaseSpec with IntegrationPatience {

  def goodsDao: GoodsDao

  "CategorizedGoodsDao" should {

    val TestOffer = 1002876493L
    val TestCategory = OfferCategories.Trucks
    val TestProduct = ProductId.Color
    var max: Option[Epoch] = None

    "filter records by date, excluded products and status" in {
      val since =
        LocalDate
          .parse("2014-12-10")
          .atStartOfDay()
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli
      val isActivation = collection.Set(ProductId.Placement, ProductId.Add)
      val activeNonPlacementsSince = goodsDao
        .get(
          RecentFilter(
            since,
            excludedProducts = isActivation.toSeq,
            status = Some(GoodStatuses.Active)
          )
        )
        .get
      activeNonPlacementsSince.foreach { record =>
        record.epoch.get should be > since
        isActivation(record.product) shouldBe false
        record.status shouldBe GoodStatuses.Active
      }
      val activeGoodsSince = goodsDao
        .get(
          RecentFilter(
            since,
            Seq(),
            status = Some(GoodStatuses.Active)
          )
        )
        .get
      activeGoodsSince.size should be > activeNonPlacementsSince.size
      activeGoodsSince.foreach { record =>
        record.epoch.get should be > since
        record.status shouldBe GoodStatuses.Active
      }
      activeGoodsSince.exists(record => isActivation(record.product)) shouldBe true
      val goodsSince = goodsDao.get(RecentFilter(since, Seq(), None)).get
      goodsSince.size should be > activeGoodsSince.size
      goodsSince.foreach { record =>
        record.epoch.get should be > since
      }
      goodsSince.exists(record => isActivation(record.product)) shouldBe true
      goodsSince.exists(record => record.status == GoodStatuses.Inactive) shouldBe true
    }

    "find that offer placement exists when it's active" in {
      val offerId = 1002876493
      val category = OfferCategories.Trucks
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "find that offer placement exists when it's inactive" in {
      val offerId = 2222
      val category = OfferCategories.Swapbody
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "find that offer placement doesn't exist" in {
      val offerId = 2134798
      val category = OfferCategories.Trucks
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption shouldBe empty
    }

    "find that offer premium exists" in {
      val offerId = 1002876471
      val category = OfferCategories.Swapbody
      val product = ProductId.Premium
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "find that offer premium doesn't exist" in {
      val offerId = 1002876472
      val category = OfferCategories.Trailer
      val product = ProductId.Premium
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption shouldBe empty
    }

    "don't find non-billed product" in {
      val offerId = 1002876527
      val category = OfferCategories.Snowmobile
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption shouldBe empty
    }

    "find that archived offer placement exists" in {
      val offerId = 1163788
      val category = OfferCategories.Motorcycle
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "get nothing for filter with empty result" in {
      goodsDao
        .get(Since(now().plusYears(1).getMillis))
        .success
        .value shouldBe empty
    }
    "failed for filter by offer any status" in {
      failedForFilter(ForOfferAnyStatus(TestOffer))
    }
    "failed for filter by offer product" in {
      failedForFilter(ForOfferProduct(TestOffer, ProductId.Color))
    }
    "failed for filter by offer product and status" in {
      failedForFilter(
        ForOfferProductStatus(TestOffer, ProductId.Color, GoodStatuses.Active)
      )
    }

    "get data since" in {
      val since0 = goodsDao.get(Since(0L)).success.value
      since0 should not be empty
      max = since0.map(_.epoch).max
      val sinceMax = goodsDao.get(Since(max.get)).success.value
      sinceMax should have size 11
      sinceMax.map(_.epoch).toList.distinct should have size 1
      goodsDao.get(Since(max.get + 1)).success.value shouldBe empty
      goodsDao.get(Since(max.get - 1)).success.value should not be empty
    }
    "get smth need activation" in {
      val gs = goodsDao
        .get(NeedActivation(1.hour, Partition.all(1).head))
        .success
        .value
      gs should have size 16
      val good = gs.find(_.primaryKeyId == 1677900L).value
      good.offerId shouldBe 1002876472L
      good.offerHash shouldBe "34jqq"
      good.category shouldBe OfferCategories.Atv
      good.section shouldBe Section.USED
      good.clientId shouldBe 16283
      good.product shouldBe ProductId.Fresh
      good.status shouldBe GoodStatuses.Active
    }

    "update activate date" in {
      // for epoch update
      Thread.sleep(1)

      val activateDate = FirstActivateDate(now().plusMinutes(2))
      val goodsBefore = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsBefore should have size 1
      val before = goodsBefore.head

      goodsDao
        .update(
          Condition.WithGoodsId(before.primaryKeyId),
          Patch(firstActivateDate = Some(GoodsDao.Update(activateDate)))
        )
        .success

      val goodsAfter = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsAfter should have size 1
      val after = goodsAfter.head
      // this check is wrong, see SALESMAN-476 for details
      // there are a lot of such checks in this file
      (after.firstActivateDate should be).equals(activateDate)
      before.epoch should not be after.epoch
    }

    "update expire date" in {
      val expireDate = now().plusMinutes(2)

      val goodsBefore = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsBefore should have size 1
      val before = goodsBefore.head

      goodsDao
        .update(
          Condition.WithGoodsId(before.primaryKeyId),
          Patch(expireDate = Some(expireDate))
        )
        .success

      val goodsAfter = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsAfter should have size 1
      val after = goodsAfter.head

      before.expireDate shouldBe empty

      (after.expireDate.value should be).equals(expireDate)
    }

    "update ob and ob deadline" in {
      val offerBilling = Array[Byte]()
      val offerBillingDeadline = now().plusHours(2)

      val goodsBefore = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsBefore should have size 1
      val before = goodsBefore.head
      before.offerBilling shouldBe empty
      before.offerBillingDeadline shouldBe empty

      val patch = Patch(
        offerBilling = Some(GoodsDao.Update(offerBilling)),
        offerBillingDeadline = Some(GoodsDao.Update(offerBillingDeadline))
      )

      goodsDao.update(Condition.WithGoodsId(before.primaryKeyId), patch).success

      val goodsAfter = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsAfter should have size 1
      val after = goodsAfter.head
      (after.offerBilling.value should be).equals(offerBilling)

      (after.offerBillingDeadline.value should be).equals(offerBillingDeadline)
    }

    "update hold id" in {
      val holdId = s"$TestOffer-$TestProduct"

      val goodsBefore = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsBefore should have size 1
      val before = goodsBefore.head
      before.holdTransactionId shouldBe empty

      val patch = Patch(
        holdId = Some(GoodsDao.Update(holdId))
      )

      goodsDao.update(Condition.WithGoodsId(before.primaryKeyId), patch).success

      val goodsAfter = goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value
      goodsAfter should have size 1
      val after = goodsAfter.head
      (after.holdTransactionId should be).equals(holdId)
    }

    "update status" in {
      val patch = Patch(status = Some(GoodStatuses.Inactive))

      goodsDao
        .update(
          Condition.WithOfferProduct(TestOffer, TestCategory, TestProduct),
          patch
        )
        .success

      goodsDao
        .get(ForOfferCategoryProduct(TestOffer, TestCategory, TestProduct))
        .success
        .value shouldBe empty
    }

    "update only selected Badge" in {
      val TestBadgesOffer = 1163788L
      val TestBadgesOfferCategory = OfferCategories.Motorcycle

      val recordsBefore = goodsDao
        .get(
          ForOfferCategoryProduct(
            TestBadgesOffer,
            TestBadgesOfferCategory,
            ProductId.Badge
          )
        )
        .success
        .value
      recordsBefore should have size 2

      val firstRecord = recordsBefore.head
      val secondRecord = recordsBefore.last

      goodsDao
        .update(
          Condition.WithGoodsId(secondRecord.primaryKeyId),
          Patch(holdId = Some(GoodsDao.Update("badge-holdId")))
        )
        .success

      val recordsAfter = goodsDao
        .get(
          ForOfferCategoryProduct(
            TestBadgesOffer,
            TestBadgesOfferCategory,
            ProductId.Badge
          )
        )
        .success
        .value
      recordsAfter should have size 2
      recordsAfter.head shouldBe firstRecord
      recordsAfter.last.holdTransactionId.value shouldBe "badge-holdId"
    }

    "activate only selected Badge" in {
      val TestBadgesOffer = 1163788L
      val TestBadgesOfferCategory = OfferCategories.Motorcycle

      val recordsBefore = goodsDao
        .get(
          ForOfferCategoryProduct(
            TestBadgesOffer,
            TestBadgesOfferCategory,
            ProductId.Badge
          )
        )
        .success
        .value
      recordsBefore.size shouldBe 2

      val firstRecord = recordsBefore.head
      val secondRecord = recordsBefore.last

      goodsDao
        .update(
          Condition.WithGoodsId(firstRecord.primaryKeyId),
          Patch(status = Some(GoodStatuses.Inactive))
        )
        .success

      val recordsAfter = goodsDao
        .get(
          ForOfferCategoryProduct(
            TestBadgesOffer,
            TestBadgesOfferCategory,
            ProductId.Badge
          )
        )
        .success
        .value
      recordsAfter should have size 1
      recordsAfter.head shouldBe secondRecord
    }

    "failed for insert new goods record" in {
      val NewOffer = 1003276986L
      val NewStatus = GoodStatuses.Active
      val NewProduct = ProductId.Top
      val NewActivateDate = now().withMillisOfSecond(0)
      val request = Source(NewOffer, NewProduct, NewStatus, "", NewActivateDate)
      goodsDao.insert(request).failure
    }

    "insert one new record on two parallel requests" in {
      val offerId = 1163788L
      val category = OfferCategories.Motorcycle
      val status = GoodStatuses.Active
      val product = ProductId.Special
      val activateDate = now().withMillisOfSecond(0)
      val details = SourceDetails(None, category, Section.NEW, 1)
      val request =
        Source(offerId, product, status, "", activateDate, Some(details))
      goodsDao
        .get(ForOfferCategoryProduct(offerId, category, product))
        .success
        .value
        .size shouldBe 0

      ZIO
        .collectAllParN(2)(
          List(
            goodsDao.insert(request),
            goodsDao.insert(request)
          )
        )
        .success
        .value

      goodsDao
        .get(ForOfferCategoryProduct(offerId, category, product))
        .success
        .value
        .size shouldBe 1
    }

    "insert good ready to be applied" in {
      val offerId = 1163788L
      val product = ProductId.Fresh
      val status = GoodStatuses.Active
      val activateDate = ActivateDate(now().withMillisOfSecond(0))
      val expireDate = activateDate.plusDays(1)
      val offerBilling = List[Byte](0, 1, 2, 3).toArray
      val offerBillingDeadline = now().withMillisOfSecond(0).plusWeeks(1)
      val holdId = "test-hold"
      val offerHash = "2b81"
      val category = OfferCategories.Motorcycle
      val section = Section.USED
      val clientId = 16301
      val source =
        new AppliedSource(
          offerId,
          product,
          status,
          activateDate,
          expireDate,
          offerBilling,
          offerBillingDeadline,
          holdId,
          SourceDetails(Some(offerHash), category, section, clientId)
        )
      goodsDao.insertApplied(source)
      val allInserted =
        goodsDao
          .get(ForOfferCategoryProduct(offerId, category, product))
          .success
          .value
      allInserted should have size 1
      val inserted = allInserted.head
      inserted.offerId shouldBe offerId
      inserted.offerHash shouldBe offerHash
      inserted.category shouldBe category
      inserted.section shouldBe section
      inserted.clientId shouldBe clientId
      inserted.product shouldBe product
      inserted.status shouldBe status
      inserted.extraData shouldBe ""
      inserted.expireDate.value shouldBe expireDate
      inserted.firstActivateDate shouldBe FirstActivateDate(
        activateDate.asDateTime
      )
      inserted.offerBillingDeadline.value shouldBe offerBillingDeadline
      inserted.offerBilling.value shouldBe offerBilling
      inserted.holdTransactionId.value shouldBe holdId
    }

    "archive zero records with impossible expiration date" in {
      val originalInactiveCount = countInactiveRecords()
      goodsDao.archive(InactiveOlder(0L)).success
      countInactiveRecords() shouldBe originalInactiveCount
    }
    "archive several records" in {
      val originalInactiveCount = countInactiveRecords()
      //увеличил время выполнения до 360 секунд так как за 30 секунд в teamcity просто не успевает
      // локально все работает нормально
      goodsDao.archive(InactiveOlder(now().plusSeconds(360).getMillis)).success
      val currentInactiveCount = countInactiveRecords()
      currentInactiveCount should not be originalInactiveCount
      countInactiveRecords shouldBe 0
    }
  }

  private def failedForFilter(filter: Filter) =
    goodsDao.get(filter).failure

  private def countInactiveRecords(): Int =
    goodsDao.get(Since(0L)).get.count(_.status == GoodStatuses.Inactive)
}
