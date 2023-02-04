package ru.yandex.vos2.realty.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vertis.vos2.model.realty.billing.ProductKind
import ru.yandex.vertis.vos2.model.realty.VasRestriction.Reason.{VUR_NEW_FLAT, VUR_NO_PHOTO}

@RunWith(classOf[JUnitRunner])
class ProductCheckerSpec extends WordSpecLike with Matchers with Checkers {

  "ProductChecker" should {

    "always allow placements" in {
      val offer = TestUtils.createOffer()
      clearPhotos(offer)
      setNewFlat(value = true, offer)
      assert(ProductChecker.isApplicable(ProductKind.PK_PLACEMENT, offer))
    }

    "not allow some products for offer without photos" in {
      val offer = TestUtils.createOffer()
      clearPhotos(offer)
      setNewFlat(value = false, offer)
      Seq(ProductKind.PK_PREMIUM, ProductKind.PK_PROMOTION, ProductKind.PK_RAISING)
        .foreach(kind => {
          assert(!ProductChecker.isApplicable(kind, offer))
          assert(ProductChecker.check(kind, offer).contains(VUR_NO_PHOTO))
        })
    }

    "not allow some products for offer from new building" in {
      val offer = TestUtils.createOffer()
      addPhoto(offer)
      setNewFlat(value = true, offer)
      Seq(ProductKind.PK_PREMIUM, ProductKind.PK_PROMOTION, ProductKind.PK_RAISING)
        .foreach(kind => {
          assert(!ProductChecker.isApplicable(kind, offer))
          assert(ProductChecker.check(kind, offer).contains(VUR_NEW_FLAT))
        })
    }
  }

  private def setNewFlat(value: Boolean, offer: Offer.Builder): Offer.Builder = {
    offer.getOfferRealtyBuilder.getFacilitiesBuilder.setFlagIsNew(value)
    offer
  }

  private def clearPhotos(offer: Offer.Builder): Offer.Builder = {
    offer.clearImageRef().getOfferRealtyBuilder.clearPhoto()
    offer
  }

  private def addPhoto(offer: Offer.Builder): Offer.Builder = {
    offer.addImageRefBuilder().setActive(true).setKey("some").setUrl("https://example.com")
    offer
  }
}
