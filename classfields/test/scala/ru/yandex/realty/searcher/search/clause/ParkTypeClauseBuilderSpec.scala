package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.{Location, Park, ParkType}
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{HasParkClauseBuilder, ParkTypeClauseBuilder}
import ru.yandex.realty.searcher.search.clause.ParkTypeClauseBuilderSpec._
import ru.yandex.realty.storage.ParkStorage

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ParkTypeClauseBuilderSpec extends SpecBase with NRTIndexFixture {

  private val clauseBuilders = Seq(new HasParkClauseBuilder())
  override lazy val parksStorage = ParkStorage(
    parks.map { p =>
      (p.getId -> p)
    }.toMap
  )

  insertOffers(offers)

  "ParkTypeClauseBuilder" should {
    "search offers near park" in {
      val result = searchByParkTypes(ParkType.PARK)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(_.getId).toSet shouldEqual Set(parkOffer, parkAndForestOffer).map(_.getId)
      result.getItems.asScala.foreach {
        case offer if offer.getId == parkOffer.getId =>
          offer.getLocation.getParks.size shouldBe 1
          offer.getLocation.getParks.get(0).getParkId shouldEqual park1.getId
        case offer if offer.getId == parkAndForestOffer.getId =>
          offer.getLocation.getParks.size shouldBe 2
          offer.getLocation.getParks.asScala.map(_.getParkId).toSet shouldEqual Set(park1, forest1).map(_.getId)
      }
    }

    "search offers near forest" in {
      val result = searchByParkTypes(ParkType.FOREST)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(_.getId).toSet shouldEqual Set(forestOffer, parkAndForestOffer).map(_.getId)
      result.getItems.asScala.foreach {
        case offer if offer.getId == forestOffer.getId =>
          offer.getLocation.getParks.size shouldBe 1
          offer.getLocation.getParks.get(0).getParkId shouldEqual forest1.getId
        case offer if offer.getId == parkAndForestOffer.getId =>
          offer.getLocation.getParks.size shouldBe 2
          offer.getLocation.getParks.asScala.map(_.getParkId).toSet shouldEqual Set(park1, forest1).map(_.getId)
      }
    }

    "search offers near garden" in {
      val result = searchByParkTypes(ParkType.GARDEN)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual gardenOffer.getId
      resultOffer.getLocation.getParks.size shouldBe 1
      resultOffer.getLocation.getParks.get(0).getParkId shouldEqual garden1.getId
    }

    "search offers near natpark" in {
      val result = searchByParkTypes(ParkType.NATPARK)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual natparkOffer.getId
      resultOffer.getLocation.getParks.size shouldBe 1
      resultOffer.getLocation.getParks.get(0).getParkId shouldEqual natpark1.getId
    }

    "search offers without parks" in {
      val searchQuery = new SearchQuery()
      searchQuery.setHasPark(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual noParksOffer.getId
      resultOffer.getLocation.getParks.isEmpty shouldBe true
    }

    "search offers having any parks" in {
      val searchQuery = new SearchQuery()
      searchQuery.setHasPark(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe offersWithParks.size
      result.getItems.asScala.map(_.getId).toSet shouldEqual offersWithParks.map(_.getId).toSet
    }
  }

  private def searchByParkTypes(parkTypes: ParkType*) = {
    val searchQuery = new SearchQuery()
    parkTypes.foreach(searchQuery.setParkType)
    search(searchQuery, Seq(new ParkTypeClauseBuilder()))
  }
}

object ParkTypeClauseBuilderSpec extends NRTIndexOfferGenerator {

  val park1 = new Park(1L, "Park1", ParkType.PARK, "", null)
  val forest1 = new Park(2L, "Forest1", ParkType.FOREST, "", null)
  val garden1 = new Park(3L, "Garden1", ParkType.GARDEN, "", null)
  val natpark1 = new Park(4L, "Natpark1", ParkType.NATPARK, "", null)
  val parks = Seq(park1, forest1, garden1, natpark1)

  val parkOffer = buildOffer(1, Seq(park1))
  val forestOffer = buildOffer(2, Seq(forest1))
  val gardenOffer = buildOffer(3, Seq(garden1))
  val natparkOffer = buildOffer(4, Seq(natpark1))
  val parkAndForestOffer = buildOffer(5, Seq(park1, forest1))
  val offersWithParks = Seq(parkOffer, forestOffer, gardenOffer, natparkOffer, parkAndForestOffer)
  val noParksOffer = buildOffer(6, Seq.empty)
  val offers = offersWithParks :+ noParksOffer

  def buildOffer(offerId: Long, parksToBind: Seq[Park]): Offer = {
    import ru.yandex.realty.proto.unified.offer.address.Park
    val offer = offerGen(offerId = offerId).next
    val location = new Location()
    location.setParks(
      parksToBind.map { p =>
        Park.newBuilder.setParkId(p.getId).build
      }.asJava
    )
    offer.setLocation(location)
    offer
  }
}
