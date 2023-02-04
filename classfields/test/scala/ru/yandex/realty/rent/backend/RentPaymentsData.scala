package ru.yandex.realty.rent.backend

import com.google.protobuf.Timestamp
import org.joda.time.DateTime
import ru.yandex.realty.model.gen.RealtyGenerators
import ru.yandex.realty.rent.TestUtil
import ru.yandex.realty.rent.backend.payment.periods.GenericRentPaymentPeriod
import ru.yandex.realty.rent.backend.payment.{
  ConditionPeriod,
  DefaultConditionPeriodsGenerator,
  DefaultPaymentAmountsBuilder,
  LastConditionPeriod,
  PaymentTemplateApplier,
  RentPaymentConditions,
  RentPaymentTemplate,
  TemporaryRentConditions,
  TerminationAttributes
}
import ru.yandex.realty.rent.model.enums.ContractStatus
import ru.yandex.realty.rent.model.enums.ContractStatus.ContractStatus
import ru.yandex.realty.rent.model.{ContractParticipant, Payment, RentContract}
import ru.yandex.realty.rent.proto.model.contract.CalculationStrategyNamespace.CalculationStrategy
import ru.yandex.realty.rent.proto.model.contract.{
  ChangedContractFields,
  Commissions,
  ContractData,
  Insurance,
  TemporaryAmounts
}
import ru.yandex.realty.rent.proto.model.payment.FullnessTypeNamespace.FullnessType
import ru.yandex.realty.rent.proto.model.payment.{TenantPaymentTransactionInfo, TransactionStatusNamespace}
import ru.yandex.realty.rent.util.NowMomentProvider
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings._
import ru.yandex.realty.util.protobuf.ProtobufFormats

import scala.collection.JavaConverters._

trait RentPaymentsData extends RealtyGenerators with ProtobufFormats {
  import TestUtil.dt

  protected val ContractId = "c1"
  protected val FlatId = "f1"
  protected val ContractNumber = "cn1"
  protected val OwnerRequestId = "or1"

  protected val TodayDate: DateTime = dt(2020, 1, 10)
  protected val RentAmount = 1000000
  protected val NewRentAmount = 1001000
  protected val LastRentAmount = 1000050
  protected val RentAmountForInsuranceChanged = 3100000
  protected val InsuranceAmount = 141100
  protected val NumberOfMonths = 10

  protected val DefaultCommissions: Commissions = Commissions
    .newBuilder()
    .setMonthlyTenantCommission(0.05f)
    .setMonthlyTenantHouseServiceCommission(0.0f)
    .build()

  protected val InitiallyShortType = FullnessType.SHORT_INITIALLY_MOVED_PAYMENT_DAY_MONTH
  protected val MovedShortType = FullnessType.SHORT_MOVED_PAYMENT_DAY_OF_MONTH
  protected val TerminationShortType = FullnessType.SHORT_TERMINATION

  def paymentConditions(
    rentStartDate: DateTime = TodayDate,
    paymentDayOfMonth: Option[Int] = None // Considered to be equal to rent start date if None
  ): RentPaymentConditions =
    RentPaymentConditions(
      RentAmount,
      rentStartDate,
      paymentDayOfMonth.getOrElse(rentStartDate.getDayOfMonth),
      InsuranceAmount,
      CalculationStrategy.STRATEGY_2,
      DefaultCommissions
    )

  def conditionsPeriod(
    periodStartDate: DateTime = TodayDate,
    periodEndDate: Option[DateTime] = None,
    rentStartDate: Option[DateTime] = None, // Considered to be equal to period start date if None
    paymentDayOfMonth: Option[Int] = None // Considered to be equal to period start date if None
  ): ConditionPeriod =
    LastConditionPeriod(
      periodStartDate,
      paymentConditions(
        rentStartDate.getOrElse(periodStartDate),
        paymentDayOfMonth.orElse(Some(periodStartDate.getDayOfMonth))
      ),
      periodEndDate.map { d =>
        TerminationAttributes(
          endDate = d,
          notificationDate = periodStartDate,
          tenantRefusedPayFor30Days = false,
          checkOutWithoutAdditionalPayments = true
        )
      }
    )

