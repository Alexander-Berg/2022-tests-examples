package ru.yandex.realty.rent.backend.manager

import com.google.protobuf.Timestamp
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.rent.TestUtil
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.backend.validator.ContractValidator
import ru.yandex.realty.rent.dao.{PaymentDao, RentContractDao}
import ru.yandex.realty.rent.model.enums.ContractStatus.ContractStatus
import ru.yandex.realty.rent.model.enums.PaymentStatus.PaymentStatus
import ru.yandex.realty.rent.model.enums.PaymentType.PaymentType
import ru.yandex.realty.rent.model.enums.{ContractStatus, PaymentStatus, PaymentType}
import ru.yandex.realty.rent.model.{ContractUtils, Payment, RentContract}
import ru.yandex.realty.rent.proto.api.audit.log.StaffUser
import ru.yandex.realty.rent.proto.api.contractual.agreement.ContractualAgreementCreationErrorNamespace.ContractualAgreementCreationError._
import ru.yandex.realty.rent.proto.api.contractual.agreement.ContractualAgreementCreationWarningNamespace.ContractualAgreementCreationWarning._
import ru.yandex.realty.rent.proto.api.contractual.agreement.CreateContractualAgreementRequest.PaymentDayOfMonthMovement
import ru.yandex.realty.rent.proto.api.contractual.agreement.CreateContractualAgreementRequest.RentChange
import ru.yandex.realty.rent.proto.api.contractual.agreement.CreateContractualAgreementResponse.ResultCase
import ru.yandex.realty.rent.proto.api.contractual.agreement.GetContractualAgreementsRestrictionsResponse.Restriction
import ru.yandex.realty.rent.proto.api.contractual.agreement.GetContractualAgreementsRestrictionsResponse.Restriction.RestrictionCase
import ru.yandex.realty.rent.proto.api.contractual.agreement.{
  CreateContractualAgreementRequest,
  CreateContractualAgreementResponse
}
import ru.yandex.realty.rent.proto.model.contract.{ChangedContractFields, ContractualAgreement, TemporaryAmounts}
import ru.yandex.realty.rent.proto.model.payment.FullnessTypeNamespace.FullnessType
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings._

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ContractualAgreementManagerSpec extends AsyncSpecBase with RentPaymentsData {

  import RentPaymentsData._
  import TestUtil._

  "ContractualAgreementManager for payment day of month movement" should {

    "fail for not active contract" in {
      List(
        ContractStatus.Draft -> PAYMENT_DAY_MOVEMENT_NOT_ACTIVE_CONTRACT_ERROR,
        ContractStatus.Cancelled -> PAYMENT_DAY_MOVEMENT_NOT_ACTIVE_CONTRACT_ERROR,
        ContractStatus.Terminated -> PAYMENT_DAY_MOVEMENT_NOT_ACTIVE_CONTRACT_ERROR,
        ContractStatus.FixedTerm -> PAYMENT_DAY_MOVEMENT_FIXED_TERM_CONTRACT_ERROR
      ).foreach {
        case (contractStatus, creationError) =>
          new Wiring with Data {
            val contract: RentContract = mockContract(contractStatus)
            mockDao(contract, Iterable.empty, checkSameData(contract))

            callGetRestrictions().getError shouldEqual creationError
            callCreateAgreement(mockPaymentDayOfMonthMovementRequest()).getError.getError shouldEqual creationError
          }
      }
    }

    "fail for the same payment day of month" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkSameData(contract))

      val request: CreateContractualAgreementRequest = mockPaymentDayOfMonthMovementRequest(paymentDayOfMonth = 10)
      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      callCreateAgreement(request).getError.getError shouldEqual PAYMENT_DAY_MOVEMENT_SAME_DAY_ERROR
    }

    "fail if unpaid short period exists" in new Wiring with Data {
      val contract: RentContract = mockContract(paymentDayOfMonth = 20)
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 2, 19), PaymentStatus.New, InitiallyShortType)
      )
      mockDao(contract, payments, checkSameData(contract))

      val request: CreateContractualAgreementRequest = mockPaymentDayOfMonthMovementRequest(paymentDayOfMonth = 10)
      callGetRestrictions().getError shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_SHORT_PERIOD_ERROR
      callCreateAgreement(request).getError.getError shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_SHORT_PERIOD_ERROR
    }

    "fail if unpaid first period exists" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(mockPayment(contract, TodayDate, dt(2020, 2, 10), PaymentStatus.New))
      mockDao(contract, payments, checkSameData(contract))

      callGetRestrictions().getError shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_FIRST_PERIOD_ERROR
      callCreateAgreement(mockPaymentDayOfMonthMovementRequest()).getError.getError shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_FIRST_PERIOD_ERROR
    }

    "fail if unpaid temporary period exists" in new Wiring with Data {
      val tmpContract: RentContract = mockContract()
      val contract = tmpContract.copy(
        data = tmpContract.data.toBuilder
          .setTemporaryAmounts(TemporaryAmounts.newBuilder().setRentAmount(1234500).setDuration(2))
          .build()
      )
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 10), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 11), dt(2020, 3, 10), PaymentStatus.New)
      )
      mockDao(contract, payments, checkSameData(contract))

      callGetRestrictions().getError shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_TEMPORARY_PERIOD_ERROR
      callCreateAgreement(mockPaymentDayOfMonthMovementRequest()).getError.getError shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_TEMPORARY_PERIOD_ERROR
    }

    "fail if create agreement with agreement date in a future" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkSameData(contract))
      val request: CreateContractualAgreementRequest =
        mockPaymentDayOfMonthMovementRequest(contractualAgreementDate = TodayDate.plusDays(1))

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      callCreateAgreement(request).getError.getError shouldEqual FUTURE_AGREEMENT_DATE_ERROR
    }

    "fail if create agreement with agreement date is equal to a unpaid full period start date" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(
          contract,
          TodayDate.minusDays(1).minusMonths(2),
          TodayDate.minusDays(2).minusMonths(1),
          PaymentStatus.PaidToOwner
        ),
        mockPayment(
          contract,
          TodayDate.minusDays(1).minusMonths(1),
          TodayDate.minusDays(2),
          PaymentStatus.PaidOutUnderGuarantee
        )
      )
      mockDao(contract, payments, checkSameData(contract))
      val request: CreateContractualAgreementRequest =
        mockPaymentDayOfMonthMovementRequest(contractualAgreementDate = TodayDate.minusDays(1))

      callGetRestrictions().getWarning shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_FULL_PERIOD_WARNING
      callCreateAgreement(request).getError.getError shouldEqual AGREEMENT_DATE_IS_AFTER_UNPAID_PERIOD_ERROR
    }

    "fail if create agreement with agreement date is after a unpaid full period start date" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(
          contract,
          TodayDate.minusDays(1).minusMonths(2),
          TodayDate.minusDays(2).minusMonths(1),
          PaymentStatus.PaidToOwner
        ),
        mockPayment(
          contract,
          TodayDate.minusDays(1).minusMonths(1),
          TodayDate.minusDays(2),
          PaymentStatus.PaidOutUnderGuarantee
        )
      )
      mockDao(contract, payments, checkSameData(contract))
      val request: CreateContractualAgreementRequest =
        mockPaymentDayOfMonthMovementRequest(contractualAgreementDate = TodayDate)

      callGetRestrictions().getWarning shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_FULL_PERIOD_WARNING
      callCreateAgreement(request).getError.getError shouldEqual AGREEMENT_DATE_IS_AFTER_UNPAID_PERIOD_ERROR
    }

    "create agreement when all conditions checked" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkNewPaymentDayOfMonthAgreementCreated(contract, TodayDate, dt(2020, 2, 10)))

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      val response: CreateContractualAgreementResponse = callCreateAgreement(mockPaymentDayOfMonthMovementRequest())
      checkNewPaymentDayOfMonthCreateResponse(response, dt(2020, 2, 10))
    }

    "create agreement with warning if unpaid full period exists" in new Wiring with Data {
      val contract: RentContract = mockContract(nowMoment = dt(2020, 2, 15))
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkNewPaymentDayOfMonthAgreementCreated(contract, TodayDate, dt(2020, 2, 10)))

      callGetRestrictions().getWarning shouldEqual PAYMENT_DAY_MOVEMENT_UNPAID_FULL_PERIOD_WARNING
      val response: CreateContractualAgreementResponse = callCreateAgreement(mockPaymentDayOfMonthMovementRequest())
      checkNewPaymentDayOfMonthCreateResponse(response, dt(2020, 2, 10))
    }

    "create second agreement" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val historyItem: ChangedContractFields = createContractHistoryItem(contract, TodayDate, dt(2020, 2, 10), 15)
      val contractWithHistory: RentContract = contract.withHistory(historyItem)
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contractWithHistory, dt(2020, 2, 10), dt(2020, 2, 14), PaymentStatus.PaidToOwner),
        mockPayment(contractWithHistory, dt(2020, 2, 15), dt(2020, 3, 14), PaymentStatus.New)
      )
      mockDao(
        contractWithHistory,
        payments,
        checkNewPaymentDayOfMonthAgreementCreated(contractWithHistory, dt(2020, 2, 10), dt(2020, 2, 15))
      )

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      val response: CreateContractualAgreementResponse = callCreateAgreement(mockPaymentDayOfMonthMovementRequest())
      checkNewPaymentDayOfMonthCreateResponse(response, dt(2020, 2, 15))
    }

    "fail if create agreement that start while there are active temporary rent change agreement" in new Wiring
    with Data {
      val contract: RentContract = mockContract()
      val contractWithTemporaryRentChange: RentContract =
        contract.copy(
          data = contract.data.toBuilder
            .setTemporaryAmounts(
              TemporaryAmounts
                .newBuilder()
                .setRentAmount(RentAmount + 100)
                .setStartDate(DateTimeFormat.write(TodayDate))
                .setDuration(3)
            )
            .build()
        )
      val payments = Iterable(
        mockPayment(contractWithTemporaryRentChange, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contractWithTemporaryRentChange, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contractWithTemporaryRentChange, payments, checkSameData(contractWithTemporaryRentChange))
      val request: CreateContractualAgreementRequest = mockPaymentDayOfMonthMovementRequest()

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.ERROR
      callCreateAgreement(request).getError.getError shouldEqual TEMPORAL_RENT_CHANGE_IS_ACTIVE_ERROR
    }
  }

  "ContractualAgreementManager for rent change" should {
    "fail if create agreement with agreement date in a future" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkSameData(contract))
      val request: CreateContractualAgreementRequest =
        mockRentChangeRequest(NewRentAmount, contractualAgreementDate = TodayDate.plusDays(1))

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      callCreateAgreement(request).getError.getError shouldEqual FUTURE_AGREEMENT_DATE_ERROR
    }

    "fail if create agreement with rent amount is equal to previous amount" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val contractWithAgreement: RentContract =
        contract.copy(
          data = contract.data.toBuilder
            .addContractualAgreements(
              mockRentChangeAgreement(NewRentAmount, 0, TodayDate.minusDays(10), TodayDate.minusDays(9))
            )
            .addContractualAgreements(
              mockRentChangeAgreement(LastRentAmount, NumberOfMonths, TodayDate.minusDays(5), TodayDate.minusDays(2))
            )
            .build()
        )
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkSameData(contract))
      val request: CreateContractualAgreementRequest =
        mockRentChangeRequest(RentAmount, contractualAgreementDate = TodayDate.minusDays(1))

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      callCreateAgreement(request).getError.getError shouldEqual RENT_CHANGE_INVALID_AMOUNT_ERROR
    }

    "fail if create agreement with invalid rent amount" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkSameData(contract))
      val request: CreateContractualAgreementRequest =
        mockRentChangeRequest(-RentAmount, contractualAgreementDate = TodayDate.minusDays(1))

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      callCreateAgreement(request).getError.getError shouldEqual RENT_CHANGE_INVALID_AMOUNT_ERROR
    }

    "fail if create agreement with invalid number of months" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val payments = Iterable(
        mockPayment(contract, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contract, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contract, payments, checkSameData(contract))
      val request: CreateContractualAgreementRequest =
        mockRentChangeRequest(NewRentAmount, Some(-NumberOfMonths), TodayDate.minusDays(1))

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      callCreateAgreement(request).getError.getError shouldEqual RENT_CHANGE_INVALID_NUMBER_OF_MONTHS_ERROR
    }

    "fail if create agreement while there are active rent change agreement" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val contractWithTemporaryRentChange: RentContract =
        contract.copy(
          data = contract.data.toBuilder
            .setTemporaryAmounts(
              TemporaryAmounts
                .newBuilder()
                .setRentAmount(RentAmount + 100)
                .setStartDate(DateTimeFormat.write(TodayDate))
                .setDuration(5)
            )
            .build()
        )
      val payments = Iterable(
        mockPayment(contractWithTemporaryRentChange, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contractWithTemporaryRentChange, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(contractWithTemporaryRentChange, payments, checkSameData(contractWithTemporaryRentChange))
      val request: CreateContractualAgreementRequest =
        mockRentChangeRequest(NewRentAmount, contractualAgreementDate = TodayDate.minusDays(1))

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.ERROR
      callCreateAgreement(request).getError.getError shouldEqual TEMPORAL_RENT_CHANGE_IS_ACTIVE_ERROR
    }

    "create agreement with permanent when all conditions checked" in new Wiring with Data {
      val contract: RentContract = mockContract()
      val contractWithAgreement: RentContract =
        contract.copy(
          data = contract.data.toBuilder
            .addContractualAgreements(
              mockRentChangeAgreement(NewRentAmount, 0, TodayDate.minusDays(10), TodayDate.minusDays(9))
            )
            .build()
        )
      val payments = Iterable(
        mockPayment(contractWithAgreement, TodayDate, dt(2020, 2, 9), PaymentStatus.PaidToOwner),
        mockPayment(contractWithAgreement, dt(2020, 2, 10), dt(2020, 3, 9), PaymentStatus.New)
      )
      mockDao(
        contractWithAgreement,
        payments,
        checkRentChangeAgreementCreated(contractWithAgreement, TodayDate, dt(2020, 2, 10))
      )
      val request: CreateContractualAgreementRequest =
        mockRentChangeRequest(LastRentAmount, NumberOfMonths, TodayDate)

      callGetRestrictions().getRestrictionCase shouldEqual RestrictionCase.NO_RESTRICTION
      val response: CreateContractualAgreementResponse = callCreateAgreement(request)
      checkRentChangeCreateResponse(LastRentAmount, NumberOfMonths, response, dt(2020, 2, 10))
    }
  }

  trait Wiring {
    self: Data =>

    implicit val traced: Traced = Traced.empty
    val features = new SimpleFeatures

    private val contractDao: RentContractDao = mock[RentContractDao]
    private val paymentDao: PaymentDao = mock[PaymentDao]

    private val contractValidator = new ContractValidator(features)
    private val manager: ContractualAgreementManager =
      new ContractualAgreementManager(contractDao, paymentDao, contractValidator, features)

    private type UpdateFuture = Future[(RentContract, CreateContractualAgreementResponse)]

    def mockDao(contract: RentContract, payments: Iterable[Payment], contractChecks: RentContract => Unit): Unit = {
      (contractDao
        .findById(_: String)(_: Traced))
        .expects(contract.id, *)
        .returning(Future.successful(contract))
        .once()
      (contractDao
        .updateFR(_: String)(_: RentContract => Future[(RentContract, CreateContractualAgreementResponse)])(_: Traced))
        .expects(contract.id, *, *)
        .onCall(updateHandler(contract, contractChecks))
        .once()
      (paymentDao
        .getContractPayments(_: String, _: PaymentType)(_: Traced))
        .expects(contract.id, PaymentType.Rent, *)
        .returning(Future.successful(payments))
        .twice()
    }

    private def updateHandler(
      contract: RentContract,
      contractChecks: RentContract => Unit
    ): (String, RentContract => UpdateFuture, Traced) => UpdateFuture =
      (_: String, action: RentContract => UpdateFuture, _) => {
        val result = action(contract).futureValue
        contractChecks(result._1)
        Future.successful(result)
      }

    def callGetRestrictions(): Restriction =
      manager
        .getContractualAgreementsRestrictions(FlatId, ContractId)
        .futureValue
        .getPaymentDayOfMonthMovementRestriction

    def callCreateAgreement(request: CreateContractualAgreementRequest): CreateContractualAgreementResponse = {
      if (request.hasPaymentDayOfMonthMovement) {
        features.AllowPaymentDayOfMonthMovement.setNewState(true)
      }
      manager.createContractualAgreement(FlatId, ContractId, request).futureValue
    }

    def checkSameData(initialContract: RentContract): RentContract => Unit =
      (updatedContract: RentContract) => {
        updatedContract.data shouldEqual initialContract.data
      }

    def checkNewPaymentDayOfMonthAgreementCreated(
      initialContract: RentContract,
      previousStartDate: DateTime,
      startDate: DateTime
    ): RentContract => Unit =
      (updatedContract: RentContract) => {
        updatedContract.data.getChangeHistoryList.size shouldEqual initialContract.data.getChangeHistoryList.size + 1
        val newHistoryItem = updatedContract.data.getChangeHistoryList.asScala.last
        newHistoryItem.getActualFromDate shouldEqual (previousStartDate: Timestamp)
        newHistoryItem.getActualToDate shouldEqual (startDate: Timestamp)
        newHistoryItem.getFields.getPaymentDayOfMonth shouldEqual 10

        updatedContract.data.getContractualAgreementsList.size shouldEqual
          initialContract.data.getContractualAgreementsList.size + 1
        val newAgreement = updatedContract.data.getContractualAgreementsList.asScala.last
        newAgreement.getContractualAgreementNumber shouldEqual AgreementNumber
        newAgreement.getContractualAgreementDate shouldEqual (TodayDate: Timestamp)
        newAgreement.hasCreateDate shouldEqual true
        newAgreement.getStartDate shouldEqual (startDate: Timestamp)
        newAgreement.getResponsibleUser shouldEqual StaffManager
        newAgreement.getPaymentDayOfMonthMovement.getNewPaymentDayOfMonth shouldEqual NewPaymentDayOfMonth
      }

    def checkRentChangeAgreementCreated(
      initialContract: RentContract,
      previousStartDate: DateTime,
      startDate: DateTime
    ): RentContract => Unit =
      (updatedContract: RentContract) => {
        updatedContract.data.getChangeHistoryList.size shouldEqual initialContract.data.getChangeHistoryList.size + 1
        val newHistoryItem = updatedContract.data.getChangeHistoryList.asScala.last
        newHistoryItem.getActualFromDate shouldEqual (previousStartDate: Timestamp)
        newHistoryItem.getActualToDate shouldEqual (startDate: Timestamp)
        newHistoryItem.getFields.getPaymentDayOfMonth shouldEqual 10

        updatedContract.data.getContractualAgreementsList.size shouldEqual
          initialContract.data.getContractualAgreementsList.size + 1
        val newAgreement = updatedContract.data.getContractualAgreementsList.asScala.last
        newAgreement.getContractualAgreementNumber shouldEqual AgreementNumber
        newAgreement.getContractualAgreementDate shouldEqual (TodayDate: Timestamp)
        newAgreement.hasCreateDate shouldEqual true
        newAgreement.getStartDate shouldEqual (startDate: Timestamp)
        newAgreement.getResponsibleUser shouldEqual StaffManager
        newAgreement.getRentChange.getRentAmount shouldEqual LastRentAmount
        newAgreement.getRentChange.getNumberOfMonths shouldEqual NumberOfMonths
      }

    def checkNewPaymentDayOfMonthCreateResponse(
      response: CreateContractualAgreementResponse,
      startDate: DateTime
    ): Unit = {
      response.getResultCase shouldEqual ResultCase.SUCCESS
      val agreement = response.getSuccess.getContractualAgreement
      agreement.getContractualAgreementId.nonEmpty shouldEqual true
      agreement.getContractualAgreementNumber shouldEqual AgreementNumber
      agreement.getContractualAgreementDate shouldEqual (TodayDate: Timestamp)
      agreement.hasCreateDate shouldEqual true
      agreement.getStartDate shouldEqual (startDate: Timestamp)
      agreement.getResponsibleUser shouldEqual StaffManager
      agreement.getPaymentDayOfMonthMovement.getNewPaymentDayOfMonth shouldEqual NewPaymentDayOfMonth
    }

    def checkRentChangeCreateResponse(
      rentAmount: Int,
      numberOfMonths: Int,
      response: CreateContractualAgreementResponse,
      startDate: DateTime
    ): Unit = {
      response.getResultCase shouldEqual ResultCase.SUCCESS
      val agreement = response.getSuccess.getContractualAgreement
      agreement.getContractualAgreementId.nonEmpty shouldEqual true
      agreement.getContractualAgreementNumber shouldEqual AgreementNumber
      agreement.getContractualAgreementDate shouldEqual (TodayDate: Timestamp)
      agreement.hasCreateDate shouldEqual true
      agreement.getStartDate shouldEqual (startDate: Timestamp)
      agreement.getResponsibleUser shouldEqual StaffManager
      agreement.getRentChange.getRentAmount shouldEqual rentAmount
      agreement.getRentChange.getNumberOfMonths shouldEqual numberOfMonths
    }
  }

  trait Data {

    def mockContract(
      status: ContractStatus = ContractStatus.Active,
      paymentDayOfMonth: Int = 10,
      nowMoment: DateTime = TodayDate
    ): RentContract =
      createContract(TodayDate, paymentDayOfMonth = paymentDayOfMonth, nowMoment = Some(nowMoment))
        .copy(status = status)

    def mockPayment(
      contract: RentContract,
      startDate: DateTime,
      endDate: DateTime,
      status: PaymentStatus,
      fullnessType: FullnessType = FullnessType.FULL
    ): Payment =
      createPayment(contract, startDate, endDate, fullnessType)
        .copy(status = status)

    val StaffManager: StaffUser =
      StaffUser
        .newBuilder()
        .setUid(123)
        .setLogin("staff-login")
        .setName("staff-name")
        .build()

    val AgreementNumber = "123-456"
    val NewPaymentDayOfMonth = 20

    def mockPaymentDayOfMonthMovementRequest(
      paymentDayOfMonth: Int = NewPaymentDayOfMonth,
      contractualAgreementDate: DateTime = TodayDate
    ): CreateContractualAgreementRequest =
      CreateContractualAgreementRequest
        .newBuilder()
        .setContractualAgreementNumber(AgreementNumber)
        .setContractualAgreementDate(contractualAgreementDate)
        .setResponsibleUser(StaffManager)
        .setPaymentDayOfMonthMovement(PaymentDayOfMonthMovement.newBuilder().setNewPaymentDayOfMonth(paymentDayOfMonth))
        .build()

    def mockRentChangeRequest(
      rentAmount: Int,
      numberOfMonths: Option[Int] = None,
      contractualAgreementDate: DateTime = TodayDate
    ): CreateContractualAgreementRequest =
      CreateContractualAgreementRequest
        .newBuilder()
        .setContractualAgreementNumber(AgreementNumber)
        .setContractualAgreementDate(contractualAgreementDate)
        .setResponsibleUser(StaffManager)
        .setRentChange(
          RentChange
            .newBuilder()
            .setRentAmount(rentAmount)
            .applyTransforms[Int](numberOfMonths, (b, n) => b.setNumberOfMonths(n))
        )
        .build()

    def mockRentChangeAgreement(
      rentAmount: Int,
      numberOfMonths: Int = 0,
      contractualAgreementDate: DateTime = TodayDate,
      startDate: DateTime = TodayDate
    ): ContractualAgreement =
      ContractualAgreement
        .newBuilder()
        .setContractualAgreementId(ContractUtils.generateContractualAgreementId())
        .setContractualAgreementNumber(AgreementNumber)
        .setContractualAgreementDate(contractualAgreementDate)
        .setCreateDate(DateTime.now())
        .setStartDate(startDate)
        .setRentChange {
          ContractualAgreement.RentChange
            .newBuilder()
            .setRentAmount(rentAmount)
            .setNumberOfMonths(numberOfMonths)
        }
        .build()
  }

  def mockChangeHistory(
    rentAmount: Int,
    numberOfMonths: Int = 0,
    startDate: DateTime = TodayDate,
    endDate: DateTime = TodayDate.plusMonths(1)
  ): ChangedContractFields =
    ChangedContractFields
      .newBuilder()
      .setActualFromDate(DateTimeFormat.write(startDate))
      .setActualToDate(DateTimeFormat.write(endDate))
      .setFields(
        ChangedContractFields.Fields
          .newBuilder()
          .setTemporaryAmounts(
            TemporaryAmounts
              .newBuilder()
              .setRentAmount(rentAmount)
              .setStartDate(DateTimeFormat.write(startDate))
              .setDuration(numberOfMonths)
          )
      )
      .build()
}
