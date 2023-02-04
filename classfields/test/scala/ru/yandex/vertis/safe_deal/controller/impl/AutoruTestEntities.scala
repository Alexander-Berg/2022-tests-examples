package ru.yandex.vertis.safe_deal.controller.impl

import java.time.{Instant, LocalTime}
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.{@@, Tagger}
import ru.yandex.vertis.palma.encrypted.content.{Image => EncryptedImage}
import ru.auto.api.api_offer_model.Category
import ru.yandex.vertis.safe_deal.model.Arbitraries.DealIdArb
import ru.yandex.vertis.safe_deal.model.Arbitraries.DealNumberArb
import ru.yandex.vertis.safe_deal.model.AutoruDeal.Offer
import ru.yandex.vertis.safe_deal.model.DealParty.ConfirmationCode
import ru.yandex.vertis.safe_deal.model.Entity.{BankingEntity, NameEntity, PassportRfEntity, PhoneEntity}
import ru.yandex.vertis.safe_deal.model._
import ru.yandex.vertis.safe_deal.model.TestSyntax._
import ru.yandex.vertis.safe_deal.proto.model.DocumentPhoto.DocumentType
import ru.yandex.vertis.safe_deal.proto.common.{CancellationReason, CancelledBy, DealState, DealStep}
import ru.yandex.vertis.safe_deal.proto.common.BuyerStep._
import ru.yandex.vertis.safe_deal.proto.common.SellerStep._
import com.softwaremill.quicklens._
import ru.yandex.vertis.safe_deal.model.Deal.{CancelInfo, PriceInfo}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.AutoUser
import ru.yandex.vertis.zio_baker.util.EmptyString

object AutoruTestEntities {

  val dealId: DealId = DealIdArb.arbitrary.sample.get
  val dealNumber: DealNumber = DealNumberArb.arbitrary.sample.get
  val created: Instant = Instant.now()
  val buyerUser: AutoUser = AutoUser("user:1234".taggedWith[zio_baker.Tag.UserId])
  val sellerUser: AutoUser = AutoUser("user:1337".taggedWith[zio_baker.Tag.UserId])
  val offer: Offer = Offer(Category.CARS, "1234567890-abcdefg".taggedWith[zio_baker.Tag.OfferId])
  val vin: String @@ Tag.VinNumber = "ABCDEFG0123456789".taggedWith[Tag.VinNumber]
  val autoruSubject: AutoruSubject = AutoruSubject(offer.some, vin.some, "VAZ".some, "2106".some, None, None, None)

  val newDeal: AutoruDeal = AutoruDeal.forTest(
    id = dealId,
    dealNumber = dealNumber,
    updateInfo = Deal.UpdateInfo(
      created = created,
      updated = created,
      completed = None,
      stepUpdated = created,
      buyerStepUpdated = created,
      sellerStepUpdated = created
    ),
    state = DealState.IN_PROGRESS,
    dealStep = DealStep.DEAL_CREATED,
    sellerStep = SELLER_ACCEPTING_DEAL,
    buyerStep = BUYER_AWAITING_ACCEPT,
    priceInfo = Deal.PriceInfo(
      sellingPriceRub = 500_000L.some,
      feeAmountRub = 5_000L.some,
      totalPriceRub = 505_000L.some
    ),
    totalProvidedRub = 0L,
    safeDealAccount = None,
    cancelInfo = None,
    meetingInfo = Deal.MeetingInfo(None, None, None),
    escrows = List.empty,
    buyer = Buyer.forTest(buyerUser, isDealAccepted = true),
    seller = Seller.forTest(sellerUser).some,
    subject = autoruSubject,
    documents = Seq.empty
  )

  val sellerPersonProfile: PersonProfileImpl = PersonProfileImpl.forTest(
    None,
    None, // sellerUser.some,
    None,
    None,
    NameEntity(
      "Аркадий".taggedWith[Tag.Name],
      "Аркадиев".taggedWith[Tag.Surname],
      "Аркадиевич".taggedWith[Tag.Patronymic].some
    ).some,
    PassportRfEntity(
      "2010".taggedWith[Tag.DocSeries],
      "123456".taggedWith[Tag.DocNumber],
      "02.01.2010".taggedWith[Tag.StringDate],
      "300-200".taggedWith[Tag.DepartCode],
      "УФМС".taggedWith[Tag.DepartName],
      "Урюпиеск".taggedWith[Tag.BirthPlace],
      "02.01.1990".taggedWith[Tag.StringDate],
      "г. Урюпиеск, ул. Ленина, д. 2".taggedWith[Tag.Address]
    ).some,
    PhoneEntity(phone = Phone("+78005553535"), isConfirmed = true).some
  )

