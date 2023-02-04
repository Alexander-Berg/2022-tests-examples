package ru.yandex.realty.seller.dao

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.DbSpecBase
import ru.yandex.realty.db.mysql.DuplicateRecordException
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.seller.dao.PurchaseDbActions.{Filter, Sort}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.purchase.PurchasePatch
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering
import slick.dbio.DBIOAction

trait PurchaseDbActionsSpec extends AsyncSpecBase with DbSpecBase with SellerModelGenerators {

  def purchaseDb: PurchaseDbActions

  "PurchaseDbActions" should {
    "handle duplicate inserts" in {
      val purchase = purchaseGen.next
      purchaseDb.insert(purchase).databaseValue.futureValue

      interceptCause[DuplicateRecordException] {
        purchaseDb.insert(purchase).databaseValue.futureValue
      }
    }
    "insert and get purchases" in {
      val purchases = listNUnique(10, purchaseGen)(_.id).next

      DBIOAction.sequence(purchases.map(p => purchaseDb.insert(p))).databaseValue.futureValue

      val gotByFilter = DBIOAction
        .sequence(purchases.map { p =>
          purchaseDb.get(Filter.ForId(p.id)).map(p -> _)
        })
        .databaseValue
        .futureValue

      gotByFilter.foreach {
        case (source, got) =>
          got.head shouldBe source
      }

      val gotById = DBIOAction
        .sequence(purchases.map { p =>
          purchaseDb.get(p.id).map(p -> _)
        })
        .databaseValue
        .futureValue

      gotById.foreach {
        case (source, got) =>
          got shouldBe source
      }
    }

    "correctly fail on single gets" in {
      interceptCause[NoSuchElementException] {
        purchaseDb.get("").databaseValue.futureValue
      }
      interceptCause[NoSuchElementException] {
        purchaseDb.getForUpdate("").databaseValue.futureValue
      }
    }

    "update purchases" in {
      val purchases = listNUnique(10, purchaseGen)(_.id).next

      val withPatches = purchases.map(p => p -> purchasePatchGen.next)

      DBIOAction.sequence(purchases.map(p => purchaseDb.insert(p))).databaseValue.futureValue

      val expected = withPatches.map {
        case (purchase, p) =>
          purchase.copy(
            status = p.status,
            deliveryStatus = p.deliveryStatus,
            basis = p.basis,
            visitTime = p.visitTime,
            renewalContext = p.renewalContext
          )
      }

      DBIOAction
        .sequence(withPatches.map {
          case (p, patch) => purchaseDb.update(p.id, patch)
        })
        .databaseValue
        .futureValue

      val got = DBIOAction.sequence(expected.map(e => purchaseDb.get(e.id).map(e -> _))).databaseValue.futureValue

      got.foreach {
        case (exp, actual) =>
          actual shouldBe exp
      }
    }

    "store history" in {
      val purchase = purchaseGen.next
      val patches = list(3, 6, purchasePatchGen).next

      purchaseDb.insert(purchase).databaseValue.futureValue

      DBIOAction.sequence(patches.map(p => purchaseDb.update(purchase.id, p))).databaseValue.futureValue

      val history = purchaseDb.getHistory(purchase.id).databaseValue.futureValue

      val insertPatch =
        PurchasePatch(purchase.status, purchase.deliveryStatus, purchase.basis, purchase.visitTime, None, None)

      val expectedPatches = insertPatch :: patches
      history.map(_.patch) should contain theSameElementsAs expectedPatches
    }

    "list purchases" in {
      val purchases = listNUnique(10, purchaseGen)(_.id).next
      val total = purchases.size
      val page = 5

      DBIOAction.sequence(purchases.map(p => purchaseDb.insert(p))).databaseValue.futureValue

      val first10 = purchaseDb.list(Page(0, 10))().databaseValue.futureValue
      val first5 = purchaseDb.list(Page(0, 5))().databaseValue.futureValue
      val second5 = purchaseDb.list(Page(1, 5))().databaseValue.futureValue

      first10.total shouldBe total
      first10.values should contain theSameElementsAs purchases

      first5.total shouldBe total
      first5.size shouldBe page
      second5.total shouldBe total
      second5.size shouldBe page

      (first5.values ++ second5.values) should contain theSameElementsAs first10.values

      val sortedAsc = purchaseDb.list(Page(0, 10))(Sort.TimeAsc).databaseValue.futureValue
      val sortedDesc = purchaseDb.list(Page(0, 10))(Sort.TimeDesc).databaseValue.futureValue

      sortedAsc.values.toList shouldBe purchases.sortBy(_.createTime)
      sortedDesc.values.toList shouldBe purchases.sortBy(_.createTime).reverse
    }
  }
}