  def createPayment(
    contract: RentContract,
    startDate: DateTime,
    endDate: DateTime,
    fullnessType: FullnessType = FullnessType.FULL
  ): Payment = {
    val template = createRentPaymentTemplate(contract, startDate, endDate, fullnessType)
    val nowMomentProvider = NowMomentProvider(contract)
    PaymentTemplateApplier.createPaymentByTemplate(template, contract)(Traced.empty, nowMomentProvider)
  }

  def createRentPaymentTemplate(
    contract: RentContract,
    startDate: DateTime,
    endDate: DateTime,
    fullnessType: FullnessType = FullnessType.FULL
  ): RentPaymentTemplate = {
    val conditionPeriod = DefaultConditionPeriodsGenerator.buildConditionPeriods(contract).last
    val dates = GenericRentPaymentPeriod(startDate, endDate.plusDays(1), fullnessType, conditionPeriod)
    val amounts = DefaultPaymentAmountsBuilder.buildPaymentAmounts(
      dates,
      conditionPeriod.conditions,
      Seq.empty
    )
    RentPaymentTemplate(dates, conditionPeriod.conditions, amounts)
  }

  def createTenantPaymentTransactionInfo(paymentDate: DateTime): TenantPaymentTransactionInfo =
    TenantPaymentTransactionInfo
      .newBuilder()
      .setTransactionId("t1")
      .setAmount(RentAmount)
      .setPaymentUrl(uidGen().next)
      .setPaymentDate(DateTimeFormat.write(paymentDate))
      .setStatus(TransactionStatusNamespace.TransactionStatus.CONFIRMED)
      .setPanMask(uidGen().next)
      .build()

  def createContract(
    rentStartDate: DateTime,
    paymentDayOfMonth: Int,
    terminationDate: Option[DateTime] = None,
    temporaryRentConditions: Option[TemporaryRentConditions] = None,
    notificationDate: Option[DateTime] = None,
    nowMoment: Option[DateTime] = None,
    status: ContractStatus = ContractStatus.Active,
    usePayoutUnderGuarantee: Boolean = false
  ): RentContract = {
    val owner = ContractParticipant(uid = Some(1), name = Some("owner"), phone = Some(""), email = Some("email@test"))
    val tenant = ContractParticipant(uid = Some(2), name = Some("tenant"), phone = Some(""), email = Some("email@test"))
    RentContract(
      contractId = ContractId,
      contractNumber = ContractNumber,
      ownerRequestId = Some(OwnerRequestId),
      flatId = FlatId,
      owner = owner,
      tenant = tenant,
      terminationDate = terminationDate,
      status = status,
      data = ContractData
        .newBuilder()
        .setRentAmount(RentAmount)
        .setRentStartDate(DateTimeFormat.write(rentStartDate))
        .setPaymentDayOfMonth(paymentDayOfMonth)
        .setCalculationStrategy(CalculationStrategy.STRATEGY_2)
        .setCommissions(DefaultCommissions)
        .setUsePayoutUnderGuarantee(usePayoutUnderGuarantee)
        .setInsurance(Insurance.newBuilder.setInsuranceAmount(InsuranceAmount))
        .applyTransforms[DateTime](nowMoment, _.setNowMomentForTesting(_))
        .applySideEffects[DateTime](notificationDate, { (builder, nDate) =>
          builder.setTerminatePreventionDate(nDate)
          builder.getTerminationInfoBuilder
            .setNotificationDate(nDate)
        })
        .applySideEffects[DateTime](terminationDate, { (builder, _) =>
          builder.getTerminationInfoBuilder
            .setTenantRefusedPayFor30Days(true)
            .setCheckOutWithoutAdditionalPayments(false)
        })
        .applySideEffects[TemporaryRentConditions](
          temporaryRentConditions,
          (builder, conditions) =>
            builder.setTemporaryAmounts(
              TemporaryAmounts.newBuilder().setRentAmount(conditions.rentAmount).setDuration(conditions.duration)
            )
        )
        .build(),
      createTime = DateTime.now(),
      updateTime = DateTime.now(),
      visitTime = Some(DateTime.now()),
      shardKey = 1
    )
  }