  val newSellerPassportRf: Option[PassportRfEntity] = sellerPersonProfile.passportRf.map(
    _.copy(
      series = "2011".taggedWith[Tag.DocSeries]
    )
  )

  val bicEmpty: Bic = EmptyString.taggedWith[Tag.Bic]
  val accountNumberEmpty: AccountNumber = EmptyString.taggedWith[Tag.AccountNumber]
  val sellerFullName: Name = "Аркадиев Аркадий Аркадиевич".taggedWith[Tag.Name]

  val bankingEntityEmpty: BankingEntity = BankingEntity(
    bicEmpty,
    accountNumberEmpty,
    sellerFullName,
    None
  )

  val bankingEntity: BankingEntity = BankingEntity(
    "044525225".taggedWith[Tag.Bic], // Сбер
    "40817810938160925982".taggedWith[Tag.AccountNumber],
    "Аркадиев Аркадий Аркадиевич".taggedWith[Tag.Name],
    None
  )

  val newBankingEntity: BankingEntity = BankingEntity(
    "044525974".taggedWith[Tag.Bic], // тинек
    "40817810200062517536".taggedWith[Tag.AccountNumber],
    "Аркадиев Аркадий Аркадиевич".taggedWith[Tag.Name],
    None
  )

  val buyerPersonProfile: PersonProfileImpl = PersonProfileImpl.forTest(
    None,
    None, // buyerUser.some,
    None,
    None,
    NameEntity(
      "Иван".taggedWith[Tag.Name],
      "Иванов".taggedWith[Tag.Surname],
      None
    ).some,
    PassportRfEntity(
      "2000".taggedWith[Tag.DocSeries],
      "654321".taggedWith[Tag.DocNumber],
      "02.01.2010".taggedWith[Tag.StringDate],
      "200-300".taggedWith[Tag.DepartCode],
      "УФМС".taggedWith[Tag.DepartName],
      "Москва".taggedWith[Tag.BirthPlace],
      "02.01.1990".taggedWith[Tag.StringDate],
      "г. Москва, ул. Ленина, д. 2".taggedWith[Tag.Address]
    ).some,
    PhoneEntity(phone = Phone("+79501234567"), isConfirmed = true).some
  )

  val ptsInfo: AutoruSubject.PtsCarInfo = AutoruSubject.PtsCarInfo(
    displacement = 3000.some,
    engineModel = "qweqweqw".some,
    engineNumber = "2312321".some,
    chassisNumber = "321321".some,
    bodyNumber = "12345678901234567".some,
    seriesNumber = "23TF234523".taggedWith[Tag.PtsSeriesNumber].some,
    issueDate = "02.01.2004".taggedWith[Tag.StringDate].some,
    issuer = "ГИБДД".some,
    licensePlate = "К222КК136".some
  )

  val stsInfo: AutoruSubject.StsCarInfo = AutoruSubject.StsCarInfo(
    seriesNumber = "23TF234523".taggedWith[Tag.StsSeriesNumber].some,
    issueDate = "02.01.2004".taggedWith[Tag.StringDate].some,
    issuer = "ГИБДД".some
  )

  val carInfo: AutoruSubject.OfferCarInfo = AutoruSubject.OfferCarInfo(
    year = 2015,
    mileage = 20000,
    horsePower = 90,
    subcategory = "Легковая",
    color = "Синий"
  )

  val newCarInfo: AutoruSubject.OfferCarInfo = AutoruSubject.OfferCarInfo(
    year = 2015,
    mileage = 20100,
    horsePower = 90,
    subcategory = "Легковая",
    color = "Синий"
  )

  val buyerConfirmationCode: ConfirmationCode = ConfirmationCode(LocalTime.now(), "123456", isConfirmed = false)
  val sellerConfirmationCode: ConfirmationCode = ConfirmationCode(LocalTime.now(), "654321", isConfirmed = false)

