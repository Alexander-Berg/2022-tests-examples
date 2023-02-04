package ru.yandex.realty.rent.backend.converter

import org.joda.time.{DateTime, Duration}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.proto.api.v2.service.FlatNotification.NotificationCase._
import ru.yandex.realty.proto.api.v2.service.FlatNotification.OwnerConfirmedTodo.TodoItem
import ru.yandex.realty.proto.api.v2.service.FlatNotification.TenantSearchStats
import ru.yandex.realty.rent.backend.converter.house.services.notifications.HouseServiceNotificationsInfo
import ru.yandex.realty.rent.backend.converter.notifications.{FlatNotifications, PaymentNotifications}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.{
  BillStatus,
  ContractStatus,
  FlatShowingStatus,
  KeysHandoverDirection,
  OwnerRequestStatus,
  PassportVerificationStatus,
  PaymentStatus,
  PaymentType,
  Role,
  MeterReadingsStatus => ReadingsStatus
}
import ru.yandex.realty.rent.model.{KeysHandover, Payment, RentContract, ShowingAndUsers, User}
import ru.yandex.realty.rent.proto.api.common.FlatTypeNamespace.FlatType
import ru.yandex.realty.rent.proto.api.flats.Flat.FlatInfo
import ru.yandex.realty.rent.proto.api.flats.FlatRequestFeatureNamespace.FlatRequestFeature
import ru.yandex.realty.rent.proto.api.flats.FlatRequestFeatureNamespace.FlatRequestFeature.{
  NOTIFICATION_FALLBACKS,
  USER_NOTIFICATIONS
}
import ru.yandex.realty.rent.proto.api.moderation.FlatDetailedInfo.KeysLocation
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Flat.Intercom
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Flat.RoomsNamespace.Rooms
import ru.yandex.realty.rent.proto.api.user.OwnerCardsStatusNamespace.OwnerCardsStatus
import ru.yandex.realty.rent.proto.model.flat.FlatData
import ru.yandex.realty.rent.proto.model.image.Image
import ru.yandex.realty.rent.proto.model.keys.handovers.KeysHandoverData
import ru.yandex.realty.rent.proto.model.payment.PaymentData
import ru.yandex.realty.rent.proto.model.payment.PayoutErrorNamespace.PayoutError
import ru.yandex.realty.rent.proto.model.payment.PayoutStatusNamespace.PayoutStatus
import ru.yandex.realty.rent.proto.model.user.InnFilledTypeNamespace.InnFilledType
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheck
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheck.FsspDebtCheck
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckResolutionNamespace.NaturalPersonCheckResolution
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckStatusNamespace.NaturalPersonCheckStatus
import ru.yandex.realty.rent.proto.model.user.{PersonalDataTransferAgreement, TenantQuestionnaire, UserData}
import ru.yandex.realty.rent.util.NowMomentProvider
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.util.protobuf.TimeProtoFormats.{DateTimeFormat => ProtoDateTimeFormat}
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._
import scala.util.Random
import ru.yandex.realty.rent.TestUtil._

@RunWith(classOf[JUnitRunner])
class FlatNotificationsSpec extends FlatSpec with Matchers with RentModelsGen with FeaturesStubComponent {

  features.InventoryNotifications.setNewState(true)

  private val now: DateTime = dt"2022-03-14 20:16:45"
  implicit private val nowMomentProvider: NowMomentProvider = NowMomentProvider(now)
  implicit private val requestFeatures: Set[FlatRequestFeature] = Set(NOTIFICATION_FALLBACKS, USER_NOTIFICATIONS)

  private val userTemplate = User(
    uid = 1L,
    userId = "111",
    phone = None,
    name = None,
    surname = None,
    patronymic = None,
    fullName = None,
    email = None,
    passportVerificationStatus = PassportVerificationStatus.Unknown,
    roommateLinkId = None,
    roommateLinkExpirationTime = None,
    assignedFlats = Map.empty,
    data = UserData.getDefaultInstance,
    createTime = now,
    updateTime = now,
    visitTime = None
  )

