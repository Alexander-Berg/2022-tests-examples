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
import ru.yandex.vertis.zio_baker.util.{ColonString, CommaString}
import zio.{Has, ULayer, ZLayer}
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object DuplicatePhonesPersonProfileValidatorSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  private val samplePersonProfile = gen[PersonProfileImpl].arbitrary.sample.get

  private val validatorLayer: ULayer[Has[FormValidator.Service[PersonProfile]]] =
    ZLayer.succeed(new DuplicatePhonesPersonProfileValidator)
  private val sampleEmployment = gen[EmployeeEmploymentBlock].arbitrary.sample.get
  private val sampleDriverLicense = gen[DriverLicenseEntity].arbitrary.sample.get

  private val phone1 = "79251111111"
  private val phone2 = "79252222222"
  private val phone3 = "79253333333"
  private val phone4 = "79254444444"

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

  private case class ValidatorTestCase(
      description: String,
      personProfile: PersonProfile,
      expectedResult: Result[PersonProfile])

  private def toInvalidValue(phone: String, places: Seq[String]): InvalidValue = {
    val msg = s"Same number in ${places.sorted.toSet.mkString(CommaString)}"
    val name = ("duplicate_phone".some ++ places.sorted).mkString(ColonString)
    InvalidValue(name, phone, msg)
  }

  private val validatorTestCases = Seq(
    {
      val profile = samplePersonProfile.copy(
        phones = PhonesBlock(
          phoneEntities = Seq(
            PhoneEntity(Phone(phone1), PhoneType.PERSONAL.some)
          )
        ).some,
        relatedPersons = RelatedPersonsBlock(
          relatedPersons = Seq(sampleRelatedPerson(phone2))
        ).some,
        employment = sampleEmployment.copy(phones = Seq(Phone(phone3))).some,
        driverLicense = RelatedDriverLicenseBlock(
          driverLicenseEntity = sampleDriverLicense,
          nameEntity = NameEntity(
            name = "Сергей".taggedWith,
            surname = "Наумов".taggedWith,
            patronymic = None
          ),
          phoneEntity = PhoneEntity(Phone(phone4), PhoneType.PERSONAL.some)
        ).some
      )
      ValidatorTestCase(
        description = "valid phones with no duplicates",
        personProfile = profile,
        expectedResult = profile.valid
      )
    },
    ValidatorTestCase(
      description = "invalid phones with duplicate phones in phones block",
      personProfile = samplePersonProfile.copy(
        phones = PhonesBlock(
          phoneEntities = Seq(
            PhoneEntity(Phone(phone1), PhoneType.PERSONAL.some),
            PhoneEntity(Phone(phone1), PhoneType.ADDITIONAL.some)
          )
        ).some,
        relatedPersons = RelatedPersonsBlock(
          relatedPersons = Seq(sampleRelatedPerson(phone2))
        ).some,
        employment = sampleEmployment.copy(phones = Seq(Phone(phone3))).some,
        driverLicense = RelatedDriverLicenseBlock(
          driverLicenseEntity = sampleDriverLicense,
          nameEntity = NameEntity(
            name = "Сергей".taggedWith,
            surname = "Наумов".taggedWith,
            patronymic = None
          ),
          phoneEntity = PhoneEntity(Phone(phone4), PhoneType.PERSONAL.some)
        ).some
      ),
      expectedResult = toInvalidValue(phone1, Seq("personal", "additional")).invalidNec
    ),
    ValidatorTestCase(
      description = "invalid phones with duplicate phones in related block",
      personProfile = samplePersonProfile.copy(
        phones = PhonesBlock(
          phoneEntities = Seq(
            PhoneEntity(Phone(phone1), PhoneType.PERSONAL.some)
          )
        ).some,
        relatedPersons = RelatedPersonsBlock(
          relatedPersons = Seq(sampleRelatedPerson(phone1))
        ).some,
        employment = sampleEmployment.copy(phones = Seq(Phone(phone3))).some,
        driverLicense = RelatedDriverLicenseBlock(
          driverLicenseEntity = sampleDriverLicense,
          nameEntity = NameEntity(
            name = "Сергей".taggedWith,
            surname = "Наумов".taggedWith,
            patronymic = None
          ),
          phoneEntity = PhoneEntity(Phone(phone4), PhoneType.PERSONAL.some)
        ).some
      ),
      expectedResult = toInvalidValue(phone1, Seq("personal", "related_persons")).invalidNec
    ),
    ValidatorTestCase(
      description = "invalid phones with duplicate phones in personal, related and employement block",
      personProfile = samplePersonProfile.copy(
        phones = PhonesBlock(
          phoneEntities = Seq(
            PhoneEntity(Phone(phone1), PhoneType.PERSONAL.some)
          )
        ).some,
        relatedPersons = RelatedPersonsBlock(
          relatedPersons = Seq(sampleRelatedPerson(phone1))
        ).some,
        employment = sampleEmployment.copy(phones = Seq(Phone(phone1))).some,
        driverLicense = RelatedDriverLicenseBlock(
          driverLicenseEntity = sampleDriverLicense,
          nameEntity = NameEntity(
            name = "Сергей".taggedWith,
            surname = "Наумов".taggedWith,
            patronymic = None
          ),
          phoneEntity = PhoneEntity(Phone(phone4), PhoneType.PERSONAL.some)
        ).some
      ),
      expectedResult = toInvalidValue(phone1, Seq("personal", "related_persons", "work")).invalidNec
    )
  )

  private val validatorTests = validatorTestCases.map { tc =>
    testM(tc.description)(
      assertM(FormValidator.validate(tc.personProfile))(equalTo(tc.expectedResult))
    ).provideLayer(validatorLayer)
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("DuplicatePhonesPersonProfileValidator")(
      suite("validate")(validatorTests: _*)
    )
}
