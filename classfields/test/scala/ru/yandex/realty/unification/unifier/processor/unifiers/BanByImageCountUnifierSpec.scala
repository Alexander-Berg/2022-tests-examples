package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.realty.model.offer.{CategoryType, IndexingError}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper

@RunWith(classOf[JUnitRunner])
class BanByImageCountUnifierSpec extends AsyncSpecBase with FeaturesStubComponent {

  features.BanFeedOfferWithSmallImgsCount.setNewState(true)
  val unifier = new BanByImageCountUnifier(features)

  implicit val trace: Traced = Traced.empty

  "BanByImageCountUnifier " should {

    "don't ban lot offer with one image" in {
      val offer = OfferModelGenerators.offerGen(categoryType = Some(CategoryType.LOT), imageCount = 1).next
      offer.getApartmentInfo.setNewFlat(false)
      val wrapper = new OfferWrapper(null, offer, null)
      unifier.unify(wrapper).futureValue
      wrapper.getOffer.getOfferState.getErrors.isEmpty shouldBe true
    }
    "ban lot offer without images " in {
      val offer = OfferModelGenerators.offerGen(categoryType = Some(CategoryType.LOT)).next
      offer.getApartmentInfo.setNewFlat(false)
      val wrapper = new OfferWrapper(null, offer, null)
      unifier.unify(wrapper).futureValue
      wrapper.getOffer.getOfferState.getErrors.get(0).getError shouldBe IndexingError.BAD_PHOTO
    }

    "don't ban commercial of garage offer with two or more images" in {
      val commercialOffer =
        OfferModelGenerators.offerGen(categoryType = Some(CategoryType.COMMERCIAL), imageCount = 2).next
      commercialOffer.getApartmentInfo.setNewFlat(false)
      val commercialWrapper = new OfferWrapper(null, commercialOffer, null)

      val garageOffer = OfferModelGenerators.offerGen(categoryType = Some(CategoryType.COMMERCIAL), imageCount = 5).next
      garageOffer.getApartmentInfo.setNewFlat(false)
      val garageWrapper = new OfferWrapper(null, garageOffer, null)

      unifier.unify(commercialWrapper).futureValue
      unifier.unify(garageWrapper).futureValue
      commercialWrapper.getOffer.getOfferState.getErrors.isEmpty shouldBe true
      garageWrapper.getOffer.getOfferState.getErrors.isEmpty shouldBe true
    }

    "ban commercial and garage offer with less then two images " in {
      val commercialOffer =
        OfferModelGenerators.offerGen(categoryType = Some(CategoryType.COMMERCIAL), imageCount = 1).next
      commercialOffer.getApartmentInfo.setNewFlat(false)
      val commercialWrapper = new OfferWrapper(null, commercialOffer, null)

      val garageOffer = OfferModelGenerators.offerGen(categoryType = Some(CategoryType.COMMERCIAL), imageCount = 1).next
      garageOffer.getApartmentInfo.setNewFlat(false)
      val garageWrapper = new OfferWrapper(null, garageOffer, null)

      unifier.unify(commercialWrapper).futureValue
      unifier.unify(garageWrapper).futureValue
      commercialWrapper.getOffer.getOfferState.getErrors.get(0).getError shouldBe IndexingError.BAD_PHOTO
      garageWrapper.getOffer.getOfferState.getErrors.get(0).getError shouldBe IndexingError.BAD_PHOTO
    }

  }
}
