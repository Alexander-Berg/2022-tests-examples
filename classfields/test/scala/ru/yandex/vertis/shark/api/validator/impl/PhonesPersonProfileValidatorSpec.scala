package ru.yandex.vertis.shark.api.validator.impl

import cats.implicits.{catsSyntaxOptionId, catsSyntaxValidatedId, catsSyntaxValidatedIdBinCompat0}
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.api.validator.FormValidator
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.Block.RelatedPersonsBlock.RelatedPerson
import ru.yandex.vertis.shark.model.Block.{
  EmployeeEmploymentBlock,
  PhonesBlock,
  RelatedDriverLicenseBlock,
  RelatedPersonsBlock
}
import ru.yandex.vertis.shark.model.Entity.{DriverLicenseEntity, NameEntity, PhoneEntity}
import ru.yandex.vertis.shark.model.{PersonProfile, PersonProfileImpl, Phone}
import ru.yandex.vertis.shark.proto.model.Block.RelatedPersonsBlock.RelatedPerson.RelatedPersonType
import ru.yandex.vertis.shark.proto.model.Entity.PhoneEntity.PhoneType
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.Error.InvalidValue
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.Result
import zio.{Has, ULayer, ZLayer}
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object PhonesPersonProfileValidatorSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  private val samplePersonProfile = gen[PersonProfileImpl].arbitrary.sample.get

  private val validatorLayer: ULayer[Has[FormValidator.Service[PersonProfile]]] =
    ZLayer.succeed(new PhonesPersonProfileValidator)
  private val sampleEmployment = gen[EmployeeEmploymentBlock].arbitrary.sample.get
  private val sampleDriverLicense = gen[DriverLicenseEntity].arbitrary.sample.get

  private val invalidPhone = "70001111111"
  private val validPhone = "79652861010"

  private def sampleRelatedPerson(phone: String): RelatedPerson = RelatedPerson(
    nameEntity = NameEntity(
      name = "Василий".taggedWith,
      surname = "Пупкин".taggedWith,
      patronymic = None
    ),
    phoneEntity = PhoneEntity(Phone(phone), phoneType = PhoneType.ADDITIONAL.some),
    relatedPersonType = RelatedPersonType.RELATIVES,
    None
  )

  private val validPersonProfile = samplePersonProfile.copy(
    phones = PhonesBlock(
      phoneEntities = Seq(
        PhoneEntity(Phone(validPhone), PhoneType.PERSONAL.some)
      )
    ).some,
    relatedPersons = RelatedPersonsBlock(
      relatedPersons = Seq(sampleRelatedPerson(validPhone))
    ).some,
    employment = sampleEmployment.copy(phones = Seq(Phone(validPhone))).some,
    driverLicense = RelatedDriverLicenseBlock(
      driverLicenseEntity = sampleDriverLicense,
      nameEntity = NameEntity(
        name = "Сергей".taggedWith,
        surname = "Наумов".taggedWith,
        patronymic = None
      ),
      phoneEntity = PhoneEntity(Phone(validPhone), PhoneType.PERSONAL.some)
    ).some
  )

  private case class ValidatorTestCase(
      description: String,
      personProfile: PersonProfile,
      expectedResult: Result[PersonProfile])

  private def toInvalidValue(phone: String, prefix: String): InvalidValue = InvalidValue(s"$prefix.phone", phone)

  private val validatorTestCases = Seq(
    ValidatorTestCase(
      description = "valid phones",
      personProfile = validPersonProfile,
      expectedResult = validPersonProfile.valid
    ),
    ValidatorTestCase(
      description = "invalid phone in phones block",
      personProfile = validPersonProfile.copy(
        phones = PhonesBlock(
          phoneEntities = Seq(
            PhoneEntity(Phone(invalidPhone), PhoneType.PERSONAL.some),
            PhoneEntity(Phone(validPhone), PhoneType.ADDITIONAL.some)
          )
        ).some
      ),
      expectedResult = toInvalidValue(invalidPhone, "phones").invalidNec
    ),
    ValidatorTestCase(
      description = "invalid phone in related block",
      personProfile = validPersonProfile.copy(
        relatedPersons = RelatedPersonsBlock(
          relatedPersons = Seq(sampleRelatedPerson(invalidPhone))
        ).some
      ),
      expectedResult = toInvalidValue(invalidPhone, "related_persons").invalidNec
    ),
    ValidatorTestCase(
      description = "invalid phone in driver license block",
      personProfile = validPersonProfile.copy(
        driverLicense = RelatedDriverLicenseBlock(
          driverLicenseEntity = sampleDriverLicense,
          nameEntity = NameEntity(
            name = "Сергей".taggedWith,
            surname = "Наумов".taggedWith,
            patronymic = None
          ),
          phoneEntity = PhoneEntity(Phone(invalidPhone), PhoneType.PERSONAL.some)
        ).some
      ),
      expectedResult = toInvalidValue(invalidPhone, "driver_license.related").invalidNec
    ),
    ValidatorTestCase(
      description = "invalid phone in employment block",
      personProfile = validPersonProfile.copy(
        employment = sampleEmployment.copy(phones = Seq(Phone(invalidPhone))).some
      ),
      expectedResult = toInvalidValue(invalidPhone, "employment.employee").invalidNec
    )
  )

  private val validatorTests = validatorTestCases.map { tc =>
    testM(tc.description)(
      assertM(FormValidator.validate(tc.personProfile))(equalTo(tc.expectedResult))
    ).provideLayer(validatorLayer)
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PhonesPersonProfileValidator")(
      suite("validate")(validatorTests: _*)
    )
}
