package ru.yandex.vertis.telepony.geo.impl

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.geo.model.{RegionType, TskvGeocodePhoneInterval}

import scala.io.{Codec, Source}

/**
  * @author neron
  */
class IntervalSpec extends SpecBase {

  private val regionTree = RegionTreeFactory.buildFromResource("/geobase.xml")(Codec.UTF8)
  private val src = Source.fromResource("regions.csv")(Codec.UTF8)
  private val intervals = src.getLines().collect { case TskvGeocodePhoneInterval(gi) => gi }.toList
  // Region has parent_geo_id = -1, when it eliminated
  // for example:
  // Region(geo_id=98601,name=Орехово-Зуевский район,parent=-1,type=HIDDEN(code:-1,desc:Скрытые))
  private val BlackHoleGeoId = -1

  // if it fails, you probably update geobase.xml and forget to update regions.csv
  "intervals" should {
    "use existing geo ids, that not eliminated. Has ascendant Country" in {
      intervals.foreach { interval =>
        val region = regionTree.getRegion(interval.geoId).futureValue
        val ascendants = regionTree.getAscendants(region.geoId).futureValue
        ascendants.map(_.geoId) should not contain BlackHoleGeoId
        ascendants.map(_.regionType) should contain(RegionType.COUNTRY)
      }
    }
  }

}
