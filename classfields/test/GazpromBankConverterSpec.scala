package ru.yandex.vertis.shark.client.bank.converter.impl

import baker.common.client.dadata.model.DadataOrganization
import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.Mock.GeocoderClientMock
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.GazpromBankConverter
import ru.yandex.vertis.shark.client.bank.converter.GazpromBankConverter.Source
import ru.yandex.vertis.shark.client.bank.converter.impl.gazprom.ConvertJson
import ru.yandex.vertis.shark.client.bank.converter.impl.gazprom.DefaultValues._
import ru.yandex.vertis.shark.client.bank.data.gazprom.Entities._
import ru.yandex.vertis.shark.client.bank.data.gazprom.Value
import ru.yandex.vertis.shark.client.bank.data.gazprom.Value._
import ru.yandex.vertis.shark.client.bank.dictionary.gazprom.GazpromBankDictionary.Identifier._
import ru.yandex.vertis.shark.client.bank.dictionary.gazprom.{GazpromBankDictionary, StaticGazpromBankResource}
import ru.yandex.vertis.shark.dictionary.RegionsDictionary
import ru.yandex.vertis.shark.dictionary.impl.RegionsDictionarySpecBase
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{Tag, _}
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.shark.util.GeobaseUtils
import ru.yandex.vertis.test_utils.assertions.Assertions
import ru.yandex.vertis.zio_baker.util.DateTimeUtil._
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio._
import zio.clock.Clock
import zio.test.Assertion.{anything, equalTo}
import zio.test.diff.Diff.DiffOps
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.{Instant, LocalDate, OffsetDateTime}

