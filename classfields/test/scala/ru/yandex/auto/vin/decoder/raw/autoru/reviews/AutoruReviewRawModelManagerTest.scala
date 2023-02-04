package ru.yandex.auto.vin.decoder.raw.autoru.reviews

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.reviews.ReviewModel.{Review => AutoReview}
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Review, VinInfoHistory}
import ru.yandex.auto.vin.decoder.raw.autoru.reviews.models._
import ru.yandex.auto.vin.decoder.yt.diff.DbActions.Delete
import ru.yandex.vertis.protobuf.ProtobufUtils

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

class AutoruReviewRawModelManagerTest extends AnyWordSpec with Matchers {

  import AutoruReviewRawModelManagerTest._

  val manager = new AutoruReviewRawModelManager

  "parse" should {
    "parse correct json" in {
      manager.parse(correctReviewWithOfferJson, "", "") should be(Right(correctRawModel))
    }

    "parse deleted json" in {
      manager.parse(deletedReviewWithOfferJson, "", "") should be(Right(deletedRawModel))
    }

    "return Left for incorrect json" in {
      manager.parse("{}", "", "").isLeft should be(true)
    }
  }

  "convert" should {
    "convert correct raw model to vin history correctly" in {
      Await.result(manager.convert(correctRawModel), 1.seconds) should be(correctVinHistoryMessage)
    }

    "fail process when raw model contains bad mds link" in {
      assertThrows[RuntimeException](manager.convert(rawModelWithBadMdsLink))
    }
  }

  "buildDeleted" should {
    "build deleted review" in {
      val deleteAction = Delete("", "", 1L, 1L, 1L, deletedReviewWithOfferJson)

      manager.buildDeleted(deleteAction) should be(deletedRawModel)
    }
  }

  "alreadyDeleted" should {
    "return false for not deleted review" in {
      manager.alreadyDeleted(correctReviewWithOfferJson) should be(false)
    }

    "return true for deleted review" in {
      manager.alreadyDeleted(deletedReviewWithOfferJson) should be(true)
    }
  }
}

object AutoruReviewRawModelManagerTest {

  def genReviewWithOfferRawJson(offerId: String, vin: String, reviewProto: Array[Byte], deleted: Boolean = false) =
    s"""{"offer_id":"$offerId","offer_vin":"$vin","review":${Json.toJson(reviewProto)},"deleted":$deleted}"""

  val correctReviewVin = "VF1LM1B0H34087029"

  lazy val correctReviewProtoBytes: Array[Byte] = {
    val json = ResourceUtils.getStringFromResources("/autoru/reviews/correct_review.json")
    val review = ProtobufUtils.fromJson[AutoReview](AutoReview.getDefaultInstance, json)
    review.toByteArray
  }

  lazy val correctReviewWithOfferJson: String =
    genReviewWithOfferRawJson("1103360309-7ce1463a", "VF1LM1B0H34087029", correctReviewProtoBytes)

  lazy val deletedReviewWithOfferJson: String =
    genReviewWithOfferRawJson("1103360309-7ce1463a", "VF1LM1B0H34087029", correctReviewProtoBytes, deleted = true)

  def genContent(contentType: String, value: String): AutoruReviewContent =
    AutoruReviewContent(
      Some(
        List(
          AutoruReviewContentValue(
            deleted = false,
            Some(value)
          )
        )
      ),
      contentType
    )

  val correctReview: AutoruReview = AutoruReview(
    "7651074881573582306",
    "RENAULT",
    "MEGANE",
    Some(6321881),
    "Для своей цены этот авто трудно в чем-то упрекнуть, они плюсы....",
    List(
      genContent(
        "IMAGE",
        "//avatars.mds.yandex.net/get-autoru-reviews/1393169/2mp8TNheGCYXmNUF677RftyT2cjWAyngM"
      ),
      genContent(
        "TEXT",
        "Полстраны на нем проехали, Питер, Москва, Вологда, Устюг, Коряжма, Нижний"
      ),
      genContent(
        "IMAGE",
        "//avatars.mds.yandex.net/get-autoru-reviews/1393169/2mp8TNheGCYXmNUF677RftyT2cjWAyngM"
      )
    ),
    "2009-03-10T17:10:00Z",
    Some(4.5f)
  )

  val reviewWithBadLink: AutoruReview = correctReview.copy(content =
    correctReview.content.updated(
      0,
      genContent(
        "IMAGE",
        "//avatars.mds.yandex.net/get-autoru-reviews/1393169"
      )
    )
  )

  val reviewWithOffer: AutoruReviewWithOffer =
    AutoruReviewWithOffer("1103360309-7ce1463a", "VF1LM1B0H34087029", correctReview, deleted = false)

  val correctRawModel: AutoruReviewRawModel =
    AutoruReviewRawModel(VinCode(correctReviewVin), correctReviewWithOfferJson, correctReview.id, reviewWithOffer)

  val deletedRawModel: AutoruReviewRawModel =
    correctRawModel.copy(data = correctRawModel.data.copy(deleted = true), raw = deletedReviewWithOfferJson)

  val rawModelWithBadMdsLink: AutoruReviewRawModel =
    correctRawModel.copy(data = reviewWithOffer.copy(review = reviewWithBadLink))

  val correctPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(
        MdsPhotoInfo
          .newBuilder()
          .setNamespace("autoru-reviews")
          .setGroupId(1393169)
          .setName("2mp8TNheGCYXmNUF677RftyT2cjWAyngM")
      )
      .build()

  val correctReviewMessage =
    Review
      .newBuilder()
      .setSource(Review.Source.AUTORU)
      .setId(correctRawModel.data.review.id)
      .setMark(correctRawModel.data.review.mark)
      .setModel(correctRawModel.data.review.model)
      .setSuperGenId(correctRawModel.data.review.superGenId.get)
      .setTitle(correctRawModel.data.review.title)
      .setPublishDate(1236705000000L)
      .setPreviewText(correctRawModel.data.review.content(1).value.get(0).value.get)
      .setRating(correctRawModel.data.review.rating.get)
      .addAllPhotos(
        List(
          correctPhotoInfo,
          correctPhotoInfo
        ).asJava
      )
      .build()

  val correctVinHistoryMessage = VinInfoHistory
    .newBuilder()
    .setStatus(VinInfoHistory.Status.OK)
    .setVin(correctRawModel.data.offerVin)
    .setGroupId(correctRawModel.data.review.id)
    .setEventType(EventType.AUTORU_REVIEW)
    .setReview(correctReviewMessage)
    .build()

}
