package ru.yandex.realty.geo

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.graph.core.GeoObjectType.{
  CITY,
  CITY_DISTRICT,
  COUNTRY,
  SUBJECT_FEDERATION,
  SUBJECT_FEDERATION_DISTRICT
}
import ru.yandex.realty.graph.core.{GeoObjectType, Name, Node}
import ru.yandex.realty.graph.{MutableRegionGraph, RegionGraph}
import ru.yandex.realty.model.geometry.{Geometry, Polygon}
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.region.{NodeRgid, Regions}

trait RegionGraphTestComponents {
  val regionGraphProvider: Provider[RegionGraph] = RegionGraphTestComponents.regionGraphProvider
}

object RegionGraphTestComponents {

  private def geoPoint(latitude: Float, longitude: Float): GeoPoint =
    new GeoPoint(latitude, longitude)

  private def node(
    rgid: Long,
    geoId: Int,
    name: String,
    `type`: GeoObjectType,
    point: GeoPoint,
    lt: GeoPoint,
    rb: GeoPoint,
    geometry: Option[Geometry] = None,
    official: Option[String] = None
  ) = {
    val node = Node.createNodeForGeoObjectType(`type`)
    val nodeName = new Name
    nodeName.setDisplay(name)
    official.foreach(nodeName.setOfficial)
    node.setId(rgid)
    node.setGeoId(geoId)
    node.setName(nodeName)
    node.setPoint(point)
    node.setLt(lt)
    node.setRb(rb)
    geometry.foreach(node.setGeometry)
    node
  }

  val RussiaCountryNode: Node = node(
    NodeRgid.RUSSIA,
    225,
    "Россия",
    COUNTRY,
    geoPoint(61.698654f, 99.50541f),
    geoPoint(81.886116f, 19.484768f),
    geoPoint(41.185997f, -168.872f)
  )

  val TatarstanSubjectFederationNode: Node = node(
    426660,
    11119,
    "Республика Татарстан",
    SUBJECT_FEDERATION,
    geoPoint(55.35034f, 50.91102f),
    geoPoint(56.67493f, 47.259155f),
    geoPoint(53.974216f, 54.266438f)
  )

  val YaroslavskayaOblastSubjectFederationNode: Node = node(
    NodeRgid.YAROSLAVSKAYA_OBLAST,
    Regions.YAROSLAVSKAYA_OBLAST,
    "Ярославская область",
    SUBJECT_FEDERATION,
    geoPoint(57.817361f, 39.105138f),
    geoPoint(57.817362f, 39.105139f),
    geoPoint(57.817363f, 39.105140f)
  )

  val SverdlovskayaOblastSubjectFederationNode: Node = node(
    NodeRgid.SVERDLOVSKAYA_OBLAST,
    Regions.SVERDLOVSKAYA_OBLAST,
    "Свердловская область",
    SUBJECT_FEDERATION,
    geoPoint(61.530766f, 58.586758f),
    geoPoint(66.18574f, 56.052208f),
    geoPoint(57.22833f, 61.94651f)
  )

  val AlmetyevskCityNode: Node = node(
    51928362,
    11121,
    "Альметьевск",
    CITY,
    geoPoint(54.901383f, 52.297113f),
    geoPoint(54.901384f, 52.297114f),
    geoPoint(54.901385f, 52.297115f)
  )

  val KazanCityNode: Node = node(
    582357,
    43,
    "Казань",
    CITY,
    geoPoint(55.798553f, 49.106327f),
    geoPoint(55.936447f, 48.823513f),
    geoPoint(55.603363f, 49.381927f)
  )

  val KazanSovetskyDistrictNode: Node = node(
    432345236,
    102028,
    "Советский район",
    CITY_DISTRICT,
    geoPoint(55.816672f, 49.240281f),
    geoPoint(55.816673f, 49.240282f),
    geoPoint(55.816674f, 49.240283f)
  )

