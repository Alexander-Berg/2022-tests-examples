package ru.yandex.realty.searcher.query

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.proto.crypta.Profile
import ru.yandex.realty.experiments.AllSupportedExperiments
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.lucene.OfferDocumentFields
import ru.yandex.realty.model.offer.{CategoryType, FlatType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.relevance.RelevanceSwitcher

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RelevanceSwitcherTest extends WordSpec with MockFactory with Matchers with RegionGraphFixture {

  def searchQueryBase: SearchQuery = {

    val searchQuery = new SearchQuery()
    searchQuery.setCategory(CategoryType.APARTMENT)
    searchQuery.setFlatType(FlatType.SECONDARY)
    searchQuery.setBigbResponse(Profile.newBuilder().build())

    searchQuery
  }

  def getSwitcherField(
    rgid: Long,
    offerType: OfferType,
    experiment: String
  ): OfferDocumentFields = {

    val searchQuery = searchQueryBase
    searchQuery.setType(offerType)
    searchQuery.setSelectedRegionId(rgid.toInt)
    searchQuery.setExpFlags(experiment)

    RelevanceSwitcher.getRelevanceField(searchQuery)
  }

  "RelevanceSwitcher" should {
    // вторичная продажа
    "return in secondary Moscow field OfferDocumentFields.RELEVANCE_SECONDARY " in new RegionGraphFixture {
      val relevanceField = getSwitcherField(NodeRgid.MOSCOW_AND_MOS_OBLAST, OfferType.SELL, "")
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY
    }

    "return in secondary Moscow field OfferDocumentFields.RELEVANCE_SECONDARY_AB 2 " in new RegionGraphFixture {
      val relevanceField =
        getSwitcherField(NodeRgid.MOSCOW_AND_MOS_OBLAST, OfferType.SELL, "VSML-797_offline_exp_realtime_prod")
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY_AB
    }

    "return in secondary Moscow field OfferDocumentFields.RELEVANCE_SECONDARY 1 " in new RegionGraphFixture {
      val relevanceField =
        getSwitcherField(NodeRgid.MOSCOW_AND_MOS_OBLAST, OfferType.SELL, "VSML-797_offline_prod_realtime_exp")
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY
    }

    "return in secondary Spb field OfferDocumentFields.RELEVANCE_SECONDARY " in new RegionGraphFixture {
      val relevanceField = getSwitcherField(
        NodeRgid.SPB_AND_LEN_OBLAST,
        OfferType.SELL,
        ""
      )
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY
    }

    "return in secondary region field OfferDocumentFields.RELEVANCE_SECONDARY " in new RegionGraphFixture {
      val relevanceField = getSwitcherField(
        NodeRgid.KOSTROMA_OBLAST,
        OfferType.SELL,
        ""
      )
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY
    }

    // аренда
    "return in rent Moscow field OfferDocumentFields.RELEVANCE_SECONDARY " in new RegionGraphFixture {
      val relevanceField = getSwitcherField(NodeRgid.MOSCOW_AND_MOS_OBLAST, OfferType.RENT, "")
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY
    }

    "return in rent Spb field OfferDocumentFields.RELEVANCE_SECONDARY " in new RegionGraphFixture {
      val relevanceField = getSwitcherField(NodeRgid.SPB_AND_LEN_OBLAST, OfferType.RENT, "")
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY
    }

    "return in rent region field OfferDocumentFields.RELEVANCE_SECONDARY_AB " in new RegionGraphFixture {
      val relevanceField = getSwitcherField(NodeRgid.KOSTROMA_OBLAST, OfferType.RENT, "")
      relevanceField shouldEqual OfferDocumentFields.RELEVANCE_SECONDARY
    }
  }
}

trait RegionGraphFixture extends MockFactory {

  def createRegionGraph(rgid: Long): RegionGraph = {

    val geoNode = new Node()
    geoNode.setId(rgid)
    geoNode.setParentIds(
      List(NodeRgid.RUSSIA)
        .map(l => java.lang.Long.valueOf(l))
        .asJavaCollection
    )

    val RussiaNode = new Node()
    RussiaNode.setId(NodeRgid.RUSSIA)
    RussiaNode.setType("COUNTRY")

    val regionGraph: RegionGraph = mock[RegionGraph]
    (regionGraph.getNodeByGeoId _).expects(*).returns(geoNode).anyNumberOfTimes()
    (regionGraph.getParentNodes(_: Node)).expects(geoNode).returns(List(RussiaNode).asJava).anyNumberOfTimes()
    (regionGraph.getParentNodes(_: Node)).expects(RussiaNode).returns(List.empty.asJava).anyNumberOfTimes()
    regionGraph
  }
}
