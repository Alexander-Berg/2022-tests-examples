package ru.yandex.realty.seller.service.impl

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.seller.dao.PurchasedProductDao
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{
  ManualSource,
  PackageSource,
  ProductTypes,
  PurchaseTarget,
  PurchasedProduct,
  PurchasedProductStatuses
}
import ru.yandex.realty.seller.model.{ProductType, PurchasedProductStatus}
import ru.yandex.vertis.generators.ProtobufGenerators
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering

import scala.concurrent.Future

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class ProductSequencerImplSpec extends AsyncSpecBase with PropertyChecks {

  import ProductSequencerImplSpec._

  private val productsDao: PurchasedProductDao = mock[PurchasedProductDao]

  private val sequencer = new ProductSequencerImpl(productsDao)
  private val allowedStatuses = Set(PurchasedProductStatuses.Active, PurchasedProductStatuses.Pending)

  "ProductSequencerImpl" should {
    "do nothing to raising package product" in {
      val filteredGen = RaisingProductGen
        .filterNot(p => p.source == ManualSource || p.source.isInstanceOf[PackageSource])
      forAll(filteredGen) { p =>
        sequencer.sequenceProduct(p).futureValue shouldBe p
      }
    }

    "do nothing if no active or pending products present" in {
      forAll(SuitableProductGen) { p =>
        (productsDao
          .getProducts(_: ProductType, _: PurchaseTarget, _: Set[PurchasedProductStatus]))
          .expects(p.product, p.target, allowedStatuses)
          .returning(Future.successful(Seq.empty))

        sequencer.sequenceProduct(p).futureValue shouldBe p
      }
    }

    "do nothing to inactive product" in {
      forAll(InactiveProductGen) { p =>
        sequencer.sequenceProduct(p).futureValue shouldBe p
      }
    }

    "do not shift product if broken products with end time in past found" in {
      forAll(SuitableProductGen, listUnique(1, 5, BrokenProductInPastGen)(_.id)) { (p, brokenProducts) =>
        (productsDao
          .getProducts(_: ProductType, _: PurchaseTarget, _: Set[PurchasedProductStatus]))
          .expects(p.product, p.target, allowedStatuses)
          .returning(Future.successful(brokenProducts))

        sequencer.sequenceProduct(p).futureValue shouldBe p
      }
    }

    "sequence product" in {
      forAll(SuitableProductGen, listUnique(1, 5, purchasedProductGen)(_.id)) { (p, products) =>
        (productsDao
          .getProducts(_: ProductType, _: PurchaseTarget, _: Set[PurchasedProductStatus]))
          .expects(p.product, p.target, allowedStatuses)
          .returning(Future.successful(products))

        val result = sequencer.sequenceProduct(p).futureValue

        val lastEnd = products
          .filter(p => p.endTime.isDefined)
          .sortBy(_.endTime)
          .lastOption
          .flatMap(_.endTime)

        sequencer.actualDuration(result) shouldBe sequencer.actualDuration(p)

        if (lastEnd.isDefined) {
          result.startTime shouldBe lastEnd
          result.status shouldBe PurchasedProductStatuses.Pending
        } else {
          result shouldBe p
        }
      }
    }
  }
}

object ProductSequencerImplSpec extends SellerModelGenerators with ProtobufGenerators {

  val SuitableProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    pt <- Gen.oneOf(productTypeGen, packageProductTypeGen).filter(_ != ProductTypes.Raising)
    s <- Gen.oneOf(
      Gen.const(PurchasedProductStatuses.Active),
      Gen.const(PurchasedProductStatuses.Pending)
    )
  } yield product.copy(product = pt, status = s)

  val InactiveProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    pt <- Gen.oneOf(productTypeGen, packageProductTypeGen)
    s <- Gen.oneOf(
      Gen.const(PurchasedProductStatuses.Expired),
      Gen.const(PurchasedProductStatuses.Cancelled)
    )
  } yield product.copy(product = pt, status = s)

  val RaisingProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
  } yield product.copy(product = ProductTypes.Raising)

  val BrokenProductInPastGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    pt <- Gen.oneOf(productTypeGen, packageProductTypeGen).filter(_ != ProductTypes.Raising)
    s <- Gen.oneOf(
      Gen.const(PurchasedProductStatuses.Active),
      Gen.const(PurchasedProductStatuses.Pending)
    )
    endTime <- dateTimeInPast
  } yield product.copy(product = pt, status = s, endTime = Some(endTime))
}
