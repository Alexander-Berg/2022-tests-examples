package ru.yandex.vertis.shark.controller

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.model.Api._
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.CreditProduct.{AmountRange, InterestRateRange, TermMonthsRange}
import ru.yandex.vertis.shark.model.{Arbitraries, CommonApplicationObjectPayload, ConsumerCreditProduct}
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object PreconditionsCalculatorSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  private val sampleCreditProduct: ConsumerCreditProduct = Arbitraries
    .generate[ConsumerCreditProduct]
    .sample
    .get
    .copy(
      amountRange = AmountRange(from = 10000L.taggedWith.some, to = 2000000L.taggedWith.some),
      interestRateRange = InterestRateRange(from = 5f.taggedWith.some, to = 10f.taggedWith.some),
      termMonthsRange = TermMonthsRange(from = 12.taggedWith.some, to = 36.taggedWith.some),
      minInitialFeeRate = 10f.taggedWith
    )

  private val preconditionsCalculatorLayer = PreconditionsCalculator.live

  case class TestCase(
      description: String,
      objectInfo: PreconditionObjectCase,
      creditProducts: Seq[ConsumerCreditProduct],
      expected: Precondition)

  object TestCase {

    def apply(
        description: String,
        price: Long,
        objectId: String,
        geobaseIds: Seq[Long] = Seq.empty,
        requestedLoanTerm: Option[Int] = None,
        requestedInitialFee: Option[Long] = None,
        creditProduct: ConsumerCreditProduct,
        expected: Precondition): TestCase = TestCase(
      description,
      PreconditionObjectCase(objectId, geobaseIds, price, requestedLoanTerm, requestedInitialFee),
      Seq(creditProduct),
      expected
    )
  }

  case class PreconditionObjectCase(
      objectId: String,
      geobaseIds: Seq[Long] = Seq.empty,
      price: Long,
      requestedLoanTerm: Option[Int] = None,
      requestedInitialFee: Option[Long] = None,
      objectPayload: Option[CommonApplicationObjectPayload] = None)

  sealed trait PreconditionObjectPayload

  object PreconditionObjectPayload {
    case class Auto(sellerType: Option[proto.AutoSellerType]) extends PreconditionObjectPayload
  }

  private val getPreconditionCases: Seq[TestCase] = Seq(
    TestCase(
      description = "Offer with a price less than the maximum possible for the credit",
      price = 1000000L,
      objectId = "123",
      creditProduct = sampleCreditProduct,
      expected = Precondition(
        objectId = "123",
        productPreconditions = Seq(
          ProductPrecondition(
            productId = sampleCreditProduct.id,
            initialFee = 100000L.taggedWith,
            interestRate = 5f.taggedWith,
            termsMonths = 36.taggedWith,
            monthlyPayment = 27000L.taggedWith // 26973
          )
        )
      )
    ),
    TestCase(
      description = "Offer with a price higher than the maximum possible for the credit",
      price = 3000000L,
      objectId = "123",
      creditProduct = sampleCreditProduct,
      expected = Precondition(
        objectId = "123",
        productPreconditions = Seq(
          ProductPrecondition(
            productId = sampleCreditProduct.id,
            initialFee = 1000000L.taggedWith,
            interestRate = 5f.taggedWith,
            termsMonths = 36.taggedWith,
            monthlyPayment = 59950L.taggedWith // 59941
          )
        )
      )
    ),
    TestCase(
      description = "Zero interest rate",
      price = 1000000L,
      objectId = "123",
      creditProduct = sampleCreditProduct
        .copy(interestRateRange = sampleCreditProduct.interestRateRange.copy(from = 0f.taggedWith.some)),
      expected = Precondition(
        objectId = "123",
        productPreconditions = Seq(
          ProductPrecondition(
            productId = sampleCreditProduct.id,
            initialFee = 100000L.taggedWith,
            interestRate = 0f.taggedWith,
            termsMonths = 36.taggedWith,
            monthlyPayment = 25000L.taggedWith
          )
        )
      )
    ),
    TestCase(
      description = "Defaults on zero terms months",
      price = 1000000L,
      objectId = "123",
      creditProduct = sampleCreditProduct.copy(termMonthsRange =
        sampleCreditProduct.termMonthsRange.copy(from = 0.taggedWith.some, to = 0.taggedWith.some)
      ),
      expected = Precondition(
        objectId = "123",
        productPreconditions = Seq(
          ProductPrecondition(
            productId = sampleCreditProduct.id,
            initialFee = 100000L.taggedWith,
            interestRate = 5f.taggedWith,
            termsMonths = 36.taggedWith,
            monthlyPayment = 27000L.taggedWith // 26973
          )
        )
      )
    ),
    TestCase(
      description = "Defaults on zero terms months",
      price = 0L,
      objectId = "123",
      creditProduct = sampleCreditProduct,
      expected = Precondition(
        objectId = "123",
        productPreconditions = Seq(
          ProductPrecondition(
            productId = sampleCreditProduct.id,
            initialFee = 0L.taggedWith,
            interestRate = 5f.taggedWith,
            termsMonths = 36.taggedWith,
            monthlyPayment = 0L.taggedWith
          )
        )
      )
    ),
    TestCase(
      description = "With requested initial fee",
      price = 1000000L,
      objectId = "123",
      creditProduct = sampleCreditProduct,
      requestedLoanTerm = 60.some,
      requestedInitialFee = 500000L.some,
      expected = Precondition(
        objectId = "123",
        productPreconditions = Seq(
          ProductPrecondition(
            productId = sampleCreditProduct.id,
            initialFee = 500000L.taggedWith,
            interestRate = 5f.taggedWith,
            termsMonths = 36.taggedWith,
            monthlyPayment = 15000L.taggedWith // 14985
          )
        )
      )
    )
  )

  private val getPreconditionsTests = getPreconditionCases.map { case TestCase(description, info, cp, expected) =>
    val preconditionsResponse = for {
      calculator <- ZIO.service[PreconditionsCalculator.Service]
      infos = ObjectInfo(
        info.objectId.taggedWith[zio_baker.Tag.OfferId],
        info.geobaseIds.map(_.taggedWith),
        info.price.taggedWith,
        info.requestedLoanTerm.map(_.taggedWith),
        info.requestedInitialFee.map(_.taggedWith),
        info.objectPayload.map {
          case CommonApplicationObjectPayload.Auto(offerSellerType, category, _, section, userRef) =>
            PreconditionsRequest.ObjectPayload.Auto(offerSellerType, category, section, userRef)
        }
      )
      response <- calculator.calculate(infos, cp)
    } yield response

    testM(description)(assertM(preconditionsResponse)(equalTo(Some(expected))))
      .provideLayer(preconditionsCalculatorLayer)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PreconditionsManager")(
      suite("list")(getPreconditionsTests: _*)
    )
}
