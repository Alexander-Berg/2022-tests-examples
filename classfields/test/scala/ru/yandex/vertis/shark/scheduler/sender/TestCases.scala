package ru.yandex.vertis.shark.scheduler.sender

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.model.Block._
import ru.yandex.vertis.shark.model.Entity.{AddressEntity, DriverLicenseEntity, NameEntity, PhoneEntity}
import ru.yandex.vertis.shark.model.{AutoruCreditApplication, CreditApplicationClaimSource, CreditProductId, Phone}
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.proto.model.Block.ForeignPassportBlock.Yes.VisitForeignCountriesFrequency
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.shark.scheduler.sender.TestCases.{TestCase, _}
import ru.yandex.vertis.shark.scheduler.sender.TestLayers.TestLayer
import ru.yandex.vertis.zio_baker.model.Inn
import ru.yandex.vertis.zio_baker.model.WithValidate.ValidationErrors
import zio.RIO
import zio.test.Assertion._
import zio.test.{assertM, TestResult}

import java.time.LocalDate

trait TestCases {

  protected def testCases: Seq[TestCase] = Seq(
    employee,
    selfEmployee,
    pensionAge,
    nameWithPatronymic,
    nameWithOutPatronymic,
    foreignPassportYes,
    foreignPassportNo,
    driverLicenseHas,
    driverLicenseRelated,
    driverLicenseNo
  )

//  protected def successMaxAmountMin: TestCase = testIsSome(
  //    name = "maxAmount min success",
  //    builder = _.setRequirementsMaxAmount(100_000)
  //  )
  //
  //  protected def failureMaxAmountMin: TestCase = testValidationError(
  //    name = "maxAmount min failure",
  //    builder = _.setRequirementsMaxAmount(99_999)
  //  )
  //
  //  protected def successMaxAmountMax: TestCase = testIsSome(
  //    name = "maxAmount max success",
  //    builder = _.setRequirementsMaxAmount(5_000_000)
  //  )
  //
  //  protected def failureMaxAmountMax: TestCase = testValidationError(
  //    name = "maxAmount max failure",
  //    builder = _.setRequirementsMaxAmount(5_000_001)
  //  )
  //
  //  protected def successTermMonthsMin: TestCase = testIsSome(
  //    name = "termMonths min success",
  //    builder = _.setRequirementsTermMonths(12)
  //  )
  //
  //  protected def failureTermMonthsMin: TestCase = testValidationError(
  //    name = "termMonths min failure",
  //    builder = _.setRequirementsTermMonths(11)
  //  )
  //
  //  protected def successTermMonthsMax: TestCase = testIsSome(
  //    name = "termMonths max success",
  //    builder = _.setRequirementsTermMonths(60)
  //  )
  //
  //  protected def failureTermMonthsMax: TestCase = testValidationError(
  //    name = "termMonths max failure",
  //    builder = _.setRequirementsTermMonths(61)
  //  )

  protected def nameWithPatronymic: TestCase = testIsSome(
    name = "name with patronymic",
    builder = _.setName(NameEntity("Вася".taggedWith, "Иванов".taggedWith, "Сергеевич".taggedWith.some))
  )

  protected def nameWithOutPatronymic: TestCase = testIsSome(
    name = "name without patronymic",
    builder = _.setName(NameEntity("Вася".taggedWith, "Иванов".taggedWith, None))
  )

  protected def foreignPassportYes: TestCase = testIsSome(
    name = "foreign passport yes",
    builder = _.setForeignPassport(withForeignPassportBlock).setDriverLicense(NoDriverLicenseBlock)
  )

  protected def foreignPassportNo: TestCase = testValidationError(
    name = "foreign passport no",
    builder = _.setForeignPassport(WithoutForeignPassportBlock).setDriverLicense(NoDriverLicenseBlock)
  )

  protected def driverLicenseHas: TestCase = testIsSome(
    name = "driver license has",
    builder = _.setDriverLicense(hasDriverLicenseBlock)
      .setForeignPassport(WithoutForeignPassportBlock)
  )

  protected def driverLicenseRelated: TestCase = testIsSome(
    name = "driver license related",
    builder = _.setDriverLicense(relatedDriverLicenseBlock)
      .setForeignPassport(WithoutForeignPassportBlock)
  )

  protected def driverLicenseNo: TestCase = testValidationError(
    name = "driver license not",
    builder = _.setDriverLicense(NoDriverLicenseBlock)
      .setForeignPassport(WithoutForeignPassportBlock)
  )

