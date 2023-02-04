package mockups

import java.sql.ResultSet
import ru.yandex.realty.model.archive.ArchiveOfferHolocron

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 2019-03-18
  */
class OfferAsResultSet(data: Map[String, Array[Byte]]) extends ResultSet with ResultSetNotImplementedMethods {

  import ru.yandex.realty.archive.scheduler.updater.persistence.ImplicitsFromBytesArray._

  var flagWasNull: Boolean = false

  private def getWithNullAndDefault[A](maybeV: Array[Byte], defaultValue: A)(
    implicit transformerA: Array[Byte] => Option[A]
  ): A = {
    transformerA(maybeV) match {
      case Some(v) =>
        flagWasNull = false
        v
      case _ =>
        flagWasNull = true
        defaultValue
    }
  }

  override def wasNull(): Boolean = flagWasNull

  override def getString(columnLabel: String): String = getWithNullAndDefault(data(columnLabel), null: String)

  override def getBoolean(columnLabel: String): Boolean = getWithNullAndDefault(data(columnLabel), true)

  override def getInt(columnLabel: String): Int = getWithNullAndDefault(data(columnLabel), 0)

  override def getLong(columnLabel: String): Long = getWithNullAndDefault(data(columnLabel), 0)

  override def getFloat(columnLabel: String): Float = getWithNullAndDefault(data(columnLabel), 0)

  override def getDouble(columnLabel: String): Double = getWithNullAndDefault(data(columnLabel), 0)

  override def getBytes(columnLabel: String): Array[Byte] = data(columnLabel)
}

object OfferAsResultSet {

  //scalastyle:off method.length
  def transform(offer: ArchiveOfferHolocron): ResultSet = {
    import ru.yandex.realty.archive.scheduler.updater.persistence.ImplicitsToBytesArray._
    import ru.yandex.realty.archive.scheduler.updater.persistence.ImplicitsEnumToInt._

    val images = for { i <- Range(1, 20) } yield s"offer_image__prefixes_$i" -> toBytesOptString(
      offer.images.urls.flatMap(_.lift(i))
    )

    val imagesMap: Map[String, Array[Byte]] =
      Map(images.map { case (k, v) => (k, v) }: _*) ++ Map[String, Array[Byte]] {
        "offer_image__prefixes" -> offer.images.urls.flatMap(_.headOption)
      }
    val data: Map[String, Array[Byte]] = Map(
      "offer_id" -> offer.offerId,
      "day" -> offer.date.toLocalDate.toString,
      "offer_transaction_whole__in__rubles" -> offer.price.value,
      "offer_areaprice_price_periodint" -> offer.price.period,
      "offer_apartmentinfo_floors" -> offer.floorsOffered,
      "offer_buildinginfo_floorstotal" -> offer.floorsTotal,
      "offer_apartmentinfo_rooms" -> offer.roomsTotal,
      "offer_apartmentinfo_roomsoffered" -> offer.roomsOffered,
      "offer_apartmentinfo_openplan" -> offer.openPlan,
      "offer_apartmentinfo_studio" -> offer.house.studio,
      "offer_apartmentinfo_apartments" -> offer.apartment.apartments,
      "offer_apartmentinfo_renovationint" -> offer.apartment.renovation,
      "offer_apartmentinfo_balconyint" -> offer.house.balconyType,
      "offer_apartmentinfo_newflat" -> offer.apartment.newFlat,
      "offer_apartmentinfo_flattypeint" -> offer.flatType,
      "offer_buildinginfo_parkingtypeint" -> offer.building.parkingType,
      "offer_apartmentinfo_ceilingheight" -> offer.apartment.ceilingHeight,
      "offer_areaprice_area_value" -> offer.areaInfo.area.value,
      "offer_area_value" -> offer.areaInfo.totalArea.map(_.value),
      "offer_houseinfo_kitchenspace" -> offer.areaInfo.kitchenSpace.map(_.value),
      "offer_houseinfo_livingspace" -> offer.areaInfo.livingSpace.map(_.value),
      "offer_buildinginfo_buildingid" -> offer.building.buildingId,
      "offer_buildinginfo_buildingseriesid" -> offer.building.buildingSeriesId,
      "offer_buildinginfo_buildingtypeint" -> offer.building.buildingType,
      "offer_transaction_agentfee" -> offer.offerInfo.agentFee,
      "offer_totalimages" -> offer.images.totalImages,
      "offer_offertypeint" -> offer.offerType,
      "offer_categorytypeint" -> offer.offerCategory,
      "offer_location_geocoderaddress" -> offer.location.address,
      "offer_location_localityname" -> offer.location.localityName,
      "offer_location_subjectfederationid" -> offer.location.subjectFederationId,
      "offer_location_accuracyint" -> 1,
      "offer_premoderation" -> offer.offerInfo.premoderation,
      "offer_internal" -> offer.offerInfo.internal,
      "offer_partnerid" -> offer.offerInfo.partnerId,
      "offer_clusterid" -> offer.offerInfo.clusterId,
      "offer_offerstate_error_errortypeint" -> offer.error0,
      "offer_offerstate_error_1_errortypeint" -> offer.error1,
      "description" -> offer.description,
      "offer_cluster__header" -> true
    )

    new OfferAsResultSet(data ++ imagesMap)
  }

  //scalastyle:on
}
