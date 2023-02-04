package ru.yandex.vertis.shark.client.bank.converter.impl

import baker.common.client.dadata.model.DadataOrganization
import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.{Offer, Section}
import ru.auto.api.catalog_model.{Mark, Model}
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.SberBankUrlConverter
import ru.yandex.vertis.shark.client.bank.converter.SberBankUrlConverter.SberBankUrl
import ru.yandex.vertis.shark.client.bank.dictionary.sber.{SberBankDictionary, StaticSberBankResource}
import ru.yandex.vertis.shark.config.SberBankConfig
import ru.yandex.vertis.shark.dictionary.impl.RegionsDictionarySpecBase
import ru.yandex.vertis.shark.model.{CreditApplication, SenderConverterContext, Tag}
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment

import java.net.URL
import java.time._

object SberBankUrlConverterSpec
  extends DefaultRunnableSpec
  with RegionsDictionarySpecBase
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples {

  private val sberBankConfig = SberBankConfig(new URL("https://sberbank.ru"), source = "autoru")

  private lazy val converterLayer =
    ZLayer.requires[Blocking] ++
      ZLayer.requires[Clock] ++
      ZLayer.succeed(sberBankConfig) ++
      ZLayer.succeed[Resource[Any, SberBankDictionary.Service]](new StaticSberBankResource) >>>
      SberBankUrlConverter.live

  private lazy val expected = SberBankUrl(
    bankClaimId = EmptyBankClaimId,
    url =
      s"https://sberbank.ru/sms/carloanrequest/?model=A3&isNewCar=true&brand=AUDI&downPayment=500000&durationMonth=24&source=autorujjj$EmptyBankClaimId&carPrice=490000"
  )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("SberBankConverter")(
      testM("convert") {
        val timestamp = Instant.now()
        val creditApplication: CreditApplication = sampleCreditApplication
        val vosOffer: Option[Offer] = {
          val sample = sampleOffer()
          sample
            .withSection(Section.NEW)
            .withCarInfo(
              sample.getCarInfo
                .withMarkInfo(Mark.defaultInstance.withName("AUDI"))
                .withModelInfo(Model.defaultInstance.withName("A3"))
            )
            .some
        }
        val organization: Option[DadataOrganization] = sampleDadataOrganization.suggestions.headOption
        val gender: GenderType = GenderType.MALE
        val res = for {
          converter <- ZIO.service[SberBankUrlConverter.Service]
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            vosOffer = vosOffer,
            organization = organization,
            gender = gender
          )
          context = SenderConverterContext.forTest(converterContext)
          source = SberBankUrlConverter.Source(context)
          res <- converter.convert(source)
          resWithoutBankClaimId = res.copy(
            bankClaimId = EmptyBankClaimId,
            url = res.url.replaceFirst(BankClaimIdRegex, EmptyBankClaimId)
          )
        } yield resWithoutBankClaimId
        assertM(res)(equalTo(expected)).provideLayer(converterLayer)
      }
    )

  private val EmptyBankClaimId = "".taggedWith[Tag.CreditApplicationBankClaimId]
  private val BankClaimIdRegex = "(?<=jjj)\\w{27,27}(?=&)"
}
