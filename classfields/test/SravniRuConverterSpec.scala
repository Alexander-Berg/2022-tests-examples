package ru.yandex.vertis.shark.client.bank.converter.impl

import cats.implicits.catsSyntaxOptionId
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.SravniRuConverter
import ru.yandex.vertis.shark.client.bank.converter.impl.sravniru.DefaultValues
import ru.yandex.vertis.shark.client.bank.data.sravniru.Entities._
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model.{AutoruCreditApplication, SenderConverterContext}
import ru.yandex.vertis.test_utils.assertions.Assertions
import zio._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.{Instant, LocalDate}

object SravniRuConverterSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with StaticSamples
  with Assertions.DiffSupport {

  private val autoruCreditApplication: AutoruCreditApplication = sampleCreditApplication
  private val timestamp = Instant.parse("2020-01-01T00:00:00Z")

  private lazy val expected = SravniRuClaim(
    firstName = "Василий",
    middleName = "нет",
    lastName = "Иванов",
    maritalStatus = MaritalStatus.Divorced.some,
    education = Education.IncompleteHigher.some,
    dependentsCount = 1.some,
    phone = "79267010001",
    verified = true,
    email = "vasyivanov@yandex.ru".some,
    additionalPhone = "79267010002".some,
    additionalPhoneOwner = PhoneOwner.FriendNumber.some,
    additionalPhoneOwnerFio = "Налуне Незнайка".some,
    workPhone = "74950011111".some,
    birthDate = LocalDate.parse("1982-11-25").some,
    birthPlace = "Самарская обл г Тольятти".some,
    passportIssueDate = LocalDate.parse("2005-02-02").some,
    passportNumber = "1234 506789".some,
    passportIssuedBy = "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ.".some,
    passportIssuedByCode = "987-654".some,
    snils = None,
    isCitizenRF = true.some,
    employmentType = EmploymentType.Employment.some,
    unemploymentReason = None,
    organizationName = "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"".some,
    organizationInn = "7704366364".some,
    organizationType = OrganizationType.OOO,
    organizationActivity = OrganizationActivity.TradeServicesCommunications,
    lastExperienceStartDate = LocalDate.parse("2014-01-01").some,
    jobType = JobType.Specialist.some,
    jobTitle = "IT специалист".some,
    salaryBank = None,
    region = None,
    registrationAddress = None,
    registrationDate = DefaultValues.RegistrationDate.some,
    residenceAddress = None,
    workAddress = "г Москва, р-н Замоскворечье, ул Садовническая, д 82".some,
    monthlyIncome = 100_000L.some,
    solvencyProof = SolvencyProof.Ndfl2.some,
    additionalIncome = None,
    flatRentAmount = 40_000L.some,
    hasRealEstate = None,
    estateType = EstateType.Nothing.some,
    hasCar = None,
    carType = CarType.No.some,
    amount = 490_000L.some,
    initialFee = 500_000L.some,
    term = 13.some,
    purpose = Purpose.UsedCar.some,
    personalDataConsent = true.some,
    BKIConsent = true.some,
    seniority = Seniority.P10y,
    query = Utm(
      utmSource = "auto.ru",
      utmCampaign = "creditselection_backflow_order_refused",
      utmMedium = "cpa",
      utmTerm = "",
      utmContent = "conscred--api"
    ).some
  )

  private lazy val converterLayer = SravniRuConverter.live

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("SravniRuConverter")(
      testM("convert") {
        val res = for {
          converter <- ZIO.service[SravniRuConverter.Service]
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = autoruCreditApplication,
            organization = sampleDadataOrganization.suggestions.headOption
          )
          context = SenderConverterContext.forTest(converterContext)
          source = SravniRuConverter.Source(context)
          res <- converter.convert(source)
        } yield res
        assertM(res)(noDiff(expected)).provideLayer(converterLayer)
      }
    )
}
