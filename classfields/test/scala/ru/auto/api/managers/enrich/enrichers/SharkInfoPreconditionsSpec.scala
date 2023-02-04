package ru.auto.api.managers.enrich.enrichers

import ru.auto.api.ApiOfferModel.{Offer, SharkInfo}
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.DiscountPrice.DiscountPriceStatus
import ru.auto.api.managers.enrich.enrichers.SharkInfoEnricher.PreconditionSource
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._

class SharkInfoPreconditionsSpec extends BaseSpec {

  import SharkInfoPreconditionsSpec._

  private val offer = OfferGen.next

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      "Offer with a price less than the maximum possible for the credit (by CreditProduct)",
      offer.updated(_.getPriceInfoBuilder.setPrice(1000000f)),
      preconditionSource,
      None,
      None,
      SharkInfo.Precondition.newBuilder
        .setInitialFee(100000L)
        .setInterestRate(5f)
        .setTermsMonths(36)
        .setMonthlyPayment(26973)
        .build
    ),
    TestCase(
      "Offer with a price higher than the maximum possible for the credit (by CreditProduct)",
      offer.updated(_.getPriceInfoBuilder.setPrice(3000000f)),
      preconditionSource,
      None,
      None,
      SharkInfo.Precondition.newBuilder
        .setInitialFee(1000000L)
        .setInterestRate(5f)
        .setTermsMonths(36)
        .setMonthlyPayment(59941)
        .build
    ),
    TestCase(
      "Zero interest rate (by CreditProduct)",
      offer.updated(_.getPriceInfoBuilder.setPrice(1000000f)),
      preconditionSource.copy(minInterestRate = 0f),
      None,
      None,
      SharkInfo.Precondition.newBuilder
        .setInitialFee(100000L)
        .setInterestRate(0f)
        .setTermsMonths(36)
        .setMonthlyPayment(25000L)
        .build
    ),
    TestCase(
      "Defaults on zero terms months (by CreditProduct)",
      offer.updated(_.getPriceInfoBuilder.setPrice(1000000f)),
      preconditionSource.copy(maxTermMonths = 0),
      None,
      None,
      SharkInfo.Precondition.newBuilder
        .setInitialFee(100000L)
        .setInterestRate(5f)
        .setTermsMonths(36)
        .setMonthlyPayment(26973L)
        .build
    ),
    TestCase(
      "Zero price (by CreditProduct)",
      offer.updated(_.getPriceInfoBuilder.setPrice(0f)),
      preconditionSource,
      None,
      None,
      SharkInfo.Precondition.newBuilder
        .setInitialFee(0L)
        .setInterestRate(5f)
        .setTermsMonths(36)
        .setMonthlyPayment(0L)
        .build
    ),
    TestCase(
      "With requested initial fee",
      offer.updated(_.getPriceInfoBuilder.setPrice(1000000f)),
      preconditionSource,
      Some(60),
      Some(500000L),
      SharkInfo.Precondition.newBuilder
        .setInitialFee(500000L)
        .setInterestRate(5f)
        .setTermsMonths(36)
        .setMonthlyPayment(14985L)
        .build
    ),
    TestCase(
      "Offer with discount price (by CreditProduct)",
      offer.updated { o =>
        o.getPriceInfoBuilder.setPrice(2000000f)
        o.getDiscountPriceBuilder.setStatus(DiscountPriceStatus.ACTIVE).setPrice(1000000f)
      },
      preconditionSource,
      None,
      None,
      SharkInfo.Precondition.newBuilder
        .setInitialFee(100000L)
        .setInterestRate(5f)
        .setTermsMonths(36)
        .setMonthlyPayment(26973)
        .build
    )
  )

  "SharkInfoEnricher.precondition" should {
    testCases.foreach {
      case TestCase(description, offer, source, requestedLoanTerm, requestedInitialFee, expected) =>
        description in {
          SharkInfoEnricher.precondition(source, offer, requestedLoanTerm, requestedInitialFee) shouldBe expected
        }
    }
  }
}

object SharkInfoPreconditionsSpec {

  private[this] val amountRangeTo: Long = 2000000L
  private[this] val interestRateRangeFrom: Float = 5f
  private[this] val termMonthsRangeTo: Int = 36
  private[this] val minInitialFeeRate: Float = 10f

  private val preconditionSource: PreconditionSource =
    PreconditionSource(
      maxAmount = amountRangeTo,
      minInterestRate = interestRateRangeFrom,
      maxTermMonths = termMonthsRangeTo,
      minInitialFeeRate = minInitialFeeRate
    )

  private case class TestCase(description: String,
                              offer: Offer,
                              preconditionSource: PreconditionSource,
                              requestedLoanTerm: Option[Int],
                              requestedInitialFee: Option[Long],
                              expected: SharkInfo.Precondition)
}