  val acceptedDeal: AutoruDeal = newDeal
    .modify(_.dealStep)
    .setTo(DealStep.DEAL_INVITE_ACCEPTED)
    .modify(_.sellerStep)
    .setTo(SELLER_INTRODUCING_PASSPORT_DETAILS)
    .modify(_.buyerStep)
    .setTo(BUYER_INTRODUCING_PASSPORT_DETAILS)
    .modify(_.seller.each.isDealAccepted)
    .setTo(true)

  val acceptedDealWithCheckingSellerPassport: AutoruDeal = acceptedDeal
    .modify(_.sellerStep)
    .setTo(SELLER_CHECKING_PASSPORT_DETAILS)
    .modify(_.seller.each.personProfile)
    .setTo(sellerPersonProfile.some)
    .modify(_.seller.each.personProfile.each.when[PersonProfileImpl].bankingEntity)
    .setTo(Seq(bankingEntityEmpty))
    .modify(_.seller.each.isPassportProvided)
    .setTo(true)

  val acceptedDealValidatedWithSellerPassport: AutoruDeal = acceptedDealWithCheckingSellerPassport
    .modify(_.sellerStep)
    .setTo(SELLER_INTRODUCING_SUBJECT_DETAILS)
    .modify(_.seller.each.isPassportValid)
    .setTo(true.some)

  val acceptedDealWithCheckingBuyerPassport: AutoruDeal = acceptedDeal
    .modify(_.buyerStep)
    .setTo(BUYER_CHECKING_PASSPORT_DETAILS)
    .modify(_.buyer.personProfile)
    .setTo(buyerPersonProfile.some)
    .modify(_.buyer.isPassportProvided)
    .setTo(true)
    .modify(_.seller.each.isPassportValid)
    .setTo(None)

  val acceptedDealWithInvalidBuyerPassport: AutoruDeal = acceptedDealWithCheckingBuyerPassport
    .modify(_.buyerStep)
    .setTo(BUYER_INVALID_PASSPORT)
    .modify(_.buyer.isPassportValid)
    .setTo(false.some)

  val acceptedDealWithValidatedBuyerPassport: AutoruDeal = acceptedDealWithCheckingBuyerPassport
    .modify(_.buyerStep)
    .setTo(BUYER_INTRODUCING_SELLING_PRICE)
    .modify(_.buyer.isPassportValid)
    .setTo(true.some)

  val acceptedDealWithSellerAndCheckingBuyerPassport: AutoruDeal = acceptedDealValidatedWithSellerPassport
    .modify(_.buyerStep)
    .setTo(BUYER_CHECKING_PASSPORT_DETAILS)
    .modify(_.buyer.personProfile)
    .setTo(buyerPersonProfile.some)
    .modify(_.buyer.isPassportProvided)
    .setTo(true)

  val acceptedDealWithCheckingSellerAndBuyerPassport: AutoruDeal = acceptedDealWithValidatedBuyerPassport
    .modify(_.sellerStep)
    .setTo(SELLER_CHECKING_PASSPORT_DETAILS)
    .modify(_.seller.each.personProfile)
    .setTo(
      sellerPersonProfile
        .modify(_.bankingEntity)
        .setTo(Seq(bankingEntityEmpty))
        .some
    )
    .modify(_.seller.each.isPassportProvided)
    .setTo(true)

  val acceptedDealWithSellerAndBuyerPassport: AutoruDeal = acceptedDealWithValidatedBuyerPassport
    .modify(_.sellerStep)
    .setTo(SELLER_INTRODUCING_SUBJECT_DETAILS)
    .modify(_.seller.each.personProfile)
    .setTo(sellerPersonProfile.some)
    .modify(_.seller.each.isPassportProvided)
    .setTo(true)
    .modify(_.seller.each.isPassportValid)
    .setTo(true.some)

  val acceptedDealWithSellerPassportAndSubjectDetails: AutoruDeal = acceptedDealValidatedWithSellerPassport
    .modify(_.sellerStep)
    .setTo(SELLER_INTRODUCED_SUBJECT_DETAILS)
    .modify(_.subject)
    .setTo(autoruSubject.copy(ptsInfo = ptsInfo.some, stsInfo = stsInfo.some, carInfo = carInfo.some))
    .modify(_.seller.each.isSubjectInfoProvided)
    .setTo(true)

