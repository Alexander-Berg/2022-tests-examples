package ru.auto.api.services.shark

import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.BaseSpec
import ru.auto.api.credits.CreditsModel
import ru.auto.api.services.shark.SharkTinkoffBankStatusUpdater._
import ru.yandex.vertis.shark.proto._

import scala.jdk.CollectionConverters._

class SharkTinkoffBankStatusUpdaterSpec extends BaseSpec {

  private val updateCreditStatusRequest: CreditsModel.UpdateCreditStatusRequest = {
    val options = Map(
      MaxCreditAmountKey -> "900000",
      InterestRateKey -> "5.5",
      MaxDownPaymentKey -> "100000"
    )
    CreditsModel.UpdateCreditStatusRequest.newBuilder
      .setStatus(CreditsModel.UpdateStatusType.SECOND_AGREEMENT)
      .putAllOptions(options.asJava)
      .build
  }

  private val offerId = "12345-somehash"
  private val offerCategory = Category.CARS

  private val creditApplication: CreditApplication = {
    val builder = CreditApplication.newBuilder
    val offers = Seq(
      CreditApplication.Payload.Autoru.Offer.newBuilder
        .setCategory(offerCategory)
        .setId(offerId)
        .build
    )
    builder.getPayloadBuilder.getAutoruBuilder.addAllOffers(offers.asJava)
    builder.build
  }

  "SharkTinkoffBankStatusUpdater" should {
    "toSharkRequest" in {
      val actual = updateCreditStatusRequest.toSharkRequest(creditApplication)
      val expected = {
        val builder = CreditApplicationClaimSource.newBuilder
          .setState(CreditApplication.Claim.ClaimState.PREAPPROVED)
          .setBankState("SECOND_AGREEMENT")
          .setApprovedMaxAmount(900000)
          .setApprovedInterestRate(5.5f)
          .setApprovedMinInitialFeeRate(10f)
        val offerEntities = Seq(
          CreditApplicationClaimSource.Payload.Autoru.OfferEntity.newBuilder
            .setState(CreditApplication.Claim.ClaimPayload.ObjectState.APPROVED)
            .setOffer(CreditApplication.Payload.Autoru.Offer.newBuilder.setCategory(offerCategory).setId(offerId))
            .build
        )
        builder.getPayloadBuilder.getAutoruBuilder.addAllOfferEntities(offerEntities.asJava)
        builder.build
      }
      actual shouldBe expected
    }
  }
}
