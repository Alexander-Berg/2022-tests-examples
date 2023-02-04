package ru.yandex.realty.rent.backend.converter.notifications

import org.joda.time.{DateTime, Duration}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.proto.api.v2.service.FlatNotification
import ru.yandex.realty.proto.api.v2.service.FlatNotification.NotificationCase._
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{Payment, RentContract}
import ru.yandex.realty.rent.model.enums.{ContractStatus, PaymentStatus, PaymentType, Role}
import ru.yandex.realty.rent.proto.api.flats.FlatRequestFeatureNamespace.FlatRequestFeature
import ru.yandex.realty.rent.proto.api.flats.FlatRequestFeatureNamespace.FlatRequestFeature.{
  NOTIFICATION_FALLBACKS,
  USER_NOTIFICATIONS
}
import ru.yandex.realty.rent.proto.model.payment.PaymentData
import ru.yandex.realty.rent.proto.model.payment.PayoutErrorNamespace.PayoutError
import ru.yandex.realty.rent.proto.model.payment.PayoutStatusNamespace.PayoutStatus
import ru.yandex.realty.rent.util.NowMomentProvider
import ru.yandex.realty.rent.TestUtil._
import ru.yandex.realty.rent.backend.converter.notifications.PaymentNotifications.Emoji.{
  ExclamationMark,
  Eyes,
  Fire,
  Greed,
  Grimacing,
  HighVoltage,
  Hushed,
  Ok,
  Smile
}
import ru.yandex.realty.rent.proto.model.payment.FullnessTypeNamespace.FullnessType
import ru.yandex.realty.rent.proto.model.payment.PayoutTransactionStatusNamespace.PayoutTransactionStatus
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class PaymentNotificationsSpec extends FlatSpec with Matchers with RentModelsGen with FeaturesStubComponent {
  private val flatNotifications: FlatNotifications = new FlatNotifications()(features)

  private val now: DateTime = dt"2022-03-14 20:16:45"
  implicit private val nowMomentProvider: NowMomentProvider = NowMomentProvider(now)
  implicit private val requestFeatures: Set[FlatRequestFeature] = Set(NOTIFICATION_FALLBACKS, USER_NOTIFICATIONS)

  behavior of "FlatNotifications.collect"
  it should "display waitingForPaymentDate notification" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(Duration.standardDays(-1), PaymentStatus.New, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_WAITING_FOR_PAYMENT_DATE)
      .getOrElse(fail("missing notification"))
    n.getOwnerRentWaitingForPaymentDate.getPaymentDate should not be ""
  }

  it should "not display waitingForPaymentDate notification for first payment" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(Duration.standardDays(-1), PaymentStatus.New, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = true,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_WAITING_FOR_PAYMENT_DATE)
    n shouldBe None
  }

  it should "display expectingPayment for today's payment date notification" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(Duration.standardDays(0), PaymentStatus.New, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_EXPECTING_PAYMENT)
      .getOrElse(fail("missing notification"))
    n.getOwnerRentExpectingPayment.getPaymentDate should not be ""
  }

  it should "display expectingPayment notification" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(Duration.standardDays(1), PaymentStatus.New, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        inventoryOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_EXPECTING_PAYMENT)
      .getOrElse(fail("missing notification"))
    n.getOwnerRentExpectingPayment.getPaymentDate should not be ""
  }

  it should "not display expectingPayment notification for first payment" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(Duration.standardDays(1), PaymentStatus.New, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = true,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_EXPECTING_PAYMENT)
    n shouldBe None
  }

  it should "display waitingForPayout notification" in {
    val flat = flatGen(recursive = false).next
    flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment =
          makePayment(Duration.standardDays(1), PaymentStatus.PaidByTenant, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_WAITING_FOR_PAYOUT)
      .getOrElse(fail("missing notification"))
  }

  it should "display holdingForPaymentDate notification" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment =
          makePayment(Duration.standardDays(-1), PaymentStatus.PaidByTenant, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_HOLDING_FOR_PAYMENT_DATE)
      .getOrElse(fail("missing notification"))
    n.getOwnerRentHoldingForPaymentDate.getPaymentDate should not be ""
  }

  it should "not display holdingForPaymentDate notification for first payment" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment =
          makePayment(Duration.standardDays(-1), PaymentStatus.PaidByTenant, payoutError = None, patchData = identity),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = true,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_HOLDING_FOR_PAYMENT_DATE)
    n shouldBe None
  }

  it should "display paid notification with the paid sheet" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(-11),
      PaymentStatus.FuturePayment,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAID)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe true
    val expectedSheet = PaymentNotifications.Tenant.Sheet.paid(FlatNotification.newBuilder()).build()
    List(HighVoltage, Eyes, Ok, Smile, Fire).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }

  it should "display ready to pay notification with the ready to pay sheet" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(-1),
      PaymentStatus.New,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_READY_TO_PAY)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe true
    val expectedSheet = PaymentNotifications.Tenant.Sheet.readyToPay(FlatNotification.newBuilder()).build()
    List(HighVoltage, Eyes, Ok, Smile, Fire).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }

  it should "display outdated notification with the debt sheet" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(1),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe true
    val expectedSheet = PaymentNotifications.Tenant.Sheet.debt(FlatNotification.newBuilder(), 1).build()
    List(HighVoltage, Eyes, Fire).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    n.getTextLinkSheet.getText.substring(2) shouldBe expectedSheet.getText.substring(2)
  }

  it should "not display debt sheet in the notification because of the contract" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(1),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = false))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "not display debt sheet in the notification because of the previous payment" in {
    val flat = flatGen(recursive = false).next
    val paymentOpt = makePayment(
      Duration.standardDays(1),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = paymentOpt.toSeq,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "not display debt sheet in the notification because of the current payment" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(1),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val currentPayment =
      paymentOpt.map { p =>
        p.copy(
          data = p.data.toBuilder
            .setRentPayment(p.data.getRentPayment.toBuilder.setFullnessType(FullnessType.SHORT_TERMINATION).build())
            .build()
        )
      }
    val contractOpt = paymentOpt.map(
      p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true, terminationDate = dt"2022-03-14")
    )
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = currentPayment,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ currentPayment,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "display outdated notification with the penalty sheet" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(3),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe true
    val expectedSheet = PaymentNotifications.Tenant.Sheet.penalty(FlatNotification.newBuilder(), 3).build()
    List(HighVoltage, Eyes, ExclamationMark, Hushed, Grimacing).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }

  it should "not display penalty sheet in the notification because of the contract" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(3),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = false))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "not display penalty sheet in the notification because of the previous payment" in {
    val flat = flatGen(recursive = false).next
    val paymentOpt = makePayment(
      Duration.standardDays(3),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = paymentOpt.toSeq,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "not display penalty sheet in the notification because of the current payment" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(3),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val currentPayment =
      paymentOpt.map { p =>
        p.copy(
          data = p.data.toBuilder
            .setRentPayment(p.data.getRentPayment.toBuilder.setFullnessType(FullnessType.SHORT_TERMINATION).build())
            .build()
        )
      }
    val contractOpt = paymentOpt.map(
      p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true, terminationDate = dt"2022-03-14")
    )
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = currentPayment,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ currentPayment,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "display outdated notification with the serious violation sheet" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(15),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe true
    val expectedSheet = PaymentNotifications.Tenant.Sheet.seriousViolation(FlatNotification.newBuilder(), 15).build()
    List(HighVoltage, Eyes, ExclamationMark, Hushed, Grimacing)
      .contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }

  it should "not display serious violation sheet in the notification because of the contract" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(15),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = false))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "not display serious violation sheet in the notification because of the previous payment" in {
    val flat = flatGen(recursive = false).next
    val paymentOpt = makePayment(
      Duration.standardDays(15),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = paymentOpt,
        lastActualContractPayments = paymentOpt.toSeq,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "not display serious violation sheet in the notification because of the current payment" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(15),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val currentPayment =
      paymentOpt.map { p =>
        p.copy(
          data = p.data.toBuilder
            .setRentPayment(p.data.getRentPayment.toBuilder.setFullnessType(FullnessType.SHORT_TERMINATION).build())
            .build()
        )
      }
    val contractOpt = paymentOpt.map(
      p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true, terminationDate = dt"2022-03-14")
    )
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = currentPayment,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ currentPayment,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_RENT_PAYMENT_OUTDATED)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe false
  }

  it should "display waitingForPaymentDate notification with the payout sheet" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(2),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_WAITING_FOR_PAYMENT_DATE)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe true
    val expectedSheet = PaymentNotifications.Owner.Sheet.payout(FlatNotification.newBuilder()).build()
    List(Greed, HighVoltage, Ok, Fire).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }
  it should "display waitingForPaymentDate notification with the payoutWithDelay sheet" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(16),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map(p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = paymentOpt,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ paymentOpt,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_WAITING_FOR_PAYMENT_DATE)
      .getOrElse(fail("missing notification"))
    n.hasTextLinkSheet shouldBe true
    val expectedSheet = PaymentNotifications.Owner.Sheet.payoutWithDelay(FlatNotification.newBuilder()).build()
    List(HighVoltage, Eyes, Grimacing).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }

  it should "display waitingForPaymentDate notification with payoutDisabledBecauseOfPreviousPeriod sheet because of the previous payment" in {
    val flat = flatGen(recursive = false).next
    val paymentOpt = makePayment(
      Duration.standardDays(14),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val contractOpt = paymentOpt.map { p =>
      makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true, terminationDate = dt"2022-03-14")
    }
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = paymentOpt,
        lastActualContractPayments = paymentOpt.toSeq,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_WAITING_FOR_PAYMENT_DATE)
      .getOrElse(fail("missing notification"))

    val expectedSheet = PaymentNotifications.Owner.Sheet
      .payoutDisabledBecauseOfPreviousPeriod(FlatNotification.newBuilder())
      .build
    List(HighVoltage, Eyes).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }

  it should "display waitingForPaymentDate notification with payoutDisabledBecauseOfTermination sheet because of the current payment" in {
    val flat = flatGen(recursive = false).next
    val previousPaymentOpt = makePayment(
      Duration.standardDays(31),
      PaymentStatus.PaidToOwner,
      payoutError = None,
      patchData = identity
    )
    val paymentOpt = makePayment(
      Duration.standardDays(15),
      PaymentStatus.PaidOutUnderGuarantee,
      payoutError = None,
      patchData = identity
    )
    val currentPayment =
      paymentOpt.map { p =>
        p.copy(
          data = p.data.toBuilder
            .setRentPayment(p.data.getRentPayment.toBuilder.setFullnessType(FullnessType.SHORT_TERMINATION).build())
            .build()
        )
      }
    val contractOpt = paymentOpt.map(
      p => makeContract(p.contractId, isGuaranteePayoutAvailableForContract = true, terminationDate = dt"2022-03-14")
    )
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = currentPayment,
        lastActualContractPayments = previousPaymentOpt.toSeq ++ currentPayment,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = contractOpt.toSeq,
        userContracts = contractOpt.toSeq,
        ownerRequestOpt = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_WAITING_FOR_PAYMENT_DATE)
      .getOrElse(fail("missing notification"))

    val expectedSheet = PaymentNotifications.Owner.Sheet
      .payoutDisabledBecauseOfTermination(FlatNotification.newBuilder())
      .build
    List(HighVoltage, Eyes).contains(n.getTextLinkSheet.getText.split(" ")(0)) shouldBe true
    expectedSheet.getText.substring(2) shouldBe n.getTextLinkSheet.getText.substring(2)
  }
  it should "display cardUnavailable notification" in {
    val flat = flatGen(recursive = false).next
    flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(
          Duration.standardDays(1),
          PaymentStatus.PaidByTenant,
          payoutError = Some(PayoutError.BOUND_OWNER_CARD_IS_INACTIVE),
          patchData = identity
        ),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_CARD_UNAVAILABLE)
      .getOrElse(fail("missing notification"))
  }

  it should "display payoutBroken notification" in {
    val flat = flatGen(recursive = false).next
    flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(
          Duration.standardDays(1),
          PaymentStatus.PaidByTenant,
          payoutError = Some(PayoutError.RETRIES_LIMIT_REACHED),
          patchData = identity
        ),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_PAYOUT_BROKEN)
      .getOrElse(fail("missing notification"))
  }

  it should "NOT display a payment notification for zero bound cards" in {
    val flat = flatGen(recursive = false).next
    flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(
          Duration.standardDays(1),
          PaymentStatus.PaidByTenant,
          payoutError = Some(PayoutError.BOUND_OWNER_CARD_IS_ABSENT),
          patchData = identity
        ),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .isEmpty shouldBe true
  }

  it should "display paidOutToCard notification" in {
    val flat = flatGen(recursive = false).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(
          Duration.standardDays(1),
          PaymentStatus.PaidToOwner,
          payoutError = None,
          patchData = _.addOwnerTransactionsBuilder()
            .setPanMask("1234****7890")
            .setStatus(PayoutTransactionStatus.COMPLETED)
        ),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_PAID_OUT_TO_CARD)
      .getOrElse(fail("missing notification"))
    n.getOwnerRentPaidOutToCard.getMaskedCardNumber shouldBe "*7890"
  }

  it should "display paidOutToAccount notification" in {
    val flat = flatGen(recursive = false).next
    flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(
          Duration.standardDays(1),
          PaymentStatus.PaidToOwner,
          payoutError = None,
          patchData = _.getManualOwnerTransactionBuilder()
        ),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_RENT_PAID_OUT_TO_ACCOUNT)
      .getOrElse(fail("missing notification"))
  }

  private def makePayment(
    timeSincePeriodStart: Duration,
    status: PaymentStatus.PaymentStatus,
    payoutError: Option[PayoutError],
    patchData: PaymentData.Builder => Any
  ): Some[Payment] = Some(
    Payment(
      "paymentid",
      "contractid",
      PaymentType.Rent,
      isPaidOutUnderGuarantee = false,
      now.minus(timeSincePeriodStart).withTimeAtStartOfDay(),
      now.minus(timeSincePeriodStart).withTimeAtStartOfDay(),
      now.minus(timeSincePeriodStart).plusDays(30).withTimeAtStartOfDay(),
      status,
      PaymentData
        .newBuilder()
        .applySideEffects[PayoutError](
          payoutError,
          (b, e) => b.setPayoutStatus(PayoutStatus.UNRECOVERABLE_ERROR).setPayoutError(e)
        )
        .applySideEffect(patchData)
        .build(),
      createTime = DateTimeUtil.now(),
      updateTime = DateTimeUtil.now()
    )
  )

  private def makeContract(
    contractId: String,
    isGuaranteePayoutAvailableForContract: Boolean = false,
    terminationDate: Option[DateTime] = None
  ): RentContract = {
    val contract = rentContractGen(ContractStatus.Active).next
    contract.copy(
      contractId = contractId,
      terminationDate = terminationDate,
      data = contract.data.toBuilder
        .setUsePayoutUnderGuarantee(isGuaranteePayoutAvailableForContract)
        .build()
    )
  }

}