  val dealWithPassportsAndSubjectDetails: AutoruDeal = acceptedDealWithSellerAndBuyerPassport
    .modify(_.sellerStep)
    .setTo(SELLER_INTRODUCED_SUBJECT_DETAILS)
    .modify(_.subject)
    .setTo(autoruSubject.copy(ptsInfo = ptsInfo.some, stsInfo = stsInfo.some, carInfo = carInfo.some))
    .modify(_.seller.each.isSubjectInfoProvided)
    .setTo(true)

  val dealWithSellingPriceProvided: AutoruDeal = dealWithPassportsAndSubjectDetails
    .modify(_.sellerStep)
    .setTo(SELLER_APPROVING_SELLING_PRICE)
    .modify(_.buyerStep)
    .setTo(BUYER_INTRODUCED_SELLING_PRICE)
    .modify(_.priceInfo)
    .setTo(
      Deal.PriceInfo(
        sellingPriceRub = 800_000L.some,
        totalPriceRub = 805_000L.some,
        feeAmountRub = 5_000L.some
      )
    )
    .modify(_.buyer.isPriceApproved)
    .setTo(true)

  val dealWithSellingPriceApproved: AutoruDeal = dealWithSellingPriceProvided
    .modify(_.sellerStep)
    .setTo(SELLER_INTRODUCING_ACCOUNT_DETAILS)
    .modify(_.buyerStep)
    .setTo(BUYER_PROVIDING_MONEY)
    .modify(_.seller.each.isPriceApproved)
    .setTo(true)

  val dealWithAccountDetails: AutoruDeal = dealWithSellingPriceApproved
    .modify(_.sellerStep)
    .setTo(SELLER_CHECKING_ACCOUNT_DETAILS)
    .modify(_.buyerStep)
    .setTo(BUYER_PROVIDING_MONEY)
    .modify(_.seller.each.isBankingDetailsProvided)
    .setTo(true)
    .modify(_.seller.each.isBankingEntityValid)
    .setTo(None)
    .modify(_.seller.each.personProfile.each.when[PersonProfileImpl].bankingEntity)
    .setTo(Seq(bankingEntity))

  val dealWithMoneyProvided: AutoruDeal = dealWithAccountDetails
    .modify(_.sellerStep)
    .setTo(SELLER_INTRODUCED_ACCOUNT_DETAILS)
    .modify(_.buyerStep)
    .setTo(BUYER_INTRODUCING_MEETING_DETAILS)
    .modify(_.buyer.personProfile.each.when[PersonProfileImpl].bankingEntity)
    .setTo(Seq(bankingEntity))
    .modify(_.buyer.isMoneyProvided)
    .setTo(true)
    .modify(_.seller.each.isBankingEntityValid)
    .setTo(true.some)

  val dealWithMeetingDetailsProvided: AutoruDeal = dealWithMoneyProvided
    .modify(_.sellerStep)
    .setTo(SELLER_READY_FOR_MEETING)
    .modify(_.buyerStep)
    .setTo(BUYER_READY_FOR_MEETING)
    .modify(_.buyer.isReadyForMeeting)
    .setTo(true)
    .modify(_.meetingInfo.meetingDate)
    .setTo("15.06.2021".taggedWith[Tag.StringDate].some)
    .modify(_.meetingInfo.geobaseId)
    .setTo(2L.taggedWith[zio_baker.Tag.GeobaseId].some)

  val dealWithNewMeetingDetailsProvided: AutoruDeal = dealWithMoneyProvided
    .modify(_.sellerStep)
    .setTo(SELLER_READY_FOR_MEETING)
    .modify(_.buyerStep)
    .setTo(BUYER_READY_FOR_MEETING)
    .modify(_.buyer.isReadyForMeeting)
    .setTo(true)
    .modify(_.meetingInfo.meetingDate)
    .setTo("15.06.2021".taggedWith[Tag.StringDate].some)
    .modify(_.meetingInfo.geobaseId)
    .setTo(213L.taggedWith[zio_baker.Tag.GeobaseId].some)

