package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.graph.{DocumentBuilderHelper, MutableRegionGraph, RegionGraph}
import ru.yandex.realty.lucene.ProtoLuceneDocumentBuilder
import ru.yandex.realty.model.location.{Location, SecondaryRegion}
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.GeoLocationClauseBuilder
import ru.yandex.realty.searcher.search.clause.GeoLocationClauseBuilderSpec._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class GeoLocationClauseBuilderSpec extends SpecBase with NRTIndexFixture {

  private lazy val clauseBuilders = Seq(new GeoLocationClauseBuilder(regionGraph))
  private val testRegionGraph: RegionGraph = buildRegionGraph()
  override val regionGraphProvider: Provider[RegionGraph] = () => testRegionGraph
  override val documentBuilderHelper = new DocumentBuilderHelper(regionGraphProvider)
  override val documentBuilder =
    new ProtoLuceneDocumentBuilder(
      regionGraphProvider,
      documentBuilderHelper,
      sitesService,
      extStatisticsProvider,
      campaignService,
      pondStorageProvider,
      parkStorageProvider,
      mdsUrlBuilder
    )

  insertOffers(offers)

  "GeoLocationClauseBuilderSpec" should {
    "search offer by location region graph id" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRgid(819861)

      val result = search(searchQuery, clauseBuilders)
      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual offerWithSecondaryRegions.getId
    }

    "search site offers near Tumen" in {
      // offer rgid is Tumenskay oblast but secondary regions contains Tumen city
      val searchQuery = new SearchQuery()
      searchQuery.setRgid(566725)

      val result = search(searchQuery, clauseBuilders)
      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual offerWithSecondaryRegions.getId
    }
  }

}

object GeoLocationClauseBuilderSpec extends NRTIndexOfferGenerator {

  val secondaryRegions = Seq(
    SecondaryRegion(819863, 9658),
    SecondaryRegion(566725, 362),
    SecondaryRegion(819862, 3227),
    SecondaryRegion(158758, 1353),
    SecondaryRegion(327035, 4230),
    SecondaryRegion(159774, 11656),
    SecondaryRegion(1938, 362),
    SecondaryRegion(799794, 12253),
    SecondaryRegion(327031, 396),
    SecondaryRegion(17385152, 5137)
  )

  val offerWithSecondaryRegions = buildOffer(1, 819861, secondaryRegions) // site 2210264 offer

  val offers = Seq(offerWithSecondaryRegions)

  def buildOffer(offerId: Long, rgid: Long, secondaryRegions: Seq[SecondaryRegion]): Offer = {
    val offer = offerGen(offerId = offerId).next
    val location = new Location()
    location.setRegionGraphId(rgid)
    location.setSecondaryRegions(secondaryRegions.asJava)
    offer.setLocation(location)
    offer
  }

  def buildRegionGraph(): RegionGraph = {
    def addNode(id: Long, parentId: Long, graph: MutableRegionGraph): Unit = {
      val node = new Node()
      node.setId(id)
      node.setParentIds(Seq(Long.box(parentId)).asJava)
      graph.addNode(node)
    }

    val regionGraph = new MutableRegionGraph()

    val root = new Node()
    root.setId(0)
    regionGraph.addNode(root)

    addNode(143, 0, regionGraph) // Russia
    addNode(250682, 143, regionGraph) // Tumenskay oblast
    addNode(250699, 250682, regionGraph) // Tumenskay district
    addNode(819863, 250699, regionGraph) // Gusevo city
    addNode(1938, 250682, regionGraph) // Tumenskiy okrug
    addNode(566725, 1938, regionGraph) // Tumen city
    addNode(819862, 250699, regionGraph) // Paderina city
    addNode(158758, 250699, regionGraph) // Moskovsckiy city
    addNode(327035, 250699, regionGraph) // Patrusheva city
    addNode(159774, 250699, regionGraph) // Lugovskoe city
    addNode(799794, 566725, regionGraph) // Berezenki city
    addNode(327031, 250699, regionGraph) // Derbiski city
    addNode(17385152, 1938, regionGraph) // Ojogina city
    addNode(819861, 250699, regionGraph) // Dudaraeva city

    regionGraph
  }
}
