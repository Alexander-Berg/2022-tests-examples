package ru.yandex.vos2.autoru.utils.moderation

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.{ApiOfferModel, CommonModel}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.ModerationFieldsModel.ModerationFields.Fields
import ru.auto.api.ModerationFieldsModel.ModerationFields.Fields._
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.YandexVideo.YandexVideoStatus
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus
import ru.yandex.vos2.AutoruModel.AutoruOffer.{SellerType, YandexVideo, YoutubeVideo}
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.model.AutoruModelUtils._
import ru.yandex.vos2.autoru.utils.Colors
import ru.yandex.vos2.autoru.utils.docker.DockerAutoruCoreComponents
import ru.yandex.vos2.services.mds.MdsPhotoUtils

import scala.jdk.CollectionConverters._

/**
  * @author pnaydenov
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ModerationProtectionTest extends AnyWordSpec with MockitoSupport with Matchers {
  private lazy val components: AutoruCoreComponents = DockerAutoruCoreComponents

  private val feature = mock[Feature[Boolean]]
  when(feature.value).thenReturn(true)
  private val mdsPhotoUtils = mock[MdsPhotoUtils]
  private val carsCatalog = mock[CarsCatalog]
  private val fieldModifiers = new FieldModifiers(mdsPhotoUtils, carsCatalog)
  private val moderationProtection: ModerationProtection = new ModerationProtection(feature, fieldModifiers)

  private val DealerUserRef: String = "ac_2"
  private val PrivateUserRef: String = "a_123"

  def carOfferBuilder(fields: Seq[Fields]): Offer.Builder = {
    val builder = Offer
      .newBuilder()
      .setOfferService(OfferService.OFFER_AUTO)
      .setTimestampUpdate(0L)
      .setUserRef(DealerUserRef)
    builder.getOfferAutoruBuilder
      .setVersion(1)
      .setCategory(Category.CARS)
      .addAllModerationProtectedFields(fields.asJava)
      .setSellerType(SellerType.COMMERCIAL)
    builder
  }

  "ModerationProtection consistent fields" should {
    "add mark" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION))
      val newFields = moderationProtection.consistentProtectionFields(offer, Seq(MARK), addition = true)
      (newFields should contain).allOf(DESCRIPTION, MARK, MODEL, YEAR, ENGINE_VOLUME, TECH_PARAM_ID, COLOR_HEX)
    }

    "add color" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION))
      val newFileds = moderationProtection.consistentProtectionFields(offer, Seq(COLOR_HEX), addition = true)
      (newFileds should contain).allOf(DESCRIPTION, MARK, MODEL, YEAR, ENGINE_VOLUME, TECH_PARAM_ID, COLOR_HEX)
    }

    "add price" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION))
      val newFileds = moderationProtection.consistentProtectionFields(offer, Seq(PRICE_INFO), addition = true)
      newFileds.toSet shouldEqual Set(DESCRIPTION, PRICE_INFO)
    }

    "remove mark" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION, MARK, MODEL, COLOR_HEX))
      val newFileds = moderationProtection.consistentProtectionFields(offer, Seq(MARK), addition = false)
      newFileds.toSet shouldEqual Set(DESCRIPTION)
    }

    "remove color" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION, MARK, MODEL, COLOR_HEX))
      val newFileds = moderationProtection.consistentProtectionFields(offer, Seq(COLOR_HEX), addition = false)
      newFileds.toSet shouldEqual Set(DESCRIPTION)
    }

    "remove price" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION, MARK, MODEL, PRICE_INFO))
      val newFileds = moderationProtection.consistentProtectionFields(offer, Seq(PRICE_INFO), addition = false)
      newFileds.toSet shouldEqual Set(DESCRIPTION, MARK, MODEL)
    }

    "add mark for private offer" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION))
      offer
        .setUserRef(PrivateUserRef)
        .getOfferAutoruBuilder
        .setSellerType(SellerType.PRIVATE)
      val newFileds = moderationProtection.consistentProtectionFields(offer, Seq(MARK), addition = true)
      newFileds.toSet shouldEqual Set(DESCRIPTION, MARK)
    }

    "remove mark from private offer" in {
      val offer = carOfferBuilder(Seq(DESCRIPTION, MARK, MODEL))
      offer
        .setUserRef(PrivateUserRef)
        .getOfferAutoruBuilder
        .setSellerType(SellerType.PRIVATE)
      val newFileds = moderationProtection.consistentProtectionFields(offer, Seq(MARK), addition = false)
      newFileds.toSet shouldEqual Set(DESCRIPTION, MODEL)
    }
  }
  "ModerationProtection" should {
    "protect video offer" in {
      val offerBuilder = carOfferBuilder(Seq(DESCRIPTION, MARK, MODEL, PRICE_INFO, VIDEO))
      val offer = offerBuilder
        .setOfferAutoru(
          offerBuilder.getOfferAutoruBuilder
            .addVideo(
              AutoruOffer.Video
                .newBuilder()
                .setYoutubeVideo(YoutubeVideo.newBuilder().setStatus(YoutubeVideoStatus.AVAILABLE))
                .setCreated(1)
                .setUpdated(1)
                .build()
            )
        )
        .build
      val updatedOffer = offerBuilder
        .setOfferAutoru(
          offerBuilder.getOfferAutoruBuilder
            .addVideo(
              AutoruOffer.Video
                .newBuilder()
                .setYandexVideo(YandexVideo.newBuilder().setStatus(YandexVideoStatus.AVAILABLE))
                .setCreated(1)
                .setUpdated(1)
                .build()
            )
        )
        .build
      moderationProtection.checkProtect(offer, updatedOffer) shouldBe false
    }
    "protect video form" in {
      val offerBuilder = carOfferBuilder(Seq(DESCRIPTION, MARK, MODEL, PRICE_INFO, VIDEO))
      val offer = offerBuilder
        .setOfferAutoru(
          offerBuilder.getOfferAutoruBuilder
            .addVideo(
              AutoruOffer.Video
                .newBuilder()
                .setYoutubeVideo(YoutubeVideo.newBuilder().setStatus(YoutubeVideoStatus.AVAILABLE))
                .setCreated(1)
                .setUpdated(1)
                .build()
            )
        )
        .build
      val form = ApiOfferModel.Offer.newBuilder
        .setId(offer.getOfferID)
        .setCategory(offer.category)
        .setState(
          ApiOfferModel.State
            .newBuilder()
            .setVideo(
              CommonModel.Video.newBuilder().setYandexId("1")
            )
        )
        .build
      moderationProtection.protectIfNeeded(false, offer, form)._2.isEmpty shouldBe false
    }
    "protect photos if protected" in {
      components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, true)
      val moderationProtection = components.moderationProtection

      val offer = carOfferBuilder(Seq(PHOTO))

      val form = ApiOfferModel.Offer.newBuilder
        .setId(offer.getOfferID)
        .setCategory(offer.category)

      form.getStateBuilder
        .addImageUrlsBuilder()
        .setName("autoru-vos:123-aabbcc")

      val (newForm, notices) = moderationProtection.protectIfNeeded(moderator = false, offer.build, form.build)
      assert(newForm.getState.getImageUrlsCount == 0)
      assert(notices.nonEmpty)
    }
    "change photos if not protected" in {
      components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, true)
      val moderationProtection = components.moderationProtection

      val offer = carOfferBuilder(Seq(GEAR_TYPE))

      val form = ApiOfferModel.Offer.newBuilder
        .setId(offer.getOfferID)
        .setCategory(offer.category)

      form.getStateBuilder
        .addImageUrlsBuilder()
        .setName("autoru-vos:123-aabbcc")

      val (newForm, notices) = moderationProtection.protectIfNeeded(moderator = false, offer.build, form.build)
      assert(newForm.getState.getImageUrlsCount == 1)
      assert(notices.isEmpty)
    }
    "protect chat_only if protected" in {
      components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, true)
      val protection = components.moderationProtection
      val offer = carOfferBuilder(Seq(CHAT_ONLY))

      offer.getOfferAutoruBuilder.getSellerBuilder
        .setChatOnly(true)

      val form = ApiOfferModel.Offer
        .newBuilder()
        .setId(offer.getOfferID)
        .setCategory(offer.category)

      form.getAdditionalInfoBuilder
        .setChatOnly(false)

      val (newForm, notices) = protection.protectIfNeeded(moderator = false, offer.build(), form.build)
      assert(newForm.getAdditionalInfo.getChatOnly)
      assert(notices.nonEmpty)
    }
  }

  private val feedprocessorCategories = Seq(Category.CARS, Category.TRUCKS)

  for (category <- feedprocessorCategories) s"ModerationProtection.matchOfferByVitalFields ($category)" should {

    val section = Section.NEW
    // см. ru.yandex.vos2.autoru.utils.Colors: цвет выбран так, чтобы соблюдалось условие:
    // colors.find(_.hex == color.toLowerCase) == colors.find(_.searcherColor == color)
    val color = "CACECB"
    val mark = "AUDI"
    val model = "TT"
    val techParamId = 1234
    val vin = "test_vin"
    val year = 2020

    val baseForm = {
      val b = ApiOfferModel.Offer
        .newBuilder()
        .setUserRef(DealerUserRef)
        .setCategory(category)
        .setSection(section)
        .setColorHex(color)
      category match {
        case Category.CARS => b.getCarInfoBuilder.setMark(mark).setModel(model).setTechParamId(techParamId)
        case Category.TRUCKS => b.getTruckInfoBuilder.setMark(mark).setModel(model)
        case _ => fail(s"feedprocessor doesn't produce offers of category = $category")
      }
      b.getDocumentsBuilder.setVin(vin).setYear(year)
      b.build()
    }

    val baseOffer = {
      val b =
        Offer.newBuilder().setUserRef(DealerUserRef).setOfferService(OfferService.OFFER_AUTO).setTimestampUpdate(0)
      val offerB = b.getOfferAutoruBuilder
      offerB.setCategory(category).setSection(section).setVersion(1).setColorHex(color)
      category match {
        case Category.CARS => offerB.getCarInfoBuilder.setMark(mark).setModel(model).setTechParamId(techParamId)
        case Category.TRUCKS => offerB.getTruckInfoBuilder.setMark(mark).setModel(model)
        case _ => fail(s"feedprocessor doesn't produce offers of category = $category")
      }
      offerB.getDocumentsBuilder.setVin(vin)
      offerB.getEssentialsBuilder.setYear(year)
      b.build()
    }

    "match offer if form.autoru_expert isn't defined && offer.autoru_expert isn't defined" in {
      val form = baseForm
      val offer = baseOffer
      assert(!form.hasAdditionalInfo)
      assert(!offer.getOfferAutoru.hasAutoruExpert)

      val result = moderationProtection.matchOfferByVitalFields(form, Seq(offer))

      result shouldBe Some(offer)
    }

    "match offer if form.autoru_expert == false && offer.autoru_expert isn't defined" in {
      val formB = baseForm.toBuilder
      formB.getAdditionalInfoBuilder.setAutoruExpert(false)
      val form = formB.build()
      val offer = baseOffer
      assert(!offer.getOfferAutoru.hasAutoruExpert)

      val result = moderationProtection.matchOfferByVitalFields(form, Seq(offer))

      assert(result.contains(offer))
    }

    "match offer if offer.autoru_expert == false && form.autoru_expert isn't defined" in {
      val offerB = baseOffer.toBuilder
      offerB.getOfferAutoruBuilder.setAutoruExpert(false)
      val offer = offerB.build()
      val form = baseForm
      assert(!form.hasAdditionalInfo)

      val result = moderationProtection.matchOfferByVitalFields(form, Seq(offer))

      assert(result.contains(offer))
    }

    "match offer if offer.autoru_expert == false && form.autoru_expert == false" in {
      val formB = baseForm.toBuilder
      formB.getAdditionalInfoBuilder.setAutoruExpert(false)
      val form = formB.build()
      val offerB = baseOffer.toBuilder
      offerB.getOfferAutoruBuilder.setAutoruExpert(false)
      val offer = offerB.build()

      val result = moderationProtection.matchOfferByVitalFields(form, Seq(offer))

      assert(result.contains(offer))
    }

    // Если оффер не сматчили, будет создан новый оффер
    "match offer if form.autoru_expert == true && offer.autoru_expert != true" in {
      val formB = baseForm.toBuilder
      formB.getAdditionalInfoBuilder.setAutoruExpert(true)
      val form = formB.build()
      val offer = baseOffer
      assert(!offer.getOfferAutoru.getAutoruExpert)

      val result = moderationProtection.matchOfferByVitalFields(form, Seq(offer))

      assert(result.contains(offer))
    }

    "match offer if offer.autoru_expert == true && form.autoru_expert != true" in {
      val offerB = baseOffer.toBuilder
      offerB.getOfferAutoruBuilder.setAutoruExpert(true)
      val offer = offerB.build()
      val form = baseForm
      assert(!form.getAdditionalInfo.getAutoruExpert)

      val result = moderationProtection.matchOfferByVitalFields(form, Seq(offer))

      assert(result.contains(offer))
    }

    "match offer if color is changed by moderation" in {
      val colorSetByDealer = Colors.values.find(_.ruName == "Бежевый").head
      val colorSetByModeration = Colors.values.find(_.ruName == "Белый").head

      val form = baseForm.toBuilder.setColorHex(colorSetByDealer.searcherColor).build()
      val offerB = baseOffer.toBuilder
      offerB.getOfferAutoruBuilder.setColorHex(colorSetByModeration.hex).addModerationProtectedFields(COLOR_HEX).build()
      val offer = offerB.build()

      val result = moderationProtection.matchOfferByVitalFields(form, Seq(offer))

      assert(result.contains(offer))
    }
  }
}
