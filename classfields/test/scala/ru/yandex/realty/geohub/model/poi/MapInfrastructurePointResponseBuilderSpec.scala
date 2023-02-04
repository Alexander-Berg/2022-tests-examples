package ru.yandex.realty.geohub.model.poi

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.geohub.api.GeohubApi.{MapInfrastructure, MapInfrastructurePoint, MapInfrastructureRating}
import ru.yandex.realty.geohub.model.poi.MapInfrastructurePointResponseBuilderSpec._
import ru.yandex.realty.proto.GeoPoint
import yandex.maps.proto.common2.geo_object.GeoObjectOuterClass
import yandex.maps.proto.common2.geometry.GeometryOuterClass
import yandex.maps.proto.common2.metadata.MetadataOuterClass.Metadata
import yandex.maps.proto.search.address.AddressOuterClass
import yandex.maps.proto.search.business.Business
import yandex.maps.proto.search.business_rating_2x.BusinessRating2X
import yandex.maps.proto.search.hours.HoursOuterClass.OpenHours
import yandex.maps.proto.search.photos_2x.Photos2X
import yandex.maps.proto.search.search.Search

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class MapInfrastructurePointResponseBuilderSpec extends SpecBase {

  "MapInfrastructurePointResponseBuilder " should {
    val geocoderResponse = prepareGeocoderResponse()
    "Схлопнуть все три точки в один ответ и правильно их конвертировать" in {
      val builderResult = MapInfrastructurePointResponseBuilder.build(geocoderResponse)
      builderResult.getTotal shouldBe 3
      builderResult.getPointsList.size() shouldBe 3
      builderResult.getPointsList.get(0) shouldBe buildGeohubPoint(point1)
      builderResult.getPointsList.get(1) shouldBe buildGeohubPoint(point2)
      builderResult.getPointsList.get(2) shouldBe buildGeohubPoint(point3)

    }
  }

  private def buildGeohubPoint(data: Data): MapInfrastructurePoint = {
    val pointBuilder = MapInfrastructurePoint
      .newBuilder()
      .setId(data.id)
      .setName(data.name)
      .addAllCategoriesText(data.categoryText.asJava)
      .setOpenHoursText(data.openHours)
      .addPoint(GeoPoint.newBuilder.setLatitude(data.lat).setLongitude(data.lon))
      .addAllPhotoUrls(data.photoUrl.asJava)
      .setKind(data.kind)
    data.rating match {
      case (rating, reviews) =>
        val ratingBuilder = MapInfrastructureRating
          .newBuilder()
          .setRating(rating)
          .setTotalReviews(reviews)
        pointBuilder.setRating(ratingBuilder)
    }
    pointBuilder.build()

  }
  private def prepareGeocoderResponse(): Seq[GeoObjectOuterClass.GeoObject] = {
    val points1 =
      Seq(point1, point2)
    val responseMetadataExt1 = Search.ResponseMetadata.newBuilder().setFound(2).build()
    val response1 = GeoObjectOuterClass.GeoObject
      .newBuilder()
      .addMetadata(Metadata.newBuilder().setExtension(Search.rESPONSEMETADATA, responseMetadataExt1).build())
      .addAllGeoObject(points1.map(buildMapPoint).asJava)
      .build()

    val points2 = Seq(point3)
    val responseMetadataExt2 = Search.ResponseMetadata.newBuilder().setFound(1).build()
    val response2 = GeoObjectOuterClass.GeoObject
      .newBuilder()
      .addMetadata(Metadata.newBuilder().setExtension(Search.rESPONSEMETADATA, responseMetadataExt2).build())
      .addAllGeoObject(points2.map(buildMapPoint).asJava)
      .build()
    Seq(response1, response2)
  }

  private def buildMapPoint(data: Data): GeoObjectOuterClass.GeoObject = {

    val businessMetadataExt =
      Business.GeoObjectMetadata
        .newBuilder()
        .setOpenHours(OpenHours.newBuilder().setText(data.openHours).build())
        .setName(data.name)
        .setAddress(AddressOuterClass.Address.newBuilder().setFormattedAddress("").build())
        .setGeocodeResult(Business.GeocodeResult.getDefaultInstance)
        .setId(data.id)
        .addAllCategory(
          data.categoryText
            .map(text => Business.Category.newBuilder().setName(text).addTag(data.categoryIdTag).build())
            .asJava
        )
        .build()

    val businessRatingMetadata =
      data.rating match {
        case (rating, reviews) =>
          val a =
            BusinessRating2X.BusinessRatingMetadata
              .newBuilder()
              .setScore(rating)
              .setRatings(reviews)
              .setReviews(5)
              .build()
          Metadata.newBuilder().setExtension(BusinessRating2X.gEOOBJECTMETADATA, a).build()
      }

    val photosMetadataOpt = if (data.photoUrl.nonEmpty) {
      val photos = data.photoUrl.map(
        url =>
          Photos2X.Photo
            .newBuilder()
            .setUrlTemplate(url)
            .build()
      )
      val metadata = Photos2X.GeoObjectMetadata
        .newBuilder()
        .addAllPhoto(photos.asJava)
        .setCount(photos.size)
        .build()
      Some(
        Metadata.newBuilder().setExtension(Photos2X.gEOOBJECTMETADATA, metadata).build()
      )
    } else {
      None
    }

    val businessMetadata = Metadata.newBuilder().setExtension(Business.gEOOBJECTMETADATA, businessMetadataExt).build()
    val geoObjectBuilder =
      GeoObjectOuterClass.GeoObject
        .newBuilder()
        .addGeometry(
          GeometryOuterClass.Geometry
            .newBuilder()
            .setPoint(GeometryOuterClass.Point.newBuilder().setLat(data.lat).setLon(data.lon))
        )
        .addMetadata(businessMetadata)
        .setName(data.name)
    photosMetadataOpt.foreach(geoObjectBuilder.addMetadata)
    geoObjectBuilder.addMetadata(businessRatingMetadata)
    geoObjectBuilder
      .build()
  }
}

