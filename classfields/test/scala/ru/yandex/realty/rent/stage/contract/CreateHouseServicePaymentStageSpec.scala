package ru.yandex.realty.rent.stage.contract

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.dao.PeriodDao
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.{ContractStatus, PaymentStatus, PaymentType}
import ru.yandex.realty.rent.model.house.services.Period
import ru.yandex.realty.rent.model.{ContractWithPayments, Payment, RentContract}
import ru.yandex.realty.rent.proto.model.house.service.periods.Bill
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CreateHouseServicePaymentStageSpec extends AsyncSpecBase with RentModelsGen with RentPaymentsData {

  private val periodDao = mock[PeriodDao]
  implicit val traced: Traced = Traced.empty

  private val features: Features = new SimpleFeatures

  override def beforeAll(): Unit =
    features.REALTYBACK_8027_CreateHouseServicesPaymentAfterBill.setNewState(true)

  "CreateHouseServicePaymentStage" should {
    "accept active contracts" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)

      val state = ProcessingState(ContractWithPayments(contract, Nil))
      val stage = new CreateHouseServicePaymentStage(periodDao, features)
      stage.shouldProcess(state) shouldBe true
    }

    "not accept non-active contracts" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth, status = ContractStatus.Cancelled)

      val state = ProcessingState(ContractWithPayments(contract, Nil))
      val stage = new CreateHouseServicePaymentStage(periodDao, features)
      stage.shouldProcess(state) shouldBe false
    }

    "not accept active contracts when feature-flag is turned off" in {
      features.REALTYBACK_8027_CreateHouseServicesPaymentAfterBill.setNewState(false)

      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)

      val state = ProcessingState(ContractWithPayments(contract, Nil))
      val stage = new CreateHouseServicePaymentStage(periodDao, features)
      stage.shouldProcess(state) shouldBe false
    }

    "create new payment for actual bill" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)
      val period = periodGen(contract.contractId).map { p =>
        val bill = Bill.getDefaultInstance
        val data = p.data.toBuilder.clearBill().addBill(bill).build()
        p.copy(paymentId = None, data = data)
      }.next

      setupMocks(contract, List(period), 1)
      val contractWithPayments = invokeStage(contract).entry

      contractWithPayments.payments should have length 1
      contractWithPayments.payments.head.status shouldBe PaymentStatus.New
    }

    "not create payments for period without bills" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)
      val period = periodGen(contract.contractId).map { p =>
        val data = p.data.toBuilder.clearBill().build()
        p.copy(paymentId = None, data = data)
      }.next

      setupMocks(contract, List(period))
      val contractWithPayments = invokeStage(contract).entry
      contractWithPayments.payments shouldBe empty
    }

    "not create payments for period without actual bills" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)
      val period = periodGen(contract.contractId).map { p =>
        val bill = Bill.getDefaultInstance.toBuilder.setIsDeleted(true).build()
        val data = p.data.toBuilder.clearBill().addBill(bill).build()
        p.copy(paymentId = None, data = data)
      }.next

      setupMocks(contract, List(period))
      val contractWithPayments = invokeStage(contract).entry
      contractWithPayments.payments shouldBe empty
    }

    "create new payment when period contains payment_id but the payment does not exist" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)
      val period = periodGen(contract.contractId).map { p =>
        val bill = Bill.getDefaultInstance
        val data = p.data.toBuilder.clearBill().addBill(bill).build()
        p.copy(paymentId = Some("does-not-exist"), data = data)
      }.next

      setupMocks(contract, List(period), 1)
      val contractWithPayments = invokeStage(contract).entry

      contractWithPayments.payments should have length 1
      contractWithPayments.payments.head.status shouldBe PaymentStatus.New
      contractWithPayments.payments.head.id should not be period.paymentId
    }

    "update existing payment when payment status is new" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)
      val payment = paymentGen(
        paymentType = Some(PaymentType.HouseServices),
        paymentStatus = Some(PaymentStatus.New)
      ).map { p =>
        val houseServicePayment = p.data.getHouseServicePayment.toBuilder.setBillAmount(50L).build()
        val data = p.data.toBuilder.setHouseServicePayment(houseServicePayment).build()
        p.copy(data = data)
      }.next
      val period = periodGen(contract.contractId).map { p =>
        val bill = Bill.getDefaultInstance.toBuilder.setAmount(100L).build()
        val data = p.data.toBuilder.clearBill().addBill(bill).build()
        p.copy(paymentId = Some(payment.id), data = data)
      }.next

      setupMocks(contract, List(period))
      val contractWithPayments = invokeStage(contract, List(payment)).entry

      contractWithPayments.payments should have length 1
      contractWithPayments.payments.head.status shouldBe PaymentStatus.New
      contractWithPayments.payments.head.data.getHouseServicePayment.getBillAmount shouldBe 100L
    }

    "not update already paid payments" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)
      val payment = paymentGen(
        paymentType = Some(PaymentType.HouseServices),
        paymentStatus = Some(PaymentStatus.PaidByTenant)
      ).map { p =>
        val houseServicePayment = p.data.getHouseServicePayment.toBuilder.setBillAmount(50L).build()
        val data = p.data.toBuilder.setHouseServicePayment(houseServicePayment).build()
        p.copy(data = data)
      }.next
      val period = periodGen(contract.contractId).map { p =>
        val bill = Bill.getDefaultInstance.toBuilder.setAmount(100L).build()
        val data = p.data.toBuilder.clearBill().addBill(bill).build()
        p.copy(paymentId = Some(payment.id), data = data)
      }.next

      setupMocks(contract, List(period))
      val contractWithPayments = invokeStage(contract, List(payment)).entry

      contractWithPayments.payments should have length 1
      contractWithPayments.payments.head.status shouldBe PaymentStatus.PaidByTenant
      contractWithPayments.payments.head.data.getHouseServicePayment.getBillAmount shouldBe 50L
    }

    "preserve rent payments" in {
      val currentMoment = DateTimeUtil.now()
      val contract = createContract(currentMoment, currentMoment.getDayOfMonth)
      val period = periodGen(contract.contractId).map { p =>
        val bill = Bill.getDefaultInstance.toBuilder.setAmount(100L).build()
        val data = p.data.toBuilder.clearBill().addBill(bill).build()
        p.copy(paymentId = None, data = data)
      }.next
      val rentPayment = paymentGen(
        paymentType = Some(PaymentType.Rent),
        paymentStatus = Some(PaymentStatus.New)
      ).map { p =>
        val houseServicePayment = p.data.getRentPayment.toBuilder.setRentAmount(50L).build()
        val data = p.data.toBuilder.setRentPayment(houseServicePayment).build()
        p.copy(data = data)
      }.next

      setupMocks(contract, List(period), 1)
      val contractWithPayments = invokeStage(contract, List(rentPayment)).entry

      contractWithPayments.payments should have length 2

      val rentPaymentOpt = contractWithPayments.payments.find(p => p.`type` == PaymentType.Rent)
      rentPaymentOpt shouldBe defined
      rentPaymentOpt.get.status shouldBe PaymentStatus.New
      rentPaymentOpt.get.data.getRentPayment.getRentAmount shouldBe 50L

      val houseServicePaymentOpt = contractWithPayments.payments.find(p => p.`type` == PaymentType.HouseServices)
      houseServicePaymentOpt shouldBe defined
      houseServicePaymentOpt.get.status shouldBe PaymentStatus.New
      houseServicePaymentOpt.get.data.getHouseServicePayment.getBillAmount shouldBe 100L
    }
  }

  private def setupMocks(contract: RentContract, periods: List[Period], expectedNewPayments: Int = 0): Unit = {
    (periodDao
      .getByContractId(_: String)(_: Traced))
      .expects(contract.contractId, *)
      .once()
      .returning(Future.successful(periods))

    if (expectedNewPayments > 0) {
      (periodDao
        .update(_: String)(_: Period => Period)(_: Traced))
        .expects(*, *, *)
        .onCall { (id, fun, _) =>
          periods
            .find(_.periodId == id)
            .map(fun)
            .map(Future.successful)
            .getOrElse(Future.failed(new NoSuchElementException()))
        }
        .repeat(expectedNewPayments)
    }
  }

  private def invokeStage(
    contract: RentContract,
    payments: List[Payment] = Nil
  ): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(ContractWithPayments(contract, payments))
    val createHouseServicePaymentStage =
      new CreateHouseServicePaymentStage(periodDao, features)
    createHouseServicePaymentStage.doProcess(state).futureValue
  }
}
