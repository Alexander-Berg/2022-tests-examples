package ru.yandex.realty.searcher.response.builders.inexact

import com.google.protobuf.util.JsonFormat
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.graph.core.{Name, Node}
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.proto.search.inexact.{InexactMatching, SubLocalityInexact}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._

import scala.collection.JavaConverters._

class SubLocalityInexactEnricherSpec extends WordSpec with Matchers with MockFactory {

  import ProtoHelper._

  val searchQuery = new SearchQuery()
  val querySubLocality = 17383352L
  searchQuery.setSubLocality(querySubLocality)

  val subLocalityInexactEnricher = new SubLocalityInexactEnricher(searchQuery)

  val builder = InexactMatching.newBuilder()
  val parser = JsonFormat.parser()

  "SubLocalityInexactEnricher" should {
    "checkAndEnrich" in new OfferBuilderContextFixture {
      val offerSubLocality: java.lang.Long = 587676L
      val fullName = "Серпуховской район"

      val districtNode = new Node()
      val name = mock[Name]
      (name.getFullName _).expects().returns(fullName)

      districtNode.setName(name)
      districtNode.setId(offerSubLocality)

      (dummyOfferBuilderContext.regionGraph.getNodeById _).expects(offerSubLocality).returns(districtNode)

      val location: Location = new Location()
      location.setDistricts(List(offerSubLocality).asJava)

      (dummyOfferBuilderContext.offer.getLocation _).expects().returns(location)

      val inexact =
        s"""{
           |  "inexact": {
           |     "items": [
           |       {
           |         "id": "$offerSubLocality",
           |         "name": "$fullName"
           |       }
           |     ]
           |  }
           |}""".stripMargin
          .toProto[SubLocalityInexact]

      val builder = InexactMatching.newBuilder()
      subLocalityInexactEnricher.checkAndEnrich(builder, dummyOfferBuilderContext).getSubLocality shouldBe inexact
    }
  }
}
