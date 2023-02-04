package ru.yandex.realty.managers.offers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.search.Count.{BatchedNumberOfOffers, BatchedNumberOfOffersRequest, NumberOfOffers, NumberOfOffersRequest}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.personal.history.HistoryClient
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.request.{Request, RequestImpl}
import ru.yandex.realty.services.ComplaintService
import ru.yandex.realty.telepony.TeleponyClient
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.crypto.Crypto

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class OffersManagerSpec extends SpecBase with HttpClientMock {

  "offersManager" should {
    implicit val executionContext: ExecutionContext =
      ExecutionContext.global
    implicit val r: Request = new RequestImpl
    val teleponyClient = mock[TeleponyClient]
    val searcherClient = mock[SearcherClient]
    val offerCallsClient = mock[HistoryClient[String]]
    val complaintService = mock[ComplaintService]
    val crypto = mock[Crypto]
    val offersManager =
      new OffersManager(teleponyClient, searcherClient, offerCallsClient, crypto, complaintService)
    "return number which take from searcher if page size < number  " in {
      val paramsWithAllExtendableFilters =
        Map(
          "type" -> List("SELL"),
          "category" -> List("APARTMENT"),
          "pageSize" -> List("10"),
          "priceMin" -> List("1000000"),
          "priceMax" -> List("1000000"),
          "leftLongitude" -> List("30.125786"),
          "topLatitude" -> List("60.111073"),
          "rightLongitude" -> List("30.939378"),
          "bottomLatitude" -> List("59.87927"),
          "extendableGroupKey" -> List("price", "viewport"),
          "countExtended" -> List("YES")
        )
      val number = 25
      val searcherFullResponse = NumberOfOffers.newBuilder().setTotalOffers(number).build()
      (searcherClient
        .countOffers(_: NumberOfOffersRequest, _: Option[String])(_: Traced))
        .expects(*, *, *)
        .returning(Future(searcherFullResponse))
        .once()

      val responseF = offersManager.getNumberOfOffers(paramsWithAllExtendableFilters)
      val response = Await.result(responseF, 1.seconds)
      response.getResponse.getNumber should be(number)
      response.getResponse.getExtendedNumber should be(0)
    }

    "return number with extend if page size > number and extended enable " in {
      val paramsWithAllExtendableFilters =
        Map(
          "type" -> List("SELL"),
          "category" -> List("APARTMENT"),
          "pageSize" -> List("20"),
          "priceMin" -> List("1000000"),
          "priceMax" -> List("1000000"),
          "leftLongitude" -> List("30.125786"),
          "topLatitude" -> List("60.111073"),
          "rightLongitude" -> List("30.939378"),
          "bottomLatitude" -> List("59.87927"),
          "extendableGroupKey" -> List("price", "viewport"),
          "countExtended" -> List("YES")
        )
      val number = 10
      val extendedNumber = 25
      val searcherFullResponse = NumberOfOffers.newBuilder().setTotalOffers(number).build()
      (searcherClient
        .countOffers(_: NumberOfOffersRequest, _: Option[String])(_: Traced))
        .expects(*, *, *)
        .returning(Future(searcherFullResponse))
        .once()

      val numbers = Seq(16, 15, 10)
      val searcherBatchedExtendedResponse = BatchedNumberOfOffers
        .newBuilder()
        .addAllNumber(numbers.map(num => NumberOfOffers.newBuilder().setTotalOffers(num).build()).asJava)
        .build()

      (searcherClient
        .batchedCountOffers(_: BatchedNumberOfOffersRequest)(_: Traced))
        .expects(*, *)
        .returning(Future(searcherBatchedExtendedResponse))
        .once()

      val responseF = offersManager.getNumberOfOffers(paramsWithAllExtendableFilters)
      val response = Await.result(responseF, 1.seconds)
      response.getResponse.getNumber should be(number)
      response.getResponse.getExtendedNumber should be(21)
    }

    "return number without extend if page size > number but extended disable " in {
      val paramsWithAllExtendableFilters =
        Map(
          "type" -> List("SELL"),
          "category" -> List("APARTMENT"),
          "pageSize" -> List("20"),
          "priceMin" -> List("1000000"),
          "priceMax" -> List("1000000"),
          "leftLongitude" -> List("30.125786"),
          "topLatitude" -> List("60.111073"),
          "rightLongitude" -> List("30.939378"),
          "bottomLatitude" -> List("59.87927")
        )
      val number = 10
      val searcherFullResponse = NumberOfOffers.newBuilder().setTotalOffers(number).build()
      (searcherClient
        .countOffers(_: NumberOfOffersRequest, _: Option[String])(_: Traced))
        .expects(*, *, *)
        .returning(Future(searcherFullResponse))
        .once()

      val responseF = offersManager.getNumberOfOffers(paramsWithAllExtendableFilters)
      val response = Await.result(responseF, 1.seconds)
      response.getResponse.getNumber should be(number)
      response.getResponse.getExtendedNumber should be(0)
    }
  }

}
