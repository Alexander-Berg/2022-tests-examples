package ru.yandex.vertis.safe_deal.model

import ru.yandex.vertis.safe_deal.model.tinkoff.{TinkoffDeal, TinkoffRefillBankDetails}

import java.time.Instant
import ru.yandex.vertis.safe_deal.proto.common.{BuyerStep, DealState, DealStep, SellerStep}
import ru.yandex.vertis.safe_deal.util.RichAutoruModel.RichDealBankType.DefaultDealBankType
import ru.yandex.vertis.zio_baker.model.{ChatRoomId, User}

object TestSyntax {

  implicit class RichSellerCompanion(val self: Seller.type) extends AnyVal {

    def forTest(
        user: User,
        personProfile: Option[PersonProfile] = None,
        isDealAccepted: Boolean = false,
        isPassportProvided: Boolean = false,
        passportCorrelationId: Option[CorrelationId] = None,
        phoneConfirmationCode: Option[DealParty.ConfirmationCode] = None,
        isSubjectInfoProvided: Boolean = false,
        isPriceApproved: Boolean = false,
        isBankingDetailsProvided: Boolean = false,
        isDocumentsApproved: Boolean = false,
        confirmationCode: Option[DealParty.ConfirmationCode] = None,
        isPassportValid: Option[Boolean] = None,
        userName: Option[String] = None,
        isBankingEntityValid: Option[Boolean] = None) =
      self(
        user = user,
        personProfile = personProfile,
        isDealAccepted = isDealAccepted,
        isPassportProvided = isPassportProvided,
        passportCorrelationId = passportCorrelationId,
        phoneConfirmationCode = phoneConfirmationCode,
        isSubjectInfoProvided = isSubjectInfoProvided,
        isPriceApproved = isPriceApproved,
        isBankingDetailsProvided = isBankingDetailsProvided,
        isDocumentsApproved = isDocumentsApproved,
        confirmationCode = confirmationCode,
        isPassportValid = isPassportValid,
        userName = userName,
        isBankingEntityValid = isBankingEntityValid
      )
  }

  implicit class RichBuyerCompanion(val self: Buyer.type) extends AnyVal {

    def forTest(
        user: User,
        personProfile: Option[PersonProfile] = None,
        isPassportProvided: Boolean = false,
        passportCorrelationId: Option[CorrelationId] = None,
        phoneConfirmationCode: Option[DealParty.ConfirmationCode] = None,
        isPriceApproved: Boolean = false,
        isMoneyProvided: Boolean = false,
        isReadyForMeeting: Boolean = false,
        isDocumentsUploaded: Boolean = false,
        confirmationCode: Option[DealParty.ConfirmationCode] = None,
        isPassportValid: Option[Boolean] = None,
        userName: Option[String] = None,
        isDealAccepted: Boolean = false) =
      self(
        user = user,
        personProfile = personProfile,
        isPassportProvided = isPassportProvided,
        passportCorrelationId = passportCorrelationId,
        phoneConfirmationCode = phoneConfirmationCode,
        isPriceApproved = isPriceApproved,
        isMoneyProvided = isMoneyProvided,
        isReadyForMeeting = isReadyForMeeting,
        isDocumentsUploaded = isDocumentsUploaded,
        confirmationCode = confirmationCode,
        isPassportValid = isPassportValid,
        userName = userName,
        isDealAccepted = isDealAccepted
      )
  }

  implicit class RichAutoruDealCompanion(val self: AutoruDeal.type) extends AnyVal {

    def forTest(
        id: DealId,
        dealNumber: DealNumber,
        updateInfo: Deal.UpdateInfo,
        scheduledAt: Option[Instant] = None,
        schedulerLastUpdate: Option[Instant] = None,
        state: DealState,
        dealStep: DealStep,
        sellerStep: SellerStep,
        buyerStep: BuyerStep,
        cancelInfo: Option[Deal.CancelInfo],
        meetingInfo: Deal.MeetingInfo,
        priceInfo: Deal.PriceInfo,
        totalProvidedRub: Long = 0L,
        safeDealAccount: Option[SafeDealAccount],
        escrows: List[Escrow],
        buyer: Buyer,
        seller: Option[Seller],
        notifications: Seq[Notification] = Seq.empty,
        subject: AutoruSubject,
        documents: Seq[DocumentPhotoSizes],
        flags: Deal.DealFlags = Deal.DealFlags.empty,
        isRollback: Boolean = false,
        lastOverdueNotification: Option[Instant] = None,
        payments: List[Payment] = List.empty,
        previousDealNumbers: List[DealNumber] = List.empty,
        chatRoomId: Option[ChatRoomId] = None,
        tinkoffDeal: Option[TinkoffDeal] = None) =
      self(
        id = id,
        dealNumber = dealNumber,
        updateInfo = updateInfo,
        scheduledAt = scheduledAt,
        schedulerLastUpdate = schedulerLastUpdate,
        dealBankType = DefaultDealBankType,
        state = state,
        dealStep = dealStep,
        sellerStep = sellerStep,
        buyerStep = buyerStep,
        cancelInfo = cancelInfo,
        meetingInfo = meetingInfo,
        priceInfo = priceInfo,
        totalProvidedRub = totalProvidedRub,
        safeDealAccount = safeDealAccount,
        escrows = escrows,
        buyer = buyer,
        seller = seller,
        notifications = notifications,
        subject = subject,
        documents = documents,
        flags = flags,
        isRollback = isRollback,
        lastOverdueNotification = lastOverdueNotification,
        payments = payments,
        previousDealNumbers = previousDealNumbers,
        chatRoomId = chatRoomId,
        tinkoffDeal = tinkoffDeal
      )
  }

  implicit class RichPersonProfileImplCompanion(val self: PersonProfileImpl.type) extends AnyVal {
    import ru.yandex.vertis.safe_deal.model.Entity._

    def forTest(
        id: Option[PersonProfileId] = None,
        user: Option[User] = None,
        dealId: Option[DealId] = None,
        created: Option[Instant] = None,
        name: Option[NameEntity] = None,
        passportRf: Option[PassportRfEntity] = None,
        phone: Option[PhoneEntity] = None,
        email: Option[EmailEntity] = None,
        bankingEntity: Seq[BankingEntity] = Seq.empty,
        documentPhotos: Seq[DocumentPhoto] = Seq.empty,
        incomingPayments: Seq[IncomingPayment] = Seq.empty,
        tinkoffRefillBankDetails: Seq[TinkoffRefillBankDetails] = Seq.empty) =
      self(
        id = id,
        user = user,
        dealId = dealId,
        created = created,
        name = name,
        passportRf = passportRf,
        phone = phone,
        email = email,
        bankingEntity = bankingEntity,
        documentPhotos = documentPhotos,
        incomingPayments = incomingPayments,
        tinkoffRefillBankDetails = tinkoffRefillBankDetails
      )
  }
}
