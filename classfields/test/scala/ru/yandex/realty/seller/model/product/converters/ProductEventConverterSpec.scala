package ru.yandex.realty.seller.model.product.converters

import billing.{CommonModel, LogModel}
import billing.LogModel.{PayerType, Platform, ProductEvent}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.seller.model.gen.SellerModelGenerators

@RunWith(classOf[JUnitRunner])
class ProductEventConverterSpec extends SpecBase with PropertyChecks with SellerModelGenerators {

  def verifyConstants(result: ProductEvent) = {
    result.getProject should be(CommonModel.Project.REALTY)
    result.getSource should be(LogModel.Source.UNKNOWN_SOURCE)
  }

  private val converter = DefaultProductEventConverter
  "ProductEventConverter" can {
    "accept only product" should {
      "convert absent purchase and random product correctly" in {
        val (product, purchase) = RandomProductWithNoPurchase
        verifyConstants(converter.convert(product, purchase).get)
      }
      "convert absent purchase and active product correctly" in {
        val (product, purchase) = ActiveProductWithNoPurchase
        val result = converter.convert(product, purchase).get
        result.getEventType should be(LogModel.EventType.ACTIVATION)
        result.getPlatform should be(Platform.DESKTOP)
        result.getDiscountsCount should be(4)
        result.getBasePriceKopecks should be(200L)
        result.getRefundedKopecks should be(0L)
        result.getSpentKopecks should be(200L)
        result.getPayerType should be(PayerType.NATURAL)
        verifyConstants(result)
      }

      "convert absent purchase and refunded product correctly" in {
        val (product, purchase) = RefundedProductWithNoPurchase
        val result = converter.convert(product, purchase).get
        result.getEventType should be(LogModel.EventType.DEACTIVATION)
        result.getPlatform should be(Platform.DESKTOP)
        result.getDiscountsCount should be(4)
        result.getBasePriceKopecks should be(200L)
        result.getRefundedKopecks should be(0L)
        result.getSpentKopecks should be(0L)
        result.getPayerType should be(PayerType.UNKNOWN_PAYER_TYPE)
        verifyConstants(result)
      }
    }
    "accept both product and purchase" should {
      "convert product correctly" in {
        val (product, purchase) = RandomProductWithPurchase
        verifyConstants(converter.convert(product, purchase).get)
      }
      "convert refunded purchase and product correctly" in {
        val (product, purchase) = RefundedProductWithPurchase
        val result = converter.convert(product, purchase).get
        result.getEventType should be(LogModel.EventType.REFUND)
        result.getPlatform should be(Platform.ANDROID)
        result.getDiscountsCount should be(4)
        result.getBasePriceKopecks should be(200L)
        result.getRefundedKopecks should be(200L)
        result.getSpentKopecks should be(0L)
        result.getPayerType should be(PayerType.UNKNOWN_PAYER_TYPE)
        verifyConstants(result)
      }

      "convert active purchase and product correctly" in {
        val (product, purchase) = ActiveProductWithPurchase
        val result = converter.convert(product, purchase).get
        result.getEventType should be(LogModel.EventType.ACTIVATION)
        result.getPlatform should be(Platform.DESKTOP)
        result.getDiscountsCount should be(4)
        result.getBasePriceKopecks should be(200L)
        result.getRefundedKopecks should be(0L)
        result.getSpentKopecks should be(200L)
        result.getPayerType should be(PayerType.NATURAL)
        verifyConstants(result)
      }
    }
  }

}
