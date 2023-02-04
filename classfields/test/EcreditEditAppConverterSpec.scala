package ru.yandex.vertis.shark.client.bank.converter.impl

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.EcreditEditAppConverter
import ru.yandex.vertis.shark.client.bank.data.ecredit.Entities
import ru.yandex.vertis.shark.client.bank.data.ecredit.Entities.{Customer, EditAppClaim}
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{CreditApplication, SenderConverterContext}
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.shark.util.RichModel.RichCreditApplication
import ru.yandex.vertis.test_utils.assertions.Assertions
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{Task, ZIO, ZLayer}

import java.time.Instant

object EcreditEditAppConverterSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with Assertions.DiffSupport {

  private val currentInstant = Instant.parse("2022-02-17T10:30:00Z")
  private val creditApplication: CreditApplication = sampleCreditApplication
  private val claim = creditApplication.getClaimByCreditProductId("dealer-1".taggedWith).get
  private val eCreditDealerId = "16453"

  private val sampleRegistrationDadataAddress = sampleDadataAddress1.suggestions.head
  private val sampleResidenceDadataAddress = sampleDadataAddress2.suggestions.head
  private val sampleOrganization = sampleDadataOrganization.suggestions.head
  private val sampleOrganizationDadataAddress = sampleOrganization.data.flatMap(_.address).get

  private val expectedCustomer: Customer = Customer(
    surname = "Иванов",
    name = "Василий",
    middleName = "отсутствует",
    birthDate = "1982-11-25",
    birthPlace = "Россия Самарская обл г Тольятти",
    gender = 1,
    email = "vasyivanov@yandex.ru",
    mobilePhone = Entities.Phone("7", "926", "7010001"),
    numOfChildrenYoungerThan_21 = 1,
    numberOfDependents = 0,
    matchesRegistration = 0,
    livingAddress = Entities.DadataAddress(sampleResidenceDadataAddress),
    livingAddressDate = "2019-02-17",
    registrationAddress = Entities.DadataAddress(sampleRegistrationDadataAddress),
    registrationAddressDate = "1999-04-02",
    realtyState = 3,
    employmentType = 1,
    employerName = "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"",
    officePhone = Entities.Phone("7", "495", "0011111"),
    employeePosition = "IT специалист",
    employerInn = "7704366364",
    employeePositionType = 1,
    employerType = 2,
    employerNumberOfEmployees = 6,
    employerBusinessType = 1,
    employerIndustryBranch = 11,
    workingLifeDate = "2016-02-17",
    employerLegalAddress = Entities.DadataAddress(sampleOrganizationDadataAddress),
    primaryIncomeSum = 100000,
    spendingsRequest = 40000,
    spendingsAnycredit = 0,
    education = 4,
    passport = Entities.Passport(
      series = "1234",
      number = "506789",
      issueDate = "2005-02-02",
      issuerCode = "987-654",
      issuerName = "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ."
    ),
    proxyFirstName = "Незнайка".some,
    proxySecondName = "Налуне".some,
    proxyThirdName = "отсутствует".some,
    proxyPhone = Entities.Phone("7", "926", "7010002").some,
    proxyBirthDate = "1982-11-25".some,
    maritalStatus = 3,
    yearsMarried = None,
    spouseSurname = None,
    spouseName = None,
    spouseMiddleName = None,
    spouseMobile = None,
    spouseBirthDate = None,
    spouseBirthPlace = None,
    spouseEmploymentType = None,
    drivingLicence = Entities.DrivingLicence(
      series = "7701",
      number = "397000",
      issueDate = "2017-12-13",
      issuerName = "ГИБДД 5801",
      drivingExperience = "4",
      experienceBegin = "2017-12-13",
      expirationDate = None
    )
  )

  private val expected = EditAppClaim(
    async = 1,
    idType = 1,
    applicationId = "test-claim-dealer-1",
    dealerIdType = 3,
    dealerId = eCreditDealerId,
    customer = expectedCustomer,
    electronicSignature = 1,
    personalData = 1,
    creditHistory = 1
  )

  private lazy val converterLayer =
    ZLayer.requires[Blocking] ++ ZLayer.requires[Clock] >>> EcreditEditAppConverter.live

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("EcreditEditAppConverter")(
      testM("convert") {
        val res = for {
          converter <- ZIO.service[EcreditEditAppConverter.Service]
          converterContext = AutoConverterContext.forTest(
            timestamp = currentInstant,
            creditApplication = sampleCreditApplication,
            vosOffer = sampleOffer().some,
            organization = sampleOrganization.some,
            gender = GenderType.MALE,
            registrationDadataAddress = sampleRegistrationDadataAddress.some,
            residenceDadataAddress = sampleResidenceDadataAddress.some
          )
          context = SenderConverterContext.forTest(converterContext)
          source = EcreditEditAppConverter.Source(context, claim.id, eCreditDealerId)
          res <- converter.convert(source)
        } yield res
        assertM(res)(noDiff(expected)).provideLayer(converterLayer)
      }
    )
}