  val KazanKirovskyDistrictNode: Node = node(
    432345237,
    102025,
    "Кировский район",
    CITY_DISTRICT,
    geoPoint(55.821421f, 48.953027f),
    geoPoint(55.821422f, 48.953028f),
    geoPoint(55.821423f, 48.953029f)
  )

  val MoscowCityNode: Node = node(
    587795,
    213,
    "Москва",
    CITY,
    geoPoint(55.75322f, 37.62251f),
    geoPoint(56.021286f, 36.803265f),
    geoPoint(55.142227f, 37.967796f)
  )

  val BalashihaUrbanDistrictNode: Node = node(
    2292,
    116705,
    "округ Балашиха",
    SUBJECT_FEDERATION_DISTRICT,
    geoPoint(55.791203f, 37.95686f),
    geoPoint(55.879673f, 37.776176f),
    geoPoint(55.696053f, 38.159718f)
  )

  val BalashihaCityNode: Node = node(
    596189,
    10716,
    "Балашиха",
    CITY,
    geoPoint(55.796345f, 37.938206f),
    geoPoint(55.861828f, 37.822292f),
    geoPoint(55.705086f, 38.14473f),
    Some(
      new Polygon(
        Array(55.861828f, 55.705086f, 55.861828f, 55.705086f),
        Array(37.822292f, 38.14473f, 37.822292f, 38.14473f)
      )
    )
  )

  val ReytovUrbanDistrictNode: Node = node(
    17383353,
    121003,
    "округ Реутов",
    SUBJECT_FEDERATION_DISTRICT,
    geoPoint(55.761776f, 37.86279f),
    geoPoint(55.783424f, 37.842567f),
    geoPoint(55.744534f, 37.890915f)
  )

  val ReytovCityNode: Node = node(
    593559,
    21621,
    "Реутов",
    CITY,
    geoPoint(55.760517f, 37.85515f),
    geoPoint(55.783424f, 37.842567f),
    geoPoint(55.744534f, 37.890915f),
    Some(
      new Polygon(
        Array(55.783424f, 55.744534f, 55.783424f, 55.744534f),
        Array(37.842567f, 37.890915f, 37.842567f, 37.890915f)
      )
    )
  )

  val MoscowAndMosOblastNode: Node = node(
    NodeRgid.MOSCOW_AND_MOS_OBLAST,
    1,
    "Москва и МО",
    SUBJECT_FEDERATION,
    geoPoint(55.75322f, 37.62251f),
    geoPoint(56.961323f, 35.14411f),
    geoPoint(54.255665f, 40.20486f)
  )

  val SpbAndLenOblastNode: Node = node(
    NodeRgid.SPB_AND_LEN_OBLAST,
    Regions.SPB_AND_LEN_OBLAST,
    "Питер и ЛО",
    SUBJECT_FEDERATION,
    geoPoint(59.337013f, 29.608975f),
    geoPoint(59.337014f, 29.608976f),
    geoPoint(59.337015f, 29.608977f)
  )

  val SpbNode: Node = node(
    NodeRgid.SPB,
    Regions.SPB,
    "Санкт-Петербург",
    CITY,
    geoPoint(59.938951f, 30.315635f),
    geoPoint(59.938952f, 30.315636f),
    geoPoint(59.938953f, 30.315637f)
  )

  val MosOblastNode: Node = node(
    NodeRgid.MOS_OBLAST,
    1,
    "Московская область",
    SUBJECT_FEDERATION,
    geoPoint(55.815796f, 37.380035f),
    geoPoint(56.958393f, 35.14411f),
    geoPoint(54.255665f, 40.20486f)
  )

  val NovosibOblastNode: Node = node(
    NodeRgid.NOVOSIBIRSKAYA_OBLAST,
    Regions.NOVOSIBIRSKAYA_OBLAST,
    "Новосибирская область",
    SUBJECT_FEDERATION,
    geoPoint(55.815796f, 37.380035f),
    geoPoint(56.958393f, 35.14411f),
    geoPoint(54.255665f, 40.20486f)
  )

