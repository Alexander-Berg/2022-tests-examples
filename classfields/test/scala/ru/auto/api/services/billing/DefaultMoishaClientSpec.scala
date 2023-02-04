package ru.auto.api.services.billing

import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.JsBoolean
import ru.auto.api.model.AutoruProduct.Placement
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}

class DefaultMoishaClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with OptionValues {

  private val moishaClient = new DefaultMoishaClient(http)

  private val regionId = 1
  private val cityId = None
  private val product = Placement
  private val offerPlacementDay = None

  "DefaultMoishaClient" should {

    "pass autoruExpert=true from offer to moisha" in {
      forAll(OfferGen) { baseOffer =>
        val offerB = baseOffer.toBuilder
        offerB.getAdditionalInfoBuilder.setAutoruExpert(true)
        val offer = offerB.build()
        http.respondWithJsonFrom("/moisha/response.json")

        // assert
        http.expectJsonField(_ \ "offer" \ "autoruExpert") shouldBe JsBoolean(true)

        // act
        moishaClient.getPrice(offer, regionId, cityId, product, offerPlacementDay).futureValue
      }
    }

    "pass autoruExpert=false from offer to moisha" in {
      forAll(OfferGen) { baseOffer =>
        val offerB = baseOffer.toBuilder
        offerB.getAdditionalInfoBuilder.setAutoruExpert(false)
        val offer = offerB.build()
        http.respondWithJsonFrom("/moisha/response.json")

        // assert
        http.expectJsonField(_ \ "offer" \ "autoruExpert") shouldBe JsBoolean(false)

        // act
        moishaClient.getPrice(offer, regionId, cityId, product, offerPlacementDay).futureValue
      }
    }

    "return duration from moisha" in {
      forAll(OfferGen) { offer =>
        http.respondWithJsonFrom("/moisha/response.json")
        val price = moishaClient.getPrice(offer, regionId, cityId, product, offerPlacementDay).futureValue
        price.product.duration.value.days shouldBe 1
      }
    }
  }
}