  private val flatNotifications: FlatNotifications = new FlatNotifications()(features)

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

  behavior of "FlatNotifications.collect"

  it should "display a payment requisite notification for zero bound cards" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Completed).next.copy(isRented = true)
    flatNotifications
      .collect(
        userOpt = Some(
          userTemplate.copy(
            data = UserData.newBuilder().setOwnerCardsStatus(OwnerCardsStatus.NO_CARDS_BOUND).build()
          )
        ),
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
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == OWNER_WITHOUT_CARD)
      .getOrElse(fail("missing notification"))
  }

  it should "NOT display a payment notification for many bound cards" in {
    val flat = flatGen(recursive = false).next
    flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = makePayment(
          Duration.standardDays(1),
          PaymentStatus.PaidByTenant,
          payoutError = Some(PayoutError.BOUND_OWNER_CARD_IS_NOT_THE_ONLY),
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

  it should "display a payment requisite notification for many bound cards" in {
    val flat = flatGen(recursive = false).next
    flatNotifications
      .collect(
        userOpt = Some(
          userTemplate.copy(
            data = UserData.newBuilder().setOwnerCardsStatus(OwnerCardsStatus.SEVERAL_CARDS_BOUND).build()
          )
        ),
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        role = Role.Owner,
        payment = makePayment(
          Duration.standardDays(1),
          PaymentStatus.PaidByTenant,
          payoutError = Some(PayoutError.BOUND_OWNER_CARD_IS_NOT_THE_ONLY),
          patchData = identity
        ),
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
      .find(_.getNotificationCase == OWNER_WITH_MANY_CARDS)
      .getOrElse(fail("missing notification"))
  }

  it should "display a payment requisite notification for missing INN" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next
    flatNotifications
      .collect(
        userOpt = Some(
          userTemplate.copy(
            data = UserData
              .newBuilder()
              .setOwnerCardsStatus(OwnerCardsStatus.SINGLE_CARD_BOUND)
              .setInnFilledType(InnFilledType.SHOULD_MANUAL_FILLED)
              .build()
          )
        ),
        role = Role.Owner,
        payment = None,
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
      .find(_.getNotificationCase == OWNER_WITHOUT_INN)
      .getOrElse(fail("missing notification"))
  }

  it should "display draftNeedToFinish notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Draft).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    n.find(_.getNotificationCase == DRAFT_NEED_TO_FINISH)
      .getOrElse(fail("missing notification"))
  }

  it should "display draftNeedConfirmation notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.WaitingForConfirmation).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    n.find(_.getNotificationCase == DRAFT_NEED_CONFIRMATION)
      .getOrElse(fail("missing notification"))
  }

  it should "display waitingForArendaTeamContact notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    n.find(_.getNotificationCase == WAITING_FOR_ARENDA_TEAM_CONTACT)
      .getOrElse(fail("missing notification"))
  }

  it should "display preparingFlatForExposition notification in WorkInProgress" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    n.find(_.getNotificationCase == PREPARING_FLAT_FOR_EXPOSITION)
      .getOrElse(fail("missing notification"))
  }

  it should "display preparingFlatForExposition notification in LookingForTenant" in {
    val flatData = FlatData.newBuilder()
    val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next.copy(data = flatData.build())
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    n.find(_.getNotificationCase == PREPARING_FLAT_FOR_EXPOSITION)
      .getOrElse(fail("missing notification"))
  }

  it should "display needToAddPassport notification" in {
    val statuses = List(OwnerRequestStatus.WorkInProgress, OwnerRequestStatus.LookingForTenant)
    val userWithoutPassport = userGen(false).next.copy(passportVerificationStatus = PassportVerificationStatus.Absent)
    statuses.foreach { status =>
      val flat = flatWithOwnerRequest(status).next.copy(assignedUsers = Map(Role.Owner -> Seq(userWithoutPassport)))
      val n = flatNotifications
        .collect(
          userOpt = None,
          role = Role.Owner,
          payment = None,
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

      n.find(_.getNotificationCase == NEED_TO_ADD_PASSPORT)
        .getOrElse(fail("missing notification"))
    }
  }

  it should "display checkTenantCandidates notification" in {
    val user = userGen(false).next
    val tenantCandidate = {
      val tenantQuestionnaire = TenantQuestionnaire.newBuilder().build()
      val dataBuilder =
        user.data.toBuilder
          .setApprovedTenantQuestionnaire(tenantQuestionnaire)
          .setPersonalDataTransferAgreement(
            PersonalDataTransferAgreement
              .newBuilder()
              .setAgreementDate(ProtoDateTimeFormat.write(DateTime.now()))
          )
      dataBuilder.getNaturalPersonChecksBuilder
        .addChecks(
          NaturalPersonCheck
            .newBuilder()
            .setFsspDebtCheck(FsspDebtCheck.getDefaultInstance)
            .setStatus(NaturalPersonCheckStatus.READY)
        )
        .setResolution(NaturalPersonCheckResolution.VALID)
        .setStatus(NaturalPersonCheckStatus.READY)
      user.copy(data = dataBuilder.build())
    }
    val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next.copy(
      assignedUsers = Map(Role.TenantCandidate -> Seq(tenantCandidate))
    )
    val showing = flatShowingGen(FlatShowingStatus.ConfirmedByOwner).next
    val showingAndUsers = ShowingAndUsers(showing, Iterable(user))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = None,
        tenantSearchStats = None,
        flat = flat,
        showingsAndUsers = Iterable(showingAndUsers),
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)

    n.find(_.getNotificationCase == CHECK_TENANT_CANDIDATES)
      .getOrElse(fail("missing notification"))
  }

