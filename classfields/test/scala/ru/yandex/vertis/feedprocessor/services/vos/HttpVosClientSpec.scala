package ru.yandex.vertis.feedprocessor.services.vos

import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import ru.auto.api.ApiOfferModel.{Category, OfferStatus}
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.yandex.vertis.feedprocessor.BaseSpec
import ru.yandex.vertis.feedprocessor.services.vos.VosClient.SearchFilter
import ru.yandex.vertis.feedprocessor.util.{MockHttpClient, Resources}

class HttpVosClientSpec extends BaseSpec with BeforeAndAfter {

  implicit val scheduler = ActorSystem("test").scheduler

  val http = new MockHttpClient
  val vosClient: HttpVosClient = new HttpVosClient(http)

  before {
    http.reset()
  }

  "HttpVosClient" should {

    "request offers from each page" in {
      http.expect("/api/v1/offers/cars/dealer:16453?status=ACTIVE&page=1&page_size=20")
      http.respondWith(Resources.toProto[OfferListingResponse]("/vos_get_client_offers.json"))

      vosClient
        .getClientOffers(16453, SearchFilter(Category.CARS, status = Some(OfferStatus.ACTIVE)))
        .futureValue(Timeout(Span(5, Seconds)))
        .size shouldBe 2
    }

    "request multiposting offers from each page" in {
      http.expect("/api/v1/offers/cars/dealer:16453?multiposting_status=ACTIVE&page=1&page_size=20")
      http.respondWith(Resources.toProto[OfferListingResponse]("/vos_get_client_offers.json"))

      vosClient
        .getClientOffers(16453, SearchFilter(Category.CARS, multipostingStatus = Some(OfferStatus.ACTIVE)))
        .futureValue(Timeout(Span(5, Seconds)))
        .size shouldBe 2
    }

    "request offers from each page by tags" in {
      http.expect("/api/v1/offers/cars/dealer:16453?tag=tagA&tag=tagB&page=1&page_size=20")
      http.respondWith(Resources.toProto[OfferListingResponse]("/vos_get_client_offers.json"))

      vosClient
        .getClientOffers(16453, SearchFilter(Category.CARS, tags = Some(Seq("tagA", "tagB"))))
        .futureValue(Timeout(Span(5, Seconds)))
        .size shouldBe 2
    }
  }

}
