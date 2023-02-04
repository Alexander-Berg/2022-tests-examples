package ru.auto.salesman.dao

import java.time.{LocalDate, ZoneOffset}
import org.scalatest.concurrent.IntegrationPatience
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.dao.GoodsDao.Filter._
import ru.auto.salesman.dao.GoodsDao._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.{
  ActivateDate,
  FirstActivateDate,
  GoodStatuses,
  OfferCategories,
  ProductId
}
import ru.auto.salesman.tasks.Partition
import ru.auto.salesman.test.BaseSpec
import zio.ZIO

import scala.concurrent.duration._

// This test is subject to rewrite, since it's really hard to add new tests.
// Also, it's hard to even read them, because arrange phase of all tests is
// defined in separate .sql files.
trait GoodsDaoSpec extends BaseSpec with IntegrationPatience {

  def goodsDao: GoodsDao

  "GoodsDao" should {

    val TestOffer = 1003275380L
    val TestCategory = OfferCategories.Cars
    val TestSection = Section.USED
    val TestProduct = ProductId.Placement

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
      val category = OfferCategories.Cars
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "find that offer placement exists when it's inactive" in {
      val offerId = 2222
      val category = OfferCategories.Cars
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "find that offer placement doesn't exist" in {
      val offerId = 2134798
      val category = OfferCategories.Cars
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption shouldBe empty
    }

    "find that offer premium exists" in {
      val offerId = 1002876471
      val category = OfferCategories.Cars
      val product = ProductId.Premium
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "find that offer premium doesn't exist" in {
      val offerId = 1002876493
      val category = OfferCategories.Cars
      val product = ProductId.Premium
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption shouldBe empty
    }

    "don't find non-billed product" in {
      val offerId = 1002876527
      val category = OfferCategories.Cars
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption shouldBe empty
    }

    "find that archived offer placement exists" in {
      val offerId = 1003332211
      val category = OfferCategories.Cars
      val product = ProductId.Placement
      val filter = AlreadyBilled(offerId, category, product)
      goodsDao.get(filter).success.value.headOption should not be empty
    }

    "get nothing for filter with empty result" in {
      goodsDao.get(Since(now().plusYears(1).getMillis)).success
      goodsDao.get(ForOfferCategory(-1L, TestCategory)).success
    }
    "get data since" in {
      val max = goodsDao.get(Since(0L)).success.value.map(_.epoch).max
      val sinceMax = goodsDao.get(Since(max.get)).success.value
      sinceMax should have size 22
      sinceMax.map(_.epoch).toList.distinct should have size 1
      goodsDao.get(Since(max.get + 1)).success.value shouldBe empty
      goodsDao.get(Since(max.get - 1)).success.value should not be empty
    }
    "get smth need activation" in {
      val partitions = Partition.all(16)
      val gs = partitions.flatMap { partition =>
        goodsDao
          .get(NeedActivation(1.hour, partition))
          .success
          .value
      }
      gs should have size 17
    }

    "get test record" in {
      val List(good) =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      good.createDate.getMillis should be < now().getMillis
      good.expireDate shouldBe empty
      good.category shouldBe TestCategory
      good.section shouldBe TestSection
      good.clientId shouldBe 16301
      good.product shouldBe ProductId.Placement
      good.status shouldBe GoodStatuses.Active
    }

    "update activate date" in {
      val max = goodsDao.get(Since(0L)).success.value.map(_.epoch).max
      // for epoch update
      Thread.sleep(1)

      val goodsBefore =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      goodsBefore should have size 1
      val before = goodsBefore.head

      goodsDao
        .update(
          Condition.WithGoodsId(before.primaryKeyId),
          Patch(
            firstActivateDate =
              Some(GoodsDao.Update(FirstActivateDate(now().plusMinutes(2))))
          )
        )
        .success

      val goodsAfter =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      goodsAfter should have size 1
      val after = goodsAfter.head

      before.epoch should not be after.epoch

      val sinceAfterMax = goodsDao.get(Since(max.get + 1)).success.value
      sinceAfterMax should have size 3
      sinceAfterMax.find(_.offerId != TestOffer) shouldBe None
      // there select all goods for changed offer
      sinceAfterMax
        .find(_.product == TestProduct)
        .value
        .epoch
        .value should be > max.value
    }

    "update expire date" in {
      val max = goodsDao.get(Since(0L)).success.value.map(_.epoch).max
      val goodsBefore =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      goodsBefore should have size 1
      val before = goodsBefore.head

      goodsDao
        .update(
          Condition.WithOfferProduct(TestOffer, TestCategory, TestProduct),
          Patch(expireDate = Some(now().plusMinutes(2)))
        )
        .success

      val goodsAfter =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      goodsAfter should have size 1
      val after = goodsAfter.head

      before.expireDate shouldBe empty
      after.expireDate should not be empty

      val gs = goodsDao.get(Since(max.get + 1)).success.value
      gs should have size 3
      gs.find(_.product == TestProduct).value.epoch.value should be > max.value
    }

    "update ob and ob deadline" in {
      val currentTime = now().withMillisOfSecond(0)
      val goodsBefore =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      goodsBefore should have size 1
      val before = goodsBefore.head
      before.offerBilling shouldBe empty
      before.offerBillingDeadline shouldBe empty

      goodsDao.get(WithOfferBillingSince(0L)).success.value.size shouldBe 28

      val patch = Patch(
        offerBilling = Some(GoodsDao.Update(Array[Byte]())),
        offerBillingDeadline = Some(GoodsDao.Update(currentTime.plusHours(2)))
      )

      goodsDao.update(Condition.WithGoodsId(before.primaryKeyId), patch).success

      val forOfferProduct =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      forOfferProduct should have size 1
      val forOfferProductGood = forOfferProduct.head
      forOfferProductGood.offerBilling should not be empty
      forOfferProductGood.offerBillingDeadline should not be empty
      val withOfferBilling =
        goodsDao.get(WithOfferBillingSince(0L)).success.value
      withOfferBilling should have size 29
      withOfferBilling.find(g =>
        g.offerId == TestOffer &&
        g.product == TestProduct
      ) should not be None
      goodsDao
        .get(NeedActivation(1.hour, Partition.all(1).head))
        .success
        .value
        .find(g =>
          g.offerId == TestOffer &&
          g.product == TestProduct
        )
        .flatMap(_.offerBillingDeadline)
        .get shouldBe currentTime.plusHours(2)
    }

    "update hold id" in {
      val goodsBefore =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      goodsBefore should have size 1
      val before = goodsBefore.head
      before.holdTransactionId shouldBe empty

      val patch = Patch(
        holdId = Some(GoodsDao.Update(""))
      )

      goodsDao.update(Condition.WithGoodsId(before.primaryKeyId), patch).success

      val goodsAfter =
        goodsDao.get(ForOfferProduct(TestOffer, TestProduct)).success.value
      goodsAfter should have size 1
      val after = goodsAfter.head
      after.holdTransactionId should not be empty
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
        .get(Since(0L))
        .success
        .value
        .head
        .status shouldBe GoodStatuses.Inactive
      goodsDao
        .get(ForOfferProduct(TestOffer, TestProduct))
        .success
        .value shouldBe empty
      val gs = goodsDao.get(ForOfferAnyStatus(TestOffer)).success.value
      gs should not be empty
      gs.find(g =>
        g.offerId == TestOffer &&
        g.product == TestProduct &&
        g.status == GoodStatuses.Inactive
      ) should not be None
      gs.map(_.status).toSet should (contain(GoodStatuses.Active)
        and contain(GoodStatuses.Inactive))
    }

    "update only selected Badge" in {
      val TestBadgesOffer = 1003332211L

      val recordsBefore = goodsDao
        .get(ForOfferProduct(TestBadgesOffer, ProductId.Badge))
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

      val after = goodsDao
        .get(ForOfferProduct(TestBadgesOffer, ProductId.Badge))
        .success
        .value
      after should have size 2
      after.head shouldBe firstRecord
      after.last.holdTransactionId.value shouldBe "badge-holdId"
    }

    "activate only selected Badge" in {
      val TestBadgesOffer = 1003332211L

      val recordsBefore = goodsDao
        .get(ForOfferProduct(TestBadgesOffer, ProductId.Badge))
        .success
        .value
      recordsBefore should have size 2

      val firstRecord = recordsBefore.head
      val secondRecord = recordsBefore.last

      goodsDao
        .update(
          Condition.WithGoodsId(firstRecord.primaryKeyId),
          Patch(status = Some(GoodStatuses.Inactive))
        )
        .success

      val after = goodsDao
        .get(ForOfferProduct(TestBadgesOffer, ProductId.Badge))
        .success
        .value
      after should have size 1
      after.head shouldBe secondRecord
    }

    "insert new goods record" in {
      val NewOffer = 1003276986L
      val NewStatus = GoodStatuses.Active
      val NewProduct = ProductId.Top
      val NewActivateDate = now().withMillisOfSecond(0)
      val request = Source(NewOffer, NewProduct, NewStatus, "", NewActivateDate)
      val inserted = goodsDao.insert(request).success.value
      inserted.offerId shouldBe NewOffer
      inserted.status shouldBe NewStatus
      inserted.product shouldBe NewProduct
      inserted.extraData shouldBe ""
      inserted.firstActivateDate shouldBe FirstActivateDate(NewActivateDate)
      val gs = goodsDao.get(ForOfferProduct(NewOffer, NewProduct)).success.value
      gs should have size 1
      val good = gs.head
      good.offerId shouldBe NewOffer
      good.status shouldBe NewStatus
      good.product shouldBe NewProduct
      good.extraData shouldBe ""
      good.firstActivateDate shouldBe FirstActivateDate(NewActivateDate)
      val withExtraData =
        goodsDao.insert(request.copy(extraData = "eeeeeee")).success.value
      withExtraData.offerId shouldBe NewOffer
      withExtraData.status shouldBe NewStatus
      withExtraData.product shouldBe NewProduct
      withExtraData.extraData shouldBe ""
      withExtraData.firstActivateDate shouldBe FirstActivateDate(
        NewActivateDate
      )
    }

    "insert one new record on two parallel requests" in {
      val offerId = 1003276986L
      val status = GoodStatuses.Active
      val product = ProductId.Special
      val activateDate = now().withMillisOfSecond(0)
      val request = Source(offerId, product, status, "", activateDate)
      goodsDao
        .get(ForOfferProduct(offerId, product))
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
        .get(ForOfferProduct(offerId, product))
        .success
        .value
        .size shouldBe 1
    }

    "insert good ready to be applied" in {
      val offerId = 1003276986L
      val product = ProductId.Fresh
      val status = GoodStatuses.Active
      val activateDate = ActivateDate(now().withMillisOfSecond(0))
      val expireDate = activateDate.plusDays(1)
      val offerBilling = List[Byte](0, 1, 2, 3).toArray
      val offerBillingDeadline = now().withMillisOfSecond(0).plusWeeks(1)
      val holdId = "test-hold"
      val offerHash = "2b81"
      val category = OfferCategories.Cars
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
        goodsDao.get(ForOfferProduct(offerId, product)).success.value
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
      countInactiveRecords() shouldBe 0
    }
  }

  private def countInactiveRecords(): Int =
    goodsDao.get(Since(0L)).get.count(_.status == GoodStatuses.Inactive)
}
