package ru.yandex.realty.misc.servantlets.location

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.buildinginfo.storage.BuildingStorage
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.geocoder.RemoteLocationUnifierService
import ru.yandex.realty.geohub.api.GeohubApi
import ru.yandex.realty.geohub.api.GeohubApi.{UnifyLocationRequest, UnifyLocationResponse}
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.model.message.RealtySchema.{GeoPointMessage, LocationMessage, RawLocationMessage}
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.searcher.jetty.HttpServRequest
import ru.yandex.realty.searcher.response.location.CheckLocationResponse
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.unification.unifier.processor.FieldUnifier
import ru.yandex.realty.unification.unifier.processor.unifiers.{LocationUnifier, LocationUnifierJava, SitesResolver}
import ru.yandex.realty.util.process.SyncProcessor

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CheckLocationServantletSpec extends SpecBase {

  private val regionGraph: RegionGraph = mock[RegionGraph]
  private val geohubClient: GeohubClient = mock[GeohubClient]
  private val longitude = 33.28492736816406f
  private val latitude = 45.18985366821289f
  private val unifyLocationRequest: UnifyLocationRequest = UnifyLocationRequest
    .newBuilder()
    .setRawLocation(RawLocationMessage.newBuilder().setVersion(5).setLongitude(longitude).setLatitude(latitude))
    .setAddMetro(true)
    .setUseReverseGeocoder(true)
    .build()
  private val unifyLocationResponse: UnifyLocationResponse = UnifyLocationResponse
    .newBuilder()
    .setLocation(
      LocationMessage
        .newBuilder()
        .setVersion(2)
        .setGeocoderId(177079)
        .setLocalityName("садоводческий потребительский кооператив Прибой")
        .setGeocoderPoint(
          GeoPointMessage.newBuilder().setLatitude(45.189854f).setLongitude(33.284927f).setVersion(1)
        )
        .setManualPoint(
          GeoPointMessage.newBuilder().setLatitude(45.189854f).setLongitude(33.284927f).setVersion(1)
        )
        .setGeocoderAddress(
          "Россия, Республика Крым, Сакский район, село Уютное, садоводческий потребительский кооператив Прибой"
        )
        .setAccuracyInt(6) //UNKNOWN
        .setRegionGraphId(298064)
        .setParsingStatus(1)
        .setSubjectFederationId(977)
        .setRegionName("Республика Крым")
        .setSubjectFederationRgid(681264)
    )
    .build()
  private val locationUnifierService: RemoteLocationUnifierService =
    new RemoteLocationUnifierService(geohubClient)
  private val sitesResolver: SitesResolver = mock[SitesResolver]
  private val buildingStorage: BuildingStorage = mock[BuildingStorage]
  private val locationUnifier: LocationUnifierJava =
    new LocationUnifierJava(locationUnifierService, () => regionGraph, sitesResolver)
  private val servantlet = new CheckLocationServantlet(
    Seq[SyncProcessor[OfferWrapper]](locationUnifier).asJava,
    Seq.empty.asJava,
    () => regionGraph,
    buildingStorage
  )

  "CheckLocationServantletSpec" should {
    "process" in {
      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(unifyLocationRequest, *)
        .returning(
          Future.successful(
            unifyLocationResponse
          )
        )

      val node = new Node()
      node.setId(298064L)
      node.setGeoId(298064)

      (regionGraph
        .getNodeById(_: java.lang.Long))
        .expects(Long.box(298064L))
        .returns(node)

      val countryNode = new Node()
      countryNode.setId(Regions.RUSSIA)
      countryNode.setGeoId(Regions.RUSSIA)
      countryNode.setType("country")

      (regionGraph
        .getRandomParent(_: Node))
        .expects(node)
        .returns(countryNode)

      val r = new HttpServRequest();
      r.setParam("category", "LOT")
      r.setParam("longitude", longitude.toString)
      r.setParam("latitude", latitude.toString)

      val response = servantlet.process(r)
      response shouldBe a[CheckLocationResponse]
      response match {
        case r: CheckLocationResponse => {
          r.getGeocoderAddress shouldBe "Россия, Республика Крым, Сакский район, село Уютное, садоводческий потребительский кооператив Прибой"
          r.getRgid shouldBe 298064
          r.getGeoId shouldBe 298064
        }
        case _ => throw new ClassCastException
      }
    }
  }
}
