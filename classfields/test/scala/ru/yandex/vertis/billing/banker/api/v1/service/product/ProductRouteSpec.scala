package ru.yandex.vertis.billing.banker.api.v1.service.product

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.api.v1.view.{ProductPatchView, ProductView}
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.{DuplicateProductIdException, ProductNotFoundException}
import ru.yandex.vertis.billing.banker.model.{PaymentSystemIds, Product}
import ru.yandex.vertis.billing.receipt.model.TaxTypes

import scala.concurrent.Future

class ProductRouteSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service/autoru/products/gate/trust"
  private def ps = allSetups.find(_.support.psId == PaymentSystemIds.Trust).map(_.support).get

  private val productId = "wallet"
  private val product = Product(productId, "Wallet", "Пополнение кошелька", TaxTypes.Nds20)

  "/ POST" should {
    import spray.json.enrichAny
    val view = ProductView.asView(product).toJson.compactPrint
    "return 409 for already existing product" in {
      when(ps.createProduct(product)).thenReturn(Future.failed(DuplicateProductIdException(productId)))
      Post(basePath).withEntity(ContentTypes.`application/json`, view) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.Conflict
      }
    }
    "create a new product" in {
      when(ps.createProduct(product)).thenReturn(Future.successful(product))
      Post(basePath).withEntity(ContentTypes.`application/json`, view) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[ProductView]
        response.id shouldBe productId
        response.name shouldBe product.name
        response.fiscalTitle shouldBe product.fiscalTitle
        response.fiscalNds shouldBe product.fiscalNds
      }
    }
  }

  "/{id} GET" should {
    "return 404 for un-existing product" in {
      when(ps.getProduct("unknown")).thenReturn(Future.failed(ProductNotFoundException(productId)))
      Get(url("/unknown")) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "return product by id" in {
      when(ps.getProduct(productId)).thenReturn(Future.successful(product))
      Get(url(s"/$productId")) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[ProductView]
        response.id shouldBe productId
        response.name shouldBe product.name
        response.fiscalTitle shouldBe product.fiscalTitle
        response.fiscalNds shouldBe product.fiscalNds
      }
    }
  }

  "/{id} PATCH" should {
    import spray.json.enrichAny
    val newFiscalNds = TaxTypes.`Nds18/118`
    val view = ProductPatchView(name = None, fiscalTitle = None, fiscalNds = Some(newFiscalNds)).toJson.compactPrint
    "return 404 for un-existing product" in {
      when(ps.updateProduct(eq("unknown"), ?)).thenReturn(Future.failed(ProductNotFoundException(productId)))
      Patch(url("/unknown")).withEntity(ContentTypes.`application/json`, view) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "update existing product" in {
      when(ps.updateProduct(productId, Product.Patch(fiscalNds = Some(newFiscalNds))))
        .thenReturn(Future.successful(product.copy(fiscalNds = newFiscalNds)))
      Patch(url(s"/$productId")).withEntity(ContentTypes.`application/json`, view) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[ProductView]
        response.fiscalNds shouldBe newFiscalNds
      }
    }
  }
}