  val dealWithSignedDocumentUploadedFirstPhoto: AutoruDeal = dealWithMeetingDetailsProvided
    .modify(_.buyer.personProfile.each.when[PersonProfileImpl].documentPhotos)
    .setTo(
      Seq(
        DocumentPhoto(
          DocumentType.DEAL_CONTRACT,
          EncryptedImage.defaultInstance,
          isDeleted = false,
          "DEAL_CONTRACT:3:201756".taggedWith[Tag.DocumentId],
          None,
          isJustAdded = false
        )
      )
    )

  val dealWithSignedDocumentUploadedSecondPhoto: AutoruDeal = withPhotos(dealWithMeetingDetailsProvided)

  val dealWithSignedDocsCompleteUpload: AutoruDeal = withPhotos(
    dealWithSignedDocumentUploadedFirstPhoto
      .modify(_.sellerStep)
      .setTo(SELLER_APPROVING_DOCS)
      .modify(_.buyerStep)
      .setTo(BUYER_UPLOADED_DOCS)
      .modify(_.buyer.isDocumentsUploaded)
      .setTo(true)
  )

  val dealWithSignedDocsApproved: AutoruDeal = dealWithSignedDocsCompleteUpload
    .modify(_.sellerStep)
    .setTo(SELLER_APPROVING_DEAL)
    .modify(_.buyerStep)
    .setTo(BUYER_UPLOADED_DOCS)
    .modify(_.seller.each.isDocumentsApproved)
    .setTo(true)

  val cancelledDealBeforeMoneyTransferred: AutoruDeal = dealWithAccountDetails
    .modify(_.dealStep)
    .setTo(DealStep.DEAL_CANCELLED)
    .modify(_.cancelInfo)
    .setTo(CancelInfo(CancelledBy.CANCELLED_BY_SELLER, CancellationReason.SELLER_DOESNT_WANT_TO_SELL, None).some)

  val restoredDeal: AutoruDeal = cancelledDealBeforeMoneyTransferred
    .modify(_.dealStep)
    .setTo(DealStep.DEAL_CREATED)
    .modify(_.state)
    .setTo(DealState.UNKNOWN_STATE)
    .modify(_.seller.each.personProfile.each.when[PersonProfileImpl].phone.each.isConfirmed)
    .setTo(false)
    .modify(_.buyer.personProfile.each.when[PersonProfileImpl].phone.each.isConfirmed)
    .setTo(false)
    .modify(_.cancelInfo)
    .setTo(None)
    .modify(_.dealNumber)
    .setTo(0L.taggedWith[Tag.DealNumber])
    .modify(_.priceInfo)
    .setTo(PriceInfo(Some(900000), Some(8000), Some(908000)))
    .modify(_.buyer.isDealAccepted)
    .setTo(false)
    .modify(_.buyer.isPassportProvided)
    .setTo(false)
    .modify(_.buyer.isPassportValid)
    .setTo(None)
    .modify(_.seller.each.isPassportProvided)
    .setTo(false)
    .modify(_.seller.each.isPassportValid)
    .setTo(None)
    .modify(_.seller.each.isSubjectInfoProvided)
    .setTo(false)
    .modify(_.seller.each.isBankingDetailsProvided)
    .setTo(false)
    .modify(_.previousDealNumbers)
    .using(_ :+ cancelledDealBeforeMoneyTransferred.dealNumber)

  val dealWithSellerCodeRequested: AutoruDeal = dealWithSignedDocsApproved
    .modify(_.seller.each.confirmationCode)
    .setTo(sellerConfirmationCode.some)

  val dealWithSellerCodeApproved: AutoruDeal = dealWithSellerCodeRequested
    .modify(_.sellerStep)
    .setTo(SELLER_APPROVED_DEAL)
    .modify(_.buyerStep)
    .setTo(BUYER_APPROVING_DEAL)
    .modify(_.seller.each.confirmationCode.each.isConfirmed)
    .setTo(true)

  val dealWithBuyerCodeRequested: AutoruDeal = dealWithSellerCodeApproved
    .modify(_.buyer.confirmationCode)
    .setTo(buyerConfirmationCode.some)

