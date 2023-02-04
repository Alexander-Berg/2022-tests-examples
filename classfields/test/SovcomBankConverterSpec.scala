package ru.yandex.vertis.shark.client.bank.converter.impl

import cats.implicits._
import io.circe.syntax.EncoderOps
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.MockDadataClient
import ru.yandex.vertis.shark.client.bank.converter.SovcomBankConverter
import ru.yandex.vertis.shark.client.bank.converter.SovcomBankConverter.Source
import ru.yandex.vertis.shark.client.bank.data.sovcom.Entities._
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.test_utils.assertions.Assertions
import zio.ZIO
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.{Instant, LocalDate}

object SovcomBankConverterSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with StaticSamples
  with Assertions.DiffSupport {

  private lazy val converterLayer = MockDadataClient.live >+> SovcomBankConverter.live

  private val expected = SovcomBankClaim(
    term = 12,
    sum = 490000L.some,
    agree = true,
    name = "Василий",
    surname = "Иванов",
    phone = "89267010001",
    validPhone = "sms".some,
    gender = Gender.Male,
    birthDate = LocalDate.parse("1982-11-25"),
    birthPlace = "Россия Самарская обл г Тольятти",
    passportSeries = "1234",
    passportNumber = "506789",
    passportCode = "987-654",
    passportDate = LocalDate.parse("2005-02-02"),
    passportOrgan = "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ.",
    hasOldPassport = false,
    registrationAddress = "Самарская обл, г Тольятти, Обводное шоссе, д 3",
    lifeAddress = "г Москва, пр-кт Вернадского, д 99, корп 1",
    changedCredentials = false,
    socialStatus = SocialStatus.Worker,
    workName = "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"".some,
    experience = "72".some,
    employerType = EmployerType.Commercial.some,
    workPosition = "IT специалист".some,
    workAddress = "г Москва, р-н Замоскворечье, ул Садовническая, д 82, стр 2, кв 3А".some,
    salary = 87000,
    maritalStatus = MaritalStatus.Divorced,
    education = Education.IncompleteHigher,
    livingType = LivingType.Rent,
    productName = "Из рук в руки",
    utmMedium = "Agent".some,
    utmSource = "auto.ru".some,
    utmCampaign = "complete_app".some,
    patronymic = None,
    spouseName = None,
    spouseSurname = None,
    spousePatronymic = None,
    oldPassportSeries = None,
    oldPassportNumber = None,
    oldPassportCode = None,
    oldPassportDate = None,
    oldPassportOrgan = None,
    registrationDate = None,
    lifeAddressDate = None,
    oldSurname = None,
    lastNameChangeReason = None,
    changeCredentialsYear = None,
    otherSalary = None,
    utmContent = None,
    utmTerm = None,
    ipAddressClient = None
  )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("SovcomBankConverterImpl")(
      testM("convert") {
        val autoruCreditApplication = sampleCreditApplication
        val timestamp = Instant.now()
        val res = for {
          converter <- ZIO.service[SovcomBankConverter.Service]
          org = sampleDadataOrganization.suggestions.headOption
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = autoruCreditApplication,
            organization = org
          )
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context)
          res <- converter.convert(source)
        } yield res
        assertM(res)(noDiff(expected)).provideLayer(converterLayer)
      }
    )
}
