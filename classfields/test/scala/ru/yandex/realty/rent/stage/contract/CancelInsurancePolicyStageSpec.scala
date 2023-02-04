package ru.yandex.realty.rent.stage.contract

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.rent.backend.InsurancePolicyManager.InsuranceType.InsuranceType
import ru.yandex.realty.rent.backend.InsurancePolicyManager.{InsuranceType, InsuranceWithType}
import ru.yandex.realty.rent.backend.{InsurancePolicyManager, RentPaymentsData}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.ContractStatus._
import ru.yandex.realty.rent.model.{ContractWithPayments, RentContract}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CancelInsurancePolicyStageSpec extends AsyncSpecBase with RentPaymentsData with MockFactory with RentModelsGen {

  import RentPaymentsData._

  implicit private val traced: Traced = Traced.empty
  val features: Features = new SimpleFeatures()
  private val RentStartDate = DateTime.now
  val CorrectInsuranceAmount = 51100L
  val CorrectNewInsuranceAmount = 81100
  val now = RentStartDate.plusMonths(1)
  private val ContractWithInsurancePolicy = createContract(
    rentStartDate = RentStartDate,
    paymentDayOfMonth = RentStartDate.getDayOfMonth,
    nowMoment = Some(now)
  ).withInsurancePolicy("ownerId", "tenantId", CorrectInsuranceAmount, true)
  private val ContractWithPreviousInsurancePolicy = createContract(
    rentStartDate = RentStartDate,
    paymentDayOfMonth = RentStartDate.getDayOfMonth,
    nowMoment = Some(now)
  ).withInsurancePolicy("ownerId", "tenantId", CorrectInsuranceAmount, true)
    .withPreviousInsurancePolicy(
      "ownerId",
      "tenantId",
      CorrectInsuranceAmount,
      isActive = true,
      Some(RentStartDate),
      Some(now)
    )
  private val ContractWithTemporaryInsurancePolicy = createContract(
    rentStartDate = RentStartDate,
    paymentDayOfMonth = RentStartDate.getDayOfMonth,
    nowMoment = Some(now)
  ).withInsurancePolicy("ownerId", "tenantId", CorrectInsuranceAmount, true)
    .withTemporaryInsurancePolicy(
      "ownerId",
      "tenantId",
      CorrectInsuranceAmount,
      isActive = true,
      Some(RentStartDate),
      Some(now)
    )

  "InsurancePolicyStage" should {
    "process state when contract is terminated and has insurance policy" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val terminationDate = Some(now.minusDays(1))
      val contract = ContractWithInsurancePolicy
        .copy(terminationDate = terminationDate, status = Terminated)
        .withInsurancePolicy(
          "ownerId",
          "tenantId",
          CorrectInsuranceAmount,
          isActive = true,
          terminationPolicyDateOpt = terminationDate.map(DateTimeFormat.write)
        )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val expectedContract = contract.withInsurancePolicy("ownerId", "tenantId", CorrectInsuranceAmount, false)
      handleShouldProcessMocks(expectedContract, InsuranceType.Current, insurancePolicyManager)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      resultContract should equal(expectedContract)
    }

    "not process state when contract is active and has insurance policy" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val terminationDate = Some(now.minusDays(1))
      val contract = ContractWithInsurancePolicy.copy(terminationDate = terminationDate, status = Active)
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      resultContract.data should equal(contract.data)
    }

    "process state when insurance policy is terminated" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val terminationDate = Some(now.minusDays(1))
      val contract = ContractWithInsurancePolicy.withInsurancePolicy(
        "ownerId",
        "tenantId",
        CorrectInsuranceAmount,
        isActive = true,
        Some(RentStartDate),
        Some(now)
      )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val expectedContract = contract.withInsurancePolicy(
        "ownerId",
        "tenantId",
        CorrectInsuranceAmount,
        isActive = false,
        Some(RentStartDate),
        Some(now)
      )
      handleShouldProcessMocks(expectedContract, InsuranceType.Current, insurancePolicyManager)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      resultContract should equal(expectedContract)
    }

    "process state when contract has terminated previous insurance" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val contract = ContractWithPreviousInsurancePolicy
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val expectedContract = contract.withPreviousInsurancePolicy(
        "ownerId",
        "tenantId",
        CorrectInsuranceAmount,
        isActive = false,
        Some(RentStartDate),
        Some(now)
      )
      handleShouldProcessMocks(expectedContract, InsuranceType.Previous, insurancePolicyManager)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      resultContract should equal(expectedContract)
    }

    "process state when contract has terminated temporary insurance" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val contract = ContractWithTemporaryInsurancePolicy
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val expectedContract = contract.withTemporaryInsurancePolicy(
        "ownerId",
        "tenantId",
        CorrectInsuranceAmount,
        isActive = false,
        Some(RentStartDate),
        Some(now)
      )
      handleShouldProcessMocks(expectedContract, InsuranceType.Temporary, insurancePolicyManager)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      resultContract should equal(expectedContract)
    }

    "not process state when contract has not terminated temporary insurance" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val terminationDate = now.plusMonths(1)
      val contract = ContractWithTemporaryInsurancePolicy.withTemporaryInsurancePolicy(
        "ownerId",
        "tenantId",
        CorrectInsuranceAmount,
        isActive = true,
        Some(RentStartDate),
        Some(terminationDate)
      )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      resultContract.data should equal(contract.data)
      resultContract.visitTime should equal(Some(terminationDate.minusMinutes(5)))
    }
  }

  private def handleShouldProcessMocks(
    contract: RentContract,
    insuranceType: InsuranceType,
    mockInsurancePolicyManager: InsurancePolicyManager
  ) = {
    (mockInsurancePolicyManager
      .cancelInsurancePolicySubscription(_: RentContract, _: InsuranceWithType)(_: Traced))
      .expects(where { (_: RentContract, insuranceWithType: InsuranceWithType, _: Traced) =>
        insuranceWithType.insuranceType == insuranceType
      })
      .once()
      .returning(Future.successful(contract))
  }

  private def mockInsurancePolicyManager = {
    mock[InsurancePolicyManager]
  }

  private def invokeStage(
    contractWithPayments: ContractWithPayments,
    insurancePolicyManager: InsurancePolicyManager
  ): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(contractWithPayments)
    features.EnableMangoCommunication.setNewState(true)
    val stage = new CancelInsurancePolicyStage(insurancePolicyManager, features)
    stage.process(state).futureValue
  }
}