  protected def employee: TestCase = testIsSome(name = "employee")

  protected def selfEmployee: TestCase = TestCase(
    name = "self employed",
    builder = _.setPersonProfileEmployment(selfEmployedEmploymentBlock),
    assertion = res => assertM(res)(isNone)
  )

  protected def pensionAge: TestCase = TestCase(
    name = "pension age",
    builder = _.setPersonProfileEmployment(notEmployedEmploymentBlockPensionAge),
    assertion = res => assertM(res)(isNone)
  )
}

object TestCases {

  type CreditApplicationBuilder = AutoruCreditApplication => AutoruCreditApplication
  type AssertResult = RIO[TestLayer, Option[CreditApplicationClaimSource]] => RIO[TestLayer, TestResult]

  def testIsSome(name: String): TestCase = testIsSome(name, identity)

  def testIsSome(name: String, builder: AutoruCreditApplication => AutoruCreditApplication): TestCase =
    TestCase(name, builder, assertIsSome)

  def testValidationError(name: String): TestCase = testValidationError(name, identity)

  def testValidationError(name: String, builder: AutoruCreditApplication => AutoruCreditApplication): TestCase =
    TestCase(name, builder, assertValidationError)

  def assertIsSome: AssertResult = res => assertM(res)(isSome)

  def assertValidationError: AssertResult = res => assertM(res.run)(fails(isSubtype[ValidationErrors](anything)))

  final case class TestCase(
      name: String,
      builder: CreditApplicationBuilder,
      assertion: AssertResult,
      isIgnore: Boolean = false) {
    def withBuilder(b: CreditApplicationBuilder): TestCase = copy(builder = b)
    def withAssertion(a: AssertResult): TestCase = copy(assertion = a)
    def ignore: TestCase = copy(isIgnore = true)
  }

  case class TestCaseContext(test: TestCase, ca: AutoruCreditApplication, creditProduct: CreditProductId)

  def notEmployedEmploymentBlockPensionAge: NotEmployedEmploymentBlock =
    NotEmployedEmploymentBlock(
      reason = proto.Block.EmploymentBlock.NotEmployed.Reason.PENSION_AGE,
      otherReason = None
    )

  def selfEmployedEmploymentBlock: SelfEmployedEmploymentBlock = {
    val kladr = AddressEntity.Kladr.forTest(
      id = "7700000000000".taggedWith.some,
      regionId = "7700000000000".taggedWith.some,
      cityId = "7700000000000".taggedWith.some
    )
    val fias = AddressEntity.Fias.forTest(
      id = "0c5b2444-70a0-4932-980c-b4dc0d3f02b5".taggedWith.some,
      regionId = "0c5b2444-70a0-4932-980c-b4dc0d3f02b5".taggedWith.some,
      cityId = "0c5b2444-70a0-4932-980c-b4dc0d3f02b5".taggedWith.some
    )
    val address = AddressEntity(
      region = "Москва",
      city = "Москва".some,
      settlement = None,
      district = None,
      street = None,
      building = None,
      corpus = None,
      construction = None,
      apartment = None,
      postCode = 101000.taggedWith,
      kladr = kladr,
      fias = fias
    )
    SelfEmployedEmploymentBlock(
      orgName = "ИП Соловьев Андрей",
      inn = Inn("772405318913").some,
      headCount = 2,
      okveds = Seq("62.01".taggedWith),
      addressEntity = address
    )
  }

  def withForeignPassportBlock: ForeignPassportBlock = {
    WithForeignPassportBlock(
      isReadyToProvidePassport = true,
      visitForeignCountriesFrequency = VisitForeignCountriesFrequency.LESS_ONCE_YEAR
    )
  }

  def hasDriverLicenseBlock: DriverLicenseBlock = {
    val license = DriverLicenseEntity("12АА455334".taggedWith, LocalDate.parse("2017-12-13"), None)
    HasDriverLicenseBlock(license)
  }

  def relatedDriverLicenseBlock: DriverLicenseBlock = {
    val license = DriverLicenseEntity("12АА455334".taggedWith, LocalDate.parse("2017-12-13"), None)
    val name = NameEntity("Ирина".taggedWith, "Василькова".taggedWith, "Геннадиевна".taggedWith.some)
    val phone = PhoneEntity.forTest(Phone("89267917752"))
    RelatedDriverLicenseBlock(license, name, phone)
  }
}
