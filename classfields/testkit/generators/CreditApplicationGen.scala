package ru.yandex.vertis.shark.model.generators

import cats.implicits.catsSyntaxOptionId
import org.scalacheck.magnolia
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.Block._
import ru.yandex.vertis.shark.model.CreditApplication.Requirements
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.model.User

trait CreditApplicationGen {
  import magnolia._

  protected def sampleAutoruCreditApplication(): AutoruCreditApplication = {
    val base = Arbitraries.generate[AutoruCreditApplication]()
    val borrowerPersonProfile = PersonProfileImpl(
      id = Arbitraries.generate[PersonProfileId]().some,
      user = Arbitraries.generate[User]().some,
      name = Arbitraries.generate[NameBlock]().some,
      gender = Arbitraries.generate[GenderBlock]().some,
      passportRf = Arbitraries.generate[PassportRfBlock]().some,
      oldPassportRf = Arbitraries.generate[OldPassportRfBlock]().some,
      oldName = Arbitraries.generate[OldNameBlock]().some,
      foreignPassport = Arbitraries.generate[ForeignPassportBlock]().some,
      insuranceNumber = Arbitraries.generate[InsuranceNumberBlock]().some,
      driverLicense = Arbitraries.generate[HasDriverLicenseBlock]().some,
      birthDate = Arbitraries.generate[BirthDateBlock]().some,
      birthPlace = Arbitraries.generate[BirthPlaceBlock]().some,
      residenceAddress = Arbitraries.generate[ResidenceAddressBlock]().some,
      registrationAddress = Arbitraries.generate[RegistrationAddressBlock]().some,
      education = Arbitraries.generate[EducationBlock]().some,
      maritalStatus = Arbitraries.generate[MaritalStatusBlock]().some,
      dependents = Arbitraries.generate[DependentsBlock]().some,
      income = Arbitraries.generate[IncomeBlock]().some,
      expenses = Arbitraries.generate[ExpensesBlock]().some,
      propertyOwnership = Arbitraries.generate[PropertyOwnershipBlock]().some,
      vehicleOwnership = Arbitraries.generate[VehicleOwnershipBlock]().some,
      employment = Arbitraries.generate[EmploymentBlock]().some,
      relatedPersons = Arbitraries.generate[RelatedPersonsBlock]().some,
      phones = Arbitraries.generate[PhonesBlock]().some,
      emails = Arbitraries.generate[EmailsBlock]().some
    )
    val requirements = Arbitraries.generate[Requirements]()
    base.copy(
      state = proto.CreditApplication.State.DRAFT,
      borrowerPersonProfile = borrowerPersonProfile.some,
      requirements = requirements.some
    )
  }
}
