package ru.yandex.realty.seller.dao

import org.joda.time.DateTime
import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.DbSpecBase
import ru.yandex.realty.db.mysql.DuplicateRecordException
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.seller.dao.PurchasedProductDbActions.{Filter, Sort}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{
  OfferTarget,
  ProductPatch,
  ProductTypes,
  PurchasedProduct,
  PurchasedProductStatuses
}
import ru.yandex.realty.seller.model.{PersonPaymentType, PersonPaymentTypes}
import ru.yandex.realty.sharding.Shard
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering
import slick.dbio.DBIOAction

trait PurchasedProductDbActionsSpec extends AsyncSpecBase with DbSpecBase with SellerModelGenerators {

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(Span(15, Seconds), Span(15, Millis))

  def productDb: PurchasedProductDbActions

  "PurchasedProductDbActions" should {
    "handle duplicate inserts" in {
      val product = purchasedProductGen.next
      productDb.insert(Iterable(product)).databaseValue.futureValue

      interceptCause[DuplicateRecordException] {
        productDb.insert(Iterable(product)).databaseValue.futureValue
      }
    }

    "insert and get purchased products" in {
      val products = listUnique(10, 20, purchasedProductGen)(_.id).next

      productDb.insert(products).databaseValue.futureValue

      val gotByFilter = DBIOAction
        .sequence(products.map { p =>
          productDb.get(Filter.ForId(p.id)).map(p -> _)
        })
        .databaseValue
        .futureValue

      gotByFilter.foreach {
        case (source, got) =>
          got.head shouldBe source
      }

      val gotById = DBIOAction
        .sequence(products.map { p =>
          productDb.get(p.id).map(p -> _)
        })
        .databaseValue
        .futureValue

      gotById.foreach {
        case (source, got) =>
          got shouldBe source
      }

      val grouped = products.groupBy(_.purchaseId).collect {
        case (Some(purchaseId), values) =>
          purchaseId -> values
      }

      val gotByPurchase = DBIOAction
        .sequence(grouped.map {
          case (purchaseId, group) =>
            productDb.get(Filter.ForPurchaseId(purchaseId)).map(group -> _)
        })
        .databaseValue
        .futureValue

      gotByPurchase.foreach {
        case (source, got) => got should contain theSameElementsAs source
      }

      grouped.headOption.foreach {
        case (purchaseId, head :: _) =>
          val got = productDb.get(Filter.ForPurchaseId(purchaseId), Filter.ForId(head.id)).databaseValue.futureValue
          got should (have size 1 and contain(head))
        case _ =>
      }
    }

    "correctly fail on single gets" in {
      interceptCause[NoSuchElementException] {
        productDb.get("").databaseValue.futureValue
      }
    }

    "update products" in {
      val products = listNUnique(10, purchasedProductGen)(_.id).next

      val withPatches = products.map(_ -> productPatchGen.next)

      productDb.insert(products).databaseValue.futureValue

      DBIOAction
        .sequence(withPatches.map {
          case (p, patch) => productDb.update(p.id, patch)
        })
        .databaseValue
        .futureValue

      val expected = withPatches.map {
        case (product, p) =>
          p.applyTo(product)
      }

      val got = DBIOAction.sequence(expected.map(e => productDb.get(e.id).map(e -> _))).databaseValue.futureValue

      got.foreach {
        case (exp, actual) =>
          actual shouldBe exp
      }
    }

    "store history" in {
      val product = purchasedProductGen.next
      val patches = list(3, 6, productPatchGen).next

      productDb.insert(Seq(product)).databaseValue.futureValue

      DBIOAction.sequence(patches.map(p => productDb.update(product.id, p))).databaseValue.futureValue

      val history = productDb.getHistory(product.id).databaseValue.futureValue
      val insertPatch = ProductPatch(
        product.startTime,
        product.endTime,
        product.status,
        product.deliveryStatus,
        product.visitTime,
        product.expirationPolicy,
        product.priceContext,
        product.billingContext,
        product.context,
        None
      )
      val expectedPatches = insertPatch :: patches
      history.map(_.patch) should contain theSameElementsAs expectedPatches
    }

    "delete patches for given interval" in {
      val product = purchasedProductGen.next
      val patches = list(3, 6, productPatchGen).next

      productDb.insert(Seq(product)).databaseValue.futureValue

      DBIOAction.sequence(patches.map(p => productDb.update(product.id, p))).databaseValue.futureValue

      val history = productDb.getHistory(product.id).databaseValue.futureValue
      val insertPatch = ProductPatch(
        product.startTime,
        product.endTime,
        product.status,
        product.deliveryStatus,
        product.visitTime,
        product.expirationPolicy,
        product.priceContext,
        product.billingContext,
        product.context,
        None
      )
      val expectedPatches = insertPatch :: patches
      history.map(_.patch) should contain theSameElementsAs expectedPatches

      productDb.deleteStalePatches(new DateTime(0), DateTime.now().plusDays(1)).databaseValue.futureValue

      val historyAfterDelete = productDb.getHistory(product.id).databaseValue.futureValue
      historyAfterDelete.isEmpty should be(true)
    }

    "delete patches for one day" in {
      val product = purchasedProductGen.next
      val patches = List(productPatchGen.next)

      productDb.insert(Seq(product)).databaseValue.futureValue

      DBIOAction.sequence(patches.map(p => productDb.update(product.id, p))).databaseValue.futureValue

      val history = productDb.getHistory(product.id).databaseValue.futureValue
      val insertPatch = ProductPatch(
        product.startTime,
        product.endTime,
        product.status,
        product.deliveryStatus,
        product.visitTime,
        product.expirationPolicy,
        product.priceContext,
        product.billingContext,
        product.context,
        None
      )
      val expectedPatches = insertPatch :: patches
      history.map(_.patch) should contain theSameElementsAs expectedPatches

      val now = new DateTime
      val fromTs = now.withTimeAtStartOfDay()
      val toTs = now.withTimeAtStartOfDay().plusDays(1).minusMillis(1)

      productDb.deleteStalePatches(fromTs, toTs).databaseValue.futureValue

      val historyAfterDelete = productDb.getHistory(product.id).databaseValue.futureValue
      historyAfterDelete.isEmpty should be(true)
    }

    "list products" in {
      val products = listNUnique(10, purchasedProductGen)(_.id).next
      val total = products.size
      val page = 5

      productDb.insert(products).databaseValue.futureValue

      val first10 = productDb.list(Page(0, 10))().databaseValue.futureValue
      val first5 = productDb.list(Page(0, 5))().databaseValue.futureValue
      val second5 = productDb.list(Page(1, 5))().databaseValue.futureValue

      first10.total shouldBe total
      first10.values should contain theSameElementsAs products

      first5.total shouldBe total
      first5.size shouldBe page
      second5.total shouldBe total
      second5.size shouldBe page

      (first5.values ++ second5.values) should contain theSameElementsAs first10.values

      val sortedTimeAsc = productDb.list(Page(0, 10))(Sort.TimeAsc).databaseValue.futureValue
      val sortedTimeDesc = productDb.list(Page(0, 10))(Sort.TimeDesc).databaseValue.futureValue

      val sortedVisitTimeAsc = productDb.list(Page(0, 10))(Sort.VisitTimeAsc).databaseValue.futureValue
      val sortedVisitTimeDesc = productDb.list(Page(0, 10))(Sort.VisitTimeDesc).databaseValue.futureValue

      sortedTimeAsc.values.toList shouldBe products.sortBy(_.createTime)
      sortedTimeDesc.values.toList shouldBe products.sortBy(_.createTime).reverse

      val sortByVisitTime = products.filter(_.visitTime.isDefined).sortBy(_.visitTime)
      sortedVisitTimeAsc.values.filter(_.visitTime.isDefined).toList shouldBe sortByVisitTime
      sortedVisitTimeDesc.values.filter(_.visitTime.isDefined).toList shouldBe sortByVisitTime.reverse
    }

    "get watch list of products" in {
      testWithPaymentType(PersonPaymentTypes.JuridicalPerson)
      testWithPaymentType(PersonPaymentTypes.NaturalPerson)

      def testWithPaymentType(personPaymentType: PersonPaymentType) {
        val shardsTotal = 2
        val toVisitRandomPaymentType = listNUnique(10, purchasedProductToBeVisitedGen)(_.id).next
        val toVisit = toVisitRandomPaymentType.map(p => p.copy(paymentType = personPaymentType))

        val notToVisitRandomPaymentType =
          listNUnique(10, purchasedProductGen)(_.id).next.map(_.copy(visitTime = None))
        val notToVisit = notToVisitRandomPaymentType.map(p => p.copy(paymentType = personPaymentType))

        val (first, second) = toVisit.partition(_.owner match {
          case PassportUser(uid) => uid % shardsTotal == 0
        })

        productDb.insert(toVisit ++ notToVisit).databaseValue.futureValue

        checkShard(0, first)
        checkShard(1, second)

        def checkShard(n: Int, products: List[PurchasedProduct]): Unit = {
          val total = products.size
          val page = total / 2 + 1

          val all = productDb
            .getWatchList(
              total,
              Shard(n, shardsTotal),
              personPaymentType
            )
            .databaseValue
            .futureValue
          val firstPage = productDb
            .getWatchList(page, Shard(n, shardsTotal), personPaymentType)
            .databaseValue
            .futureValue

          all.toList shouldBe products.sortBy(_.visitTime)
          firstPage.size shouldBe Math.min(page, total)
        }
      }
    }

    "get list of products by status and type in filter" in {
      val activePromotionProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Active, product = ProductTypes.Promotion))
      val activeRaisingProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Active, product = ProductTypes.Raising))
      val expiringPromotionProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Expired, product = ProductTypes.Promotion))
      val pendingRaisingProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Pending, product = ProductTypes.Raising))
      val activeTurboProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Active, product = ProductTypes.PackageTurbo))

      productDb
        .insert(
          activePromotionProducts ++ activeRaisingProducts ++
            expiringPromotionProducts ++ pendingRaisingProducts ++ activeTurboProducts
        )
        .databaseValue
        .futureValue

      val filterResult = productDb
        .get(
          Filter.ForStatuses(Set(PurchasedProductStatuses.Active)),
          Filter.ForTypes(Set(ProductTypes.Promotion, ProductTypes.Raising))
        )
        .databaseValue
        .futureValue

      filterResult.size should be(2)
      filterResult.count(_.status == PurchasedProductStatuses.Active) should be(2)
      filterResult.count(_.product == ProductTypes.Promotion) should be(1)
      filterResult.count(_.product == ProductTypes.Raising) should be(1)
    }

    "get list of products for offerIds and statuses" in {
      val activePromotionProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Active, product = ProductTypes.Promotion))
      val expiringPromotionProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Expired, product = ProductTypes.Promotion))
      val pendingRaisingProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Pending, product = ProductTypes.Raising))
      val activeTurboProducts = listNUnique(1, purchasedProductGen)(_.id).next
        .map(_.copy(status = PurchasedProductStatuses.Active, product = ProductTypes.PackageTurbo))

      productDb
        .insert(
          activePromotionProducts ++
            expiringPromotionProducts ++
            pendingRaisingProducts ++
            activeTurboProducts
        )
        .databaseValue
        .futureValue

      val filters =
        Seq(
          Filter.ForStatuses(Set(PurchasedProductStatuses.Active)),
          Filter.ForTargets(
            Set(
              activePromotionProducts.head.target,
              expiringPromotionProducts.head.target,
              activeTurboProducts.head.target,
              pendingRaisingProducts.head.target,
              OfferTarget("someId")
            )
          ),
          Filter.ForTypes(Set.empty),
          Filter.ForSources(Set.empty)
        )

      val filterResult = productDb.get(filters: _*).databaseValue.futureValue

      filterResult.size should be(2)
      filterResult.count(_.status == PurchasedProductStatuses.Active) should be(2)
      filterResult.count(_.product == ProductTypes.Promotion) should be(1)
      filterResult.count(_.product == ProductTypes.PackageTurbo) should be(1)
    }
  }
}