//ВРЕМЕННО отключили
//  it should "display KeysStillWithYou notification" in {
//    val flat = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next
//    val statuses = List(OwnerRequestStatus.WorkInProgress, OwnerRequestStatus.LookingForTenant)
//    statuses.foreach { status =>
//      val n = flatNotifications
//        .collect(
//          userOpt = None,
//          role = Role.Owner,
//          payment = None,
//          lastActualContractPayments = Seq.empty,
//          lastTerminatedContractPayments = Seq.empty,
//          isFirstPayment = false,
//          houseServiceInfos = Seq.empty,
//          userContracts = Seq.empty,
//          ownerRequestOpt = None,
//          tenantSearchStats = None,
//          flat = flat,
//          showingsAndUsers = Iterable.empty,
//          keysHandoverOpt = None,
//          inventoryOpt = None
//        )
//        .asScala
//        .flatMap(_.getNotificationsList.asScala)
//
//      n.find(_.getNotificationCase == KEYS_STILL_WITH_YOU)
//        .getOrElse(fail("missing notification"))
//    }
//  }

  it should "display ownerRequestDeclined notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.CancelledWithoutSigning).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    n.find(_.getNotificationCase == OWNER_REQUEST_DECLINED)
      .getOrElse(fail("missing notification"))
  }

  it should "display ownerRequestCanceledByOwner notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Denied).next
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    n.find(_.getNotificationCase == OWNER_REQUEST_CANCELED_BY_OWNER)
      .getOrElse(fail("missing notification"))
  }

  it should "display rentIsFinished notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Completed).next
    val contract = rentContractGen(status = ContractStatus.Terminated).next.copy(flatId = flat.flatId)
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq(contract),
        userContracts = Seq(contract),
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

    n.find(_.getNotificationCase == OWNER_RENT_IS_FINISHED)
      .getOrElse(fail("missing notification"))
  }

  it should "display lookingForTenants notification" in {
    val offerId = "123"
    val flatData = FlatData.newBuilder()
    flatData.getRealtyInfoBuilder.setOfferId(offerId).setIsPublished(true)
    val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next.copy(data = flatData.build())
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    val notification = n
      .find(_.getNotificationCase == LOOKING_FOR_TENANTS)
      .getOrElse(fail("missing notification"))
    assert(notification.getLookingForTenants.getOfferId == offerId)
  }

  it should "display ConfirmedTodo notification with not done passport & photo & flatInfo items" in {
    val userWithoutPassport = userGen(false).next.copy(passportVerificationStatus = PassportVerificationStatus.Absent)
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next
      .copy(assignedUsers = Map(Role.Owner -> Seq(userWithoutPassport)))
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    val notification = n
      .find(_.getNotificationCase == OWNER_CONFIRMED_TODO)
      .getOrElse(fail("missing notification"))

    val todoItems = notification.getOwnerConfirmedTodo.getItemsList

    if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_PASSPORT)) {
      fail("missing todo item: NeedToAddPassport")
    }

    if (todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_PASSPORT).exists(_.getDone)) {
      fail("todo item NeedToAddPassport shouldnot be done")
    }

    if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_PHOTOS)) {
      fail("missing todo item: NeedToAddPhotos")
    }

    if (todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_PHOTOS).exists(_.getDone)) {
      fail("todo item NeedToAddPhotos shouldnot be done")
    }

    if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_INFO)) {
      fail("missing todo item: NeedToAddFlatInfo")
    }

    if (todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_INFO).exists(_.getDone)) {
      fail("todo item NeedToAddFlatInfo shouldnot be done")
    }
  }

  it should "display ConfirmedTodo notification with done passport & photo items & flatInfo" in {
    val userWithoutPassport = userGen(false).next.copy(passportVerificationStatus = PassportVerificationStatus.Saved)
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
      assignedUsers = Map(Role.Owner -> Seq(userWithoutPassport)),
      data = FlatData
        .newBuilder()
        .addImages(Image.newBuilder().setName("test"))
        .setFlatInfo(
          FlatInfo
            .newBuilder()
            .setArea(1.0f)
            .setFloor(1)
            .setFlatType(FlatType.FLAT)
            .setRooms(Rooms.TWO)
            .setEntrance(1)
            .setIntercom(Intercom.newBuilder().setCode("code"))
            .setDesiredRentPrice(1)
        )
        .build()
    )
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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

    val notification = n
      .find(_.getNotificationCase == OWNER_CONFIRMED_TODO)
      .getOrElse(fail("missing notification"))

    val todoItems = notification.getOwnerConfirmedTodo.getItemsList

    if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_PASSPORT)) {
      fail("missing todo item: NeedToAddPassport")
    }

    if (!todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_PASSPORT).exists(_.getDone)) {
      fail("todo item NeedToAddPassport should be done")
    }

    if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_PHOTOS)) {
      fail("missing todo item: NeedToAddPhotos")
    }

    if (!todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_PHOTOS).exists(_.getDone)) {
      fail("todo item NeedToAddPhotos should be done")
    }

    if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_INFO)) {
      fail("missing todo item: NeedToAddFlatInfo")
    }

    if (!todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_FLAT_INFO).exists(_.getDone)) {
      fail("todo item NeedToAddFlatInfo shouldnot be done")
    }
  }

  it should "display DECLINE readings notification when there is SEND notification in the same period" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Completed).next

    val periodId = readableString.next
    val declineHouseServiceInfo = HouseServiceNotificationsInfo(
      period = periodGen(periodId).next.copy(billStatus = BillStatus.NotSent),
      meterReadings = Seq(
        meterReadingsGen("1", periodId, ReadingsStatus.ShouldBeSent).next,
        meterReadingsGen("1", periodId, ReadingsStatus.Declined).next
      )
    )

    val decline = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq(declineHouseServiceInfo),
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

    assert(decline.nonEmpty)
    if (decline.exists(_.hasHouseServiceSendMeterReadings)) {
      fail("There is SEND notification in DECLINE case")
    }
  }

  it should "display SEND meter readings notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Completed).next

    val periodId = readableString.next
    val sendHouseServiceInfo = HouseServiceNotificationsInfo(
      period = periodGen(periodId).next.copy(billStatus = BillStatus.NotSent),
      meterReadings = Seq(meterReadingsGen("1", periodId, ReadingsStatus.ShouldBeSent).next)
    )

    val send = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq(sendHouseServiceInfo),
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

    assert(send.nonEmpty)
    if (send.exists(_.hasHouseServiceMeterReadingsDeclined)) {
      fail("There is DECLINE notification in SEND case")
    }
  }

  it should "display both SEND and DECLINE meter readings notification" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.Completed).next

    val periodId1 = readableString.next
    val sendHouseServiceInfo = HouseServiceNotificationsInfo(
      period = periodGen(periodId1).next.copy(billStatus = BillStatus.NotSent),
      meterReadings = Seq(meterReadingsGen("1", periodId1, ReadingsStatus.ShouldBeSent).next)
    )
    val periodId2 = readableString.next
    val declineHouseServiceInfo = HouseServiceNotificationsInfo(
      period = periodGen(periodId2).next.copy(billStatus = BillStatus.NotSent),
      meterReadings = Seq(meterReadingsGen("1", periodId2, ReadingsStatus.Declined).next)
    )

    val sendAndDecline = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Tenant,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq(sendHouseServiceInfo, declineHouseServiceInfo),
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

    assert(sendAndDecline.nonEmpty)
    assertResult(true) {
      val hasSend = sendAndDecline.exists(_.hasHouseServiceSendMeterReadings)
      val hasDecline = sendAndDecline.exists(_.hasHouseServiceMeterReadingsDeclined)
      hasSend && hasDecline
    }
  }

  it should "display ownerNeedToFillOutInventory notification" in {
    val initialInventory = inventoryGen.next.copy(version = 0)
    val confirmedOwnerRequest = ownerRequestGen.next.copy(status = OwnerRequestStatus.Confirmed)

    val notificationsWhenInitialInventory = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = Some(confirmedOwnerRequest),
        flat = flatGen().next,
        showingsAndUsers = Iterable.empty,
        inventoryOpt = Some(initialInventory),
        tenantSearchStats = None,
        keysHandoverOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .toList
      .flatMap(_.getNotificationsList.asScala)

    val notificationsWhenInventoryIsAbsent = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = Some(confirmedOwnerRequest),
        flat = flatGen().next,
        showingsAndUsers = Iterable.empty,
        inventoryOpt = None,
        tenantSearchStats = None,
        keysHandoverOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .toList
      .flatMap(_.getNotificationsList.asScala)

    notificationsWhenInitialInventory
      .find(_.hasOwnerNeedToFillOutInventory)
      .getOrElse(fail("missing notification"))

    notificationsWhenInventoryIsAbsent
      .find(_.hasOwnerNeedToFillOutInventory)
      .getOrElse(fail("missing notification"))
  }

  it should "display ownerNeedToConfirmInventory notification" in {
    val initialInventory = inventoryGen.next
    val lookingForTenantOwnerRequest = ownerRequestGen.next.copy(status = OwnerRequestStatus.LookingForTenant)
    val completedOwnerRequest = ownerRequestGen.next.copy(status = OwnerRequestStatus.Completed)
    val ownerRequests = Seq(lookingForTenantOwnerRequest, completedOwnerRequest)

    val notificationsForLookingForTenant = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = Some(ownerRequests(Random.nextInt(ownerRequests.size))),
        flat = flatGen().next,
        showingsAndUsers = Iterable.empty,
        inventoryOpt = Some(initialInventory),
        tenantSearchStats = None,
        keysHandoverOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .toList
      .flatMap(_.getNotificationsList.asScala)

    notificationsForLookingForTenant
      .find(_.hasOwnerNeedToConfirmInventory)
      .getOrElse(fail("missing notification"))
  }

  it should "display tenantNeedToConfirmInventory notification when tenant check-in date has arrived" in {
    val initialInventory = inventoryGen.next
    val confirmedByOwnerInventory = initialInventory.copy(
      data = initialInventory.data.toBuilder.setConfirmedByOwnerDate(DateTimeFormat.defaultInstance).build()
    )
    val contract = rentContractGen(ContractStatus.Active).next
    val tenantCheckInDate = DateTimeUtil.now().minusHours(1)
    val rentContract = contract.copy(
      data = contract.data.toBuilder
        .setTenantCheckInDate(ProtoDateTimeFormat.write(tenantCheckInDate))
        .build()
    )
    val tenant = userGen().next.copy(uid = rentContract.tenant.uid.getOrElse(0L))

    val notifications = flatNotifications
      .collect(
        userOpt = Some(tenant),
        role = Role.Tenant,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq(rentContract),
        userContracts = Seq(rentContract),
        ownerRequestOpt = Some(ownerRequestGen.next),
        flat = flatGen().next,
        showingsAndUsers = Iterable.empty,
        inventoryOpt = Some(confirmedByOwnerInventory),
        tenantSearchStats = None,
        keysHandoverOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .toList
      .flatMap(_.getNotificationsList.asScala)

    notifications
      .find(_.hasTenantNeedToConfirmInventory)
      .getOrElse(fail("missing notification"))
  }

  it should "display tenant search stats" in {
    val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
    flat.getStatus
    val offerId = "123456"
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
        tenantSearchStats = Some(
          TenantSearchStats
            .newBuilder()
            .setOfferId(offerId)
            .build()
        ),
        keysHandoverOpt = None,
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
      .find(_.getNotificationCase == TENANT_SEARCH_STATS)
      .getOrElse(fail("missing notification"))
    n.getTenantSearchStats.getOfferId shouldBe offerId
  }

// ВРЕМЕннО отключили
//  it should "display keysStillWithYou for owner" in {
//    val flat = {
//      val f = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
//      f.copy(data = f.data.toBuilder.setKeysLocation(KeysLocation.UNKNOWN).build())
//    }
//    val n = flatNotifications
//      .collect(
//        userOpt = None,
//        role = Role.Owner,
//        payment = None,
//        lastActualContractPayments = Seq.empty,
//        lastTerminatedContractPayments = Seq.empty,
//        isFirstPayment = false,
//        houseServiceInfos = Seq.empty,
//        userContracts = Seq.empty,
//        ownerRequestOpt = None,
//        flat = flat,
//        showingsAndUsers = Iterable.empty,
//        tenantSearchStats = None,
//        keysHandoverOpt = None,
//        inventoryOpt = None
//      )
//      .asScala
//      .flatMap(_.getNotificationsList.asScala)
//    n.find(_.getNotificationCase == KEYS_STILL_WITH_YOU)
//      .getOrElse(fail("missing notification"))
//  }

  it should "display keysHandedOverToManager for owner" in {
    val flat = {
      val f = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      f.copy(data = f.data.toBuilder.setKeysLocation(KeysLocation.MANAGER).build())
    }
    val now = DateTime.now()
    val keysHandover = KeysHandover(
      handoverId = "handoverId",
      flatId = flat.flatId,
      contractId = None,
      direction = KeysHandoverDirection.FromOwnerToManager,
      documentId = None,
      data = KeysHandoverData.getDefaultInstance,
      createTime = now,
      updateTime = now
    )
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
        lastActualContractPayments = Seq.empty,
        lastTerminatedContractPayments = Seq.empty,
        isFirstPayment = false,
        houseServiceInfos = Seq.empty,
        allUserContracts = Seq.empty,
        userContracts = Seq.empty,
        ownerRequestOpt = flat.lastOwnerRequest,
        flat = flat,
        showingsAndUsers = Iterable.empty,
        tenantSearchStats = None,
        keysHandoverOpt = Some(keysHandover),
        inventoryOpt = None,
        hasApprovedFlatShowingGroupStatus = false
      )
      .asScala
      .flatMap(_.getNotificationsList.asScala)
    n.find(_.getNotificationCase == KEYS_HANDED_OVER_TO_MANAGER)
      .getOrElse(fail("missing notification"))
  }

  it should "display buildKeysStillWithManager for owner" in {
    val flat = {
      val f = flatWithOwnerRequest(OwnerRequestStatus.Denied).next
      f.copy(data = f.data.toBuilder.setKeysLocation(KeysLocation.MANAGER).build())
    }
    val n = flatNotifications
      .collect(
        userOpt = None,
        role = Role.Owner,
        payment = None,
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
    n.find(_.getNotificationCase == KEYS_STILL_WITH_MANAGER)
      .getOrElse(fail("missing notification"))
  }

}
