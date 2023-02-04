package ru.yandex.realty.rent.backend.manager

import com.google.protobuf.Int32Value
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.realty.cashbox.proto.receipt.{
  Receipt,
  ReceiptContentRow,
  ReceiptType,
  UserRequisite,
  Payment => ProtoPayment
}
import ru.yandex.realty.clients.cashbox.CashboxClient
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.realty.model.receipt.ReceiptConst
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.backend.manager.flat.FlatSearcher
import ru.yandex.realty.rent.backend.manager.house.services.PeriodManager
import ru.yandex.realty.rent.backend.payment.HouseServicePaymentsGenerator
import ru.yandex.realty.clients.tinkoff.eacq.TinkoffEACQClient
import ru.yandex.realty.clients.tinkoff.eacq.customer.{GetCustomerRequest, GetCustomerResponse}
import ru.yandex.realty.clients.tinkoff.eacq.init.{InitRequest, InitResponse}
import ru.yandex.realty.rent.dao.{PaymentDao, PeriodDao, RentContractDao, UserDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.{BillStatus, PaymentStatus, PaymentType, Role}
import ru.yandex.realty.rent.model.house.services.Period
import ru.yandex.realty.rent.model.{ContractTerms, Flat, Payment, RentContract, User}
import ru.yandex.realty.rent.proto.api.payment.RentPaymentInitResponse.ResultCase
import ru.yandex.realty.rent.proto.api.payment.{RentPaymentInitRequest, RentPaymentInitResponse}
import ru.yandex.realty.rent.proto.model.house.service.periods.Bill
import ru.yandex.realty.rent.proto.model.payment.ReceiptIncomeTypeNamespace.ReceiptIncomeType
import ru.yandex.realty.rent.proto.model.payment.ReceiptRentTypeNamespace.ReceiptRentType
import ru.yandex.realty.rent.proto.model.payment.{PaymentData, ReceiptInfo}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.vertis.protobuf.BasicProtoFormats
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class DefaultPaymentManagerSpec
  extends SpecBase
  with AsyncSpecBase
  with RequestAware
  with ScalaCheckPropertyChecks
  with SearcherResponseModelGenerators
  with TracedLogging {

  "DefaultPaymentManager.updatePaymentStatus" should {

    "return expected payment with referenced receipt" in new Wiring with Data {
      val payment: Payment =
        buildPayment(uidGen().next, ReceiptInfo.getDefaultInstance)
      val receiptId: String = uidGen().next
      val amount: Int = Gen.posNum[Int].next
      val receiptContentRow: ReceiptContentRow =
        buildReceiptContentRow(receiptId, amount, ReceiptConst.AgentPaymentIdName, ReceiptType.RECEIPT_TYPE_INCOME)
      (mockCashboxClient
        .getReceipt(_: String)(_: Traced))
        .expects(receiptId, *)
        .returning(Future.successful(Some(receiptContentRow)))
      val actualPayment: Payment = paymentManager
        .referencePaymentReceipt(receiptId, payment)
        .futureValue
      val expectedPayment: Payment = payment.copy(
        data = payment.data.toBuilder
          .addReceipts(
            ReceiptInfo
              .newBuilder()
              .setReceiptId(receiptId)
              .setReceiptRentType(ReceiptRentType.AGENT)
              .setReceiptIncomeType(ReceiptIncomeType.INCOME)
              .setAmount(amount)
              .setCreateTime(receiptContentRow.getCreateDate)
              .build()
          )
          .build()
      )
      // check results
      actualPayment shouldEqual expectedPayment
    }

    "throw IllegalArgumentException for referenced receipt" in new Wiring with Data {
      val receiptId: String = uidGen().next
      val amount: Int = Gen.posNum[Int].next
      val receiptContentRow: ReceiptContentRow = buildReceiptContentRow(
        receiptId,
        amount,
        ReceiptConst.CommissionPaymentIdName,
        ReceiptType.RECEIPT_TYPE_RETURN_INCOME
      )
      val receiptInfo: ReceiptInfo = ReceiptInfo
        .newBuilder()
        .setReceiptId(receiptId)
        .setReceiptRentType(ReceiptRentType.COMMISSION)
        .setReceiptIncomeType(ReceiptIncomeType.RETURN_INCOME)
        .setAmount(amount)
        .setCreateTime(receiptContentRow.getCreateDate)
        .build()
      val payment: Payment = buildPayment(uidGen().next, receiptInfo)
      (mockCashboxClient
        .getReceipt(_: String)(_: Traced))
        .expects(receiptId, *)
        .returning(Future.successful(Some(receiptContentRow)))
      val err: IllegalArgumentException = interceptCause[IllegalArgumentException] {
        paymentManager.referencePaymentReceipt(receiptId, payment).futureValue
      }
      err.getMessage shouldEqual s"Payment already has been referenced with receipt $receiptId"
    }

    "throw IllegalArgumentException for not found receipt" in new Wiring with Data {
      val receiptId: String = uidGen().next
      val amount: Int = Gen.posNum[Int].next
      val receiptContentRow: ReceiptContentRow =
        buildReceiptContentRow(
          receiptId,
          amount,
          ReceiptConst.CommissionPrepaymentIdName,
          ReceiptType.RECEIPT_TYPE_UNKNOWN
        )
      val receiptInfo: ReceiptInfo = ReceiptInfo
        .newBuilder()
        .setReceiptId(receiptId)
        .setReceiptRentType(ReceiptRentType.COMMISSION_PREPAYMENT)
        .setReceiptIncomeType(ReceiptIncomeType.UNKNOWN)
        .setAmount(amount)
        .setCreateTime(receiptContentRow.getCreateDate)
        .build()
      val payment: Payment = buildPayment(uidGen().next, receiptInfo)
      (mockCashboxClient
        .getReceipt(_: String)(_: Traced))
        .expects(receiptId, *)
        .returning(Future.successful(None))
      val err: IllegalArgumentException = interceptCause[IllegalArgumentException] {
        paymentManager.referencePaymentReceipt(receiptId, payment).futureValue
      }
      err.getMessage shouldEqual s"No receipt with $receiptId found"
    }
  }

  "DefaultPaymentManager.init" should {
    "succeed on valid request" in new InitPaymentData {
      val response: RentPaymentInitResponse = runInit

      response.getResultCase shouldBe ResultCase.SUCCESS
    }

    "return error when house service payment is outdated" in new InitPaymentData {
      override val finalPeriod: Period = periodGen(contract.contractId).map { p =>
        val bill = Bill.getDefaultInstance.toBuilder.setAmount(120L).build()
        val data = p.data.toBuilder.clearBill().addBill(bill).build()
        p.copy(paymentId = None, billStatus = BillStatus.ShouldBePaid, data = data)
      }.next

      override val isRentPayment = false
      val response: RentPaymentInitResponse = runInit

      response.getResultCase shouldBe ResultCase.ERROR
      response.getError.getShortMessage shouldBe "Internal server error"
      response.getError.getDetails should startWith("Outdated payment")
    }
  }

  trait Wiring {
    val mockContractDao: RentContractDao = mock[RentContractDao]
    val mockUserDao: UserDao = mock[UserDao]
    val mockPaymentDao: PaymentDao = mock[PaymentDao]
    val mockTinkoffClient: TinkoffEACQClient = mock[TinkoffEACQClient]
    val mockCashboxClient: CashboxClient = mock[CashboxClient]
    val mockFlatSearcher: FlatSearcher = mock[FlatSearcher]
    val mockPeriodDao: PeriodDao = mock[PeriodDao]
    val mockPeriodManager: PeriodManager = mock[PeriodManager]
    val features = new SimpleFeatures

    val paymentManager = new DefaultPaymentManager(
      mockUserDao,
      mockContractDao,
      mockPaymentDao,
      mockPeriodDao,
      mockTinkoffClient,
      mockTinkoffClient,
      mockCashboxClient,
      mockFlatSearcher,
      mockPeriodManager,
      "",
      features
    )
  }

  trait Data extends BasicProtoFormats {

    def buildPayment(paymentId: String, receiptInfo: ReceiptInfo): Payment =
      Payment(
        paymentId,
        uidGen().next,
        PaymentType.Rent,
        isPaidOutUnderGuarantee = false,
        DateTimeUtil.now(),
        DateTimeUtil.now(),
        DateTimeUtil.now(),
        PaymentStatus.New,
        PaymentData.newBuilder().addReceipts(receiptInfo).build(),
        createTime = DateTimeUtil.now(),
        updateTime = DateTimeUtil.now()
      )

    def buildReceiptContentRow(
      receiptId: String,
      amount: Int,
      idName: String,
      receiptType: ReceiptType
    ): ReceiptContentRow = {
      val payment: ProtoPayment = ProtoPayment
        .newBuilder()
        .setAmount(Int32Value.newBuilder().setValue(amount).build())
        .build()
      val userRequisite = UserRequisite.newBuilder().setName(idName).build()
      ReceiptContentRow
        .newBuilder()
        .setReceiptId(receiptId)
        .setReceiptRequest(
          Receipt
            .newBuilder()
            .setReceiptId(receiptId)
            .setReceiptType(receiptType)
            .setAdditionalUserRequisite(userRequisite)
            .addPayments(payment)
            .build()
        )
        .setCreateDate(DateTimeFormat.write(DateTimeUtil.now()))
        .build()
    }
  }

  trait InitPaymentData extends Wiring with Data with RentModelsGen with RentPaymentsData {
    val currentMoment: DateTime = DateTimeUtil.now()
    val contract: RentContract = createContract(currentMoment, currentMoment.getDayOfMonth)

    val initialPeriod: Period = periodGen(contract.contractId).map { p =>
      val bill = Bill.getDefaultInstance.toBuilder.setAmount(100L).build()
      val data = p.data.toBuilder.clearBill().addBill(bill).build()
      p.copy(paymentId = None, billStatus = BillStatus.ShouldBePaid, data = data)
    }.next
    val payment: Payment = HouseServicePaymentsGenerator(contract).generateForPeriod(initialPeriod)

    val isRentPayment = true

    val finalPeriod: Period = initialPeriod

    val user: User = userGen().next

    val flat: Flat = flatGen().map { f =>
      f.copy(assignedUsers = f.assignedUsers + (Role.Tenant -> List(user)))
    }.next
    val request: RentPaymentInitRequest = RentPaymentInitRequest.getDefaultInstance

    (mockFlatSearcher
      .findFlatWithUserByRoles(_: String, _: Long, _: Set[Role.Role])(_: Traced))
      .expects(flat.flatId, user.uid, *, *)
      .returning(Future.successful(Some((flat, user, Role.Tenant))))
    (mockContractDao
      .findByFlatIds(_: Iterable[String])(_: Traced))
      .expects(Set(flat.flatId), *)
      .returning(Future.successful(Seq(contract)))
    (mockPaymentDao
      .get(_: String)(_: Traced))
      .expects(payment.id, *)
      .returning(Future.successful(Some(payment)))
    (mockPeriodDao
      .findByPaymentId(_: String)(_: Traced))
      .expects(payment.id, *)
      .onCall(_ => Future.successful(finalPeriod))
    if (isRentPayment) {
      (mockUserDao
        .updateAcceptedTenantTermsVersion(_: Long, _: Int, _: Option[DateTime])(_: Traced))
        .expects(user.uid, ContractTerms.latestTerms.version, *, *)
        .onCall(_ => Future.successful(user))
    }
    (mockTinkoffClient
      .getCustomer(_: GetCustomerRequest)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returning(Future.successful(GetCustomerResponse(Success = true, "")))
    (mockTinkoffClient
      .init(_: InitRequest)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returning(
        Future.successful(
          InitResponse(
            Success = true,
            "",
            PaymentURL = Some("https://payment.url"),
            PaymentId = Some(readableString.next)
          )
        )
      )
    (mockPaymentDao
      .update(_: String)(_: Payment => Payment)(_: Traced))
      .expects(*, *, *)
      .anyNumberOfTimes()
      .returning(Future.successful(payment))
    (mockPeriodDao
      .update(_: String)(_: Period => Period)(_: Traced))
      .expects(*, *, *)
      .anyNumberOfTimes()
      .returning(Future.successful(finalPeriod))
    (mockContractDao
      .refresh(_: String)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returns(Future.successful(contract))

    def runInit: RentPaymentInitResponse =
      paymentManager.init(PassportUser(user.uid), flat.flatId, payment.id, request).futureValue
  }
}
