package ru.yandex.realty.graph

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.graph.RegionTreeTestComponents.regionTree
import ru.yandex.realty.graph.region.{CustomRegionAttribute, Region, RegionTree, RegionType}

trait RegionTreeTestComponents {
  lazy val regionTreeProvider: Provider[RegionTree[Region]] = () => regionTree
}

object RegionTreeTestComponents {

  val Russia = build(
    225,
    "Россия",
    RegionType.COUNTRY,
    null,
    Map(
      CustomRegionAttribute.POPULATION -> "146880432",
      CustomRegionAttribute.ZOOM -> "3",
      CustomRegionAttribute.PHONE_CODE -> "7"
    )
  )

  val CentralFederalDistrict =
    build(
      3,
      "Центральный федеральный округ",
      RegionType.COUNTRY_DISTRICT,
      Russia,
      Map(
        CustomRegionAttribute.POPULATION -> "39311413",
        CustomRegionAttribute.ZOOM -> "5",
        CustomRegionAttribute.PHONE_CODE -> "7"
      )
    )

  val NorthwesternFederalDistrict =
    build(
      17,
      "Северо-Западный федеральный округ",
      RegionType.COUNTRY_DISTRICT,
      Russia,
      Map(
        CustomRegionAttribute.POPULATION -> "13952003",
        CustomRegionAttribute.ZOOM -> "3",
        CustomRegionAttribute.PHONE_CODE -> "7",
        CustomRegionAttribute.LAT -> "61.469749",
        CustomRegionAttribute.LON -> "36.498137",
        CustomRegionAttribute.SPN_LAT -> "27.567908",
        CustomRegionAttribute.SPN_LON -> "49.842547"
      )
    )

  val SpbAndLo =
    build(
      10174,
      "Санкт-Петербург и Ленинградская область",
      RegionType.SUBJECT_FEDERATION,
      NorthwesternFederalDistrict,
      Map(
        CustomRegionAttribute.POPULATION -> "1813816",
        CustomRegionAttribute.ZOOM -> "8",
        CustomRegionAttribute.PHONE_CODE -> "812 813",
        CustomRegionAttribute.LAT -> "59.337013",
        CustomRegionAttribute.LON -> "29.608975",
        CustomRegionAttribute.SPN_LAT -> "2.912674",
        CustomRegionAttribute.SPN_LON -> "9.141184"
      )
    )

  val Spb =
    build(
      2,
      "Санкт-Петербург",
      RegionType.CITY,
      SpbAndLo,
      Map(
        CustomRegionAttribute.POPULATION -> "5351935",
        CustomRegionAttribute.ZOOM -> "11",
        CustomRegionAttribute.PHONE_CODE -> "812",
        CustomRegionAttribute.LAT -> "59.938951",
        CustomRegionAttribute.LON -> "30.315635",
        CustomRegionAttribute.SPN_LAT -> "0.611098",
        CustomRegionAttribute.SPN_LON -> "1.334420"
      )
    )

  val MskAndMo = build(
    1,
    "Москва и Московская область",
    RegionType.SUBJECT_FEDERATION,
    CentralFederalDistrict,
    Map(
      CustomRegionAttribute.POPULATION -> "7503385",
      CustomRegionAttribute.ZOOM -> "8",
      CustomRegionAttribute.PHONE_CODE -> "495 496 498 499"
    )
  )

  val Msk = build(
    213,
    "Москва",
    RegionType.CITY,
    MskAndMo,
    Map(
      CustomRegionAttribute.POPULATION -> "12506468",
      CustomRegionAttribute.ZOOM -> "10",
      CustomRegionAttribute.PHONE_CODE -> "495 499"
    )
  )

  val VolokolamskiyDistrict =
    build(
      98580,
      "Волоколамский район",
      RegionType.SUBJECT_FEDERATION_DISTRICT,
      MskAndMo,
      Map(
        CustomRegionAttribute.POPULATION -> "41496",
        CustomRegionAttribute.ZOOM -> "9",
        CustomRegionAttribute.PHONE_CODE -> "49636 496"
      )
    )

  val EasternAdministrativeDistrict =
    build(
      20358,
      "Восточный административный округ",
      RegionType.CITY_DISTRICT,
      Msk,
      Map(
        CustomRegionAttribute.POPULATION -> "1515942",
        CustomRegionAttribute.ZOOM -> "14",
        CustomRegionAttribute.PHONE_CODE -> "495 499"
      )
    )

  val SokolnikiDistrict = build(
    116995,
    "район Сокольники",
    RegionType.SECONDARY_DISTRICT,
    EasternAdministrativeDistrict,
    Map(
      CustomRegionAttribute.POPULATION -> "59958",
      CustomRegionAttribute.ZOOM -> "13",
      CustomRegionAttribute.PHONE_CODE -> "495 499"
    )
  )

  val SokolnikiMetro = build(
    20402,
    "Сокольники",
    RegionType.METRO_STATION,
    SokolnikiDistrict,
    Map(
      CustomRegionAttribute.POPULATION -> "0",
      CustomRegionAttribute.ZOOM -> "13",
      CustomRegionAttribute.PHONE_CODE -> "495 499"
    )
  )

  val regionTree = new RegionTree[Region](Russia)

  private def build(
    id: Int,
    name: String,
    regionType: RegionType,
    parent: Region,
    customAttributes: Map[CustomRegionAttribute, String]
  ): Region = {
    val region = new Region(id, name, regionType, parent)
    customAttributes.foreach { case (attr, value) => region.setCustomAttributeValue(attr, value) }
    region
  }
}
