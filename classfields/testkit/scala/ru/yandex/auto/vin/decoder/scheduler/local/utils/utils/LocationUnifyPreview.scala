package ru.yandex.auto.vin.decoder.scheduler.local.utils

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import play.api.libs.json.{JsObject, Json}
import ru.yandex.auto.vin.decoder.extdata.region.GeoRegionType._
import ru.yandex.auto.vin.decoder.extdata.region.{GeoRegion, Tree}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.scheduler.components.DefaultSchedulerComponents
import auto.carfax.common.storages.yt.YtUtils.RichNodeIterator
import auto.carfax.common.utils.collections.RichIterable
import auto.carfax.common.utils.misc.IoUtils
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.vertis.util.concurrent.future.RichFuture

import java.util.Optional
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object LocationUnifyPreview extends LocalScript {

  /* PARAMS */

  private val ytRawDataPath = "//home/verticals/_home/plomovtsev/fine_raw_data"
  private val ytOutputPath = "//home/verticals/_home/plomovtsev/fine_locations_unified_3"

  private val locationBatchSize = 300
  private val maxParExternalRequests = 20

  private val regionTypesAboveCity = List(
    REGION_OF_THE_SUBJECT_OF_THE_FEDERATION,
    REGION,
    SUBJECT_OF_THE_FEDERATION,
    FEDERATION_DISTRICT,
    COUNTRY,
    OTHER_COUNTRY
  )
  private val regionTypesCities = List(CITY, VILLAGE, RURAL_SETTLEMENT)
  private val regionTypes = regionTypesAboveCity ++ regionTypesCities

  /* DEPENDENCIES */

  implicit private val system: ActorSystem = ActorSystem(s"GeocoderTestTask-${System.currentTimeMillis}-system")
  implicit private val ec: ExecutionContextExecutor = system.dispatcher

  private val schedulerComponents = new DefaultSchedulerComponents()

  private val yt = schedulerComponents.coreComponents.ytHahn
  private val geocoderManager = schedulerComponents.geocoderManager
  private val wizard = schedulerComponents.yandexWizard
  private val regionTree = initRegionTree

  def action: Future[Any] = {
    progressBar.start()
    locationBatchSource
      .mapAsync(parallelism = 1)(unify)
      .map(saveResultsToYt)
      .map(progressBar.inc)
      .runWith(Sink.last)
  }

  private def locationBatchSource: Source[List[FineLocation], NotUsed] = {
    Source
      .fromIterator(() => ytLocationIterator)
      .grouped(locationBatchSize)
      .map(_.toList)
  }

  private def ytLocationIterator: Iterator[FineLocation] = {
    IoUtils
      .using(
        yt.tables.read(Optional.empty[GUID](), false, YPath.simple(ytRawDataPath), YTableEntryTypes.YSON)
      )(_.asScala.toList.flatMap { node =>
        val vin = VinCode(node.getString("vin"))
        val rawData = Json.parse(node.getString("raw_data")).as[JsObject]
        parseLocations(vin, rawData)
      })
      .iterator
  }

  private def parseLocations(vinCode: VinCode, rawData: JsObject): List[FineLocation] = {
    val finez = rawData \ "data" \ 0 \ "content" \ "fines" \ "items"
    val fines = Try(finez.as[List[JsObject]]).toOption.getOrElse(List.empty)
    fines.map { fine =>
      FineLocation(
        vinCode,
        location = Try((fine \ "location" \ "raw").as[String]).toOption,
        vendorName = Try((fine \ "vendor" \ "name").as[String]).toOption
      )
    }
  }

  private def unify(locations: List[FineLocation]): Future[List[UnifyResult]] = {
    val successNone = Future.successful(Success(None))
    val successEmptyList = Future.successful(Success(List.empty))
    locations
      .grouped(maxParExternalRequests / 4)
      .toList
      .runSequential { locationsBatch =>
        val results = locationsBatch.map { location =>
          val locGeoF = location.location.map(geocodeUnify).getOrElse(successNone)
          val locWizAllRegionsF = location.location.map(wizardUnify).getOrElse(successEmptyList)
          val locWizBestF = locWizAllRegionsF.map(_.map(chooseBestSkipCities))
          val vNameGeoF = location.vendorName.map(geocodeUnify).getOrElse(successNone)
          val vNameWizAllRegionsF = location.vendorName.map(wizardUnify).getOrElse(successEmptyList)
          val vNameWizBestF = vNameWizAllRegionsF.map(_.map(chooseLeastCommonAboveCity))
          for {
            locGeo <- locGeoF
            locWizAll <- locWizAllRegionsF.map(_.map(_.map(_.name)))
            locWizBest <- locWizBestF
            vNameGeo <- vNameGeoF
            vNameWizAll <- vNameWizAllRegionsF.map(_.map(_.map(_.name)))
            vNameWizBest <- vNameWizBestF
          } yield {
            UnifyResult(
              location.vinCode,
              location.location,
              locGeo,
              locWizAll,
              locWizBest,
              location.vendorName,
              vNameGeo,
              vNameWizAll,
              vNameWizBest
            )
          }
        }
        Future.sequence(results)
      }
      .map(_.toList.flatten)
  }

  private def geocodeUnify(input: String): Future[Try[Option[String]]] = {
    geocoderManager
      .findRegionBy(input, regionTypes)
      .map(_.map(_.name))
      .asSuccessfulTry
  }

  private def wizardUnify(input: String): Future[Try[List[GeoRegion]]] = {
    wizard
      .extractLocations(input)
      .map(_.map(_.id).distinct.flatMap(regionTree.findUpByTree(_, regionTypes)))
      .asSuccessfulTry
  }

  private def chooseBestSkipCities(locations: List[GeoRegion]): Option[String] = {
    val notCities = locations.filterNot(reg => regionTypesCities.exists(_.id == reg.regionType))
    chooseLeastCommonAboveCity(notCities)
  }

  private def chooseLeastCommonAboveCity(locations: List[GeoRegion]): Option[String] = {
    locations
      .map(_.id)
      .flatMap(regionTree.findUpByTree(_, regionTypesAboveCity))
      .sortBy(reg => regionTypesAboveCity.indexWhere(_.id == reg.regionType))
      .headOption
      .map(_.name)
  }

  private def saveResultsToYt(results: List[UnifyResult]): Int = {
    yt.tables.write(
      YPath.simple(ytOutputPath).append(true),
      YTableEntryTypes.YSON,
      results.map(_.toYtNode).iterator.asYt
    )
    results.size
  }

  private def initRegionTree: Tree = {
    schedulerComponents.coreComponents.s3DataService.regions
  }

  private case class FineLocation(
      vinCode: VinCode,
      location: Option[String],
      vendorName: Option[String])

  private case class UnifyResult(
      vin: VinCode,
      location: Option[String],
      locationGeo: Try[Option[String]],
      locationWizAll: Try[List[String]],
      locationWizBest: Try[Option[String]],
      vendorName: Option[String],
      vendorNameGeo: Try[Option[String]],
      vendorNameWizAll: Try[List[String]],
      vendorNameWizBest: Try[Option[String]]) {

    val toYtNode: YTreeMapNode = {
      YTree.mapBuilder
        .key("vin")
        .value(vin.toString)
        .key("date")
        .value(System.currentTimeMillis)
        .key("location")
        .value(location.getOrElse("None"))
        .key("location_geo")
        .value(optToStr(locationGeo))
        .key("location_wiz_all")
        .value(listToStr(locationWizAll))
        .key("location_wiz_best")
        .value(optToStr(locationWizBest))
        .key("vendor_name")
        .value(vendorName.getOrElse("None"))
        .key("vendor_name_geo")
        .value(optToStr(vendorNameGeo))
        .key("vendor_name_wiz_all")
        .value(listToStr(vendorNameWizAll))
        .key("vendor_name_wiz_best")
        .value(optToStr(vendorNameWizBest))
        .buildMap
    }

    private def optToStr(s: Try[Option[String]]): String = s match {
      case Success(Some(location)) => location
      case Success(None) => "None"
      case Failure(ex) => s"ERROR: ${ex.getMessage}"
    }

    private def listToStr(s: Try[List[String]]): String = s match {
      case Success(locations) if locations.nonEmpty => locations.mkString(", ")
      case Success(_) => "None"
      case Failure(ex) => s"ERROR: ${ex.getMessage}"
    }
  }
}