object MapInfrastructurePointResponseBuilderSpec {
  private case class Data(
    id: String,
    categoryText: Seq[String],
    openHours: String,
    photoUrl: Seq[String],
    rating: Tuple2[Float, Int],
    lat: Float,
    lon: Float,
    categoryIdTag: String,
    kind: MapInfrastructure.Kind,
    name: String
  )

  private val point1 = Data(
    "1015287181",
    Seq("Театр"),
    "ежедневно, 10:00–21:00",
    Seq("https://avatars.mds.yandex.net/get-altay/5477999/2a0000017d3a608b9034264346d09f37462b/%s"),
    Tuple2(4.6f, 4),
    55.7633f,
    37.604507f,
    "id:184105892",
    MapInfrastructure.Kind.ENTERTAINMENTS,
    "Московский художественный академический театр имени М. Горького"
  )

  private val point2 = Data(
    "48738365036",
    Seq("Кондитерская", "Кофейня", "Торты на заказ"),
    "ежедневно, 11:00–21:00",
    Seq(
      "https://avatars.mds.yandex.net/get-altay/363317/2a0000015ed05c58941a4e5adc8ff38f796f/%s",
      "https://avatars.mds.yandex.net/get-altay/372953/2a0000015ed05c7e72e4d5838212c7a4ce88/%s"
    ),
    Tuple2(5f, 10),
    55.7633f,
    37.604507f,
    "id:184106394",
    MapInfrastructure.Kind.FOOD,
    "Пушкинъ"
  )
  private val point3 = Data(
    "1106897219",
    Seq("Ресторан", "Банкетный зал"),
    "ежедневно, 12:00–00:00",
    Seq("https://avatars.mds.yandex.net/get-altay/225456/2a00000160442e91c0bad050ee770c94f7c7/%s"),
    Tuple2(4.3f, 2),
    55.765f,
    37.604f,
    "id:184106390",
    MapInfrastructure.Kind.FOOD,
    "Турандот"
  )
}
