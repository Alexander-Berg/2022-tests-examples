package ru.yandex.realty.geosrc

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.geosrc.GeoSrcTestComponents.geoSrcStorage
import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.geosrc.{Envelope, Name, Toponym}
import ru.yandex.realty.model.location.GeoPoint

trait GeoSrcTestComponents {
  lazy val geoSrcProvider: Provider[ToponymsStorage] = () => geoSrcStorage
}

object GeoSrcTestComponents {

  val RussiaEnvelope = Envelope(GeoPoint.getPoint(41.185997f, 19.484768f), GeoPoint.getPoint(81.886116f, -168.872f))

  val Russia = Toponym(
    53000001,
    "country",
    Seq(Name("Росия", "display", "ru_LOCAL")),
    225,
    GeoPoint.getPoint(61.698654f, 99.50541f),
    RussiaEnvelope,
    Seq.empty,
    Some(getPolygon(RussiaEnvelope))
  )

  val CentralFederalDistrictEnvelope =
    Envelope(GeoPoint.getPoint(49.556988f, 30.750269f), GeoPoint.getPoint(59.62374f, 47.642597f))

  val CentralFederalDistrict = Toponym(
    53000013,
    "province",
    Seq(Name("Центральный федеральный округ", "display", "ru_LOCAL")),
    3,
    GeoPoint.getPoint(54.87375f, 38.064724f),
    CentralFederalDistrictEnvelope,
    Seq.empty,
    Some(getPolygon(CentralFederalDistrictEnvelope))
  )

  val MskAndMoEnvelope = Envelope(GeoPoint.getPoint(54.255665f, 35.148865f), GeoPoint.getPoint(56.958393f, 40.20486f))

  val MskAndMo = Toponym(
    53000044,
    "province",
    Seq(Name("Московская область", "display", "ru_LOCAL")),
    1,
    GeoPoint.getPoint(55.53113f, 38.87476f),
    MskAndMoEnvelope,
    Seq.empty,
    Some(getPolygon(MskAndMoEnvelope))
  )

  val MskEnvelope = Envelope(GeoPoint.getPoint(55.142227f, 36.803265f), GeoPoint.getPoint(56.021286f, 37.967796f))

  val Msk = Toponym(
    53000094,
    "province",
    Seq(Name("Москва", "display", "ru_LOCAL")),
    213,
    GeoPoint.getPoint(55.755863f, 37.6177f),
    MskEnvelope,
    Seq.empty,
    Some(getPolygon(MskEnvelope))
  )

  val VolokolamskiyDistrictEnvelope =
    Envelope(GeoPoint.getPoint(55.70545f, 35.602314f), GeoPoint.getPoint(56.315517f, 36.350452f))

  val VolokolamskiyDistrict = Toponym(
    53002298,
    "area",
    Seq(Name("Волоколамский район", "display", "ru_LOCAL")),
    98580,
    GeoPoint.getPoint(56.00783f, 35.97036f),
    VolokolamskiyDistrictEnvelope,
    Seq.empty,
    Some(getPolygon(VolokolamskiyDistrictEnvelope))
  )

  val EasternAdministrativeDistrictEnvelope =
    Envelope(GeoPoint.getPoint(55.695286f, 37.650154f), GeoPoint.getPoint(56.0097f, 37.945663f))

  val EasternAdministrativeDistrict = Toponym(
    53183642,
    "district",
    Seq(Name("Восточный административный округ", "display", "ru_LOCAL")),
    20358,
    GeoPoint.getPoint(55.787712f, 37.775635f),
    EasternAdministrativeDistrictEnvelope,
    Seq.empty,
    Some(getPolygon(EasternAdministrativeDistrictEnvelope))
  )

  val SokolnikiDistrictEnvelope =
    Envelope(GeoPoint.getPoint(55.7821f, 37.650154f), GeoPoint.getPoint(55.8244f, 37.705467f))

  val SokolnikiDistrict = Toponym(
    53211692,
    "district",
    Seq(Name("район Сокольники", "display", "ru_LOCAL")),
    116995,
    GeoPoint.getPoint(55.799946f, 37.677166f),
    SokolnikiDistrictEnvelope,
    Seq.empty,
    Some(getPolygon(SokolnikiDistrictEnvelope))
  )

  val SokolnikiMetro = Toponym(
    100079228,
    "metro",
    Seq(Name("метро Сокольники", "display", "ru_LOCAL")),
    20402,
    GeoPoint.getPoint(55.788994f, 37.679882f),
    Envelope(GeoPoint.getPoint(55.788994f, 37.679882f), GeoPoint.getPoint(55.788994f, 37.679882f)),
    Seq(GeoPoint.getPoint(55.7893f, 37.67987f), GeoPoint.getPoint(55.789223f, 37.6795f)),
    None
  )

  val geoSrcStorage = new ToponymsStorage(
    Seq(
      Russia,
      CentralFederalDistrict,
      MskAndMo,
      Msk,
      VolokolamskiyDistrict,
      EasternAdministrativeDistrict,
      SokolnikiDistrict,
      SokolnikiMetro
    )
  )

  private def getPolygon(envelope: Envelope): Polygon = {
    val latitudes = Seq(
      envelope.lowerCorner.getLatitude,
      envelope.upperCorner.getLatitude,
      envelope.upperCorner.getLatitude,
      envelope.lowerCorner.getLatitude
    )
    val longitudes = Seq(
      envelope.lowerCorner.getLongitude,
      envelope.upperCorner.getLongitude,
      envelope.upperCorner.getLongitude,
      envelope.lowerCorner.getLongitude
    )
    new Polygon(latitudes.toArray, longitudes.toArray)
  }
}
