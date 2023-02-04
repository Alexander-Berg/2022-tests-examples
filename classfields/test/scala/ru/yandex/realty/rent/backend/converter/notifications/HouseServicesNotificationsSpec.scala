package ru.yandex.realty.rent.backend.converter.notifications

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.util.NowMomentProvider
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.proto.api.v2.service.FlatNotification.NotificationCase
import ru.yandex.realty.proto.api.v2.service.FlatNotification.NotificationCase._
import ru.yandex.realty.proto.api.v2.service.{FlatNotification, FlatNotificationGroup}
import ru.yandex.realty.rent.backend.converter.house.services.notifications.HouseServiceNotificationsInfo
import ru.yandex.realty.rent.model.enums.{
  AggregatedMeterReadingsStatus,
  ContractStatus,
  MeterReadingsStatus,
  OwnerRequestSettingStatus,
  OwnerRequestStatus,
  PaymentConfirmationStatus,
  PeriodType,
  ReceiptStatus,
  Role
}
import ru.yandex.realty.rent.model.enums.BillStatus
import ru.yandex.realty.rent.model.enums.PaymentConfirmationStatus.PaymentConfirmationStatus
import ru.yandex.realty.rent.model.enums.Role.Role
import ru.yandex.realty.rent.model.house.services
import ru.yandex.realty.rent.proto.model.house.service.periods.PeriodData
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class HouseServicesNotificationsSpec extends FlatSpec with Matchers with RentModelsGen {

  private val now: DateTime = DateTime.now()
  implicit private val nowMomentProvider: NowMomentProvider = NowMomentProvider(now)
  implicit private val features: SimpleFeatures = new SimpleFeatures

  behavior of "HouseServicesNotifications.buildNotifications"

  it should "display Settings.configurationRequired notification" in {
    val ng = HouseServicesNotifications.buildNotifications(
      uidOpt = None,
      role = Role.Owner,
      flatId = readableString.next,
      ownerRequestStatus = OwnerRequestStatus.WorkInProgress,
      houseServiceInfos = Seq.empty,
      userContracts = Seq.empty,
      ownerRequestOpt = Some(ownerRequestGen.next.copy(settingsStatus = OwnerRequestSettingStatus.New)),
      hasApprovedFlatShowingGroupStatus = false,
      addFallbacks = true
    )
    val n = findNotificationCase(ng, HOUSE_SERVICE_SETTINGS_CONFIGURATION_REQUIRED)
    n.hasHouseServiceSettingsConfigurationRequired should be(true)
  }

  it should "display Settings.configurationIncomplete notification" in {
    val ng = HouseServicesNotifications.buildNotifications(
      uidOpt = None,
      role = Role.Owner,
      flatId = readableString.next,
      ownerRequestStatus = OwnerRequestStatus.WorkInProgress,
      houseServiceInfos = Seq.empty,
      userContracts = Seq.empty,
      ownerRequestOpt = Some(ownerRequestGen.next.copy(settingsStatus = OwnerRequestSettingStatus.Draft)),
      hasApprovedFlatShowingGroupStatus = false,
      addFallbacks = true
    )
    val n = findNotificationCase(ng, HOUSE_SERVICE_SETTINGS_CONFIGURATION_INCOMPLETE)
    n.hasHouseServiceSettingsConfigurationIncomplete should be(true)
  }

  it should "display Settings.acceptanceRequired notification" in {
    val uid = 123
    features.CheckHouseServicesIsAccepted.setNewState(true)
    val contract = rentContractGen(ContractStatus.Draft).next
    val ng = HouseServicesNotifications.buildNotifications(
      uidOpt = Some(uid),
      role = Role.Tenant,
      flatId = readableString.next,
      ownerRequestStatus = OwnerRequestStatus.LookingForTenant,
      houseServiceInfos = Seq.empty,
      userContracts = Seq(contract.copy(tenant = contract.tenant.copy(uid = Some(uid)))),
      ownerRequestOpt = Some(ownerRequestGen.next.copy(settingsStatus = OwnerRequestSettingStatus.FilledByOwner)),
      hasApprovedFlatShowingGroupStatus = false,
      addFallbacks = true
    )
    val n = findNotificationCase(ng, HOUSE_SERVICE_SETTINGS_ACCEPTANCE_REQUIRED)
    n.hasHouseServiceSettingsAcceptanceRequired should be(true)
  }

  it should "display Settings.acceptanceRequired notification without contract" in {
    val uid = 123
    features.CheckHouseServicesIsAccepted.setNewState(false)
    val ng = HouseServicesNotifications.buildNotifications(
      uidOpt = Some(uid),
      role = Role.Tenant,
      flatId = readableString.next,
      ownerRequestStatus = OwnerRequestStatus.LookingForTenant,
      houseServiceInfos = Seq.empty,
      userContracts = Nil,
      ownerRequestOpt = Some(ownerRequestGen.next.copy(settingsStatus = OwnerRequestSettingStatus.FilledByOwner)),
      hasApprovedFlatShowingGroupStatus = true,
      addFallbacks = true
    )
    val n = findNotificationCase(ng, HOUSE_SERVICE_SETTINGS_ACCEPTANCE_REQUIRED)
    n.hasHouseServiceSettingsAcceptanceRequired should be(true)
  }

  private def emptyPeriod: services.Period = services.Period(
    periodId = "",
    contractId = "",
    periodType = PeriodType.Regular,
    period = DateTimeUtil.now(),
    meterReadingsStatus = AggregatedMeterReadingsStatus.NotSent,
    billStatus = BillStatus.NotSent,
    receiptStatus = ReceiptStatus.NotSent,
    paymentConfirmationStatus = PaymentConfirmationStatus.NotSent,
    paymentId = None,
    data = PeriodData.getDefaultInstance,
    createTime = DateTimeUtil.now(),
    updateTime = DateTimeUtil.now()
  )

  it should "display MeterReadings.send notification" in {
    val periodId = "2021-11-21"
    val n = buildMeterReadingsNotification(
      Seq(MeterReadingsStatus.ShouldBeSent),
      periodId,
      Role.Tenant,
      HOUSE_SERVICE_SEND_METER_READINGS
    )
    n.getHouseServiceSendMeterReadings.getPeriodId should be(periodId)
  }

  it should "display MeterReadings.declined notification" in {
    val periodId = "2021-12-21"
    val n = buildMeterReadingsNotification(
      Seq(MeterReadingsStatus.Declined),
      periodId,
      Role.Tenant,
      HOUSE_SERVICE_METER_READINGS_DECLINED
    )
    n.getHouseServiceMeterReadingsDeclined.getPeriodId should be(periodId)
  }

  it should "display MeterReadings.received notification" in {
    val periodId = "2021-12-21"
    val n = buildMeterReadingsNotification(
      Seq(MeterReadingsStatus.Sent, MeterReadingsStatus.ShouldBeSent),
      periodId,
      Role.Owner,
      HOUSE_SERVICE_RECEIVED_METER_READINGS
    )
    n.getHouseServiceReceivedMeterReadings.getPeriodId should be(periodId)
  }

  it should "display MeterReadings.allReceived notification" in {
    val periodId = "2021-12-21"
    val n = buildMeterReadingsNotification(
      Seq(MeterReadingsStatus.Sent, MeterReadingsStatus.Sent),
      periodId,
      Role.Owner,
      HOUSE_SERVICE_RECEIVED_ALL_METER_READINGS
    )
    n.getHouseServiceReceivedAllMeterReadings.getPeriodId should be(periodId)
  }

  private def buildMeterReadingsNotification(
    meterReadingsStatuses: Seq[MeterReadingsStatus.MeterReadingsStatus],
    periodId: String,
    role: Role.Role,
    expectedNotificationCase: NotificationCase
  ): FlatNotification =
    findNotificationCase(
      HouseServicesNotifications.buildNotifications(
        uidOpt = None,
        role = role,
        flatId = readableString.next,
        ownerRequestStatus = OwnerRequestStatus.Completed,
        houseServiceInfos = Seq(
          HouseServiceNotificationsInfo(
            period = emptyPeriod.copy(periodId = periodId),
            meterReadings = meterReadingsStatuses.map(meterReadingsGen("1", periodId, _).next)
          )
        ),
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        hasApprovedFlatShowingGroupStatus = false,
        addFallbacks = true
      ),
      expectedNotificationCase
    )

  it should "display Bills.timeToSend notification" in {
    val periodId = "2021-12-01"
    val n = buildBillStatusNotification(BillStatus.ShouldBeSent, periodId, Role.Owner, HOUSE_SERVICE_TIME_TO_SEND_BILLS)
    n.getHouseServiceTimeToSendBills.getPeriodId should be(periodId)
  }

  it should "display Bills.received notification" in {
    val periodId = "2021-12-02"
    val n = buildBillStatusNotification(BillStatus.ShouldBePaid, periodId, Role.Tenant, HOUSE_SERVICE_BILLS_RECEIVED)
    n.getHouseServiceBillsReceived.getPeriodId should be(periodId)
  }

  it should "display Bills.declined notification" in {
    val periodId = "2021-12-03"
    val n = buildBillStatusNotification(BillStatus.Declined, periodId, Role.Owner, HOUSE_SERVICE_BILLS_DECLINED)
    n.getHouseServiceBillsDeclined.getPeriodId should be(periodId)
  }

  it should "display Bills.paid notification" in {
    val periodId = readableString.next
    val n = buildBillStatusNotification(BillStatus.Paid, periodId, Role.Owner, HOUSE_SERVICE_BILLS_PAID)
    n.hasHouseServiceBillsPaid should be(true)
  }

  private def findNotificationCase(ng: FlatNotificationGroup, notificationCase: NotificationCase): FlatNotification =
    ng.getNotificationsList.asScala
      .find(_.getNotificationCase == notificationCase)
      .getOrElse(fail(s"missing notification $notificationCase"))

  private def buildBillStatusNotification(
    billStatus: BillStatus.BillStatus,
    periodId: String,
    role: Role,
    expectedNotificationCase: FlatNotification.NotificationCase
  ): FlatNotification =
    findNotificationCase(
      HouseServicesNotifications.buildNotifications(
        uidOpt = None,
        role = role,
        flatId = readableString.next,
        ownerRequestStatus = OwnerRequestStatus.Completed,
        houseServiceInfos = Seq(
          HouseServiceNotificationsInfo(
            period = emptyPeriod.copy(periodId = periodId, billStatus = billStatus),
            meterReadings = Seq.empty
          )
        ),
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        hasApprovedFlatShowingGroupStatus = false,
        addFallbacks = true
      ),
      expectedNotificationCase
    )

  it should "display Receipts.timeToSend notification" in {
    val periodId = "2022-01-01"
    val n = buildReceiptStatusNotification(
      ReceiptStatus.ShouldBeSent,
      periodId,
      Role.Tenant,
      HOUSE_SERVICE_TIME_TO_SEND_RECEIPTS
    )
    n.getHouseServiceTimeToSendReceipts.getPeriodId should be(periodId)
  }

  it should "display Receipts.declined notification" in {
    val periodId = "2022-01-02"
    val n = buildReceiptStatusNotification(
      ReceiptStatus.Declined,
      periodId,
      Role.Tenant,
      HOUSE_SERVICE_RECEIPTS_DECLINED
    )
    n.getHouseServiceReceiptsDeclined.getPeriodId should be(periodId)
  }

  it should "display Receipts.received notification" in {
    val periodId = "2022-01-03"
    val n = buildReceiptStatusNotification(ReceiptStatus.Sent, periodId, Role.Owner, HOUSE_SERVICE_RECEIPTS_RECEIVED)
    n.getHouseServiceReceiptsReceived.getPeriodId should be(periodId)
  }

  private def buildReceiptStatusNotification(
    receiptStatus: ReceiptStatus.ReceiptStatus,
    periodId: String,
    role: Role,
    expectedNotificationCase: FlatNotification.NotificationCase
  ): FlatNotification =
    findNotificationCase(
      HouseServicesNotifications.buildNotifications(
        uidOpt = None,
        role = role,
        flatId = readableString.next,
        ownerRequestStatus = OwnerRequestStatus.Completed,
        houseServiceInfos = Seq(
          HouseServiceNotificationsInfo(
            period = emptyPeriod.copy(
              periodId = periodId,
              receiptStatus = receiptStatus,
              billStatus = BillStatus.NotSent
            ),
            meterReadings = Seq.empty
          )
        ),
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        hasApprovedFlatShowingGroupStatus = false,
        addFallbacks = true
      ),
      expectedNotificationCase
    )

  it should "display PaymentConfirmation.timeToSend notification" in {
    val periodId = "2022-02-01"
    val n = buildConfirmationStatusNotification(
      PaymentConfirmationStatus.ShouldBeSent,
      periodId,
      Role.Tenant,
      HOUSE_SERVICE_TIME_TO_SEND_PAYMENT_CONFIRMATION
    )
    n.getHouseServiceTimeToSendPaymentConfirmation.getPeriodId should be(periodId)
  }

  it should "display PaymentConfirmation.declined notification" in {
    val periodId = "2022-02-02"
    val n = buildConfirmationStatusNotification(
      PaymentConfirmationStatus.Declined,
      periodId,
      Role.Tenant,
      HOUSE_SERVICE_PAYMENT_CONFIRMATION_DECLINED
    )
    n.getHouseServicePaymentConfirmationDeclined.getPeriodId should be(periodId)
  }

  it should "display PaymentConfirmation.received notification" in {
    val periodId = "2022-02-03"
    val n = buildConfirmationStatusNotification(
      PaymentConfirmationStatus.Sent,
      periodId,
      Role.Owner,
      HOUSE_SERVICE_PAYMENT_CONFIRMATION_RECEIVED
    )
    n.getHouseServicePaymentConfirmationReceived.getPeriodId should be(periodId)
  }

  private def buildConfirmationStatusNotification(
    confirmationStatus: PaymentConfirmationStatus,
    periodId: String,
    role: Role,
    expectedNotificationCase: FlatNotification.NotificationCase
  ): FlatNotification =
    findNotificationCase(
      HouseServicesNotifications.buildNotifications(
        uidOpt = None,
        role = role,
        flatId = readableString.next,
        ownerRequestStatus = OwnerRequestStatus.Completed,
        houseServiceInfos = Seq(
          HouseServiceNotificationsInfo(
            period = emptyPeriod.copy(
              periodId = periodId,
              paymentConfirmationStatus = confirmationStatus,
              billStatus = BillStatus.NotSent
            ),
            meterReadings = Seq.empty
          )
        ),
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        hasApprovedFlatShowingGroupStatus = false,
        addFallbacks = true
      ),
      expectedNotificationCase
    )
}