  def createContractHistoryItem(
    contract: RentContract,
    fromDate: DateTime,
    toDate: DateTime,
    paymentDayOfMonth: Int
  ): ChangedContractFields =
    ChangedContractFields
      .newBuilder()
      .setActualFromDate(fromDate)
      .setActualToDate(toDate)
      .setFields {
        ChangedContractFields.Fields
          .newBuilder()
          .setRentStartDate(contract.data.getRentStartDate)
          .setPaymentDayOfMonth(paymentDayOfMonth)
          .setRentAmount(contract.data.getRentAmount)
          .setInsurance(Insurance.newBuilder().setInsuranceAmount(contract.data.getInsurance.getInsuranceAmount))
          .setCommissions(contract.data.getCommissions)
          .setCalculationStrategy(contract.data.getCalculationStrategy)
      }
      .build()
}

object RentPaymentsData {

  implicit class RichTestContract(val contract: RentContract) extends AnyVal {

    def withHistory(historyItems: ChangedContractFields*): RentContract = {
      val contractDataWithHistory = contract.data.toBuilder
        .addAllChangeHistory(historyItems.asJava)
        .build()
      contract.copy(data = contractDataWithHistory)
    }

    def withPaymentDayOfMonth(paymentDayOfMonth: Int): RentContract = {
      val updatedData = contract.data.toBuilder
        .setPaymentDayOfMonth(paymentDayOfMonth)
        .build()
      contract.copy(data = updatedData)
    }

    def withInsurancePolicy(
      ownerSubscriptionId: String,
      tenantSubscriptionId: String,
      insuranceAmount: Long,
      isActive: Boolean = true,
      policyDateOpt: Option[Timestamp] = None,
      terminationPolicyDateOpt: Option[Timestamp] = None
    ): RentContract = {
      val insurancePolicy = buildInsurance(
        ownerSubscriptionId,
        tenantSubscriptionId,
        insuranceAmount,
        isActive,
        policyDateOpt.getOrElse(contract.data.getRentStartDate),
        terminationPolicyDateOpt
      )
      val contractDataWithInsurancePolicy = contract.data.toBuilder.setInsurance(insurancePolicy).build()
      contract.copy(data = contractDataWithInsurancePolicy)
    }

    def withPreviousInsurancePolicy(
      ownerSubscriptionId: String,
      tenantSubscriptionId: String,
      insuranceAmount: Long,
      isActive: Boolean,
      policyDateOpt: Option[Timestamp],
      terminationPolicyDateOpt: Option[Timestamp]
    ): RentContract = {
      val insurancePolicy = buildInsurance(
        ownerSubscriptionId,
        tenantSubscriptionId,
        insuranceAmount,
        isActive,
        policyDateOpt.getOrElse(contract.data.getRentStartDate),
        terminationPolicyDateOpt
      )
      val contractDataWithInsurancePolicy = contract.data.toBuilder.setPreviousInsurance(insurancePolicy).build()
      contract.copy(data = contractDataWithInsurancePolicy)
    }

    def withTemporaryInsurancePolicy(
      ownerSubscriptionId: String,
      tenantSubscriptionId: String,
      insuranceAmount: Long,
      isActive: Boolean,
      policyDateOpt: Option[Timestamp],
      terminationPolicyDateOpt: Option[Timestamp]
    ): RentContract = {
      val insurancePolicy = buildInsurance(
        ownerSubscriptionId,
        tenantSubscriptionId,
        insuranceAmount,
        isActive,
        policyDateOpt.getOrElse(contract.data.getRentStartDate),
        terminationPolicyDateOpt
      )
      val temporaryAmounts = TemporaryAmounts.newBuilder().setInsurance(insurancePolicy).build()
      val contractDataWithInsurancePolicy = contract.data.toBuilder.setTemporaryAmounts(temporaryAmounts).build()
      contract.copy(data = contractDataWithInsurancePolicy)
    }

    private def buildInsurance(
      ownerSubscriptionId: String,
      tenantSubscriptionId: String,
      insuranceAmount: Long,
      isActive: Boolean,
      policyDate: Timestamp,
      terminationPolicyDateOpt: Option[Timestamp]
    ): Insurance =
      Insurance.newBuilder
        .setInsuranceAmount(insuranceAmount)
        .setPolicyDate(policyDate)
        .applyTransforms[Timestamp](terminationPolicyDateOpt, _.setTerminationPolicyDate(_))
        .setOwnerSubscriptionId(ownerSubscriptionId)
        .setTenantSubscriptionId(tenantSubscriptionId)
        .setSubscriptionIsActive(isActive)
        .build()
  }
}
