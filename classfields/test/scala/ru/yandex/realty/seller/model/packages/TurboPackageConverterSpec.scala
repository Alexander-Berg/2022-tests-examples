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
class TurboPackageConverterSpec extends SpecBase with PropertyChecks with OptionValues with SellerModelGenerators {
  private val converter = TurboPackageConverter

  "TurboPackageConverter" should {
    "turn one Turbo package product into several products" in {
      val products = converter.convert(TurboProduct)
      products should have size 10

      products.foreach(_.visitTime.value should be <= DateTimeUtil.now())

      val premium = products.find(_.product == ProductTypes.Premium).get

      premium.purchaseId shouldBe Some(PurchaseId)
      premium.owner shouldBe Owner
      premium.product shouldBe ProductTypes.Premium
      premium.target shouldBe Target
      premium.source shouldBe PackageSource(Id)
      premium.createTime shouldBe StartTime
      premium.startTime shouldBe Some(StartTime)
      premium.endTime shouldBe Some(StartTime.plusDays(7))
      premium.status shouldBe PurchasedProductStatuses.Pending
      premium.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Pending
      premium.priceContext shouldBe None
      premium.billingContext shouldBe None
      premium.context shouldBe ProductContext(7.days, Some(PaymentType.NATURAL_PERSON))
      premium.expirationPolicy shouldBe Stop

      val promotion = products.find(_.product == ProductTypes.Promotion).get

      promotion.purchaseId shouldBe Some(PurchaseId)
      promotion.owner shouldBe Owner
      promotion.product shouldBe ProductTypes.Promotion
      promotion.target shouldBe Target
      promotion.source shouldBe PackageSource(Id)
      promotion.createTime shouldBe StartTime
      promotion.startTime shouldBe Some(StartTime)
      promotion.endTime shouldBe Some(StartTime.plusDays(7))
      promotion.status shouldBe PurchasedProductStatuses.Pending
      promotion.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Pending
      promotion.priceContext shouldBe None
      promotion.billingContext shouldBe None
      promotion.context shouldBe ProductContext(7.days, Some(PaymentType.NATURAL_PERSON))
      promotion.expirationPolicy shouldBe Stop

      val rises = products.filter(_.product == ProductTypes.Raising)
      rises.length shouldBe 8
      rises.head.startTime shouldBe Some(StartTime)
    }

    "throw an exception when trying to convert non-turbo product" in {
      forAll(purchasedProductGen) { p =>
        an[IllegalArgumentException] should be thrownBy converter.convert(p)
      }
    }
  }

}
