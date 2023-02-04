package ru.auto.salesman.api.v1.service.campaign

import akka.http.scaladsl.model.StatusCodes.OK
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.model.AdsRequestTypes.CarsUsed
import ru.auto.salesman.model.payment_model.PlacementPaymentModel.{Quota, Single}
import ru.auto.salesman.model._
import ru.auto.salesman.service.CampaignShowcase
import spray.json._

class CampaignHandlerSpec extends RoutingSpec {

  private val mockCampaignShowcase = mock[CampaignShowcase]
  private val tag = "some_tag"
  private val campaignSize = 50000

  private val route = new CampaignHandler(mockCampaignShowcase).route

  "GET /campaign/client/{clientId}" should {

    "get enabled single cars:used campaign" in {
      val campaign =
        Campaign(
          Single(CarsUsed),
          tag,
          Category.CARS,
          Set(),
          Set(Section.USED),
          campaignSize,
          enabled = true
        )
      forAll(Gen.posNum[Long]) { clientId =>
        (mockCampaignShowcase
          .resolve(_: ClientId, _: IncludeDisabled, _: PaidOnly))
          .expects(clientId, IncludeDisabled(false), PaidOnly(false))
          .returningZ(Set(campaign))
        Get(s"/client/$clientId")
          .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
          status shouldBe OK
          responseAs[JsArray] shouldBe JsArray(
            JsObject(
              "paymentModel" -> JsString("single"),
              "tag" -> JsString(tag),
              "category" -> JsString("cars"),
              "subcategory" -> JsArray(),
              "section" -> JsArray(JsString("used")),
              "size" -> JsNumber(campaignSize),
              "enabled" -> JsBoolean(true)
            )
          )
        }
      }
    }

    "get disabled single cars:used campaign" in {
      val campaign =
        Campaign(
          Single(CarsUsed),
          tag,
          Category.CARS,
          Set(),
          Set(Section.USED),
          campaignSize,
          enabled = false
        )
      forAll(Gen.posNum[Long]) { clientId =>
        (mockCampaignShowcase
          .resolve(_: ClientId, _: IncludeDisabled, _: PaidOnly))
          .expects(clientId, IncludeDisabled(true), PaidOnly(true))
          .returningZ(Set(campaign))
        Get(s"/client/$clientId?include_disabled=true&paid_only=true")
          .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
          status shouldBe OK
          responseAs[JsArray] shouldBe JsArray(
            JsObject(
              "paymentModel" -> JsString("single"),
              "tag" -> JsString(tag),
              "category" -> JsString("cars"),
              "subcategory" -> JsArray(),
              "section" -> JsArray(JsString("used")),
              "size" -> JsNumber(campaignSize),
              "enabled" -> JsBoolean(false)
            )
          )
        }
      }
    }

    "get enabled quota atv campaign" in {
      val campaign = Campaign(
        Quota,
        tag,
        Category.MOTO,
        Set(OfferCategories.Atv),
        Set(Section.USED),
        campaignSize,
        enabled = true
      )
      forAll(Gen.posNum[Long]) { clientId =>
        (mockCampaignShowcase
          .resolve(_: ClientId, _: IncludeDisabled, _: PaidOnly))
          .expects(clientId, IncludeDisabled(false), PaidOnly(false))
          .returningZ(Set(campaign))
        Get(s"/client/$clientId")
          .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
          status shouldBe OK
          responseAs[JsArray] shouldBe JsArray(
            JsObject(
              "paymentModel" -> JsString("quota"),
              "tag" -> JsString(tag),
              "category" -> JsString("moto"),
              "subcategory" -> JsArray(JsString("atv")),
              "section" -> JsArray(JsString("used")),
              "size" -> JsNumber(campaignSize),
              "enabled" -> JsBoolean(true)
            )
          )
        }
      }
    }
  }
}
