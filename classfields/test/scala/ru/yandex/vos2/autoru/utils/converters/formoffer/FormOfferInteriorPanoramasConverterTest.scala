package ru.yandex.vos2.autoru.utils.converters.formoffer

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.auto.api.ApiOfferModel.InteriorPanorama
import ru.auto.panoramas.{InteriorModel, PanoramasModel}
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.utils.FormTestUtils
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl
import ru.yandex.vos2.util.InteriorPanoramaUtils.{HasInteriorPoiTag, InteriorPanoramasTag}

import java.time.Instant
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner]) //scalastyle:off
class FormOfferInteriorPanoramasConverterTest
  extends AnyFunSuite
  with InitTestDbs
  with OptionValues
  with BeforeAndAfterAll {
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

  private val formPanorama1 = InteriorModel.InteriorPanorama
    .newBuilder()
    .setId("1111-xxx-yyy-zzz")
    .build()

  private val formPanorama2 = InteriorModel.InteriorPanorama
    .newBuilder()
    .setId("2222-xxx-yyy-zzz")
    .build()

  private val formPanorama3 = InteriorModel.InteriorPanorama
    .newBuilder()
    .setId("3333-xxx-yyy-zzz")
    .build()

  private val formPanorama4 = InteriorModel.InteriorPanorama
    .newBuilder()
    .setId("4444-xxx-yyy-zzz")
    .build()

  private val formPanorama5 = InteriorModel.InteriorPanorama
    .newBuilder()
    .setId("5555-xxx-yyy-zzz")
    .build()

  private val vosPanorama1 = AutoruOffer.InteriorPanorama
    .newBuilder()
    .setPanorama(formPanorama1.toBuilder.setStatus(PanoramasModel.Status.COMPLETED).build())
    .setPublished(true)
    .setPoiCount(1)
    .build()

  private val vosPanorama2 = AutoruOffer.InteriorPanorama
    .newBuilder()
    .setPanorama(formPanorama2.toBuilder.setStatus(PanoramasModel.Status.PROCESSING).build())
    .setPublished(false)
    .build()

  private val vosPanorama3 = AutoruOffer.InteriorPanorama
    .newBuilder()
    .setPanorama(formPanorama5.toBuilder.setStatus(PanoramasModel.Status.COMPLETED).build())
    .setPublished(true)
    .setPoiCount(1)
    .build()

  test("add panoramas") {

    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setInteriorPanorama(
        InteriorPanorama
          .newBuilder()
          .addAllPanoramas(Seq(formPanorama1, formPanorama2, formPanorama3, formPanorama4).asJava)
          .build()
      )
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllInteriorPanorama(Seq(vosPanorama1, vosPanorama2, vosPanorama3).asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingDraft(
      form,
      currOffer,
      privateAd,
      Instant.now().getEpochSecond
    )

    val panoramas = offer.getOfferAutoru.getInteriorPanoramaList.asScala
    assert(panoramas.size == 4)
    assert(offer.getTagList.asScala.contains(InteriorPanoramasTag))
    assert(offer.getTagList.asScala.contains(HasInteriorPoiTag))

    val newOptPanorama1 = panoramas.find(_.getPanorama.getId == "1111-xxx-yyy-zzz")
    assert(newOptPanorama1.nonEmpty)
    assert(newOptPanorama1.get.getPublished)
    assert(newOptPanorama1.get.getPanorama.getStatus == PanoramasModel.Status.COMPLETED)

    val newOptPanorama2 = panoramas.find(_.getPanorama.getId == "2222-xxx-yyy-zzz")
    assert(newOptPanorama2.nonEmpty)
    assert(!newOptPanorama2.get.getPublished)
    assert(newOptPanorama2.get.getPanorama.getStatus == PanoramasModel.Status.PROCESSING)

    val newOptPanorama3 = panoramas.find(_.getPanorama.getId == "3333-xxx-yyy-zzz")
    assert(newOptPanorama3.nonEmpty)
    assert(!newOptPanorama3.get.getPublished)
    assert(newOptPanorama3.get.getPanorama.getStatus == PanoramasModel.Status.AWAITING_PROCESSING)

    val newOptPanorama4 = panoramas.find(_.getPanorama.getId == "4444-xxx-yyy-zzz")
    assert(newOptPanorama4.nonEmpty)
    assert(!newOptPanorama4.get.getPublished)
    assert(newOptPanorama4.get.getPanorama.getStatus == PanoramasModel.Status.AWAITING_PROCESSING)

    val newOptPanorama5 = panoramas.find(_.getPanorama.getId == "5555-xxx-yyy-zzz")
    assert(newOptPanorama5.isEmpty)
  }

  test("skip panoramas") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.clearInteriorPanorama
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllInteriorPanorama(Seq(vosPanorama1, vosPanorama2, vosPanorama3).asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingDraft(
      form,
      currOffer,
      privateAd,
      Instant.now().getEpochSecond
    )

    val panoramas = offer.getOfferAutoru.getInteriorPanoramaList.asScala
    assert(panoramas.size == 3)
    assert(offer.getTagList.asScala.contains(InteriorPanoramasTag))
    assert(offer.getTagList.asScala.contains(HasInteriorPoiTag))

    val newOptPanorama1 = panoramas.find(_.getPanorama.getId == "1111-xxx-yyy-zzz")
    assert(newOptPanorama1.nonEmpty)
    assert(newOptPanorama1.get.getPublished)
    assert(newOptPanorama1.get.getPanorama.getStatus == PanoramasModel.Status.COMPLETED)

    val newOptPanorama2 = panoramas.find(_.getPanorama.getId == "2222-xxx-yyy-zzz")
    assert(newOptPanorama2.nonEmpty)
    assert(!newOptPanorama2.get.getPublished)
    assert(newOptPanorama2.get.getPanorama.getStatus == PanoramasModel.Status.PROCESSING)

    val newOptPanorama3 = panoramas.find(_.getPanorama.getId == "5555-xxx-yyy-zzz")
    assert(newOptPanorama3.nonEmpty)
    assert(newOptPanorama3.get.getPublished)
    assert(newOptPanorama3.get.getPanorama.getStatus == PanoramasModel.Status.COMPLETED)
  }

  test("remove all") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setInteriorPanorama(InteriorPanorama.getDefaultInstance)
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllInteriorPanorama(Seq(vosPanorama1, vosPanorama2, vosPanorama3).asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingDraft(
      form,
      currOffer,
      privateAd,
      Instant.now().getEpochSecond
    )

    val panoramas = offer.getOfferAutoru.getInteriorPanoramaList.asScala
    assert(panoramas.isEmpty)
    assert(!offer.getTagList.asScala.contains(InteriorPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasInteriorPoiTag))
  }
}
