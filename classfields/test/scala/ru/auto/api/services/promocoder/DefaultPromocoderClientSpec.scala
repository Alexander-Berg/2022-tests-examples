package ru.auto.api.services.promocoder

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes.{OK, PaymentRequired}
import org.scalatest.LoneElement
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.ErrorCode._
import ru.auto.api.exceptions.ApiException
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.AutoruUser
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.services.promocoder.PromocoderServices.AutoRuUsers
import ru.auto.api.services.promocoder.model.{FeatureDiscount, FeaturePayload, PromocoderUser}
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Request
import ru.auto.api.util.StringUtils._

class DefaultPromocoderClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest
  with LoneElement {

  val promocoderClient = new DefaultPromocoderClient(http)

  implicit override val request: Request = super.request

  "DefaultPromocoderClient" should {
    "get features" in {
      forAll(PrivateUserRefGen) { user =>
        val promocoderUser = PromocoderUser(user)
        val promocoderService = AutoRuUsers

        http.expectUrl(GET, url"/api/1.x/service/autoru-users/feature/user/autoru_common_${user.uid}")
        http.respondWithJson(
          OK,
          s"""[
               {
                 "user": "autoru_common_${user.uid}",
                 "createTs": "2018-03-19T15:08:50.569+03:00",
                 "jsonPayload": {
                   "is_personal": false,
                   "group_id": 0,
                   "discount": {
                     "value": 50,
                     "discountType": "percent"
                   }
                 },
                 "origin": {
                   "type": "Promocode",
                   "id": "test50sale:1e0c5f5f7cf4e8a"
                 },
                 "id": "highlighting:promo_test50sale:1e0c5f5f7cf4e8a",
                 "deadline": "2037-01-01T00:00:01.000+03:00",
                 "tag": "highlighting",
                 "count": 1
               },
               {
                 "user": "autoru_common_${user.uid}",
                 "createTs": "2018-03-22T16:38:36.503+03:00",
                 "jsonPayload": {
                   "discount": null,
                   "unit": "money",
                   "constraint": null,
                   "bundleParameters": null,
                   "featureType": "loyalty"
                 },
                 "origin": {
                   "type": "Api",
                   "id": "autoru_common_${user.uid}"
                 },
                 "id": "cashback:api_autoru_common_${user.uid}",
                 "deadline": "2020-12-16T16:38:36.503+03:00",
                 "tag": "cashback",
                 "count": 5400
               }
             ]"""
        )

        val response = promocoderClient.getFeatures(promocoderUser, promocoderService).await
        response.size shouldBe 2

        response.head.tag shouldBe "highlighting"
        response.head.count shouldBe 1
        response.head.jsonPayload shouldBe FeaturePayload(
          featureType = FeatureTypes.Promocode,
          unit = FeatureUnits.Items,
          constraint = None,
          discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 50)),
          groupId = None,
          bundleParameters = None
        )

        response(1).tag shouldBe "cashback"
        response(1).count shouldBe 5400
        response(1).jsonPayload shouldBe FeaturePayload(featureType = FeatureTypes.Loyalty, unit = FeatureUnits.Money)
      }
    }

    "parse feature correctly if discount value is string" in {
      forAll(PrivateUserRefGen) { user =>
        val promocoderUser = PromocoderUser(user)
        val promocoderService = AutoRuUsers

        http.expectUrl(GET, url"/api/1.x/service/autoru-users/feature/user/autoru_common_${user.uid}")
        http.respondWithJson(
          OK,
          s"""[
               {
                 "user": "autoru_common_${user.uid}",
                 "createTs": "2018-03-19T15:08:50.569+03:00",
                 "jsonPayload": {
                   "is_personal": false,
                   "group_id": 0,
                   "discount": {
                     "value": "50",
                     "discountType": "percent"
                   }
                 },
                 "origin": {
                   "type": "Promocode",
                   "id": "test50sale:1e0c5f5f7cf4e8a"
                 },
                 "id": "highlighting:promo_test50sale:1e0c5f5f7cf4e8a",
                 "deadline": "2037-01-01T00:00:01.000+03:00",
                 "tag": "highlighting",
                 "count": 1
               }
             ]"""
        )

        val response = promocoderClient.getFeatures(promocoderUser, promocoderService).futureValue

        response.loneElement.jsonPayload.discount.get.value shouldBe 50
      }
    }

    "activate promocode" in {
      forAll(PrivateUserRefGen) { user =>
        val promocode = "5efdnuasza"
        val promocoderUser = PromocoderUser(user)
        val promocoderService = AutoRuUsers

        http.expectUrl(POST, url"/api/1.x/service/autoru-users/promocode/$promocode/user/autoru_common_${user.uid}")
        http.respondWithJson(
          OK,
          """
            |{
            |  "message": "OK"
            |}
            |""".stripMargin
        )

        promocoderClient.activatePromocode(promocoderUser, promocoderService, promocode).await
      }
    }

    "read promocode data" in {
      val promocode = "5efdnuasza"
      val promocoderService = PromocoderServices.AutoRuUsers

      http.expectUrl(GET, url"/api/1.x/service/autoru-users/promocode/$promocode")

      http.respondWithJson(
        OK,
        """
      |
      |{
      |  "code": "string",
      |  "owner": "string",
      |  "features": [
      |    {
      |      "tag": "test_tag",
      |      "lifetime": "string",
      |      "count": 0,
      |      "payload": "string",
      |      "jsonPayload": {},
      |      "referring": {
      |        "user": "string",
      |        "feature": {
      |          "tag": "should_n_t_find_this_tag",
      |          "lifetime": "string",
      |          "payload": "string",
      |          "jsonPayload": "string"
      |        },
      |        "perCentCommission": 0,
      |        "fixedCommission": 0
      |      },
      |      "startTime": "2021-02-26T07:57:32.706Z"
      |    }
      |  ],
      |  "constraints": {
      |    "deadline": "string",
      |    "totalActivations": 0,
      |    "userActivations": 0,
      |    "blacklist": [
      |      "string"
      |    ]
      |  },
      |  "aliases": [
      |    "string"
      |  ]
      |}
      |""".stripMargin
      )

      val response = promocoderClient.getPromocodeTags(promocoderService, promocode).await
      response.loneElement shouldBe "test_tag"
    }
    "return error code = PROMOCODE_EXPIRED when promocoder responds with ERROR_ADD_EXPIRED" in {
      http.expectUrl(POST, "/api/1.x/service/autoru-users/promocode/test_promo/user/autoru_common_33108624")
      http.respondWithJson(
        PaymentRequired,
        """
          |{
          |  "code": "ERROR_ADD_EXPIRED",
          |  "message": "Promocode 'test_promo' expired"
          |}""".stripMargin
      )
      val result = promocoderClient
        .activatePromocode(PromocoderUser(AutoruUser(33108624)), AutoRuUsers, "test_promo")
        .failed
        .futureValue
        .asInstanceOf[ApiException]
      result.status shouldBe 402
      result.code shouldBe PROMOCODE_EXPIRED
    }

    "return error code = USER_ALREADY_ACTIVATED_PROMOCODE when promocoder responds with ERROR_ADD_USED_YOURS" in {
      http.expectUrl(POST, "/api/1.x/service/autoru-users/promocode/test_promo/user/autoru_common_33108624")
      http.respondWithJson(
        PaymentRequired,
        """
          |{
          |  "code": "ERROR_ADD_USED_YOURS",
          |  "message": "User activations limit exceed for promocode 'test_promo' and user 'autoru_common_33108624', limit is '1'"
          |}""".stripMargin
      )
      val result = promocoderClient
        .activatePromocode(PromocoderUser(AutoruUser(33108624)), AutoRuUsers, "test_promo")
        .failed
        .futureValue
        .asInstanceOf[ApiException]
      result.status shouldBe 402
      result.code shouldBe USER_ALREADY_ACTIVATED_PROMOCODE
    }

    "return error code = PROMOCODE_ACTIVATIONS_LIMIT_EXCEEDED when promocoder responds with ERROR_ADD_USED" in {
      http.expectUrl(POST, "/api/1.x/service/autoru-users/promocode/test_promo/user/autoru_common_33108624")
      http.respondWithJson(
        PaymentRequired,
        """
          |{
          |  "code": "ERROR_ADD_USED",
          |  "message": "Total activations limit exceed for promocode 'test_promo', limit is '2'"
          |}""".stripMargin
      )
      val result = promocoderClient
        .activatePromocode(PromocoderUser(AutoruUser(33108624)), AutoRuUsers, "test_promo")
        .failed
        .futureValue
        .asInstanceOf[ApiException]
      result.status shouldBe 402
      result.code shouldBe PROMOCODE_ACTIVATIONS_LIMIT_EXCEEDED
    }
  }
}
