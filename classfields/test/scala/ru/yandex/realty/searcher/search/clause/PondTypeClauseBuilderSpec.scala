package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.{Location, Pond, PondType}
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.searcher.query.clausebuilder.HasPondClauseBuilder
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.PondTypeClauseBuilder
import ru.yandex.realty.searcher.search.clause.PondTypeClauseBuilderSpec._
import ru.yandex.realty.storage.PondStorage

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class PondTypeClauseBuilderSpec extends SpecBase with NRTIndexFixture {

  private val clauseBuilders = Seq(new HasPondClauseBuilder())
  override lazy val pondsStorage = PondStorage(
    ponds.map { p =>
      (p.getId -> p)
    }.toMap
  )

  insertOffers(offers)

  "PondTypeClauseBuilder" should {
    "search offers near river" in {
      val result = searchByPondTypes(PondType.RIVER)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(_.getId).toSet shouldEqual Set(riverOffer, riverAndLakeOffer).map(_.getId)
      result.getItems.asScala.foreach {
        case offer if offer.getId == riverOffer.getId =>
          offer.getLocation.getPonds.size shouldBe 1
          offer.getLocation.getPonds.get(0).getPondId shouldEqual river1.getId
        case offer if offer.getId == riverAndLakeOffer.getId =>
          offer.getLocation.getPonds.size shouldBe 2
          offer.getLocation.getPonds.asScala.map(_.getPondId).toSet shouldEqual Set(river1, lake1).map(_.getId)
      }
    }

    "search offers near lake" in {
      val result = searchByPondTypes(PondType.LAKE)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(_.getId).toSet shouldEqual Set(lakeOffer, riverAndLakeOffer).map(_.getId)
      result.getItems.asScala.foreach {
        case offer if offer.getId == lakeOffer.getId =>
          offer.getLocation.getPonds.size shouldBe 1
          offer.getLocation.getPonds.get(0).getPondId shouldEqual lake1.getId
        case offer if offer.getId == riverAndLakeOffer.getId =>
          offer.getLocation.getPonds.size shouldBe 2
          offer.getLocation.getPonds.asScala.map(_.getPondId).toSet shouldEqual Set(river1, lake1).map(_.getId)
      }
    }

    "search offers near pond" in {
      val result = searchByPondTypes(PondType.POND)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual pondOffer.getId
      resultOffer.getLocation.getPonds.size shouldBe 1
      resultOffer.getLocation.getPonds.get(0).getPondId shouldEqual pond1.getId
    }

    "search offers near bay" in {
      val result = searchByPondTypes(PondType.BAY)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual bayOffer.getId
      resultOffer.getLocation.getPonds.size shouldBe 1
      resultOffer.getLocation.getPonds.get(0).getPondId shouldEqual bay1.getId
    }

    "search offers near sea" in {
      val result = searchByPondTypes(PondType.SEA)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual seaOffer.getId
      resultOffer.getLocation.getPonds.size shouldBe 1
      resultOffer.getLocation.getPonds.get(0).getPondId shouldEqual sea1.getId
    }

    "search offers near canal" in {
      val result = searchByPondTypes(PondType.CANAL)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual canalOffer.getId
      resultOffer.getLocation.getPonds.size shouldBe 1
      resultOffer.getLocation.getPonds.get(0).getPondId shouldEqual canal1.getId
    }

    "search offers without ponds" in {
      val searchQuery = new SearchQuery()
      searchQuery.setHasPond(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual noPondsOffer.getId
      resultOffer.getLocation.getPonds.isEmpty shouldBe true
    }

    "search offers having any ponds" in {
      val searchQuery = new SearchQuery()
      searchQuery.setHasPond(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe offersWithPonds.size
      result.getItems.asScala.map(_.getId).toSet shouldEqual offersWithPonds.map(_.getId).toSet
    }
  }

  private def searchByPondTypes(pondTypes: PondType*) = {
    val searchQuery = new SearchQuery()
    pondTypes.foreach(searchQuery.setPondType)
    search(searchQuery, Seq(new PondTypeClauseBuilder()))
  }
}

object PondTypeClauseBuilderSpec extends NRTIndexOfferGenerator {

  val river1 = new Pond(1L, "River1", PondType.RIVER, "", null)
  val lake1 = new Pond(2L, "Lake1", PondType.LAKE, "", null)
  val pond1 = new Pond(3L, "Pond1", PondType.POND, "", null)
  val bay1 = new Pond(4L, "Bay1", PondType.BAY, "", null)
  val sea1 = new Pond(5L, "Sea1", PondType.SEA, "", null)
  val canal1 = new Pond(6L, "Canal1", PondType.CANAL, "", null)
  val reservoir1 = new Pond(7L, "Reservoir1", PondType.RESERVOIR, "", null)

  val ponds = Seq(river1, lake1, pond1, bay1, sea1, canal1, reservoir1)

  val riverOffer = buildOffer(1, Seq(river1))
  val lakeOffer = buildOffer(2, Seq(lake1))
  val pondOffer = buildOffer(3, Seq(pond1))
  val bayOffer = buildOffer(4, Seq(bay1))
  val seaOffer = buildOffer(5, Seq(sea1))
  val canalOffer = buildOffer(6, Seq(canal1))
  val reservoirOffer = buildOffer(7, Seq(reservoir1))
  val riverAndLakeOffer = buildOffer(8, Seq(river1, lake1))

  val offersWithPonds =
    Seq(riverOffer, lakeOffer, pondOffer, bayOffer, seaOffer, canalOffer, reservoirOffer, riverAndLakeOffer)
  val noPondsOffer = buildOffer(9, Seq.empty)
  val offers = offersWithPonds :+ noPondsOffer

  def buildOffer(offerId: Long, pondsToBind: Seq[Pond]): Offer = {
    import ru.yandex.realty.proto.unified.offer.address.Pond
    val offer = offerGen(offerId = offerId).next
    val location = new Location()
    location.setPonds(
      pondsToBind.map { p =>
        Pond.newBuilder.setPondId(p.getId).build
      }.asJava
    )
    offer.setLocation(location)
    offer
  }
}
