package ru.yandex.realty.domrf.unifier

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.mds.AvatarsClient
import ru.yandex.realty.domrf.client.DomrfClient
import ru.yandex.realty.domrf.model.{RawComplex, RawConstructionObject}
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.sites.{House, Phase, Site}
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.geometry.{BoundingBoxUtil, PolygonUtil}
import ru.yandex.vertis.RawMdsIdentity
import ru.yandex.vertis.protobuf.ProtoInstanceProvider

import java.util.Collections
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@RunWith(classOf[JUnitRunner])
class DomrfUnifierSpec extends AsyncSpecBase with ProtobufMessageGenerators with ProtoInstanceProvider {

  private val avatarsClient = mock[AvatarsClient]
  private val sitesGroupingService = mock[SitesGroupingService]
  private val domrfClient = mock[DomrfClient]
  private val domrfUnifier = new DomrfUnifier(domrfClient, avatarsClient, sitesGroupingService)

  "DomrfUnifier" should {
    "enrich complex with siteId" in {
      val rawComplex = ModelGen.RawComplexGen.next

      val site = buildSiteFor(rawComplex)

      prepareMocks(rawComplex, site)

      val res = domrfUnifier.unify(rawComplex, None)(Traced.empty).futureValue

      res.getSiteId.getValue shouldBe site.getId
    }

    "enrich complex with houseId" in {
      val rawComplex = ModelGen.RawComplexGen.next

      val site = buildSiteFor(rawComplex)
      prepareMocks(rawComplex, site)

      val res = domrfUnifier.unify(rawComplex, None)(Traced.empty).futureValue

      res.getConstructionObjectsList.asScala.foreach { co =>
        co.getHouseId.getValue shouldBe generateHouseIdFromConstructionId(co.getConstructionObject.getObjectId)
      }
    }

    "enrich complex with photos" in {
      val rawComplex = ModelGen.RawComplexGen.next

      val site = buildSiteFor(rawComplex)

      prepareMocks(rawComplex, site)

      val res = domrfUnifier.unify(rawComplex, None)(Traced.empty).futureValue

      res.getConstructionObjectsList.asScala.foreach { co =>
        val unifiedFileIds = co.getConstructionPhotosList.asScala
          .map(_.getObjectPhoto.getFileInfo.getFileId)
          .toSet

        unifiedFileIds shouldBe rawComplex.constructionObjects
          .find(_.constructionObject.objectId == co.getConstructionObject.getObjectId)
          .map(_.constructionPhotos.map(_.fileInfo.fileId).toSet)
          .getOrElse(Set.empty)

      }
    }

    def prepareMocks(rawComplex: RawComplex, site: Site): Unit = {
      (sitesGroupingService.getAllSites _)
        .expects()
        .anyNumberOfTimes()
        .returning(java.util.Collections.singletonList(site))

      (domrfClient
        .getConstructionObjectFile(_: Long, _: String)(_: Traced))
        .expects(where {
          case (objectId, fileId, _) =>
            rawComplex.constructionObjects.exists(_.constructionObject.objectId == objectId) &&
              rawComplex.constructionObjects.exists(_.constructionPhotos.exists(_.fileInfo.fileId == fileId))
          case _ => false
        })
        .anyNumberOfTimes()
        .returning(Future.successful("Hello".getBytes))

      (avatarsClient
        .uploadData(_: Array[Byte], _: String, _: String, _: Option[Long], _: FiniteDuration)(_: Traced))
        .expects(*, *, *, *, *, *)
        .anyNumberOfTimes()
        .returning(Future.successful(RawMdsIdentity.getDefaultInstance))

    }
  }

  private def generateHouseIdFromConstructionId(constructionId: Long): Long = {
    constructionId + 42
  }

  private def buildSiteFor(rawComplex: RawComplex): Site = {
    val point =
      rawComplex.complex.coordinates.getOrElse(throw new IllegalArgumentException("Coordinates should be set"))
    val site = new Site(Gen.posNum[Long].next)
    site.setPolygon(buildPolygon(point.latitude.floatValue(), point.longitude.floatValue()))
    site.setGeoPoint(GeoPoint.getPoint(point.latitude.floatValue(), point.longitude.floatValue()))
    val houses = rawComplex.constructionObjects.zipWithIndex.map(o => buildHouse(o._2, o._1))
    val phase = new Phase(1)
    phase.setDescription("Phase")
    phase.setHouses(houses.asJava)
    site.setPhases(Collections.singletonList(phase))
    site
  }

  private def buildHouse(index: Int, rawConstructionObject: RawConstructionObject): House = {
    val house = new House(generateHouseIdFromConstructionId(rawConstructionObject.constructionObject.objectId))
    val point =
      rawConstructionObject.constructionObject.coordinates
        .getOrElse(throw new IllegalArgumentException("Coordinates should be set"))
    house.setPolygon(buildPolygon(point.latitude.floatValue(), point.longitude.floatValue()))
    house
  }

  private def buildPolygon(latitude: Float, longitude: Float): Polygon = {
    val bb = BoundingBoxUtil.create(latitude, longitude, 100, 100)

    PolygonUtil.constructPolygon(
      Array(bb.getMinLatitude, bb.getMaxLatitude, bb.getMaxLatitude, bb.getMinLatitude),
      Array(bb.getMinLongitude, bb.getMinLongitude, bb.getMaxLongitude, bb.getMaxLongitude),
      true
    )
  }
}
