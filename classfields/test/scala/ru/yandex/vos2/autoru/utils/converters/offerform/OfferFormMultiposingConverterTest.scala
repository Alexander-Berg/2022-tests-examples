package ru.yandex.vos2.autoru.utils.converters.offerform

import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.{ApiOfferModel, CommonModel}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Multiposting, Offer, OfferService}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.AdditionalDataForReading
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OfferFormMultiposingConverterTest extends AnyWordSpec with InitTestDbs {

  implicit private val t: Traced = Traced.empty

  private val converter: OfferFormConverter =
    new OfferFormConverter(
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

  def convert(offer: Offer): ApiOfferModel.Offer =
    converter.convert(ad = AdditionalDataForReading(), offer = offer)

  "setMultiposting" should {
    "don't add multiposting is empty" in {
      val offer = Offer
        .newBuilder()
        .setOfferID("offerId")
        .setUserRef("user:123")
        .setTimestampUpdate(111L)
        .setOfferService(OfferService.OFFER_AUTO)
        .build()

      val result = convert(offer)

      assert(!result.hasMultiposting)
      assert(result.getMultiposting.getStatus == OfferStatus.STATUS_UNKNOWN)
    }
  }

  "setMultiposting: actions" should {
    case class Actions(
        canActivate: Boolean,
        canEdit: Boolean,
        canHide: Boolean,
        canRemove: Boolean
    )

    object Actions {
      def apply(actions: CommonModel.Actions): Actions =
        Actions(
          canActivate = actions.getActivate,
          canEdit = actions.getEdit,
          canHide = actions.getHide,
          canRemove = actions.getArchive
        )
    }

    "set actions for ACTIVE offer" in {
      val offer = Offer
        .newBuilder()
        .setOfferID("offerId")
        .setUserRef("user:123")
        .setTimestampUpdate(111L)
        .setOfferService(OfferService.OFFER_AUTO)
        .setMultiposting {
          Multiposting
            .newBuilder()
            .setStatus(CompositeStatus.CS_ACTIVE)
            .build()
        }
        .build()

      val result = convert(offer)

      val expected = Actions(
        canActivate = false,
        canEdit = true,
        canHide = true,
        canRemove = true
      )

      assert {
        Actions(result.getMultiposting.getActions) == expected
      }
    }
  }

}
