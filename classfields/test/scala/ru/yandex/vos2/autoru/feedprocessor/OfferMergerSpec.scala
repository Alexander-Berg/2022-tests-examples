package ru.yandex.vos2.autoru.feedprocessor

import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.auto.feedprocessor.FeedprocessorModel.{Entity, EntityOrBuilder}
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.AdditionalDataForReading
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.utils.converters.offerform.OfferFormConverter

import scala.jdk.CollectionConverters._

/**
  * @author pnaydenov
  */
class OfferMergerSpec extends AnyWordSpec with Matchers with InitTestDbs {

  private val offerFormConverter: OfferFormConverter = new OfferFormConverter(
    components.mdsPhotoUtils,
    components.regionTree,
    components.mdsPanoramasUtils,
    components.offerValidator,
    components.salonConverter,
    components.currencyRates,
    components.featuresManager,
    components.banReasons,
    components.carsCatalog,
    components.trucksCatalog,
    components.motoCatalog
  )

  private val carOffer = TestUtils.createOffer(dealer = true, category = Category.CARS).build()
  private val carForm = offerFormConverter.convert(AdditionalDataForReading(), carOffer)
  private val carEntity = Entity.newBuilder().setAutoru(carForm).setPosition(0).build()

  def containNotice(messagePattern: String) = NoticeMatcher(messagePattern)

  case class NoticeMatcher(messagePattern: String) extends Matcher[EntityOrBuilder] {

    override def apply(left: EntityOrBuilder): MatchResult = {
      val result = left.getErrorsList.asScala.find { error =>
        error.getType == Entity.Error.Type.NOTICE && error.getMessage.contains(messagePattern)
      }
      val notices = left.getErrorsList.asScala
        .filter(_.getType == Entity.Error.Type.NOTICE)
        .map(_.getMessage)
        .mkString(", ")
      MatchResult(
        result.isDefined,
        s"Notice not found '$messagePattern' amoung: $notices",
        s"Notice found: $messagePattern"
      )
    }
  }

  "OfferMerger" should {
    "merge hand uploaded photos" in {
      val offerBuilder = carOffer.toBuilder
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("hand-created")
        .setIsMain(false)
        .setOrder(1)
        .setCreated(1)
        .setCreatedByFeedprocessor(false)
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("feedprocessor-created")
        .setIsMain(true)
        .setOrder(2)
        .setCreated(1)
        .setCreatedByFeedprocessor(true)
      val entityBuilder = carEntity.toBuilder
      entityBuilder.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("new-photo")

      val result = new PhotoMerger(leaveHandUploadedPhoto = true).merge(entityBuilder.build(), offerBuilder.build())
      result.getAutoru.getState.getImageUrlsList.asScala.map(_.getName).toSet shouldEqual
        Set("hand-created", "new-photo")

      val result2 = new PhotoMerger(leaveHandUploadedPhoto = false).merge(entityBuilder.build(), offerBuilder.build())
      result2.getAutoru.getState.getImageUrlsList.asScala.map(_.getName).toSet shouldEqual Set("new-photo")
    }

    "merge hand uploaded video" in {
      val offerBuilder = carOffer.toBuilder
      offerBuilder.getOfferAutoruBuilder
        .addVideoBuilder()
        .setCreated(1)
        .setUpdated(1)
        .getYoutubeVideoBuilder
        .setYoutubeId("XX")
        .setStatus(YoutubeVideoStatus.AVAILABLE)
      val entityBuilder = carEntity.toBuilder

      val result = new VideoMerger(leaveHandUploadedVideo = true).merge(entityBuilder.build(), offerBuilder.build())
      result.getAutoru.getState.getVideo.getYoutubeId shouldEqual "XX"

      val result2 = new VideoMerger(leaveHandUploadedVideo = false).merge(entityBuilder.build(), offerBuilder.build())
      assert(!result2.getAutoru.getState.hasVideo, "video should not be filled from current offer")

      entityBuilder.getAutoruBuilder.getStateBuilder.getVideoBuilder.setYoutubeId("YY")
      val result3 = new VideoMerger(leaveHandUploadedVideo = false).merge(entityBuilder.build(), offerBuilder.build())
      result3.getAutoru.getState.getVideo.getYoutubeId shouldEqual "YY"
    }

    "merge panaramas" in {
      val offerBuilder = carOffer.toBuilder
      offerBuilder.getOfferAutoruBuilder.getPanoramasBuilder.getInteriorPanoramaBuilder
        .addTileLevelsBuilder()
        .setOrigPhotoName("foo-bar")
      val entityBuilder = carEntity.toBuilder
      entityBuilder
        .addImagesBuilder()
        .setImageId("foo-bar")
        .setPhotoType(Entity.PhotoType.INTERIOR_PANORAMA)
        .setIsLoaded(true)
        .setSrcUrl("http://foo/bar")

      val result = PanoramasMerger.merge(entityBuilder.build(), offerBuilder.build())
      assert(result.getAutoru.getState.getPanoramas.hasInteriorPanorama, "Panorama not filled")
    }
  }
}