  val NovosibCityNode: Node = node(
    1221312312,
    Regions.NOVOSIBIRSK,
    "Новосибирск",
    CITY,
    geoPoint(55.815796f, 37.380035f),
    geoPoint(56.958393f, 35.14411f),
    geoPoint(54.255665f, 40.20486f)
  )

  val KrasnodarskyKrayNode: Node = node(
    353118,
    10995,
    "Краснодарский край",
    SUBJECT_FEDERATION,
    geoPoint(45.544907f, 39.610424f),
    geoPoint(47.030827f, 36.521175f),
    geoPoint(43.250263f, 41.74043f)
  )

  val NovorossijskCityNode: Node = node(
    581131,
    970,
    "Новороссийск",
    CITY,
    geoPoint(44.723915f, 37.768974f),
    geoPoint(44.78343f, 37.649338f),
    geoPoint(44.659122f, 37.870438f),
    Some(
      new Polygon(
        Array(44.78343f, 44.659122f, 44.78343f, 44.659122f),
        Array(37.649338f, 37.649338f, 37.870438f, 37.870438f)
      )
    )
  )

  val NovorossijskUrbanDistrictNode: Node = node(
    1733,
    116897,
    "Новороссийск (городской округ)",
    SUBJECT_FEDERATION_DISTRICT,
    geoPoint(44.72079f, 37.643482f),
    geoPoint(45.011932f, 37.39243f),
    geoPoint(44.501278f, 37.921913f),
    Some(
      new Polygon(
        Array(45.011932f, 44.501278f, 44.501278f, 45.011932f),
        Array(37.39243f, 37.39243f, 37.921913f, 37.921913f)
      )
    )
  )

  val SochiDistrictNode: Node = node(
    2217,
    116900,
    "Сочи (городской округ)",
    SUBJECT_FEDERATION_DISTRICT,
    geoPoint(43.709827f, 39.75952f),
    geoPoint(44.127563f, 38.9966f),
    geoPoint(43.250263f, 40.6637f),
    Some(
      new Polygon(
        Array(44.127563f, 43.250263f, 44.127563f, 43.250263f),
        Array(38.9966f, 38.9966f, 40.6637f, 40.6637f)
      )
    )
  )

  val SochiCityNode: Node = node(
    17244963,
    239,
    "Сочи",
    CITY,
    geoPoint(43.709827f, 39.75952f),
    geoPoint(44.127563f, 38.9966f),
    geoPoint(43.250263f, 40.6637f),
    None
  )

  val AdlerovskiyDistrictNode: Node = node(
    17245222,
    114798,
    "Адлерский район",
    CITY_DISTRICT,
    geoPoint(43.529144f, 39.989075f),
    geoPoint(43.873978f, 39.865852f),
    geoPoint(43.39507f, 40.6637f),
    None
  )

  val KrasnayaPolanaCityNode: Node = node(
    180000,
    10994,
    "Красная Поляна",
    CITY,
    geoPoint(43.67997f, 40.205544f),
    geoPoint(43.70076f, 40.15144f),
    geoPoint(43.650517f, 40.242386f),
    None,
    official = Some("посёлок городского типа Красная Поляна")
  )

  val GelendzhikDistrictNode: Node = node(
    17396704,
    116898,
    "Геленджик (городской округ)",
    SUBJECT_FEDERATION_DISTRICT,
    geoPoint(44.455276f, 38.24641f),
    geoPoint(44.76262f, 37.77829f),
    geoPoint(44.15874f, 38.6878f),
    Some(
      new Polygon(
        Array(44.76262f, 44.15874f, 44.76262f, 44.15874f),
        Array(37.77829f, 37.77829f, 38.6878f, 38.6878f)
      )
    )
  )

  val RostovskayaOblastNode: Node = node(
    211571,
    11029,
    "Ростовская область",
    SUBJECT_FEDERATION,
    geoPoint(47.728737f, 41.268135f),
    geoPoint(50.214252f, 38.22031f),
    geoPoint(45.951527f, 44.324253f),
    None
  )

