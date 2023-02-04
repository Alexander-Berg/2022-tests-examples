package ru.yandex.realty.buildinginfo.unification

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.common.util.IOUtils
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.buildinginfo.converter.BuildingCorrectionsVerbaConverter
import ru.yandex.realty.buildinginfo.model._
import ru.yandex.realty.buildinginfo.model.internal.AddressInfo
import ru.yandex.realty.context.ExtDataLoaders
import ru.yandex.realty.geocoder.cache.RegionGraphDistrictCache
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.message.RealtySchema.AddressComponentMessage
import ru.yandex.realty.model.offer.{BuildingType, Money}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.storage.verba.VerbaDictionary

import java.util.Collections
import scala.collection.JavaConverters._

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 19.10.17
  */
@RunWith(classOf[JUnitRunner])
class BuildingUnifierTest extends FlatSpec with Matchers {

  val regionGraph: RegionGraph = RegionGraphProtoConverter.deserialize(
    IOUtils.gunzip(
      getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
    )
  )

  val districtsCache = new RegionGraphDistrictCache()

  behavior of "Building unifier"

  it should "correct work" in {
    val verba = ExtDataLoaders.createVerbaStorage(getClass.getClassLoader.getResourceAsStream("verba2-3-4457.data"))
    val offerBuilding = new OfferBuilding.Builder()
    offerBuilding.offerId = 123L
    offerBuilding.updateTimestamp = DateTime.now().getMillis
    offerBuilding.latitude = 55f
    offerBuilding.longitude = 34f
    offerBuilding.buildingName = "test"
    offerBuilding.buildYear = 1987
    offerBuilding.totalFloors = 11
    offerBuilding.buildingType = BuildingType.BLOCK
    offerBuilding.ceilingHeight = 2.75f
    offerBuilding.sqmInRubles = Money.of(Currency.RUR, 30000)

    val rawBuilding = new RawBuilding.Builder()
    rawBuilding.buildYear = 1990
    rawBuilding.totalFloors = 12
    rawBuilding.rawBuildingType = "монолиТ"
    rawBuilding.heatingType = "ценТральное"
    rawBuilding.reconstructionYear = 1998

    val corrections = BuildingCorrectionsVerbaConverter.convert(
      verba.getRootDictionary(VerbaDictionary.BASE_HOMES),
      verba.getRootDictionary(VerbaDictionary.BUILDING_EPOCH)
    )

    val compositeBuilding = new CompositeBuilding(
      true,
      "test",
      GeoPoint.getPoint(59.9393f, 30.3337f),
      null,
      List(offerBuilding.build()).asJava,
      Map(BuildingSource.MOS_RU -> rawBuilding.build()).asJava,
      123L,
      null,
      List.empty[Metro].asJava,
      null,
      null,
      null,
      null,
      null,
      null,
      Collections.emptyList(),
      null,
      null,
      corrections.get(0),
      null
    )
    val result = BuildingUnifier.unify(compositeBuilding, verba, regionGraph, districtsCache)
    result.getPrice.size() should be(1)
    result.getRgid should be(NodeRgid.SPB_KALININSKIY_DISTRICT)
    result.getGeoId should be(Regions.SPB_KALININSKIY_DISTRICT)
    print(result)
  }

