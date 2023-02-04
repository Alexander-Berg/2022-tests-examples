package ru.yandex.realty.seller.model.packages

import org.junit.runner.RunWith
import org.scalatest.OptionValues
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering

import scala.concurrent.duration.DurationInt

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class RaisingPackageConverterSpec extends SpecBase with PropertyChecks with OptionValues with SellerModelGenerators {
  private val converter = RaisingPackageConverter

  "RaisingPackageConverter" should {
    "turn one Raising package product into several products" in {
      val products = converter.convert(RaisingProduct)
      products should have size 8

      products.foreach(_.visitTime.value should be <= DateTimeUtil.now())

      products.head.startTime shouldBe Some(StartTime)

      products.foreach { p =>
        p.purchaseId shouldBe Some(PurchaseId)
        p.owner shouldBe Owner
        p.product shouldBe ProductTypes.Raising
        p.target shouldBe Target
        p.source shouldBe PackageSource(Id)
        p.createTime shouldBe StartTime
        p.status shouldBe PurchasedProductStatuses.Pending
        p.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Pending
        p.priceContext shouldBe None
        p.billingContext shouldBe None
        p.context shouldBe ProductContext(1.day, Some(PaymentType.NATURAL_PERSON))
        p.expirationPolicy shouldBe Stop
      }
    }

    "throw an exception when trying to convert non-raising product" in {
      forAll(purchasedProductGen) { p =>
        an[IllegalArgumentException] should be thrownBy converter.convert(p)
      }
    }
  }

}
