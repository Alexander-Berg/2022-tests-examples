package ru.yandex.vertis.shark.client.bank.converter.impl

import baker.common.client.dadata.model.DadataOrganization
import cats.implicits._
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.ExpoBankConverter
import ru.yandex.vertis.shark.client.bank.converter.impl.expo.DefaultValues._
import ru.yandex.vertis.shark.client.bank.data.expo.Entities._
import ru.yandex.vertis.shark.config.ExpoBankClientConfig
import ru.yandex.vertis.shark.dictionary.impl.RegionsDictionarySpecBase
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{CreditApplication, SenderConverterContext}
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.test_utils.assertions.Assertions
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.magnolia.diff.gen

import java.time._

object ExpoBankConverterSpec
  extends DefaultRunnableSpec
  with RegionsDictionarySpecBase
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with Assertions.DiffSupport {

  private val httpClientConfig = HttpClientConfig(url = EmptyString)
  private val expoBankClientConfig = ExpoBankClientConfig(httpClientConfig, partnerId = "123456")

  private lazy val converterLayer = ZLayer.requires[Blocking] ++ ZLayer.requires[Clock] ++
    ZLayer.succeed(expoBankClientConfig) >>> ExpoBankConverter.live

  private lazy val expected =
    ExpoBankClaim(
      carType = ExpectedCarType,
      carCondition = ExpectedCarCondition,
      carProduction = ExpectedCarProduction,
      carBrand = ExpectedCarBrand,
      carModel = ExpectedCarModel,
      carYear = ExpectedCarYear,
      carPrice = ExpectedCarPrice,
      creditSum = ExpectedCreditSum,
      creditInitial = ExpectedCreditInitial,
      creditTerm = ExpectedCreditTerm,
      docLastname = ExpectedDocLastName,
      docFirstname = ExpectedDocFirstName,
      docFathername = ExpectedDocFatherName,
      docGender = ExpectedDocGender,
      mobilePhone = ExpectedMobilePhone,
      education = ExpectedEducation,
      docBirthdate = ExpectedDocBirthDate,
      docBirthplace = ExpectedDocBirthPlace,
      maritalStatus = ExpectedMaritalStatus,
      dependants = ExpectedDependants,
      docSeries = ExpectedDocSeries,
      docNumber = ExpectedDocNumber,
      docDate = ExpectedDocDate,
      docIssuer = ExpectedDocIssuer,
      docIssuercode = ExpectedDocIssuerCode,
      driveSeries = DefaultDrive.series,
      driveNumber = DefaultDrive.number,
      driveIssueDate = DefaultDrive.issueDate,
      driveIssueplace = DefaultDrive.issuePlace,
      snilsNumber = ExpectedSnilsNumber,
      email = ExpectedEmail,
      homePhone = DefaultHomePhone,
      regFulladdress = ExpectedRegistrationAddress,
      registrationDate = DefaultRegistrationDate,
      homeFulladdress = ExpectedResidenceAddress,
      workFulladdress = ExpectedWorkAddress,
      workOrgname = ExpectedWorkOrgName,
      workPhone = ExpectedWorkPhone,
      workTitle = ExpectedWorkTitle,
      workIncome = ExpectedWorkIncome,
      workDate = ExpectedWorkDate,
      workBossName = DefaultBossName,
      workInn = ExpectedWorkInn,
      spouseLastname = DefaultSpouse.lastName,
      spouseFirstname = DefaultSpouse.firstName,
      spouseFathername = DefaultSpouse.fatherName,
      spousePhone = DefaultSpouse.phone,
      spouseBirthdate = DefaultSpouse.birthDate,
      spouseDocSeries = DefaultSpouse.passport.map(_.series),
      spouseDocNumber = DefaultSpouse.passport.map(_.number),
      spouseDocDate = DefaultSpouse.passport.map(_.date),
      spouseDocIssuer = DefaultSpouse.passport.map(_.issuer),
      spouseDocIssuercode = DefaultSpouse.passport.map(_.issuerCode),
      guarantLastname = ExpectedGuarantLastName,
      guarantFirstname = ExpectedGuarantFirstName,
      guarantFathername = ExpectedGuarantFatherName,
      guarantBirthdate = DefaultGuarantBirthDate,
      guarantPhone = ExpectedGuarantPhone,
      idPartner = expoBankClientConfig.partnerId
    )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ExpoBankConverter")(
      testM("convert") {
        val timestamp = Instant.now()
        val creditApplication: CreditApplication = sampleCreditApplication
        val vosOffer: Option[Offer] = sampleOffer().some
        val organization: Option[DadataOrganization] = sampleDadataOrganization.suggestions.headOption
        val gender: GenderType = GenderType.MALE
        val res = for {
          converter <- ZIO.service[ExpoBankConverter.Service]
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            vosOffer = vosOffer,
            organization = organization,
            gender = gender
          )
          context = SenderConverterContext.forTest(converterContext)
          source = ExpoBankConverter.Source(context)
          res <- converter.convert(source)
        } yield res
        assertM(res)(noDiff(expected)).provideLayer(converterLayer)
      }
    )

  val ExpectedCarType = CarType.Cars.some
  val ExpectedCarCondition = CarCondition.New.some
  val ExpectedCarProduction = None
  val ExpectedCarBrand = "MERCEDES"
  val ExpectedCarModel = "G_KLASSE_AMG"
  val ExpectedCarYear = 2018
  val ExpectedCarPrice = 10000000f
  val ExpectedCreditSum = 490000L.some
  val ExpectedCreditInitial = 500000L
  val ExpectedCreditTerm = 13
  val ExpectedDocLastName = "Иванов"
  val ExpectedDocFirstName = "Василий"
  val ExpectedDocFatherName = None
  val ExpectedDocGender = Gender.Male
  val ExpectedMobilePhone = "9267010001"
  val ExpectedEducation = Education.SomeCollege.some
  val ExpectedDocBirthDate = LocalDate.of(1982, 11, 25)
  val ExpectedDocBirthPlace = "Самарская обл г Тольятти"
  val ExpectedMaritalStatus = MaritalStatus.Divorced.some
  val ExpectedDependants = 1.some
  val ExpectedDocSeries = "1234"
  val ExpectedDocNumber = "506789"
  val ExpectedDocDate = LocalDate.of(2005, 2, 2)
  val ExpectedDocIssuer = "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ."
  val ExpectedDocIssuerCode = "987-654"
  val ExpectedSnilsNumber = None
  val ExpectedEmail = "vasyivanov@yandex.ru".some
  val ExpectedRegistrationAddress = "Самарская обл, г Тольятти, Обводное шоссе, д 3"
  val ExpectedResidenceAddress = "г Москва, пр-кт Вернадского, д 99, корп 1".some
  val ExpectedWorkAddress = "г Москва, р-н Замоскворечье, ул Садовническая, д 82, стр 2, кв 3А"
  val ExpectedWorkOrgName = "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\""
  val ExpectedWorkPhone = "4950011111"
  val ExpectedWorkTitle = "IT_SPECIALIST"
  val ExpectedWorkIncome = 100000L

  val ExpectedWorkDate =
    LocalDate.ofInstant(Instant.ofEpochMilli(1605873214676L), ZoneOffset.UTC).minusDays(30 * 72)
  val ExpectedWorkInn = "7704366364".some
  val ExpectedGuarantLastName = "Налуне"
  val ExpectedGuarantFirstName = "Незнайка"
  val ExpectedGuarantFatherName = None
  val ExpectedGuarantPhone = "9267010002"
}
