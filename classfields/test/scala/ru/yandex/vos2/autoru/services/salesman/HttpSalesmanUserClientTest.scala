package ru.yandex.vos2.autoru.services.salesman

import java.io.ByteArrayOutputStream
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.salesman.model.user.ApiModel.ProductPrice
import ru.yandex.vos2.util.http.MockHttpClientHelper
import org.scalatest.TryValues._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel
import ru.yandex.vos2.util.HttpBlockingPool.Instance
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vos2.model.SalesmanModelGenerator
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

@RunWith(classOf[JUnitRunner])
class HttpSalesmanUserClientTest
  extends AnyFunSuite
  with ScalaCheckPropertyChecks
  with Matchers
  with MockitoSupport
  with MockHttpClientHelper {

  implicit val trace = Traced.empty
  val operationalSupport = TestOperationalSupport

  val convertedOffer = ApiOfferModel.Offer.newBuilder.build

  ignore("get product prices list") {
    forAll(SalesmanModelGenerator.priceWithFeature.map(ProductPrice.newBuilder.setPrice(_).build)) { productPrice =>
      val salesmanUserClient = mockClientSuccess(productPrice)

      val autoruOffer = ApiOfferModel.Offer.newBuilder().build
      val res = salesmanUserClient.getOfferProductPrice(autoruOffer, SalesmanProductName.Placement).success.value
      res shouldBe List(productPrice)
    }
  }

  def mockClientSuccess(productPrice: ProductPrice): HttpSalesmanUserClient = {
    val binaryResponseSuccess = {
      val baos = new ByteArrayOutputStream()
      productPrice.writeDelimitedTo(baos)
      baos.toByteArray
    }

    new HttpSalesmanUserClient(hostname = "1", port = 1, operationalSupport) {
      override val client = new Instance(mockHttpClientBinary(200, binaryResponseSuccess))
    }
  }

  def mockClientEmptySuccess(): HttpSalesmanUserClient = {
    new HttpSalesmanUserClient(hostname = "1", port = 1, operationalSupport) {
      override val client = new Instance(
        mockHttpClientBinary(200, Array())
      )
    }
  }
}
