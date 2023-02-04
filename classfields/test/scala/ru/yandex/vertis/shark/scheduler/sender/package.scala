package ru.yandex.vertis.shark.scheduler

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.model.Block._
import ru.yandex.vertis.shark.model.Entity.NameEntity
import ru.yandex.vertis.shark.model.{AutoruCreditApplication, PersonProfile, PersonProfileImpl}
import ru.yandex.vertis.shark.proto.{model => proto}

package object sender {

  implicit class ModifiableAutoruCreditApplication(val creditApplication: AutoruCreditApplication) extends AnyVal {

    def setRequirementsMaxAmount(value: Long): AutoruCreditApplication = {
      val requirements = creditApplication.requirements.get.copy(maxAmount = value.taggedWith)
      creditApplication.copy(requirements = requirements.some)
    }

    def setRequirementsInitialFee(value: Long): AutoruCreditApplication = {
      val requirements = creditApplication.requirements.get.copy(initialFee = value.taggedWith)
      creditApplication.copy(requirements = requirements.some)
    }

    def setRequirementsTermMonths(value: Int): AutoruCreditApplication = {
      val requirements = creditApplication.requirements.get.copy(termMonths = value.taggedWith)
      creditApplication.copy(requirements = requirements.some)
    }

    def setPersonProfile(value: PersonProfile): AutoruCreditApplication =
      creditApplication.copy(borrowerPersonProfile = value.some)

    def setPersonProfileEmployment(value: EmploymentBlock): AutoruCreditApplication =
      setPersonProfile(extractBorrowerPersonProfile.copy(employment = value.some))

    def setName(value: NameEntity): AutoruCreditApplication =
      setPersonProfile(extractBorrowerPersonProfile.copy(name = NameBlock(value).some))

    def setForeignPassport(value: ForeignPassportBlock): AutoruCreditApplication =
      setPersonProfile(extractBorrowerPersonProfile.copy(foreignPassport = value.some))

    def setDriverLicense(value: DriverLicenseBlock): AutoruCreditApplication =
      setPersonProfile(extractBorrowerPersonProfile.copy(driverLicense = value.some))

    def setIncomeProof(value: proto.Block.IncomeBlock.IncomeProof): AutoruCreditApplication =
      setIncome(extractIncome.copy(incomeProof = value))

    def setIncome(value: IncomeBlock): AutoruCreditApplication =
      setPersonProfile(extractBorrowerPersonProfile.copy(income = value.some))

    def extractIncome: IncomeBlock = extractBorrowerPersonProfile.income.get

    def extractBorrowerPersonProfile: PersonProfileImpl =
      creditApplication.borrowerPersonProfile.collect { case p: PersonProfileImpl =>
        p
      }.get
  }
}
