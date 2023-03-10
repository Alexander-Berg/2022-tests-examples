package ru.yandex.realty.geohub.search

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.{PropertyChecks, TableFor4}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.entry.EntryType
import ru.yandex.realty.entry.EntryType.EntryType
import ru.yandex.realty.geo.RegionGraphTestComponents.{
  EkaterinburgCityNode,
  KazanCityNode,
  MoscowAndMosOblastNode,
  MoscowCityNode,
  SpbNode,
  YaroslavskayaOblastSubjectFederationNode
}
import ru.yandex.realty.geohub.search.entry.FoundEntry
import ru.yandex.realty.microdistricts.MicroDistrictsTestComponents.{elamash, gbi, pionersky, ugoZapadniy, vtuzgorodok}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.serialization.GeoPointProtoConverter
import ru.yandex.realty.railway.RailwayStationsTestComponents.finlyandsky
import ru.yandex.realty.sites.SitesServiceTestComponents.{capitalTowers, capitalTowersPhaseHouse1, willTower}

@RunWith(classOf[JUnitRunner])
class EntrySearcherSpec extends SpecBase with PropertyChecks with EntryIndexTestComponents {
  private val searcher = new EntrySearcherImpl(entryIndexProvider)

  private val gbiEntry = Set(FoundEntry(gbi.id, EntryType.TOPONYM, gbi.point))
  private val microDistricts =
    Set(gbi, vtuzgorodok, ugoZapadniy, elamash, pionersky).map(t => FoundEntry(t.id, EntryType.TOPONYM, t.point))
  private val finlandskiyEntry = Set(
    FoundEntry(finlyandsky.getEsr, EntryType.RAILWAY_STATION, GeoPointProtoConverter.fromMessage(finlyandsky.getPoint))
  )
  private val kazanEntry = Set(FoundEntry(KazanCityNode.getId.toLong, EntryType.CITY, KazanCityNode.getPoint))
  private val mskEntries = Set(
    FoundEntry(MoscowAndMosOblastNode.getId.toLong, EntryType.SUBJECT_FEDERATION, MoscowAndMosOblastNode.getPoint),
    FoundEntry(MoscowCityNode.getId.toLong, EntryType.CITY, MoscowCityNode.getPoint)
  )
  private val spbEntries = Set(FoundEntry(SpbNode.getId.toLong, EntryType.CITY, SpbNode.getPoint)) ++ finlandskiyEntry
  private val capitalTowersEntry = Set(
    FoundEntry(capitalTowers.getId, EntryType.ZHK, capitalTowers.getLocation.getGeocoderPoint)
  )
  private val capitalTowersHouseEntry = Set(
    FoundEntry(
      capitalTowersPhaseHouse1.getId,
      EntryType.SITE_BUILDING,
      capitalTowersPhaseHouse1.getLocation.getGeocoderPoint
    )
  )

  private val willTowerEntry = Set(
    FoundEntry(willTower.getId, EntryType.ZHK, willTower.getLocation.getGeocoderPoint)
  )
  private val data: TableFor4[String, Option[Long], Seq[EntryType.Value], Set[FoundEntry]] = Table(
    ("name", "rgid", "entryType", "expected"),
    ("??????", None, Seq.empty, gbiEntry),
    ("??????", None, Seq.empty, gbiEntry),
    ("????", None, Seq.empty, gbiEntry),
    ("???????????????????? ??", None, Seq.empty, gbiEntry),
    ("??????", None, Seq(EntryType.TOPONYM), gbiEntry),
    ("??????", Some(EkaterinburgCityNode.getId.toLong), Seq.empty, gbiEntry),
    (
      "??????",
      Some(EkaterinburgCityNode.getId.toLong),
      Seq(EntryType.TOPONYM),
      gbiEntry
    ),
    (
      "??????",
      Some(YaroslavskayaOblastSubjectFederationNode.getId.toLong),
      Seq(EntryType.TOPONYM),
      Set.empty
    ),
    ("??????", None, Seq(EntryType.ZHK), Set.empty),
    ("????????????????????", None, Seq.empty, microDistricts),
    ("????????", None, Seq.empty, microDistricts),
    ("??????????????????????", None, Seq.empty, finlandskiyEntry),
    ("????????????", None, Seq.empty, finlandskiyEntry),
    ("?????????????????????? ????????????", None, Seq.empty, finlandskiyEntry),
    ("????????????", None, Seq.empty, kazanEntry),
    ("????????????", None, Seq.empty, mskEntries),
    ("vjcrdf", None, Seq.empty, mskEntries),
    ("??????????-??????????????????", None, Seq.empty, spbEntries),
    ("Capital", Some(NodeRgid.MOSCOW_AND_MOS_OBLAST), Seq(EntryType.ZHK), capitalTowersEntry),
    ("Will", Some(NodeRgid.MOSCOW_AND_MOS_OBLAST), Seq(EntryType.ZHK), willTowerEntry),
    ("????????", Some(NodeRgid.MOSCOW_AND_MOS_OBLAST), Seq(EntryType.ZHK), willTowerEntry),
    ("Capital", Some(NodeRgid.MOSCOW_AND_MOS_OBLAST), Seq(EntryType.SITE_BUILDING), capitalTowersHouseEntry)
  )

  "EntrySearcher" should {

    forAll(data) { (name: String, rgid: Option[Long], entryType: Seq[EntryType], expected: Set[FoundEntry]) =>
      s"search entry with $name ${if (rgid.nonEmpty) s"and rgid: $rgid" else ""} ${if (entryType.nonEmpty) s"and entryType: $entryType"
      else ""}  " in {
        val query = EntrySearchQuery(name, rgid, entryType, resultSize = 10, geoPoint = MoscowAndMosOblastNode.getPoint)
        searcher.search(query).toSet shouldBe expected
      }
    }
  }
}
