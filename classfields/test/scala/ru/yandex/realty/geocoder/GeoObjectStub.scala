package ru.yandex.realty.geocoder

import java.util

import ru.yandex.realty.geocoder.client.{Kind, Precision}
import ru.yandex.realty.geocoder.estimation.GeoObjectAccessor
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.message.RealtySchema
import scala.collection.JavaConverters._

case class GeoObjectStub(
  textOpt: Option[String] = None,
  precisionOpt: Option[Precision] = None,
  kindOpt: Option[Kind] = None,
  pointOpt: Option[String] = None,
  envelopeLowerOpt: Option[String] = None,
  envelopeUpperOpt: Option[String] = None,
  countryNameOpt: Option[String] = None,
  addressLineOpt: Option[String] = None,
  administrativeAreaNameOpt: Option[String] = None,
  subAdministrativeAreaNameOpt: Option[String] = None,
  localityNameOpt: Option[String] = None,
  thoroughfareNameOpt: Option[String] = None,
  thoroughfarePredirectionOpt: Option[String] = None,
  premiseNumberOpt: Option[String] = None,
  `typeOpt`: Option[String] = None,
  geoidOpt: Option[String] = None,
  premiseNameOpt: Option[String] = None,
  dependentLocalityNameOpt: Option[String] = None,
  countryCodeOpt: Option[String] = None,
  accuracyOpt: Option[String] = None,
  nameOpt: Option[String] = None,
  descriptionOpt: Option[String] = None,
  componentsOpt: Option[Seq[RealtySchema.AddressComponentMessage]] = None
)

object GeoObjectStubAccessor extends GeoObjectAccessor[GeoObjectStub] {
  override def getAddress(geoObject: GeoObjectStub): String = geoObject.addressLineOpt.get

  override def getKind(geoObject: GeoObjectStub): Kind = geoObject.kindOpt.get

  override def getGeoId(geoObject: GeoObjectStub): Int = geoObject.geoidOpt.get.toInt

  override def getPoint(geoObject: GeoObjectStub): GeoPoint = GeoPoint.getPoint(geoObject.pointOpt.get)

  override def getPrecision(geoObject: GeoObjectStub): Precision = geoObject.precisionOpt.get

  override def getSubAdministrativeAreaName(geoObject: GeoObjectStub): String =
    geoObject.subAdministrativeAreaNameOpt.get

  override def getDependentLocalityName(geoObject: GeoObjectStub): String = geoObject.dependentLocalityNameOpt.get

  override def getComponents(geoObject: GeoObjectStub): util.List[RealtySchema.AddressComponentMessage] =
    geoObject.componentsOpt.get.asJava
}
