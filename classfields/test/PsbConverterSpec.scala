package ru.yandex.vertis.shark.client.bank.converter.impl

import baker.common.client.dadata.model.DadataOrganization
import cats.implicits._
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.PsbConverter
import ru.yandex.vertis.shark.client.bank.converter.impl.psb.DefaultValues._
import ru.yandex.vertis.shark.client.bank.data.psb.Entities._
import ru.yandex.vertis.shark.config.PsbClientConfig
import ru.yandex.vertis.shark.dictionary.impl.RegionsDictionarySpecBase
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{CreditApplication, SenderConverterContext}
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.test_utils.assertions.Assertions
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test._
import zio.test.environment.TestEnvironment

import java.time._

object PsbConverterSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with Assertions.DiffSupport {

  private val httpClientConfig = HttpClientConfig(url = EmptyString)
  private val psbClientConfig = PsbClientConfig(httpClientConfig, clientId = EmptyString, lgId = EmptyString)

  private lazy val converterLayer =
    ZLayer.requires[Blocking] ++ ZLayer.requires[Clock] ++ ZLayer.succeed(psbClientConfig) >>> PsbConverter.live

  private lazy val expected =
    PsbClaim(
      applicant = Applicant(
        firstName = ExpectedFirstName,
        lastName = ExpectedLastName,
        middleName = ExpectedMiddleName,
        sex = ExpectedSex,
        russianFederationCitizenship = DefaultRussianFederationCitizenship,
        birthdate = ExpectedBirthDate,
        birthplace = ExpectedBirthPlace,
        passport = Passport(
          serie = ExpectedPassportSeries,
          number = ExpectedPassportNumber,
          issuer = ExpectedPassportIssuer,
          issuedAt = ExpectedIssuedAt,
          issuerCode = ExpectedIssuerCode
        ),
        snils = ExpectedSnils,
        registrationAddress = Address(
          fiasId = ExpectedRegistrationAddressFiasId,
          full = ExpectedRegistrationAddressFull,
          coordinates = DefaultCoordinates
        ),
        registredAt = DefaultRegisteredAt,
        livingAddress = Address(
          fiasId = ExpectedLivingAddressFiasId,
          full = ExpectedLivingAddressFull,
          coordinates = DefaultCoordinates
        ),
        contacts = Contacts(
          workPhone = ExpectedWorkPhone,
          additionalWorkPhone = DefaultAdditionalWorkPhone,
          mobilePhone = ExpectedMobilePhone,
          email = ExpectedEmail,
          registrationAddressPhone = DefaultRegistrationAddressPhone,
          livingAddressPhone = DefaultLivingAddressPhone
        ),
        maritalStatus = ExpectedMaritalStatus,
        spouse = DefaultSpouse,
        employmentType = ExpectedEmploymentType,
        employee = Employee(
          status = ExpectedEmployeeStatus,
          position = ExpectedPosition,
          employedSince = ExpectedEmployedSince,
          salaryBank = DefaultSalaryBank,
          employmentYears = ExpectedEmploymentYear,
          employmentMonths = ExpectedEmploymentMonths,
          stateEmployee = DefaultStateEmployee,
          militaryEmployee = DefaultMilitaryEmployee,
          employer = Employer(
            name = ExpectedEmployerName,
            form = ExpectedEmployerForm,
            employeesQuantity = ExpectedEmployerEmployeesQuantity,
            legalAddress = Address(
              fiasId = ExpectedEmployerLegalAddressFiasId,
              full = ExpectedEmployerLegalAddressFull,
              coordinates = DefaultCoordinates
            ),
            actualAddress = Address(
              fiasId = ExpectedEmployerActualAddressFiasId,
              full = ExpectedEmployerActualAddressFull,
              coordinates = DefaultCoordinates
            ),
            `type` = ExpectedEmployerType,
            field = ExpectedEmployerField,
            kpp = ExpectedEmployerKpp,
            inn = ExpectedEmployerInn
          )
        ).some,
        additionalInfo = AdditionalInfo(
          education = ExpectedEducation,
          dependentsNumber = ExpectedDependentsNumber
        ),
        financialPosition = FinancialPosition(
          familyMonthlyIncome = ExpectedMonthlyIncome,
          monthlyIncomeType = ExpectedMonthlyIncomeType,
          realEstateTypes = DefaultRealEstateTypes,
          apartmentRent = DefaultApartmentRent,
          additionalIncome = DefaultAdditionalIncome,
          creditHistory = DefaultCreditHistory,
          unemploymentReason = ExpectedUnemploymentReason
        )
      ),
      contactPerson = ContactPerson(
        `type` = DefaultContactPersonType,
        fullName = ExpectedContactPersonFullName,
        mobilePhone = ExpectedContactPersonMobilePhone
      ),
      loanInformation = LoanInformation(
        summ = ExpectedLoanInformationSum,
        monthPeriod = ExpectedLoanInformationMonthPeriod,
        `type` = DefaultLoanInformationType,
        lodgementDate = ExpectedLoanInformationLodgementDate,
        region = DefaultLoanInformationRegion,
        city = ExpectedLoanInformationCity
      ),
      agreements = DefaultAgreements,
      lgId = psbClientConfig.lgId,
      productIdentifier = DefaultProductIdentifier,
      extraProperties = DefaultExtraProperties
    )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PsbConverter")(
      testM("convert") {
        val timestamp = Instant.now()
        val creditApplication: CreditApplication = sampleCreditApplication
        val vosOffer: Option[Offer] = sampleOffer().some
        val organization: Option[DadataOrganization] = sampleDadataOrganization.suggestions.headOption
        val gender: GenderType = GenderType.MALE
        val res = for {
          converter <- ZIO.service[PsbConverter.Service]
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            vosOffer = vosOffer,
            organization = organization,
            gender = gender
          )
          context = SenderConverterContext.forTest(converterContext)
          source = PsbConverter.Source(context)
          res <- converter.convert(source)
        } yield res
        assertM(res)(noDiff(expected)).provideLayer(converterLayer)
      }
    )

  val ExpectedFirstName = "Василий"
  val ExpectedLastName = "Иванов"
  val ExpectedMiddleName = "-"
  val ExpectedSex = Sex.Male
  val ExpectedBirthDate = LocalDate.of(1982, 11, 25)
  val ExpectedBirthPlace = "Самарская обл г Тольятти"
  val ExpectedPassportSeries = "1234"
  val ExpectedPassportNumber = "506789"
  val ExpectedPassportIssuer = "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ."
  val ExpectedIssuedAt = LocalDate.of(2005, 2, 2)
  val ExpectedIssuerCode = "987-654"
  val ExpectedSnils = None
  val ExpectedRegistrationAddressFiasId = "98c1babe-5633-4dad-8bd6-48846f8e29f3"
  val ExpectedRegistrationAddressFull = "Самарская обл, г Тольятти, Обводное шоссе, д 3"
  val ExpectedLivingAddressFiasId = "89441876-8f58-4aa1-92ed-417eb8f07e79"
  val ExpectedLivingAddressFull = "г Москва, пр-кт Вернадского, д 99, корп 1"
  val ExpectedWorkPhone = "+74950011111"
  val ExpectedMobilePhone = "+79267010001"
  val ExpectedEmail = "vasyivanov@yandex.ru".some
  val ExpectedMaritalStatus = MaritalStatus.Divorced
  val ExpectedEmploymentType = EmploymentType.Permanent
  val ExpectedEmployeeStatus = EmployeeStatus.Specialist
  val ExpectedPosition = "IT_SPECIALIST"

  val ExpectedEmployedSince =
    LocalDate.ofInstant(Instant.ofEpochMilli(1605873214676L), ZoneOffset.UTC).minusDays(30 * 72)
  val ExpectedEmploymentYear = 6L
  val ExpectedEmploymentMonths = 0L.some
  val ExpectedEmployerName = "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\""
  val ExpectedEmployerForm = EmployerForm.OOO
  val ExpectedEmployerEmployeesQuantity = EmployeesQuantity.Between100And500
  val ExpectedEmployerLegalAddressFiasId = "c718f386-2aed-46cf-8a74-08de8b6181dc"
  val ExpectedEmployerLegalAddressFull = "г Москва, р-н Замоскворечье, ул Садовническая, д 82, стр 2, кв 3А"
  val ExpectedEmployerActualAddressFiasId = "c718f386-2aed-46cf-8a74-08de8b6181dc"
  val ExpectedEmployerActualAddressFull = "г Москва, р-н Замоскворечье, ул Садовническая, д 82, стр 2, кв 3А"
  val ExpectedEmployerType = EmployerType.CommercialOrganization
  val ExpectedEmployerField = EmployerField.InformationTechnology
  val ExpectedEmployerKpp = "770501001".some
  val ExpectedEmployerInn = "7704366364"
  val ExpectedEducation = Education.IncompleteHigher
  val ExpectedDependentsNumber = 1.some
  val ExpectedMonthlyIncome = 100000L
  val ExpectedMonthlyIncomeType = MonthlyIncomeType.Ndfl2.some
  val ExpectedUnemploymentReason = None
  val ExpectedContactPersonFullName = "Налуне Незнайка"
  val ExpectedContactPersonMobilePhone = "+79267010002"
  val ExpectedLoanInformationSum = 490000L
  val ExpectedLoanInformationMonthPeriod = 13L

  val ExpectedLoanInformationLodgementDate =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(1605873214676L), ZoneOffset.UTC)
  val ExpectedLoanInformationCity = "г Москва"
}
