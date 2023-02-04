package ru.yandex.vos2.autoru.utils.converters.formoffer

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.auto.api.ApiOfferModel.{Category, ExternalPanorama}
import ru.auto.panoramas.PanoramasModel
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.utils.FormTestUtils
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl
import ru.yandex.vos2.util.ExternalPanoramaUtils.{ExternalPanoramasTag, HasExteriorPoiTag}

import java.time.Instant
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner]) //scalastyle:off
class FormOfferExternalPanoramasConverterTest
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

  test("add new panorama as published") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setExternalPanorama(
        ExternalPanorama
          .newBuilder()
          .setPublished(PanoramasModel.Panorama.newBuilder().setId("777-xxx-zzz").setPoiCount(1))
      )
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("111-xxx-zzz")
          .setPublished(false)
          .setIsNext(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)
          .build(),
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("222-xxx-zzz")
          .setPublished(true)
          .setIsNext(false)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .setPoiCount(1)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      Some(currOffer),
      privateAd,
      Instant.now().getEpochSecond
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 3)
    assert(offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama1 = offer.getOfferAutoru.getExternalPanorama(0)
    assert(!newPanorama1.getIsNext)
    assert(!newPanorama1.getPublished)
    assert(newPanorama1.getId == "111-xxx-zzz")
    assert(newPanorama1.getStatus == AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)

    val newPanorama2 = offer.getOfferAutoru.getExternalPanorama(1)
    assert(!newPanorama2.getIsNext)
    assert(newPanorama2.getPublished)
    assert(newPanorama2.getId == "222-xxx-zzz")
    assert(newPanorama2.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)

    val newPanorama3 = offer.getOfferAutoru.getExternalPanorama(2)
    assert(newPanorama3.getIsNext)
    assert(!newPanorama3.getPublished)
    assert(newPanorama3.getId == "777-xxx-zzz")
    assert(newPanorama3.getStatus == AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)
  }

  test("add existing in next panorama as published") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setExternalPanorama(
        ExternalPanorama
          .newBuilder()
          .setPublished(PanoramasModel.Panorama.newBuilder().setId("111-xxx-zzz"))
      )
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("111-xxx-zzz")
          .setPublished(false)
          .setIsNext(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)
          .build(),
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("222-xxx-zzz")
          .setPublished(true)
          .setIsNext(false)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      Some(currOffer),
      privateAd,
      Instant.now().getEpochSecond
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 2)
    assert(offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama1 = offer.getOfferAutoru.getExternalPanorama(0)
    assert(newPanorama1.getIsNext)
    assert(!newPanorama1.getPublished)
    assert(newPanorama1.getId == "111-xxx-zzz")
    assert(newPanorama1.getStatus == AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)

    val newPanorama2 = offer.getOfferAutoru.getExternalPanorama(1)
    assert(!newPanorama2.getIsNext)
    assert(newPanorama2.getPublished)
    assert(newPanorama2.getId == "222-xxx-zzz")
    assert(newPanorama2.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)
  }

  test("add existing in published panorama as published") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setExternalPanorama(
        ExternalPanorama
          .newBuilder()
          .setPublished(PanoramasModel.Panorama.newBuilder().setId("222-xxx-zzz").setPoiCount(2))
      )
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("111-xxx-zzz")
          .setPublished(false)
          .setIsNext(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)
          .build(),
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("222-xxx-zzz")
          .setPublished(true)
          .setIsNext(false)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      Some(currOffer),
      privateAd,
      Instant.now().getEpochSecond
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 2)
    assert(offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama1 = offer.getOfferAutoru.getExternalPanorama(0)
    assert(!newPanorama1.getIsNext)
    assert(!newPanorama1.getPublished)
    assert(newPanorama1.getId == "111-xxx-zzz")
    assert(newPanorama1.getStatus == AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)

    val newPanorama2 = offer.getOfferAutoru.getExternalPanorama(1)
    assert(!newPanorama2.getIsNext)
    assert(newPanorama2.getPublished)
    assert(newPanorama2.getId == "222-xxx-zzz")
    assert(newPanorama2.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)
  }

  test("add existing in published panorama as next") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setExternalPanorama(
        ExternalPanorama
          .newBuilder()
          .setNext(PanoramasModel.Panorama.newBuilder().setId("222-xxx-zzz").setPoiCount(1))
      )
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("111-xxx-zzz")
          .setPublished(false)
          .setIsNext(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)
          .build(),
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("222-xxx-zzz")
          .setPublished(true)
          .setIsNext(false)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      Some(currOffer),
      privateAd,
      Instant.now().getEpochSecond
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 2)
    assert(offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama1 = offer.getOfferAutoru.getExternalPanorama(0)
    assert(!newPanorama1.getIsNext)
    assert(!newPanorama1.getPublished)
    assert(newPanorama1.getId == "111-xxx-zzz")
    assert(newPanorama1.getStatus == AutoruOffer.ExternalPanorama.Status.AWAITING_PROCESSING)

    val newPanorama2 = offer.getOfferAutoru.getExternalPanorama(1)
    assert(!newPanorama2.getIsNext)
    assert(newPanorama2.getPublished)
    assert(newPanorama2.getId == "222-xxx-zzz")
    assert(newPanorama2.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)
  }

  test("add new panorama for new offer") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setExternalPanorama(
        ExternalPanorama
          .newBuilder()
          .setNext(PanoramasModel.Panorama.newBuilder().setId("111-xxx-yyy").setPoiCount(1))
      )
      builder.build()
    }

    val offer = formOfferConverter.convertNewOffer(
      userRef,
      Category.CARS,
      form,
      privateAd,
      Instant.now().getEpochSecond
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 1)
    assert(!offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama = offer.getOfferAutoru.getExternalPanorama(0)
    assert(newPanorama.hasIsNext)
    assert(newPanorama.getIsNext)
    assert(!newPanorama.getPublished)
    assert(newPanorama.getId == "111-xxx-yyy")
  }

  test("unpublished panorama from form") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setExternalPanorama(ExternalPanorama.getDefaultInstance)
      builder.build()
    }

    val currOffer: OfferModel.Offer = {

      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("333-zzz-www")
          .setPublished(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .setPoiCount(1)
          .build()
      )

      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      None,
      privateAd,
      Instant.now().getEpochSecond
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 1)
    assert(!offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama1 = offer.getOfferAutoru.getExternalPanorama(0)
    assert(newPanorama1.hasIsNext)
    assert(!newPanorama1.getIsNext)
    assert(!newPanorama1.getPublished)
    assert(newPanorama1.getId == "333-zzz-www")
    assert(newPanorama1.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)
  }

  test("skip if don't set panoramas from form and cannot edit panoramas") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.clearExternalPanorama()
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("333-zzz-www")
          .setPublished(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .setPoiCount(1)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      None,
      privateAd,
      Instant.now().getEpochSecond,
      FormWriteParams.empty.copy(isFeed = false)
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 1)
    assert(offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama = offer.getOfferAutoru.getExternalPanorama(0)

    assert(!newPanorama.hasIsNext)
    assert(newPanorama.getPublished)
    assert(newPanorama.getId == "333-zzz-www")
    assert(newPanorama.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)
  }

  test("unpublish if don't set panoramas from form and can edit panoramas") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.clearExternalPanorama()
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("333-zzz-www")
          .setPublished(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .setPoiCount(2)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.addTag(ExternalPanoramasTag)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      None,
      privateAd,
      Instant.now().getEpochSecond,
      FormWriteParams.empty.copy(isFeed = false, clientSupportPanoramas = true)
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 0)
    assert(!offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasExteriorPoiTag))
  }

  test("unpublished panoramas from feed") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.setExternalPanorama(ExternalPanorama.getDefaultInstance)
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("333-zzz-www")
          .setPublished(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .setPoiCount(1)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      None,
      privateAd,
      Instant.now().getEpochSecond,
      FormWriteParams.empty.copy(isFeed = true)
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 1)
    assert(!offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(!offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama = offer.getOfferAutoru.getExternalPanorama(0)
    assert(newPanorama.hasIsNext)
    assert(!newPanorama.getIsNext)
    assert(!newPanorama.getPublished)
    assert(newPanorama.getId == "333-zzz-www")
    assert(newPanorama.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)
  }

  test("skip if isn't set panoramas from feed") {
    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getStateBuilder.clearExternalPanorama()
      builder.build()
    }

    val currOffer: OfferModel.Offer = {
      val panoramas = Seq(
        AutoruOffer.ExternalPanorama
          .newBuilder()
          .setId("333-zzz-www")
          .setPublished(true)
          .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
          .setPoiCount(1)
          .build()
      )
      val builder = curPrivateProto.toBuilder
      builder.getOfferAutoruBuilder.addAllExternalPanorama(panoramas.asJava)
      builder.build()
    }

    val offer = formOfferConverter.convertExistingOffer(
      form,
      currOffer,
      None,
      privateAd,
      Instant.now().getEpochSecond,
      FormWriteParams.empty.copy(isFeed = true)
    )

    assert(offer.getOfferAutoru.getExternalPanoramaCount == 1)
    assert(offer.getTagList.asScala.contains(ExternalPanoramasTag))
    assert(offer.getTagList.asScala.contains(HasExteriorPoiTag))

    val newPanorama = offer.getOfferAutoru.getExternalPanorama(0)

    assert(!newPanorama.hasIsNext)
    assert(newPanorama.getPublished)
    assert(newPanorama.getId == "333-zzz-www")
    assert(newPanorama.getStatus == AutoruOffer.ExternalPanorama.Status.COMPLETED)
  }
}
