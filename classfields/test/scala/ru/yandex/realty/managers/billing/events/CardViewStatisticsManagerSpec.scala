package ru.yandex.realty.managers.billing.events

import org.apache.commons.httpclient.HttpStatus
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.statistics.api.Request.{OffersAggregatedStatisticsRequest, StatisticComponent}
import realty.statistics.api.Response.{
  AggregatedStatisticsEntry,
  AggregatedStatisticsGranularEntry,
  OfferAggregatedEntry,
  OffersAggregatedStatisticsResponse
}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.statistics.{RawStatisticsClient, RawStatisticsClientImpl}
import ru.yandex.realty.http.{HttpEndpoint, MockHttpClient, RemoteHttpService, RequestTimeout}
import ru.yandex.realty.model.duration.TimeRange
import ru.yandex.realty.stat.AggregationStatLevel
import ru.yandex.realty.tracing.Traced

import java.net.SocketTimeoutException
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CardViewStatisticsManagerSpec extends AsyncSpecBase {
  implicit val t: Traced = Traced.empty

  final private val client = new MockHttpClient
  final private val httpService: RemoteHttpService = new RemoteHttpService(
    "mockHttpService",
    HttpEndpoint(""),
    client
  )

  before {
    client.reset()
  }

  after {
    client.reset()
  }

  "CardViewStatisticsManager" should {

    "not fail when request times out" in {
      val rawStatisticsClient: RawStatisticsClient = new RawStatisticsClientImpl(httpService)
      client.exception(RequestTimeout("service", "endpoint", new SocketTimeoutException()))

      val manager = new CardViewStatisticsManager(rawStatisticsClient)
      val error = manager
        .getStatistics("offerId", TimeRange.Empty)
        .recover { case e => e.getMessage }
        .futureValue
      error shouldBe "Timeout during execution of the request service.endpoint"
    }

    "not fail when request returns 400" in {
      val rawStatisticsClient: RawStatisticsClient = new RawStatisticsClientImpl(httpService)
      client.respond(HttpStatus.SC_BAD_REQUEST)

      val manager = new CardViewStatisticsManager(rawStatisticsClient)
      val error = manager
        .getStatistics("offerId", TimeRange.Empty)
        .recover { case e => e.getMessage }
        .futureValue
      error shouldBe "Unexpected response: HTTP/1.1 400 Bad Request, body = <null>"
    }

    "return offer cardShow statistic" in {
      val rawStatisticsClient = stub[RawStatisticsClient]

      val offerId = "offerId"
      val aggregatedStatisticsGranularEntry = AggregatedStatisticsGranularEntry.newBuilder
        .addEntries(AggregatedStatisticsEntry.newBuilder.setValue(11).setComponent(StatisticComponent.PHONE_SHOW))
        .addEntries(AggregatedStatisticsEntry.newBuilder.setValue(22).setComponent(StatisticComponent.CARD_SHOW))
        .addEntries(AggregatedStatisticsEntry.newBuilder.setValue(33).setComponent(StatisticComponent.OFFER_SHOW))
        .addEntries(AggregatedStatisticsEntry.newBuilder.setValue(44).setComponent(StatisticComponent.FAVOURITES_ADD))
        .build

      val response = OffersAggregatedStatisticsResponse.newBuilder
        .addOffers(
          OfferAggregatedEntry.newBuilder
            .setOfferId(offerId)
            .setPartnerId("partnerId")
            .addDetails(aggregatedStatisticsGranularEntry)
        )
        .build

      (rawStatisticsClient
        .offersAggregatedShowsStats(_: OffersAggregatedStatisticsRequest, _: AggregationStatLevel)(_: Traced))
        .when(*, AggregationStatLevel.All, *)
        .returning(Future.successful(response))

      val manager = new CardViewStatisticsManager(rawStatisticsClient)
      val longOpt = manager.getStatistics(offerId, TimeRange.Empty).futureValue
      longOpt shouldBe Some(22)
    }
  }
}
