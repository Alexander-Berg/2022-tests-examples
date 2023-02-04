package ru.yandex.vertis.shark.client.bank.converter.impl

import AlfaBankCreditCardConverterSpec.{suite, testM}
import cats.syntax.option._
import com.softwaremill.tagging.Tagger
import com.softwaremill.quicklens._
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.AlfaBankConverter
import ru.yandex.vertis.shark.client.bank.data.alfa.Entities
import ru.yandex.vertis.shark.client.bank.data.alfa.Entities.AlfaBankLeadId
import ru.yandex.vertis.shark.client.bank.dictionary.alfa.{AlfaBankDictionary, StaticAlfaBankResource}
import ru.yandex.vertis.shark.dictionary.RegionsDictionary
import ru.yandex.vertis.shark.dictionary.impl.RegionsDictionarySpecBase
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{AutoruCreditApplication, SenderConverterContext}
import ru.yandex.vertis.shark.util.GeobaseUtils
import ru.yandex.vertis.shark.util.RichModel.RichCreditApplication
import ru.yandex.vertis.test_utils.assertions.Assertions
import ru.yandex.vertis.zio_baker.model.GeobaseId
import ru.yandex.vertis.zio_baker.model.Tag
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichInstant
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, ZSpec}

import java.time.Instant

object AlfaBankCreditCardConverterSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with RegionsDictionarySpecBase
  with Assertions.DiffSupport {

  private val MoscowRegion: GeobaseId = 213L.taggedWith[Tag.GeobaseId]

  private lazy val creditCardConverterLayer =
    ZLayer.succeed[Resource[Any, AlfaBankDictionary.Service]](new StaticAlfaBankResource) ++
      regionsDictionaryLayer >>>
      AlfaBankConverter.liveCreditCard

  private lazy val converterLayer =
    ZLayer.requires[Blocking] ++ ZLayer.requires[Clock] >>>
      creditCardConverterLayer ++ regionsDictionaryLayer

  private val expected = Entities.Lead(
    firstName = "Василий",
    lastName = "Иванов",
    middleName = "-",
    sex = Entities.Sex.Male,
    mobilePhone = "9267010001",
    email = "vasyivanov@yandex.ru",
    product = Entities.Product.VISA_CLASSIC,
    productType = Entities.ProductType.CC,
    platformId = "partners_offline_autoru_credit_card_",
    passportSeries = "12 34",
    passportNumber = "506789",
    passportIssueDate = "2005-02-02",
    passportOffice = "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ.",
    passportOfficeCode = "987-654",
    passportBirthPlace = "Самарская обл г Тольятти",
    birthDate = "1982-11-25",
    workRegionCode = "77",
    workInn = "7704366364",
    workPost = "IT специалист",
    workCompanyName = "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"",
    workPhone = "4950011111",
    workStatus = "true",
    partnerLeadId = "test-claim-alfa-2",
    createdTimePartner = "2021-05-25 10:45:01",
    consentPartnerDate = "2021-12-17 04:36:02",
    consentPartnerCode = "<ignored>",
    consentPartnerId = "<ignored>",
    consentVersion = "1.1.0",
    income = "100000",
    education = Entities.Education.IncompleteHigher,
    contactFirstName = "Незнайка",
    contactMobilePhone = "9267010002",
    secondaryDocument = Entities.SecondaryDocument.DRL.some,
    salaryDocument = Entities.SalaryDocument.NFL,
    fullFlag = Entities.Flag.Yes,
    registrationRegionCode = "63",
    confirmationType = Entities.ConfirmationType.NDFL2,
    cardCategoryLimits = "100000".some,
    cardEmbossingLastName = "VASILII IVANOV".some,
    cardShippingType = "C".some,
    obtainingCity = "7700000000000".some,
    lateCall = "false".some
  )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("AlfaBankCreditCardConverter")(
      testM("convert") {
        val autoruCreditApplication: AutoruCreditApplication = sampleCreditApplication
          .modify(_.requirements.each.geobaseIds)
          .setTo(Seq(MoscowRegion))
        val optOffer = sampleOffer().some
        val claim = autoruCreditApplication.getClaimByCreditProductId("alfabank-2".taggedWith).get
        val regAddress =
          autoruCreditApplication.borrowerPersonProfile.flatMap(_.registrationAddress).map(_.addressEntity)
        val resAddress = autoruCreditApplication.borrowerPersonProfile.flatMap(_.residenceAddress).map(_.addressEntity)
        val alfaBankLeadId = AlfaBankLeadId(claim.id.take(18), claim.created.toLocalDateTime())
        val timestamp = Instant.parse("2021-12-17T04:36:02Z")
        val res =
          for {
            regionsDictionary <- ZIO.service[RegionsDictionary.Service]
            converter <- ZIO.service[AlfaBankConverter.Service]
            geobaseIds = GeobaseUtils.identityOrDefault {
              autoruCreditApplication.requirements.map(_.geobaseIds).orEmpty
            }
            parentRegions <- regionsDictionary.getParentRegions(geobaseIds)
            org = sampleDadataOrganization.suggestions.headOption
            converterContext = AutoConverterContext.forTest(
              timestamp = timestamp,
              creditApplication = autoruCreditApplication,
              vosOffer = optOffer,
              parentRegions = parentRegions,
              organization = org,
              registrationAddress = regAddress,
              residenceAddress = resAddress
            )
            context = SenderConverterContext.forTest(converterContext)
            source = AlfaBankConverter.Source(context, alfaBankLeadId)
            resultClaim <- converter.convert(source).map(_.leads.head)
          } yield resultClaim.copy(consentPartnerCode = "<ignored>", consentPartnerId = "<ignored>")

        assertM(res)(noDiff(expected)).provideLayer(converterLayer)
      }
    )
}
