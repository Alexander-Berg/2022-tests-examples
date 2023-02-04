package ru.yandex.vertis.shark.client.bank.converter.impl

import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.{Category, Offer, Section}
import ru.auto.application.palma.proto.application_palma_model.ExternalSystem
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.DealerApplicationConverter
import ru.yandex.vertis.shark.client.bank.data.dealer.Entities.{
  DealerCreditApplication,
  DealerCreditConfiguration,
  DealerExternalIntegration
}
import ru.yandex.vertis.shark.dictionary.DealerConfigurationDictionary
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{SenderConverterContext, Tag, UserRef}
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.test_utils.assertions.Assertions
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import zio.ZIO
import zio.test.Assertion.anything
import zio.test.environment.TestEnvironment
import zio.test.mock.{mockable, Expectation}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.Instant

object DealerApplicationConverterSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with Assertions.DiffSupport {

  @mockable[DealerConfigurationDictionary.Service]
  object DealerConfigurationDictionaryMock

  private val mockResult =
    DealerCreditConfiguration(
      id = "123123:category:section",
      creditTermValues = Seq(2),
      creditDefaultTerm = 2,
      creditAmountSliderStep = 2,
      creditMinAmount = 100000,
      creditMaxAmount = 5000000,
      creditMinRate = 0.025,
      creditStep = 100000,
      creditOfferInitialPaymentRate = 5.0,
      dealerId = UserRef.AutoruDealer(123123),
      category = Category.CARS,
      section = Section.NEW,
      externalIntegrations =
        Seq(DealerExternalIntegration(ExternalSystem.ECREDIT, "asd", enabled = true, tags = Seq.empty))
    )

  private val dealerConfigurationMockLayer =
    DealerConfigurationDictionaryMock
      .Find(
        anything,
        Expectation.value(mockResult.some)
      )
      .optional

  private lazy val converterLayer = dealerConfigurationMockLayer >>> DealerApplicationConverter.live

  private def expected(offer: Offer, ts: Instant) = DealerCreditApplication(
    id = "claimId-test-1111".taggedWith[Tag.CreditApplicationClaimId],
    amount = 490000L.taggedWith[Tag.MoneyRub],
    period = 2.taggedWith[Tag.YearAmount],
    initPayment = 500000L.taggedWith[Tag.MoneyRub],
    monthPayment = 38250L.taggedWith[Tag.MoneyRub],
    name = "Иванов Василий".taggedWith[Tag.Name],
    email = "vasyivanov@yandex.ru",
    phone = "79267010001",
    dealerName = offer.seller.map(_.name),
    userId = 17830914,
    dealerId = UserRef.AutoruDealer(123123),
    mark = offer.getCarInfo.mark.get.taggedWith[Tag.VehicleMark],
    model = offer.getCarInfo.model.get.taggedWith[Tag.VehicleModel],
    mileage = offer.mileageHistory.headOption.map(_.mileage).get,
    year = 2018,
    createDate = ts,
    offerId = offer.id.taggedWith[zio_baker.Tag.OfferId],
    offerLink = offer.url,
    category = offer.category,
    section = offer.section
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("PsbConverter")(
      testM("convert") {
        val timestamp = Instant.now()
        val creditApplication = sampleCreditApplication
        val vosOffer = sampleOffer()
        val organization = sampleDadataOrganization.suggestions.headOption
        val claimId = "claimId-test-1111".taggedWith[Tag.CreditApplicationClaimId]
        val gender: GenderType = GenderType.MALE
        val res = for {
          converter <- ZIO.service[DealerApplicationConverter.Service]
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            vosOffer = vosOffer.some,
            organization = organization,
            gender = gender
          )
          context = SenderConverterContext.forTest(converterContext)
          source = DealerApplicationConverter.Source(context, claimId)
          res <- converter.convert(source)
        } yield res
        assertM(res)(noDiff(expected(vosOffer, timestamp))).provideLayer(converterLayer)
      }
    )
  }

}