  val dealWithBuyerCodeApproved: AutoruDeal = dealWithBuyerCodeRequested
    .modify(_.dealStep)
    .setTo(DealStep.DEAL_CONFIRMED)
    .modify(_.sellerStep)
    .setTo(SELLER_APPROVED_DEAL)
    .modify(_.buyerStep)
    .setTo(BUYER_APPROVED_DEAL)
    .modify(_.buyer.confirmationCode.each.isConfirmed)
    .setTo(true)

  /**
    * For rollback
    */
  val dealWithNewAccountDetailsRollbackAfterApprovedDocs: AutoruDeal = dealWithSignedDocsApproved
    .modify(_.sellerStep)
    .setTo(SELLER_CHECKING_ACCOUNT_DETAILS)
    .modify(_.buyerStep)
    .setTo(BUYER_AWAITING_SELLER_INFO)
    .modify(_.seller.each.isDocumentsApproved)
    .setTo(false)
    .modify(_.seller.each.isBankingEntityValid)
    .setTo(None)
    .modify(_.buyer.isMoneyProvided)
    .setTo(true)
    .modify(_.buyer.isDocumentsUploaded)
    .setTo(false)
    .modify(_.seller.each.personProfile.each.when[PersonProfileImpl].bankingEntity)
    .setTo(Seq(newBankingEntity))

  val dealWithNewSubjectDetailsRollbackAfterApprovedDocs: AutoruDeal = dealWithSignedDocsApproved
    .modify(_.sellerStep)
    .setTo(SELLER_READY_FOR_MEETING)
    .modify(_.buyerStep)
    .setTo(BUYER_READY_FOR_MEETING)
    .modify(_.seller.each.isDocumentsApproved)
    .setTo(false)
    .modify(_.buyer.isDocumentsUploaded)
    .setTo(false)
    .modify(_.subject.carInfo)
    .setTo(newCarInfo.some)

  val dealWithNewSellingPriceRollbackAfterApprovedDocs: AutoruDeal = dealWithSignedDocsApproved
    .modify(_.sellerStep)
    .setTo(SELLER_APPROVING_SELLING_PRICE)
    .modify(_.buyerStep)
    .setTo(BUYER_INTRODUCED_SELLING_PRICE)
    .modify(_.seller.each.isDocumentsApproved)
    .setTo(false)
    .modify(_.seller.each.isPriceApproved)
    .setTo(false)
    .modify(_.buyer.isPriceApproved)
    .setTo(true)
    .modify(_.buyer.isMoneyProvided)
    .setTo(false)
    .modify(_.buyer.isDocumentsUploaded)
    .setTo(false)
    .modify(_.priceInfo)
    .setTo(
      Deal.PriceInfo(
        sellingPriceRub = 1_100_000L.some,
        totalPriceRub = 1_105_000L.some,
        feeAmountRub = 5_000L.some
      )
    )

  val dealWithNewSellerPassportRollbackAfterApprovedDocs: AutoruDeal = dealWithSignedDocsApproved
    .modify(_.sellerStep)
    .setTo(SELLER_CHECKING_PASSPORT_DETAILS)
    .modify(_.buyerStep)
    .setTo(BUYER_AWAITING_SELLER_INFO)
    .modify(_.seller.each.isDocumentsApproved)
    .setTo(false)
    .modify(_.seller.each.isPassportProvided)
    .setTo(true)
    .modify(_.seller.each.isPassportValid)
    .setTo(None)
    .modify(_.buyer.isDocumentsUploaded)
    .setTo(false)
    .modify(_.seller.each.personProfile.each.when[PersonProfileImpl].passportRf)
    .setTo(newSellerPassportRf)

  def withPhotos(deal: AutoruDeal, isDeleted: Boolean = false): AutoruDeal =
    deal
      .modify(_.buyer.personProfile.each.when[PersonProfileImpl].documentPhotos)
      .setTo(
        Seq(
          DocumentPhoto(
            DocumentType.DEAL_CONTRACT,
            EncryptedImage.defaultInstance,
            isDeleted,
            "DEAL_CONTRACT:3:201756".taggedWith[Tag.DocumentId],
            None,
            isJustAdded = false
          ),
          DocumentPhoto(
            DocumentType.DEAL_CONTRACT,
            EncryptedImage.defaultInstance,
            isDeleted,
            "DEAL_CONTRACT:3:186255".taggedWith[Tag.DocumentId],
            None,
            isJustAdded = false
          )
        )
      )
}
