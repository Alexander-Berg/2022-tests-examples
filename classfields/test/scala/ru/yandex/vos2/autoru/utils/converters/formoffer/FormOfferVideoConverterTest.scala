package ru.yandex.vos2.autoru.utils.converters.formoffer

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.auto.api.ApiOfferModel.{Category, ExternalPanorama}
import ru.auto.api.CommonModel
import ru.auto.panoramas.PanoramasModel
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.Video
import ru.yandex.vos2.AutoruModel.AutoruOffer.YandexVideo.YandexVideoStatus
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.utils.FormTestUtils
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl
import ru.yandex.vos2.util.ExternalPanoramaUtils.{ExternalPanoramasTag, HasExteriorPoiTag}

import java.time.Instant
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner]) //scalastyle:off
class FormOfferVideoConverterTest extends AnyFunSuite with InitTestDbs with OptionValues with BeforeAndAfterAll {
  initDbs()

  private val formTestUtils = new FormTestUtils(components)
  import formTestUtils._

  val formOfferConverter: FormOfferConverter =
    new FormOfferConverter(
      components.carsCatalog,
      components.recognizedLpUtils,
      EmptyDefaultBookingAllowedDeciderImpl,
      components.featuresManager
    )

  test("Add new video") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setVideo(
        CommonModel.Video
          .newBuilder()
          .setYoutubeId("dQw4w9WgXcQ")
      )
      builder.build()
    }

    val currentVideoBuilder =
      Video.newBuilder().setCreated(Instant.now().toEpochMilli).setUpdated(Instant.now().toEpochMilli)
    currentVideoBuilder.getYandexVideoBuilder.setVideoIdent("qweqweqwe").setStatus(YandexVideoStatus.AVAILABLE)
    val deletedVideoBuilder = Video
      .newBuilder()
      .setCreated(Instant.now().toEpochMilli)
      .setUpdated(Instant.now().toEpochMilli)
      .setIsDeleted(true)
      .setDeletedTimestamp(Instant.now().toEpochMilli)
    deletedVideoBuilder.getYoutubeVideoBuilder.setYoutubeId("asdasdasd").setStatus(YoutubeVideoStatus.AVAILABLE)
    val videos = Seq(currentVideoBuilder.build(), deletedVideoBuilder.build())

    val currOffer: OfferModel.Offer = {
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.clearVideo().addAllVideo(videos.asJava)
      builder.build()
    }

    val now = Instant.now().toEpochMilli
    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      Some(currOffer),
      privateAd,
      now
    )

    val newVideoBuilder = Video.newBuilder().setCreated(now).setUpdated(now)
    newVideoBuilder.getYoutubeVideoBuilder.setYoutubeId("dQw4w9WgXcQ").setStatus(YoutubeVideoStatus.UNKNOWN)

    assert(offer.getOfferAutoru.getVideoCount == 3)
    assert(offer.getOfferAutoru.getVideoList.asScala.contains(deletedVideoBuilder.build()))
    assert(
      offer.getOfferAutoru.getVideoList.asScala
        .contains(currentVideoBuilder.setIsDeleted(true).setDeletedTimestamp(now).build())
    )
    assert(offer.getOfferAutoru.getVideoList.asScala.contains(newVideoBuilder.build()))
  }

}
