package ru.yandex.realty.searcher.controllers.street

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.context.street.StreetStorage
import ru.yandex.realty.geocoder.AsyncGeocoder
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.{GeoObjectType, Node}
import ru.yandex.realty.proto.api.v2.geo.StreetRegion
import ru.yandex.realty.proto.geo.street.StreetUnifiedAddress
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class StreetManagerSpec extends AsyncSpecBase {

  val regionGraph: RegionGraph = mock[RegionGraph]
  val regionGraphProvider: Provider[RegionGraph] = () => regionGraph
  val streetStorage: StreetStorage = mock[StreetStorage]
  val geocoder: AsyncGeocoder = mock[AsyncGeocoder]

  trait StreetManagerFixture {

    val manager = new StreetManager(
      regionGraphProvider,
      streetStorage
    )
  }

  "StreetManagerSpec" should {
    "streetRegion should return region of type CITY" in new StreetManagerFixture {
      val address = "streetName"

      val streetId = 10000
      val cityId = 10002

      val cityGeoId = 102
      val countryGeoId = 103

      val streetUnifiedAddress: StreetUnifiedAddress = StreetUnifiedAddress
        .newBuilder()
        .setUnifiedAddress(address)
        .setStreetId(streetId)
        .setRgid(cityId)
        .setCityRgid(cityId)
        .build()

      val city: Node = Node.createNodeForGeoObjectType(GeoObjectType.CITY)
      city.setId(cityId)
      city.setGeoId(cityGeoId)
      city.setParentIds(Seq(Long.box(countryGeoId)).asJava)

      (streetStorage.getStreetUnifiedAddress(_: Int)).expects(streetId).returning(Some(streetUnifiedAddress))
      (regionGraph.getNodeById(_: java.lang.Long)).expects(Long.box(cityId)).returning(city)

      private val cityRegion: StreetRegion = manager.streetRegion(streetId)(Traced.empty).futureValue

      cityRegion.getRgid shouldBe cityId
      cityRegion.getGeoId shouldBe cityGeoId
      cityRegion.getStreetId shouldBe streetId
      cityRegion.getStreetName shouldBe address
    }

    "streetRegion should return initial node if region of type CITY was not found" in
      new StreetManagerFixture {
        val address = "streetName"

        val streetId = 10000
        val subjectDistrictId = 10001
        val subjectFedId = 10002

        val streetGeoId = 100
        val districtGeoId = 101
        val subjectFedGeoId = 102

        val streetUnifiedAddress: StreetUnifiedAddress = StreetUnifiedAddress
          .newBuilder()
          .setUnifiedAddress(address)
          .setStreetId(streetId)
          .build()

        val street: Node = Node.createNodeForGeoObjectType(GeoObjectType.STREET)
        street.setId(streetId)
        street.setGeoId(streetGeoId)
        street.setParentIds(Seq(Long.box(districtGeoId)).asJava)

        val subjectFedDistrict: Node = Node.createNodeForGeoObjectType(GeoObjectType.SUBJECT_FEDERATION_DISTRICT)
        subjectFedDistrict.setId(subjectDistrictId)
        subjectFedDistrict.setGeoId(districtGeoId)
        subjectFedDistrict.setParentIds(Seq(Long.box(subjectFedGeoId)).asJava)

        val subjectFed: Node = Node.createNodeForGeoObjectType(GeoObjectType.SUBJECT_FEDERATION)
        subjectFed.setId(subjectFedId)
        subjectFed.setGeoId(subjectFedGeoId)

        (streetStorage.getStreetUnifiedAddress(_: Int)).expects(streetId).returning(Some(streetUnifiedAddress))
        (regionGraph.getNodeById(_: java.lang.Long)).expects(Long.box(streetUnifiedAddress.getRgid)).returning(street)
        (regionGraph
          .getRandomParent(_: Node))
          .expects(street)
          .returning(subjectFedDistrict)
        (regionGraph
          .getRandomParent(_: Node))
          .expects(subjectFedDistrict)
          .returning(null)
        (regionGraph.getNodeById(_: java.lang.Long)).expects(Long.box(streetUnifiedAddress.getRgid)).returning(street)

        private val result: StreetRegion = manager.streetRegion(streetId)(Traced.empty).futureValue

        result.getRgid shouldBe streetId
        result.getGeoId shouldBe streetGeoId
        result.getStreetId shouldBe streetId
        result.getStreetName shouldBe address
      }
  }
}
