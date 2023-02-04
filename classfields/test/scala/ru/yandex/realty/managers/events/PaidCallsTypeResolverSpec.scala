package ru.yandex.realty.managers.events

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.events.OfferInfo.PaidCallsType
import ru.yandex.realty.model.message.ExtDataSchema.SuperCall
import ru.yandex.realty.proto.unified.offer.vas.{CommonTuzInfo, ExtendedTariffInfo, MaximumTariffInfo, VasInfo}

import java.util.Collections

@RunWith(classOf[JUnitRunner])
class PaidCallsTypeResolverSpec extends SpecBase with PropertyChecks {

  private def campaignOpt(active: java.lang.Boolean, cost: java.lang.Long, superCall: SuperCall): Option[Campaign] = {
    Some(
      new Campaign(
        "1",
        0,
        0,
        "0",
        new java.util.HashMap(),
        Collections.emptyList(),
        Collections.emptyList(),
        cost,
        0,
        active,
        false,
        0,
        0L,
        null,
        new java.util.HashMap(),
        superCall,
        null
      )
    )
  }

  private def unifiedOfferWithTuzExtended(active: Boolean): UnifiedOffer =
    UnifiedOffer
      .newBuilder()
      .setVasInfo(
        VasInfo
          .newBuilder()
          .setTuzExtended(
            ExtendedTariffInfo.newBuilder().setCommonTuzInfo(CommonTuzInfo.newBuilder().setActive(active))
          )
      )
      .build()

  private def unifiedOfferWithTuzMaximum(active: Boolean): UnifiedOffer =
    UnifiedOffer
      .newBuilder()
      .setVasInfo(
        VasInfo
          .newBuilder()
          .setTuzMaximum(
            MaximumTariffInfo.newBuilder().setCommonTuzInfo(CommonTuzInfo.newBuilder().setActive(active))
          )
      )
      .build()

  private val testData =
    Table(
      ("campaign", "offer", "expected"),
      (None, None, PaidCallsType.UNKNOWN),
      (campaignOpt(false, 0L, null), None, PaidCallsType.NO_CHARGE),
      (campaignOpt(false, 1L, null), None, PaidCallsType.NO_CHARGE),
      (campaignOpt(false, 0L, SuperCall.newBuilder().setIsActive(true).build()), None, PaidCallsType.NO_CHARGE),
      (campaignOpt(false, 1L, SuperCall.newBuilder().setIsActive(true).build()), None, PaidCallsType.NO_CHARGE),
      (campaignOpt(true, 0L, null), None, PaidCallsType.NO_CHARGE),
      (campaignOpt(true, 1L, null), None, PaidCallsType.PLACEMENT),
      (campaignOpt(true, 1L, SuperCall.newBuilder().setIsActive(true).build()), None, PaidCallsType.SUPERCALL),
      (campaignOpt(true, 0L, SuperCall.newBuilder().setIsActive(true).build()), None, PaidCallsType.SUPERCALL),
      (None, Some(UnifiedOffer.newBuilder().build()), PaidCallsType.NO_CHARGE),
      (None, Some(UnifiedOffer.newBuilder().setVasInfo(VasInfo.newBuilder()).build()), PaidCallsType.NO_CHARGE),
      (None, Some(unifiedOfferWithTuzExtended(false: Boolean)), PaidCallsType.NO_CHARGE),
      (None, Some(unifiedOfferWithTuzExtended(true: Boolean)), PaidCallsType.TUZ),
      (None, Some(unifiedOfferWithTuzMaximum(false: Boolean)), PaidCallsType.NO_CHARGE),
      (None, Some(unifiedOfferWithTuzMaximum(true: Boolean)), PaidCallsType.TUZ),
      (campaignOpt(false, 1L, null), Some(unifiedOfferWithTuzMaximum(true: Boolean)), PaidCallsType.TUZ),
      (campaignOpt(false, 1L, null), Some(unifiedOfferWithTuzMaximum(false: Boolean)), PaidCallsType.NO_CHARGE),
      (campaignOpt(false, 0L, null), Some(unifiedOfferWithTuzExtended(true: Boolean)), PaidCallsType.TUZ),
      (campaignOpt(false, 0L, null), Some(unifiedOfferWithTuzExtended(false: Boolean)), PaidCallsType.NO_CHARGE)
    )

  "PaidCallsTypeResolver" should {
    forAll(testData) {
      (offerCampaignOpt: Option[Campaign], unifiedOfferOpt: Option[UnifiedOffer], expected: PaidCallsType) =>
        {
          s"resolve type to $expected for ${offerCampaignOpt.map(toString)} and $unifiedOfferOpt" in {
            PaidCallsTypeResolver.resolvePaidCallsType(offerCampaignOpt, unifiedOfferOpt) shouldBe expected
          }
        }
    }
  }

  private def toString(c: Campaign): String = {
    s"active:${c.isActive}, cost: ${c.getCost}, super: ${c.getSuperCall})}"
  }

}