  it should "only count offer values from the same user/partner once" in {
    def makeOffer(uid: String, year: Int): OfferBuilding = {
      val offerBuilding = new OfferBuilding.Builder()
      offerBuilding.offerId = 123L
      offerBuilding.updateTimestamp = DateTime.now().getMillis
      offerBuilding.latitude = 55f
      offerBuilding.longitude = 34f
      offerBuilding.uid = uid
      offerBuilding.buildYear = year
      offerBuilding.build()
    }

    def makeCompositeBuilding(offers: List[OfferBuilding]) = new CompositeBuilding(
      true,
      "test",
      GeoPoint.getPoint(55f, 34f),
      null,
      offers.asJava,
      Map.empty[BuildingSource, RawBuilding].asJava,
      123L,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      Collections.emptyList(),
      null,
      null,
      null,
      null
    )

    val verba = ExtDataLoaders.createVerbaStorage(getClass.getClassLoader.getResourceAsStream("verba2-3-4457.data"))

    BuildingUnifier
      .unify(
        makeCompositeBuilding(
          List(
            makeOffer("1", 2000),
            makeOffer("2", 2000),
            makeOffer("3", 2000),
            makeOffer("4", 2000),
            makeOffer("5", 2000),
            makeOffer("100", 2001),
            makeOffer("101", 2001),
            makeOffer("102", 2001)
          )
        ),
        verba,
        regionGraph,
        districtsCache
      )
      .getBuildYear shouldBe Int.box(2000)

    BuildingUnifier
      .unify(
        makeCompositeBuilding(
          List(
            makeOffer("1", 2000),
            makeOffer("1", 2000),
            makeOffer("1", 2000),
            makeOffer("1", 2000),
            makeOffer("1", 2000),
            makeOffer("100", 2001),
            makeOffer("101", 2001),
            makeOffer("102", 2001)
          )
        ),
        verba,
        regionGraph,
        districtsCache
      )
      .getBuildYear shouldBe Int.box(2001)
  }

  it should "set house number" in {
    val verba = ExtDataLoaders.createVerbaStorage(getClass.getClassLoader.getResourceAsStream("verba2-3-4457.data"))
    val offerBuilding = new OfferBuilding.Builder()
    offerBuilding.offerId = 123L
    offerBuilding.updateTimestamp = DateTime.now().getMillis
    offerBuilding.latitude = 55f
    offerBuilding.longitude = 34f
    offerBuilding.buildingName = "test"
    offerBuilding.buildYear = 1987
    offerBuilding.totalFloors = 11
    offerBuilding.buildingType = BuildingType.BLOCK
    offerBuilding.ceilingHeight = 2.75f
    offerBuilding.sqmInRubles = Money.of(Currency.RUR, 30000)

    val rawBuilding = new RawBuilding.Builder()
    rawBuilding.buildYear = 1990
    rawBuilding.totalFloors = 12
    rawBuilding.rawBuildingType = "монолиТ"
    rawBuilding.heatingType = "ценТральное"
    rawBuilding.reconstructionYear = 1998

    val corrections = BuildingCorrectionsVerbaConverter.convert(
      verba.getRootDictionary(VerbaDictionary.BASE_HOMES),
      verba.getRootDictionary(VerbaDictionary.BUILDING_EPOCH)
    )

    val city = AddressComponentMessage
      .newBuilder()
      .setRegionType(RegionType.CITY)
      .setValue("Москва")
      .build()

    val street = AddressComponentMessage
      .newBuilder()
      .setRegionType(RegionType.STREET)
      .setValue("проспект")
      .build()

    val house = AddressComponentMessage
      .newBuilder()
      .setRegionType(RegionType.HOUSE)
      .setValue("15А")
      .build()

    val builder = AddressInfo.newBuilder()
    builder.setGeoId(Regions.MOSCOW)
    builder.addAllComponents(Seq(city, street, house).asJava)
    val addressInfo = builder.build()
    val compositeBuilding = new CompositeBuilding(
      true,
      "test",
      GeoPoint.getPoint(59.9393f, 30.3337f),
      addressInfo,
      List(offerBuilding.build()).asJava,
      Map(BuildingSource.MOS_RU -> rawBuilding.build()).asJava,
      123L,
      null,
      List.empty[Metro].asJava,
      null,
      null,
      null,
      null,
      null,
      null,
      Collections.emptyList(),
      null,
      null,
      corrections.get(0),
      null
    )
    val result = BuildingUnifier.unify(compositeBuilding, verba, regionGraph, districtsCache)

    result.getBuildingId shouldBe 123L
    result.getHouseNumber shouldBe "15А"
  }

}
