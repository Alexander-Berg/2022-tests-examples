package ru.yandex.realty.rent.stage.contract

import org.joda.time.DateTime
import com.google.protobuf.Timestamp
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.rent.backend.InsurancePolicyManager.InsuranceType
import ru.yandex.realty.rent.backend.InsurancePolicyManager.InsuranceType.InsuranceType
import ru.yandex.realty.rent.backend.{InsurancePolicyManager, RentPaymentsData}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.ContractStatus
import ru.yandex.realty.rent.model.{ContractWithPayments, RentContract}
import ru.yandex.realty.rent.proto.model.contract.ContractData
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CreateInsurancePolicyStageSpec extends AsyncSpecBase with RentPaymentsData with MockFactory with RentModelsGen {

  import RentPaymentsData._

  implicit private val traced: Traced = Traced.empty
  val features: Features = new SimpleFeatures()
  private val RentStartDate = DateTime.now
  private val StartDate = RentStartDate.plusMonths(3)
  val CorrectInsuranceAmount = 51100L
  private val ContractWithInsurancePolicy = createContract(
    rentStartDate = RentStartDate,
    paymentDayOfMonth = RentStartDate.getDayOfMonth
  ).withInsurancePolicy("ownerId", "tenantId", CorrectInsuranceAmount)
  private val ContractWithTemporaryInsurancePolicy = createContract(
    rentStartDate = RentStartDate,
    paymentDayOfMonth = RentStartDate.getDayOfMonth
  ).withInsurancePolicy("", "", CorrectInsuranceAmount, isActive = false, Some(StartDate))
    .withTemporaryInsurancePolicy(
      "ownerId",
      "tenantId",
      CorrectInsuranceAmount,
      isActive = true,
      Some(RentStartDate),
      Some(StartDate)
    )
    .withoutVisitTime

  "InsurancePolicyStage" should {
    "process state when contract is active and has no insurance policy" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val contract = createContract(RentStartDate, RentStartDate.getDayOfMonth).withInsurancePolicy(
        "",
        "",
        CorrectInsuranceAmount
      )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      handleShouldProcessMocks(insurancePolicyManager, InsuranceType.Current)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract

      resultContract should equal(ContractWithInsurancePolicy)
    }

    "process state when contract is active and has temporary insurance policy" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val contract = createContract(RentStartDate, RentStartDate.getDayOfMonth)
        .withInsurancePolicy(
          "",
          "",
          CorrectInsuranceAmount,
          isActive = false,
          Some(StartDate)
        )
        .withTemporaryInsurancePolicy(
          "",
          "",
          CorrectInsuranceAmount,
          isActive = false,
          Some(RentStartDate),
          Some(StartDate)
        )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      handleShouldProcessMocks(
        insurancePolicyManager,
        InsuranceType.Temporary,
        ContractWithTemporaryInsurancePolicy
      )
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      val expectedInsurance = ContractWithTemporaryInsurancePolicy.copy(visitTime = Some(StartDate))
      resultContract should equal(expectedInsurance)
    }

    "not process state and reschedule when contract already has temporary insurance policy" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val contract = createContract(RentStartDate, RentStartDate.getDayOfMonth)
        .withInsurancePolicy(
          "",
          "",
          CorrectInsuranceAmount,
          isActive = false,
          Some(StartDate)
        )
        .withTemporaryInsurancePolicy(
          "ownerId",
          "tenantId",
          CorrectInsuranceAmount,
          isActive = true,
          Some(RentStartDate),
          Some(StartDate)
        )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract
      val expectedInsurance = ContractWithTemporaryInsurancePolicy.copy(visitTime = Some(StartDate))
      resultContract.data should equal(expectedInsurance.data)
      resultContract.visitTime should equal(expectedInsurance.visitTime)
    }

    "not process state when contract is not active" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val contract = createContract(
        rentStartDate = RentStartDate,
        paymentDayOfMonth = RentStartDate.getDayOfMonth,
        status = ContractStatus.Draft
      )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract.data

      resultContract should equal(contract.data)
    }

    "reschedule when process time is after contract signing" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val testNow = DateTime.now().withDate(2022, 2, 10)
      val dayAfterToday = testNow.plusDays(1)
      val resultVisitTime = DateTime.now().plusDays(1)
      val contract =
        createContract(RentStartDate, RentStartDate.getDayOfMonth, nowMoment = Some(testNow)).withInsurancePolicy(
          "",
          "",
          CorrectInsuranceAmount,
          isActive = false,
          Some(dayAfterToday)
        )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager)

      resultContract.entry.visitTime.map(_.toLocalDate) shouldBe Some(resultVisitTime.toLocalDate)
    }

    "reschedule when rent start date is after contract signing" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val testNow = DateTime.now().withDate(2022, 2, 10)
      val dayAfterToday = testNow.plusDays(1)
      val resultVisitTime = DateTime.now().plusDays(1)
      val contract =
        createContract(dayAfterToday, dayAfterToday.getDayOfMonth, nowMoment = Some(testNow)).withInsurancePolicy(
          "",
          "",
          CorrectInsuranceAmount
        )
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager)

      resultContract.entry.visitTime.map(_.toLocalDate) shouldBe Some(resultVisitTime.toLocalDate)
    }

    "not process state when contract already has insurance policy" in {
      val insurancePolicyManager = mockInsurancePolicyManager
      val contract = ContractWithInsurancePolicy
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val resultContract = invokeStage(contractWithPayments, insurancePolicyManager).entry.contract.data

      resultContract should equal(contract.data)
    }
  }

  private def handleShouldProcessMocks(
    mockInsurancePolicyManager: InsurancePolicyManager,
    insuranceType: InsuranceType,
    contractWithInsurance: RentContract = ContractWithInsurancePolicy
  ) = {
    (mockInsurancePolicyManager
      .createInsurancePolicy(_: RentContract, _: InsuranceType)(_: Traced))
      .expects(*, insuranceType, *)
      .once()
      .returning(Future.successful(contractWithInsurance))
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
    val stage = new CreateInsurancePolicyStage(insurancePolicyManager, features)
    stage.process(state).futureValue
  }
}
