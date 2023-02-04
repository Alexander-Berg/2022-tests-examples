package ru.yandex.vertis.shark.enricher

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.{Category, Section}
import ru.yandex.vertis.shark.Mock.{resourceRegionsDictionaryLayer, DealerConfigurationDictionaryMock}
import ru.yandex.vertis.shark.client.bank.data.dealer.Entities.DealerCreditConfiguration
import ru.auto.api.common_model.ExternalIntegration
import ru.auto.application.palma.proto.application_palma_model.ExternalSystem
import ru.yandex.vertis.shark.client.bank.data.dealer.Entities.DealerExternalIntegration
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.Mock.VosAutoruClientMock
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.UserRef.AutoruDealer
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.proto.common
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResourceSpecBase._
import zio.test.Assertion.{anything, equalTo, hasField, isSubtype}
import zio.test.environment.TestEnvironment
import zio.test.{assert, Assertion, DefaultRunnableSpec, ZSpec}
import zio.test.mock.Expectation.{failure, value}

object DefaultCreditProductEnricherSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("DefaultCreditProductEnricher")(
      amountRangeSuite,
      dealerSuite
    )

  private lazy val amountRangeSuite = {

    def testAmountRange(
        description: String,
        creditProduct: CreditProduct.Full,
        geobaseIds: Seq[zio_baker.GeobaseId],
        expected: CreditProduct.AmountRange) =
      testM(description) {
        val creditProductEnricherLayer = resourceRegionsDictionaryLayer ++ DealerConfigurationDictionaryMock.empty ++
          VosAutoruClientMock.empty >>> CreditProductEnricher.live

        CreditProductEnricher
          .enrich(creditProduct, geobaseIds, None)
          .map { result =>
            assert(result)(isSubtype[CreditProduct.Full](hasField("amountRange", _.amountRange, equalTo(expected))))
          }
          .provideLayer(creditProductEnricherLayer)
      }

    val creditProduct = sampleCreditProduct()
    val creditProposal = sampleCreditProposal()

    val creditProductAmountRange = amountRange(1_000_000L, 2_000_000)
    val creditProposalAmountRange = amountRange(500_000L, 900_000L)
    val creditProposalAmountRangeOther1 = amountRange(100_000L, 200_000L)
    val creditProposalAmountRangeOther2 = amountRange(300_000L, 400_000L)

    suite("replaces amount range")(
      testAmountRange(
        "no CreditProposals",
        creditProduct.copy(amountRange = creditProductAmountRange, creditProposalEntities = Seq.empty),
        Seq(MoscowRegionId),
        expected = creditProductAmountRange
      ),
      testAmountRange(
        "no matching CreditProposals",
        creditProduct.copy(
          amountRange = creditProductAmountRange,
          creditProposalEntities = Seq(
            creditProposal.copy(geobaseIds = Seq.empty),
            creditProposal.copy(amountRange = creditProposalAmountRange, geobaseIds = Seq(SpbRegionId))
          )
        ),
        Seq(MoscowRegionId),
        expected = creditProductAmountRange
      ),
      testAmountRange(
        "matching CreditProposal",
        creditProduct.copy(
          amountRange = creditProductAmountRange,
          creditProposalEntities = Seq(
            creditProposal.copy(geobaseIds = Seq(MoscowRegionId), amountRange = creditProposalAmountRangeOther1),
            creditProposal.copy(geobaseIds = Seq(KarachayCherkessRegionId), amountRange = creditProposalAmountRange),
            creditProposal.copy(geobaseIds = Seq(SpbRegionId), amountRange = creditProposalAmountRangeOther2)
          )
        ),
        Seq(CherkesskRegionId, YaltaRegionId),
        expected = creditProposalAmountRange
      ),
      testAmountRange(
        "matching CreditProposal (default region)",
        creditProduct.copy(
          amountRange = creditProductAmountRange,
          creditProposalEntities = Seq(
            creditProposal.copy(geobaseIds = Seq(SpbRegionId), amountRange = creditProposalAmountRangeOther1),
            creditProposal.copy(geobaseIds = Seq(MoscowRegionId), amountRange = creditProposalAmountRange),
            creditProposal
              .copy(geobaseIds = Seq(KarachayCherkessRegionId), amountRange = creditProposalAmountRangeOther2)
          )
        ),
        Seq.empty,
        expected = creditProposalAmountRange
      )
    )
  }

  private lazy val dealerSuite = {

    val dealerId = AutoruDealer(123123)
    val creditMinAmount = 100000
    val creditMaxAmount = 5000000
    val creditMinRate = 5f
    val creditOfferInitialPaymentRate = 1f
    val category = Category.CARS
    val offerId = "1114692519-303c05ac".taggedWith[zio_baker.Tag.OfferId]
    val section = Section.NEW
    val ecreditPrecondition: common.EcreditPrecondition =
      common.EcreditPrecondition(
        ecreditProductId = "ecredit-product-id",
        interestRate = creditMinRate,
        minInitialFeeRate = creditOfferInitialPaymentRate,
        amountRange = common
          .AmountRange(
            from = creditMinAmount.toLong,
            to = creditMaxAmount.toLong
          )
          .some,
        termMonthsRange = common
          .TermMonthsRange(
            from = 6,
            to = 36
          )
          .some,
        monthlyPayment = 36_000L
      )
    val offer = Offer(ecreditPrecondition = ecreditPrecondition.some)

    val externalIntegration = DealerExternalIntegration(
      source = ExternalSystem.ECREDIT,
      externalId = "",
      enabled = true,
      tags = Seq(ExternalIntegration.Tag.SEND_TO_ECREDIT_API)
    )

    val configuration =
      sampleDealerCreditConfiguration().copy(
        creditTermValues = Seq(1, 3),
        creditMinAmount = creditMinAmount,
        creditMaxAmount = creditMaxAmount,
        creditMinRate = creditMinRate / 100d,
        creditOfferInitialPaymentRate = creditOfferInitialPaymentRate / 100d,
        dealerId = dealerId,
        category = category,
        section = section,
        externalIntegrations = Seq(externalIntegration)
      )

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    val commonObjectPayload = sampleOffer()
      .copy(
        category = category,
        id = offerId,
        section = section,
        userRef = dealerId.some
      )
      .commonObjectPayload
      .asInstanceOf[CommonApplicationObjectPayload.Auto]

    suite("enrich dealer credit product")(
      testM("upgraded stub to full credit product by ecredit precondition") {
        val vosAutoruClientMock = VosAutoruClientMock.Offer(equalTo((category, offerId, true)), value(offer))

        val dealerConfigurationDictionaryMock =
          DealerConfigurationDictionaryMock.Find(equalTo((dealerId, category, section)), value(None))

        val creditProductEnricherLayer = resourceRegionsDictionaryLayer ++ dealerConfigurationDictionaryMock ++
          vosAutoruClientMock >>> CreditProductEnricher.live

        val expected = sampleDealerCreditProduct().copy(
          amountRange = amountRange(creditMinAmount.toLong, creditMaxAmount.toLong),
          interestRateRange = interestRateRange(creditMinRate),
          termMonthsRange = termMonthsRange(6, 36),
          minInitialFeeRate = minInitialFeeRate(creditOfferInitialPaymentRate)
        )

        CreditProductEnricher
          .enrich(sampleDealerCreditProductStub(), Seq.empty, commonObjectPayload.some)
          .map { result =>
            assert(result)(isSubtype[DealerCreditProduct](assertionDealerCreditProduct(expected)))
          }
          .provideLayer(creditProductEnricherLayer)
      },
      testM("upgraded stub to full credit product by dealer credit config") {
        val vosAutoruClientMock =
          VosAutoruClientMock.Offer(equalTo((category, offerId, true)), failure(new NoSuchElementException))

        val dealerConfigurationDictionaryMock =
          DealerConfigurationDictionaryMock.Find(equalTo((dealerId, category, section)), value(configuration.some))

        val creditProductEnricherLayer = resourceRegionsDictionaryLayer ++ dealerConfigurationDictionaryMock ++
          vosAutoruClientMock >>> CreditProductEnricher.live

        val expected = sampleDealerCreditProduct().copy(
          amountRange = amountRange(creditMinAmount.toLong, creditMaxAmount.toLong),
          interestRateRange = interestRateRange(creditMinRate),
          termMonthsRange = termMonthsRange(12, 36),
          minInitialFeeRate = minInitialFeeRate(creditOfferInitialPaymentRate)
        )

        CreditProductEnricher
          .enrich(sampleDealerCreditProductStub(), Seq.empty, commonObjectPayload.some)
          .map { result =>
            assert(result)(isSubtype[DealerCreditProduct](assertionDealerCreditProduct(expected)))
          }
          .provideLayer(creditProductEnricherLayer)
      },
      testM("undefined dealer credit config & ecredit precondition for offer") {
        val vosAutoruClientMock =
          VosAutoruClientMock.Offer(equalTo((category, offerId, true)), value(Offer()))

        val dealerConfigurationDictionaryMock =
          DealerConfigurationDictionaryMock.Find(equalTo((dealerId, category, section)), value(None))

        val creditProductEnricherLayer = resourceRegionsDictionaryLayer ++ dealerConfigurationDictionaryMock ++
          vosAutoruClientMock >>> CreditProductEnricher.live

        CreditProductEnricher
          .enrich(sampleDealerCreditProductStub(), Seq.empty, commonObjectPayload.some)
          .map { result =>
            assert(result)(isSubtype[DealerCreditProductStub](anything))
          }
          .provideLayer(creditProductEnricherLayer)
      },
      testM("not suitable category") {
        val vosAutoruClientMock =
          VosAutoruClientMock.Offer(equalTo((Category.MOTO, offerId, true)), failure(new NoSuchElementException))

        val dealerConfigurationDictionaryMock =
          DealerConfigurationDictionaryMock.Find(equalTo((dealerId, Category.MOTO, section)), value(None))

        val creditProductEnricherLayer = resourceRegionsDictionaryLayer ++ dealerConfigurationDictionaryMock ++
          vosAutoruClientMock >>> CreditProductEnricher.live

        CreditProductEnricher
          .enrich(
            sampleDealerCreditProductStub(),
            Seq.empty,
            commonObjectPayload.copy(category = Category.MOTO.some).some
          )
          .map { result =>
            assert(result)(isSubtype[DealerCreditProductStub](anything))
          }
          .provideLayer(creditProductEnricherLayer)
      },
      testM("not suitable section") {
        val vosAutoruClientMock =
          VosAutoruClientMock.Offer(equalTo((category, offerId, true)), failure(new NoSuchElementException))

        val dealerConfigurationDictionaryMock =
          DealerConfigurationDictionaryMock.Find(equalTo((dealerId, category, Section.USED)), value(None))

        val creditProductEnricherLayer = resourceRegionsDictionaryLayer ++ dealerConfigurationDictionaryMock ++
          vosAutoruClientMock >>> CreditProductEnricher.live

        CreditProductEnricher
          .enrich(
            sampleDealerCreditProductStub(),
            Seq.empty,
            commonObjectPayload.copy(section = Section.USED.some).some
          )
          .map { result =>
            assert(result)(isSubtype[DealerCreditProductStub](anything))
          }
          .provideLayer(creditProductEnricherLayer)
      }
    )
  }

  private def assertionDealerCreditProduct(expected: DealerCreditProduct): Assertion[DealerCreditProduct] = {
    def checkFiled[B](name: String, proj: DealerCreditProduct => B, assertion: Assertion[B]) =
      hasField[DealerCreditProduct, B](name, proj, assertion)

    checkFiled("amountRange", _.amountRange, equalTo(expected.amountRange)) &&
    checkFiled("interestRateRange", _.interestRateRange, equalTo(expected.interestRateRange)) &&
    checkFiled("termMonthsRange", _.termMonthsRange, equalTo(expected.termMonthsRange)) &&
    checkFiled("minInitialFeeRate", _.minInitialFeeRate, equalTo(expected.minInitialFeeRate))
  }

  private def amountRange(form: Long, to: Long): CreditProduct.AmountRange =
    CreditProduct.AmountRange(form.taggedWith[Tag.MoneyRub].some, to.taggedWith[Tag.MoneyRub].some)

  private def interestRateRange(from: Float): CreditProduct.InterestRateRange =
    CreditProduct.InterestRateRange(from.taggedWith[Tag.Rate].some, None)

  private def termMonthsRange(from: Int, to: Int): CreditProduct.TermMonthsRange =
    CreditProduct.TermMonthsRange(from.taggedWith[Tag.MonthAmount].some, to.taggedWith[Tag.MonthAmount].some)

  private def minInitialFeeRate(value: Float): Rate = value.taggedWith[Tag.Rate]

  private def sampleCreditProduct(): ConsumerCreditProduct =
    generate[ConsumerCreditProduct].sample.get
      .copy(rateLimit = None)

  private def sampleCreditProposal(): CreditProduct.CreditProposal =
    generate[CreditProduct.CreditProposal].sample.get

  private def sampleDealerCreditProduct(): DealerCreditProduct =
    generate[DealerCreditProduct].sample.get

  private def sampleDealerCreditProductStub(): DealerCreditProductStub =
    generate[DealerCreditProductStub].sample.get

  private def sampleDealerCreditConfiguration(): DealerCreditConfiguration =
    generate[DealerCreditConfiguration].sample.get

  private def sampleOffer(): AutoruCreditApplication.Offer =
    generate[AutoruCreditApplication.Offer].sample.get
}
