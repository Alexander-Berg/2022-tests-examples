package ru.yandex.vertis.shark.client.bank.converter.impl

import baker.common.client.dadata.model.DadataOrganization
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.EcreditNewAppConverter
import ru.yandex.vertis.shark.client.bank.data.ecredit.Entities.NewAppClaim
import ru.yandex.vertis.shark.client.bank.dictionary.ecredit.{EcreditMarksDictionary, StaticEcreditMarksResource}
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{CreditApplication, SenderConverterContext}
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.shark.util.RichModel.RichCreditApplication
import ru.yandex.vertis.test_utils.assertions.Assertions
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.environment.TestEnvironment
import zio.test.magnolia.diff.gen
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.{assertM, ZSpec}
import zio.{ZIO, ZLayer}

import java.time.Instant

object EcreditNewAppConverterSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with Assertions.DiffSupport {

  private val creditApplication: CreditApplication = sampleCreditApplication
  private val claim = creditApplication.getClaimByCreditProductId("dealer-1".taggedWith).get
  private val eCreditDealerId = "ecredit-dealer-id-111"

  private val expected = NewAppClaim(
    async = 1,
    clientSurname = "Иванов",
    clientName = "Василий",
    clientMiddleName = "отсутствует",
    clientEmail = "vasyivanov@yandex.ru",
    clientPhone = "9267010001",
    carCondition = 1,
    carBrand = "mercedes-benz",
    carModel = "g-class amg".some,
    carConfiguration = "Внедорожник 5 дв.".some,
    price = 10_000_000,
    carYear = 2018.some,
    vin = "XTA210740J0390362".some,
    initialFeeMoney = 500_000.some,
    clientApplicationId = claim.id,
    dealerId = eCreditDealerId,
    dealerIdType = 3,
    site = "auto.ru".some,
    agreeTerms = 1,
    integratorUid = EmptyString,
    carIssueDate = "2022-01-16 00:00:00".some,
    engineVolume = 5439d.some,
    enginePower = 507.some,
    ptsIssueDate = None,
    ptsNumber = None,
    ptsSeries = None
  )

  private lazy val ecreditMarksDictionaryLayer =
    ZLayer.succeed[Resource[Any, EcreditMarksDictionary.Service]](new StaticEcreditMarksResource) >>>
      EcreditMarksDictionary.live

  private lazy val converterLayer =
    ZLayer.requires[Blocking] ++ ZLayer.requires[Clock] ++ ecreditMarksDictionaryLayer >>> EcreditNewAppConverter.live

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("EcreditNewAppConverter")(
      testM("convert") {
        val timestamp = Instant.now()
        val creditApplication: CreditApplication = sampleCreditApplication
        val vosOffer: Option[Offer] = sampleOffer().some
        val organization: Option[DadataOrganization] = sampleDadataOrganization.suggestions.headOption
        val gender: GenderType = GenderType.MALE
        val res = for {
          converter <- ZIO.service[EcreditNewAppConverter.Service]
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            vosOffer = vosOffer,
            organization = organization,
            gender = gender
          )
          context = SenderConverterContext.forTest(converterContext)
          source = EcreditNewAppConverter.Source(context, claim.id, eCreditDealerId)
          res <- converter.convert(source)
        } yield res
        assertM(res)(noDiff(expected)).provideLayer(converterLayer)
      }
    )
}
