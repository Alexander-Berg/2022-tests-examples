package ru.yandex.realty.seller.processing.schedule

import org.joda.time.DateTime
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.seller.dao.PurchasedProductDao
import ru.yandex.realty.seller.model.{PersonPaymentTypes, ProductType, PurchasedProductStatus}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{
  ManualSource,
  OfferTarget,
  ProductTypes,
  PurchaseTarget,
  PurchasedProduct,
  PurchasedProductStatuses
}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

class CreationProcessorSpec extends AsyncSpecBase with SellerModelGenerators {

  private val vosClient = mock[VosClientNG]
  private val productDao = mock[PurchasedProductDao]

  val processor = new ScheduleProductCreationProcessor(vosClient, productDao)

  private val user = PassportUser(1000)
  private val offer = readableString.next

  private def product(startTime: DateTime) =
    purchasedProductGen.next
      .copy(
        owner = user,
        target = OfferTarget(offer),
        product = ProductTypes.Raising,
        paymentType = PersonPaymentTypes.JuridicalPerson,
        source = ManualSource,
        startTime = Some(startTime)
      )

  private def hasProductForTemplateCall(productTemplate: ProductTemplate)(expected: PurchasedProduct) = {
    (productDao
      .getProducts(_: ProductType, _: PurchaseTarget, _: Set[PurchasedProductStatus]))
      .expects(
        productTemplate.productType,
        OfferTarget(productTemplate.offerId),
        Set(PurchasedProductStatuses.Active, PurchasedProductStatuses.Pending)
      )
      .returns(Future.successful(Seq(expected)))
  }

  "ScheduleProductCreationProcessor" must {
    "correctly return product existing" in {

      val time = DateTimeUtil.now()

      def template(startTime: DateTime) = ProductTemplate(
        user,
        offer,
        ProductTypes.Raising,
        ManualSource,
        startTime
      )

      val template1 = template(time)
      val product1 = product(time)
      hasProductForTemplateCall(template1)(product1)

      processor.hasProductForTemplate(template1).futureValue shouldBe true

      val template2 = template(time.plusMinutes(1))
      hasProductForTemplateCall(template2)(product1)

      processor.hasProductForTemplate(template2).futureValue shouldBe false

    }
  }

}