  val RostovNaDonuCityDistrictNode: Node = node(
    1746,
    121146,
    "округ Ростов-на-Дону",
    SUBJECT_FEDERATION_DISTRICT,
    geoPoint(47.237125f, 39.671574f),
    geoPoint(47.368824f, 39.412273f),
    geoPoint(47.15325f, 39.851517f),
    None,
    official = Some("Город Ростов-на-Дону (городской округ)")
  )

  val RostovNaDonuCityNode: Node = node(
    214386,
    39,
    "Ростов-на-Дону",
    CITY,
    geoPoint(47.22208f, 39.720356f),
    geoPoint(47.346725f, 39.455765f),
    geoPoint(47.15325f, 39.851517f),
    None,
    official = Some("город Ростов-на-Дону")
  )

  val EkaterinburgCityNode: Node = node(
    559132,
    54,
    "Екатеринбург",
    CITY,
    geoPoint(60.597473f, 56.838013f),
    geoPoint(60.943302f, 56.593796f),
    geoPoint(60.006832f, 56.982697f),
    None,
    official = Some("город Екатеринбург")
  )

  val RegionGraph: RegionGraph = {
    RussiaCountryNode.addChildrenId(TatarstanSubjectFederationNode.getId)
    RussiaCountryNode.addChildrenId(MoscowAndMosOblastNode.getId)
    RussiaCountryNode.addChildrenId(KrasnodarskyKrayNode.getId)
    RussiaCountryNode.addChildrenId(SpbAndLenOblastNode.getId)
    RussiaCountryNode.addChildrenId(YaroslavskayaOblastSubjectFederationNode.getId)
    RussiaCountryNode.addChildrenId(NovosibOblastNode.getId)
    RussiaCountryNode.addChildrenId(RostovskayaOblastNode.getId)
    RussiaCountryNode.addChildrenId(SverdlovskayaOblastSubjectFederationNode.getId)
    YaroslavskayaOblastSubjectFederationNode.addParentId(RussiaCountryNode.getId)
    RostovskayaOblastNode.addParentId(RussiaCountryNode.getId)
    SverdlovskayaOblastSubjectFederationNode.addParentId(RussiaCountryNode.getId)
    NovosibOblastNode.addParentId(RussiaCountryNode.getId)
    NovosibOblastNode.addChildrenId(NovosibCityNode.getId)
    NovosibCityNode.addParentId(NovosibOblastNode.getId)
    SpbAndLenOblastNode.addParentId(RussiaCountryNode.getId)
    SpbAndLenOblastNode.addChildrenId(SpbNode.getId)
    SpbNode.addParentId(SpbAndLenOblastNode.getId)
    MoscowAndMosOblastNode.addParentId(RussiaCountryNode.getId)
    MoscowAndMosOblastNode.addChildrenId(MoscowCityNode.getId)
    MoscowAndMosOblastNode.addChildrenId(MosOblastNode.getId)
    MoscowCityNode.addParentId(MoscowAndMosOblastNode.getId)
    MosOblastNode.addParentId(MoscowAndMosOblastNode.getId)
    MosOblastNode.addChildrenId(BalashihaUrbanDistrictNode.getId)
    MosOblastNode.addChildrenId(ReytovUrbanDistrictNode.getId)
    BalashihaUrbanDistrictNode.addParentId(MosOblastNode.getId)
    ReytovUrbanDistrictNode.addParentId(MosOblastNode.getId)
    BalashihaUrbanDistrictNode.addChildrenId(BalashihaCityNode.getId)
    BalashihaCityNode.addParentId(BalashihaUrbanDistrictNode.getId)
    ReytovUrbanDistrictNode.addChildrenId(ReytovCityNode.getId)
    ReytovCityNode.addParentId(ReytovUrbanDistrictNode.getId)
    TatarstanSubjectFederationNode.addParentId(RussiaCountryNode.getId)
    TatarstanSubjectFederationNode.addChildrenId(KazanCityNode.getId)
    TatarstanSubjectFederationNode.addChildrenId(AlmetyevskCityNode.getId)
    AlmetyevskCityNode.addParentId(TatarstanSubjectFederationNode.getId)
    KazanCityNode.addChildrenId(KazanSovetskyDistrictNode.getId)
    KazanSovetskyDistrictNode.addParentId(KazanCityNode.getId)
    KazanCityNode.addChildrenId(KazanKirovskyDistrictNode.getId)
    KazanKirovskyDistrictNode.addParentId(KazanCityNode.getId)
    KrasnodarskyKrayNode.addParentId(RussiaCountryNode.getId)
    KazanCityNode.addParentId(TatarstanSubjectFederationNode.getId)

    KrasnodarskyKrayNode.addChildrenId(NovorossijskUrbanDistrictNode.getId)
    KrasnodarskyKrayNode.addChildrenId(SochiDistrictNode.getId)
    KrasnodarskyKrayNode.addChildrenId(GelendzhikDistrictNode.getId)
    NovorossijskUrbanDistrictNode.addParentId(KrasnodarskyKrayNode.getId)
    SochiDistrictNode.addParentId(KrasnodarskyKrayNode.getId)
    SochiDistrictNode.addChildrenId(SochiCityNode.getId)
    SochiCityNode.addParentId(SochiDistrictNode.getId)
    SochiCityNode.addChildrenId(AdlerovskiyDistrictNode.getId)
    AdlerovskiyDistrictNode.addParentId(SochiCityNode.getId)
    AdlerovskiyDistrictNode.addChildrenId(KrasnayaPolanaCityNode.getId)
    KrasnayaPolanaCityNode.addParentId(AdlerovskiyDistrictNode.getId)
    GelendzhikDistrictNode.addParentId(KrasnodarskyKrayNode.getId)

    NovorossijskUrbanDistrictNode.addChildrenId(NovorossijskCityNode.getId)
    NovorossijskCityNode.addParentId(NovorossijskUrbanDistrictNode.getId)

    RostovskayaOblastNode.addChildrenId(RostovNaDonuCityDistrictNode.getId)
    RostovNaDonuCityDistrictNode.addParentId(RostovskayaOblastNode.getId)
    RostovNaDonuCityDistrictNode.addChildrenId(RostovNaDonuCityNode.getId)
    RostovNaDonuCityNode.addParentId(RostovNaDonuCityDistrictNode.getId)

    SverdlovskayaOblastSubjectFederationNode.addChildrenId(EkaterinburgCityNode.getId)
    EkaterinburgCityNode.addParentId(SverdlovskayaOblastSubjectFederationNode.getId)

    val graph = new MutableRegionGraph
    List(
      RussiaCountryNode,
      MoscowAndMosOblastNode,
      SpbAndLenOblastNode,
      SpbNode,
      MosOblastNode,
      NovosibOblastNode,
      NovosibCityNode,
      TatarstanSubjectFederationNode,
      KazanCityNode,
      KazanSovetskyDistrictNode,
      KazanKirovskyDistrictNode,
      AlmetyevskCityNode,
      MoscowCityNode,
      ReytovUrbanDistrictNode,
      ReytovCityNode,
      BalashihaUrbanDistrictNode,
      BalashihaCityNode,
      KrasnodarskyKrayNode,
      NovorossijskCityNode,
      NovorossijskUrbanDistrictNode,
      SochiDistrictNode,
      SochiCityNode,
      AdlerovskiyDistrictNode,
      KrasnayaPolanaCityNode,
      GelendzhikDistrictNode,
      RostovskayaOblastNode,
      RostovNaDonuCityDistrictNode,
      RostovNaDonuCityNode,
      SverdlovskayaOblastSubjectFederationNode,
      EkaterinburgCityNode
    ).foreach(
      graph.addNode
    )
    graph.setRoot(RussiaCountryNode)
    graph
  }

  val regionGraphProvider = new Provider[RegionGraph] {
    override def get(): RegionGraph = RegionGraph
  }
}