object GazpromBankConverterSpec
  extends DefaultRunnableSpec
  with RegionsDictionarySpecBase
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with ConvertJson
  with Assertions.DiffSupport {

  private lazy val geocoderLayer = GeocoderClientMock.Geocode(anything, Expectation.value(Seq.empty)).optional

  private lazy val converterLayer = Clock.any ++ geocoderLayer ++ regionsDictionaryLayer ++
    ZLayer.succeed[Resource[Any, GazpromBankDictionary.Service]](new StaticGazpromBankResource) >+>
    GazpromBankConverter.live

  private val timestamp: Instant = Instant.now
  private val clientDateTime: OffsetDateTime = timestamp.toOffsetDateTime()

  private val expected: GazpromBankClaim =
    GazpromBankClaim(
      clientContext = ClientContext(
        version = ClientContextVersion,
        clientDateTime = clientDateTime,
        params = Seq(ClientContextPartnerParams)
      ),
      operation = Operation(
        values = Seq(
          FeatureApiVersion(ApiVersion),
          ClientDesiredAmount(490000L.taggedWith[Tag.MoneyRub]),
          CreditParametersCurrencyCode(CurrencyCode),
          CreditParametersCreditTerm(13.taggedWith[Tag.MonthAmount]),
          CreditParametersInsurance(Insurance),
          ControlInfo("лужа"),
          CarCreditNew(true),
          CarCreditPawn(Pawn),
          Participants0Surname("Иванов"),
          Participants0Name("Василий"),
          Participants0SexCode(Gender.Male),
          CompleteNameManual(NameManual),
          Participants0BirthDate(LocalDate.of(1982, 11, 25)),
          Participants0BirthCountry("643".taggedWith[BirthCountry.type]),
          Participants0BirthPlace("Россия Самарская обл г Тольятти"),
          Participants0ResidentStatus(ResidentStatus),
          Participants0IdentityDocuments0DocSeries("1234"),
          Participants0IdentityDocuments0DocNum("506789"),
          Participants0IdentityDocuments0IssueDate(LocalDate.of(2005, 2, 2)),
          Participants0IdentityDocuments0DepartCode("987654"),
          Participants0IdentityDocuments0IssuedBy("АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ."),
          BorrowerIsOldPersonalData(true),
          Participants0ClientNames0Surname("Петров"),
          Participants0ClientNames0Name("Василий"),
          Participants0Contacts0PhoneNumber("79267010001"),
          Participants0Contacts1PhoneNumber("79267010002"),
          Participants0Contacts2Email(Email("vasyivanov@yandex.ru")),
          Participants0Incomes0AvgMonthlyIncome(100000L.taggedWith[Tag.MoneyRub]),
          Participants0ClientConsentsConsentPersonalData(ConsentPersonalData),
          Participants0ClientConsentsConsentPersonalDataSharing(ConsentPersonalDataSharing),
          Participants0ClientConsentsConsentCHCheck(ConsentCHCheck),
          Participants0ClientConsentAdvSubscription(ConsentAdvSubscription),
          Participants0PfrStatement(PfrStatement),
          Participants0EducationCode(Education.Higher),
          Participants0MaritalStatusIdStatusCode(MaritalStatus.Divorced),
          Location("0c5b2444-70a0-4932-980c-b4dc0d3f02b5".taggedWith[zio_baker.Tag.FiasId]),
          Participants0Addresses0PostalCode(143362.taggedWith[Tag.RusPostCode]),
          Participants0Addresses0RegionCode("6300000000000".taggedWith[Region.type]),
          Participants0Addresses0District("г Тольятти"),
          Participants0Addresses0City("г Тольятти"),
          Participants0Addresses0Street("Обводное шоссе"),
          Participants0Addresses0House("3"),
          AddressManual(DefaultAddressManual),
          Participants0RegLive(false),
          Participants0Addresses1PostalCode(119526.taggedWith[Tag.RusPostCode]),
          Participants0Addresses1RegionCode("7700000000000".taggedWith[Region.type]),
          Participants0Addresses1District("г Москва"),
          Participants0Addresses1City("г Москва"),
          Participants0Addresses1Street("пр-кт Вернадского"),
          Participants0Addresses1House("99"),
          Participants0Addresses1Housing("1"),
          AddressManual2(DefaultAddressManual2),
          Participants0Employment0EmploymentTypeCode("Работа по найму".taggedWith[EmploymentType.type]),
          Participants0Employment0EmployersFullName("ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\""),
          Participants0Employment0EmployersInn("7704366364"),
          Participants0Employment0PositionName("IT специалист"),
          Participants0Employment0EmployersContacts0PhoneNumber("74950011111"),
          Participants0Employment0LastExperienceYears(6),
          Participants0Employment0LastExperienceMonths(0),
          Participants0ClientConsentsPhone("79267010001"),
          Participants0ClientConsentsCheckTime(clientDateTime),
          Participants0ClientConsentsConditionalDataSharingWithPartner(ClientConsentsConditionalDataSharingWithPartner)
        ).sort
      )
    )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("GazpromBankConverter")(
      testM("convert") {
        val creditApplication: CreditApplication = sampleCreditApplication
        val vosOffer: Option[Offer] = sampleOffer().some
        val organization: Option[DadataOrganization] = sampleDadataOrganization.suggestions.headOption
        val gender: GenderType = GenderType.MALE
        val res = for {
          regionsDictionary <- ZIO.service[RegionsDictionary.Service]
          geobaseIds = GeobaseUtils.identityOrDefault(
            creditApplication.requirements.map(_.geobaseIds).orEmpty
          )
          parentRegions <- regionsDictionary.getParentRegions(geobaseIds)
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            vosOffer = vosOffer,
            parentRegions = parentRegions,
            organization = organization,
            gender = gender,
            registrationAddress =
              creditApplication.borrowerPersonProfile.flatMap(_.registrationAddress.map(_.addressEntity)),
            residenceAddress = creditApplication.borrowerPersonProfile.flatMap(_.residenceAddress.map(_.addressEntity))
          )
          source = Source(SenderConverterContext.forTest(converterContext))
          converter <- ZIO.service[GazpromBankConverter.Service]
          res <- converter.convert(source)
          resSignificant = res.copy(
            operation = res.operation.copy(
              values = res.operation.values
                .collect {
                  case Participants0ClientConsentsSendSmsId(_) | Participants0ClientConsentsCodeOtp(_) => None
                  case value => value.some
                }
                .flatten
                .sort
            )
          )
        } yield {
          for (v <- resSignificant.operation.values) {
            val diff = v.diffed(expected.operation.values.find(_.id == v.id).get)
            if (diff.hasDiff) println(s"diff ${v.id}: ${diff.render}")
          }
          resSignificant
        }
        assertM(res)(equalTo(expected)).provideLayer(converterLayer)
      }
    )

  implicit class RichValueSeq(val value: Seq[Value]) extends AnyVal {

    def sort: Seq[Value] = value.sortWith((l, r) => l.id < r.id)
  }
}
