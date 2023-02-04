package ru.auto.api.services.parsing

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel
import ru.auto.api.ResponseModel.{ErrorResponse, ParsedOfferInfoResponse}
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.StringUtils.RichString

/**
  * Created by andrey on 11/30/17.
  */
class DefaultParsingClientSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {
  private val parsingClient = new DefaultParsingClient(http)

  implicit private val req = {
    val r = new RequestImpl
    r.setTrace(trace)
    //    r.setUser(UserInfo("1.2.3.4", None, None, None, UserRef.anon("42"), None))
    r.setApplication(Application.iosApp)
    r
  }

  "ParsingClient" should {
    "return offer for given hash" in {
      val hash1 = "dec4870408897288b21ae4bdee408d39"
      http.expectUrl(GET, s"/v1/offer?hash=$hash1")
      http.respondWithProtoFrom[ApiOfferModel.Offer](OK, "/parsing/parsed_offer.json")
      val draft = parsingClient.getOffer(Some(hash1)).futureValue
      draft.getTruckInfo.getMark shouldBe "MERCEDES"
    }

    "return offer for given hash and custom phone" in {
      val hash = "dec4870408897288b21ae4bdee408d39"
      val phone = "79293336677"
      http.expectUrl(GET, s"/v1/offer?hash=$hash&phone=$phone")
      http.respondWithProtoFrom[ApiOfferModel.Offer](OK, "/parsing/parsed_offer.json")
      val draft = parsingClient.getOffer(Some(hash), None, Some(phone)).futureValue
      draft.getTruckInfo.getMark shouldBe "MERCEDES"
    }

    "return 404 on unknown hash" in {
      val hash1 = "hash1"
      http.expectUrl(GET, s"/v1/offer?hash=$hash1")
      http.respondWithProtoFrom[ErrorResponse](NotFound, "/parsing/parsed_offer_not_found.json")
      intercept[OfferNotFoundException] {
        parsingClient.getOffer(Some(hash1)).await
      }
    }

    "return offer info for given hash" in {
      val hash1 = "dec4870408897288b21ae4bdee408d39"
      http.expectUrl(GET, s"/v1/offer/info?hash=$hash1")
      http.respondWithProtoFrom[ParsedOfferInfoResponse](OK, "/parsing/parsed_offer_info.json")
      val offerInfo = parsingClient.getOfferInfo(hash1).futureValue
      offerInfo.getName shouldBe "Mercedes-Benz 814"
      offerInfo.getPhone shouldBe "79376640118"
      offerInfo.getOwnerName shouldBe "Вячеслав"
      offerInfo.getCanPublish.getValue shouldBe true
    }

    "return offer for given url" in {
      val url = "https://m.avito.ru/penza/avtomobili/ssangyong_korando_1997_440422701"
      http.expectUrl(GET, s"/v1/offer?url=${url.escaped}")
      http.respondWithProtoFrom[ApiOfferModel.Offer](OK, "/parsing/parsed_offer.json")
      val draft = parsingClient.getOffer(optUrl = Some(url)).futureValue
      draft.getTruckInfo.getMark shouldBe "MERCEDES"
    }

    "return 404 as offer info for unknown hash" in {
      val hash1 = "hash1"
      http.expectUrl(GET, s"/v1/offer/info?hash=$hash1")
      http.respondWithProtoFrom[ErrorResponse](NotFound, "/parsing/parsed_offer_not_found.json")
      intercept[OfferNotFoundException] {
        parsingClient.getOfferInfo(hash1).await
      }
    }

    "return 200 OK for set-not_published call" in {
      val hash1 = "dec4870408897288b21ae4bdee408d39"
      http.expectUrl(PUT, s"/v1/offer/not-published?hash=$hash1&reason=noanswer")
      http.respondWith(OK, "{}")
      parsingClient.setRejectReason(hash1, "noanswer").futureValue
    }

    "return 404 for set-not_published call for unknown hash" in {
      val hash1 = "dec4870408897288b21ae4bdee408d39"
      http.expectUrl(PUT, s"/v1/offer/not-published?hash=$hash1&reason=noanswer")
      http.respondWithProtoFrom[ErrorResponse](NotFound, "/parsing/parsed_offer_not_found.json")
      intercept[OfferNotFoundException] {
        parsingClient.setRejectReason(hash1, "noanswer").await
      }
    }
  }
}
