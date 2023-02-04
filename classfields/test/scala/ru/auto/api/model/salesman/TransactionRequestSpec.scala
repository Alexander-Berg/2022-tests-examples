package ru.auto.api.model.salesman

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.SalesmanModelGenerators._

class TransactionRequestSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "TransactionRequest factory" should {

    "create request" in {
      forAll(PrivateUserRefGen, SalesmanObjectIdGen, ProductGen, ProductPriceGen, ProlongableGen) {
        (user, objectId, product, productPrice, prolongable) =>
          val request =
            TransactionRequest(user, objectId, product, productPrice, prolongable, Map.empty[String, String])
          request.getUser shouldBe user.toPlain
          request.getAmount shouldBe productPrice.getPrice.getEffectivePrice
          request.getPayloadCount shouldBe 1
          val payload = request.getPayload(0)
          payload.getProduct shouldBe product.name
          payload.getOffer shouldBe objectId.toPlain
          payload.getAmount shouldBe productPrice.getPrice.getEffectivePrice
          payload.getContext shouldBe ProductContext(product, productPrice, vinOrLicencePlate = None, garageId = None)
          payload.getProlongable shouldBe prolongable.value
      }
    }
  }
}
