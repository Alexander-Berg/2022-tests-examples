package ru.yandex.realty.searcher.search.clause

import com.google.protobuf.Duration
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.metro.MetroTransport
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.proto.unified.offer.address.{Metro, TransportDistance}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.GeoLocationClauseBuilder

import scala.collection.JavaConverters._

/**
  * метро + 10мин пешком -> что в точном поиске понятно, в неточном - пешком от 11 до 15 мин, отсекаются > 15 мин и те, что < 10 мин на транспорте
  */
@RunWith(classOf[JUnitRunner])
class TimeToMetroClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new GeoLocationClauseBuilder(regionGraph))
  private val offers: Seq[Offer] =
    Seq(
      (9, 2),
      (11, 3),
      (14, 3),
      (16, 4),
      (20, 6),
      (30, 11),
      (35, 13),
      (5, 2),
      (17, 6)
    ).zipWithIndex
      .map {
        case ((footTime, transportTime), index) =>
          buildOffer(index, footTime, transportTime)
      }

  insertOffers(offers)

  "TimeToMetroClauseBuilder" should {
    "search offers within 10 minutes on foot from metro" in {
      val searchQuery = new SearchQuery()
      searchQuery.setTimeToMetro(10)
      searchQuery.setMetroTransport(MetroTransport.ON_FOOT)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
    }

    "search offers within 10 minutes on foot from metro with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setTimeToMetro(10)
      searchQuery.setMetroTransport(MetroTransport.ON_FOOT)
      searchQuery.setShowSimilar(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 4
    }

    "search offers within 5 minutes on transport from metro" in {
      val searchQuery = new SearchQuery()
      searchQuery.setTimeToMetro(5)
      searchQuery.setMetroTransport(MetroTransport.ON_TRANSPORT)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 5
    }

    "search offers within 5 minutes on transport from metro with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setTimeToMetro(5)
      searchQuery.setMetroTransport(MetroTransport.ON_TRANSPORT)
      searchQuery.setShowSimilar(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 7
    }

  }

  private def buildOffer(
    offerId: Long,
    footTime: Int,
    transportTime: Int
  ): Offer = {
    val offer = offerGen(offerId = offerId).next

    val location = new Location()
    val durationFoot = Duration.newBuilder().setSeconds(footTime * 60).build()
    val foot = TransportDistance
      .newBuilder()
      .setTime(durationFoot)
      .setTransportType(TransportDistance.TransportType.ON_FOOT)
      .build()
    val durationTransport = Duration.newBuilder().setSeconds(transportTime * 60).build()
    val transport = TransportDistance
      .newBuilder()
      .setTime(durationTransport)
      .setTransportType(TransportDistance.TransportType.PUBLIC_TRANSPORT)
      .build()
    val metro = Metro.newBuilder().addDistances(foot).addDistances(transport).build()
    location.setMetro(Seq(metro).asJava)
    offer.setLocation(location)

    offer
  }
}
