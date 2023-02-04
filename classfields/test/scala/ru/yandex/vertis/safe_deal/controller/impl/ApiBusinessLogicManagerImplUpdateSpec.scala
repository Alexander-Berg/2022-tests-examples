package ru.yandex.vertis.safe_deal.controller.impl

import ru.yandex.vertis.safe_deal.controller.impl.AutoruTestEntities._
import cats.implicits.catsSyntaxOptionId
import zio.test.Assertion._
import zio.test._
import com.softwaremill.tagging.Tagger

import java.time.LocalTime
import com.softwaremill.quicklens._
import com.google.protobuf.ByteString
import common.zio.features.testkit.FeaturesTest
import ru.yandex.vertis.safe_deal.util.RichAutoruModel._
import zio.test.TestAspect.sequential
import ru.yandex.vertis.safe_deal.model.DealUpdateRequest._
import ru.yandex.vertis.safe_deal.model.PartyAction._
import ru.yandex.vertis.safe_deal.model._
import ru.yandex.vertis.safe_deal.proto.common.{BuyerStep, CancellationReason, DealStep, ParticipantType, SellerStep}
import zio.test.AssertionM.Render.param
import zio.{Has, URLayer, ZIO, ZLayer}
import ru.yandex.vertis.safe_deal.controller._
import ru.yandex.vertis.safe_deal.dictionary.{ColorDictionary, RegionsDictionary}
import ru.yandex.vertis.safe_deal.model.DealCreateRequest.AutoruSubjectValueByOffer
import ru.yandex.vertis.safe_deal.model.event.DealChangedEvent.Event
import ru.yandex.vertis.safe_deal.proto.model.DocumentPhoto.DocumentType
import ru.yandex.vertis.zio_baker.zio.client.passport.PassportClient
import ru.yandex.vertis.zio_baker.zio.client.vin.VinDecoderClient
import ru.yandex.vertis.zio_baker.zio.client.vos.VosAutoruClient
import zio.test.environment.TestEnvironment

object ApiBusinessLogicManagerImplUpdateSpec extends DefaultRunnableSpec {

  private lazy val regionsDictionaryMock: URLayer[Any, RegionsDictionary] = ZLayer.succeed(new RegionsDictionaryMock())

  private lazy val colorDictionaryMock: URLayer[Any, ColorDictionary] = ZLayer.succeed(new ColorDictionaryMock())

  private lazy val vosAutoruClientMock: URLayer[Any, VosAutoruClient] = ZLayer.succeed(new VosAutoruClientMock())

  private lazy val passportClientMock: URLayer[Any, PassportClient] = ZLayer.succeed(new PassportClientMock())

  private lazy val commissionsCalculatorMock: URLayer[Any, CommissionTariffCalculator] =
    ZLayer.succeed(new CommissionTariffCalculatorMock())

  private lazy val documentControllerMock: URLayer[Any, DocumentController] =
    ZLayer.succeed(new DocumentControllerMock())

  private lazy val profilePhoneControllerMock: URLayer[Any, ProfilePhoneController] =
    ZLayer.succeed(new ProfilePhoneControllerMock())

  private lazy val codeConfirmerMock: URLayer[Any, Has[CodeConfirmer]] =
    ZLayer.succeed(new CodeConfirmerMock())

  private lazy val env =
    codeConfirmerMock ++ vosAutoruClientMock ++ passportClientMock ++ regionsDictionaryMock ++
      commissionsCalculatorMock ++ RollbackLogicManager.live ++ documentControllerMock ++ colorDictionaryMock ++
      FeaturesTest.test ++ profilePhoneControllerMock ++ VinDecoderClient.stub >>> ApiBusinessLogicManager.live

  case class UpdateRequestSuccessTestCase(
      description: String,
      deal: AutoruDeal,
      request: AutoruPayloadParty,
      expected: UpdateRequestAction,
      party: Party.Value,
      withRollback: Boolean)

  case class UpdateRequestFailureTestCase(
      description: String,
      deal: AutoruDeal,
      request: AutoruPayloadParty,
      expected: Throwable,
      party: Party.Value,
      withRollback: Boolean)

  private val updateRequestTestCases: Seq[UpdateRequestSuccessTestCase] = Seq(
    /**
      * Lifecycle
      */
    UpdateRequestSuccessTestCase(
      "Accept deal - Success",
      newDeal,
      AutoruPayloadPartyBySeller(PartyNewDealApproveAction(true)),
      UpdateRequestAction(acceptedDeal, Event.Party.AcceptedDealRequest(ParticipantType.SELLER).some),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide seller passport for accepted deal - Success",
      acceptedDeal,
      AutoruPayloadPartyBySeller(
        PartyProfileUpdateAction(
          sellerPersonProfile.name,
          sellerPersonProfile.passportRf,
          sellerPersonProfile.phone,
          None
        )
      ),
      UpdateRequestAction(
        acceptedDealWithCheckingSellerPassport,
        Event.Party.FilledProfile(ParticipantType.SELLER).some
      ),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide buyer passport for accepted deal - Success",
      acceptedDeal,
      AutoruPayloadPartyByBuyer(
        PartyProfileUpdateAction(
          buyerPersonProfile.name,
          buyerPersonProfile.passportRf,
          buyerPersonProfile.phone,
          None
        )
      ),
      UpdateRequestAction(acceptedDealWithCheckingBuyerPassport, Event.Party.FilledProfile(ParticipantType.BUYER).some),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide buyer passport for invalid passport - Success",
      acceptedDealWithInvalidBuyerPassport,
      AutoruPayloadPartyByBuyer(
        PartyProfileUpdateAction(
          buyerPersonProfile.name,
          buyerPersonProfile.passportRf,
          buyerPersonProfile.phone,
          None
        )
      ),
      UpdateRequestAction(acceptedDealWithCheckingBuyerPassport, Event.Party.FilledProfile(ParticipantType.BUYER).some),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide buyer passport after provided seller one - Success",
      acceptedDealValidatedWithSellerPassport,
      AutoruPayloadPartyByBuyer(
        PartyProfileUpdateAction(
          buyerPersonProfile.name,
          buyerPersonProfile.passportRf,
          buyerPersonProfile.phone,
          None
        )
      ),
      UpdateRequestAction(
        acceptedDealWithSellerAndCheckingBuyerPassport,
        Event.Party.FilledProfile(ParticipantType.BUYER).some
      ),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide seller passport after provided buyer one - Success",
      acceptedDealWithValidatedBuyerPassport,
      AutoruPayloadPartyBySeller(
        PartyProfileUpdateAction(
          sellerPersonProfile.name,
          sellerPersonProfile.passportRf,
          sellerPersonProfile.phone,
          None
        )
      ),
      UpdateRequestAction(
        acceptedDealWithCheckingSellerAndBuyerPassport,
        Event.Party.FilledProfile(ParticipantType.SELLER).some
      ),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide subject info after seller passport - Success",
      acceptedDealValidatedWithSellerPassport,
      AutoruPayloadPartyBySeller(
        PartySubjectInfoUpdateAction(PartySubjectAutoruInfo(ptsInfo.some, stsInfo.some, carInfo.some))
      ),
      UpdateRequestAction(acceptedDealWithSellerPassportAndSubjectDetails, Event.Party.Seller.FilledCarData.some),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide subject info after buyer and seller passport - Success",
      acceptedDealWithSellerAndBuyerPassport,
      AutoruPayloadPartyBySeller(
        PartySubjectInfoUpdateAction(PartySubjectAutoruInfo(ptsInfo.some, stsInfo.some, carInfo.some))
      ),
      UpdateRequestAction(dealWithPassportsAndSubjectDetails, Event.Party.Seller.FilledCarData.some),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide selling price - Success",
      dealWithPassportsAndSubjectDetails,
      AutoruPayloadPartyByBuyer(PartySellingPriceUpdateAction(800000L)),
      UpdateRequestAction(dealWithSellingPriceProvided, Event.Party.FilledCarPrice(ParticipantType.BUYER).some),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Approve selling price - Success",
      dealWithSellingPriceProvided,
      AutoruPayloadPartyBySeller(PartySellingPriceApproveAction(800000L)),
      UpdateRequestAction(dealWithSellingPriceApproved, Event.Party.Seller.AcceptedCarPrice.some),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide account details - Success",
      dealWithSellingPriceApproved,
      AutoruPayloadPartyBySeller(PartySellerBankAccountUpdateAction(bankingEntity)),
      UpdateRequestAction(dealWithAccountDetails, Event.Party.Seller.FilledCredentials.some),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Provide meeting details - Success",
      dealWithMoneyProvided,
      AutoruPayloadPartyByBuyer(
        PartyPersonalMeetingUpdateAction(
          dealWithMeetingDetailsProvided.meetingInfo.meetingDate.get,
          dealWithMeetingDetailsProvided.meetingInfo.geobaseId.get
        )
      ),
      UpdateRequestAction(dealWithMeetingDetailsProvided, Event.Party.Buyer.FilledMeeting.some),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Upload first photo - Success",
      dealWithMeetingDetailsProvided,
      AutoruPayloadPartyByBuyer(PartyUploadPhotoAction(DocumentType.DEAL_CONTRACT, ByteString.copyFromUtf8("qwe"))),
      UpdateRequestAction(dealWithSignedDocumentUploadedFirstPhoto, None),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Upload second photo - Success",
      dealWithSignedDocumentUploadedFirstPhoto,
      AutoruPayloadPartyByBuyer(PartyUploadPhotoAction(DocumentType.DEAL_CONTRACT, ByteString.copyFromUtf8("asd"))),
      UpdateRequestAction(dealWithSignedDocumentUploadedSecondPhoto, None),
      Party.Buyer,
      withRollback = false
    ),
//    UpdateRequestSuccessTestCase(
//      "Complete photo upload - Success",
//      dealWithSignedDocumentUploadedFirstPhoto,
//      AutoruPayloadPartyByBuyer(PartyUploadPhotoAction(DocumentType.DEAL_CONTRACT, ByteString.copyFromUtf8("asd"))),
//      UpdateRequestAction(dealWithSignedDocsCompleteUpload, Event.Party.Buyer.UploadedDocument.some),
//      Party.Buyer,
//      withRollback = false
//    ),
    UpdateRequestSuccessTestCase(
      "Approve photos - Success",
      dealWithSignedDocsCompleteUpload,
      AutoruPayloadPartyBySeller(PartySellerApprovePhotosAction()),
      UpdateRequestAction(dealWithSignedDocsApproved, Event.Party.Seller.ConfirmedDocument.some),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Seller requested code - Success",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyBySeller(PartySmsCodeRequestAction()),
      UpdateRequestAction(
        dealWithSellerCodeRequested,
        Event.Party.RequestedConfirmationCode(ParticipantType.SELLER).some
      ),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Seller confirmed code - Success",
      dealWithSellerCodeRequested,
      AutoruPayloadPartyBySeller(
        PartySmsCodeConfirmAction(dealWithSellerCodeRequested.seller.get.confirmationCode.get.code)
      ),
      UpdateRequestAction(
        dealWithSellerCodeApproved,
        Event.Party.ConfirmedDeal(ParticipantType.SELLER).some
      ),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Buyer requested code - Success",
      dealWithSellerCodeApproved,
      AutoruPayloadPartyByBuyer(PartySmsCodeRequestAction()),
      UpdateRequestAction(
        dealWithBuyerCodeRequested,
        Event.Party.RequestedConfirmationCode(ParticipantType.BUYER).some
      ),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Buyer confirmed code - Success",
      dealWithBuyerCodeRequested,
      AutoruPayloadPartyByBuyer(PartySmsCodeConfirmAction(dealWithBuyerCodeApproved.buyer.confirmationCode.get.code)),
      UpdateRequestAction(
        dealWithBuyerCodeApproved,
        Event.Party.ConfirmedDeal(ParticipantType.BUYER).some
      ),
      Party.Buyer,
      withRollback = false
    ),
    /**
      * Rollbacks
      */
    UpdateRequestSuccessTestCase(
      "Remove photos - Success rollback after approved docs",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyByBuyer(PartyRemoveDocumentAction(DocumentType.DEAL_CONTRACT, None)),
      UpdateRequestAction(
        withPhotos(dealWithMeetingDetailsProvided, isDeleted = true).copy(isRollback = true),
        None
      ),
      Party.Buyer,
      withRollback = true
    ),
    UpdateRequestSuccessTestCase(
      "Provide meeting details - Success rollback after approved docs",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyByBuyer(
        PartyPersonalMeetingUpdateAction(
          dealWithNewMeetingDetailsProvided.meetingInfo.meetingDate.get,
          dealWithNewMeetingDetailsProvided.meetingInfo.geobaseId.get
        )
      ),
      UpdateRequestAction(
        withPhotos(dealWithNewMeetingDetailsProvided, isDeleted = true).copy(isRollback = true),
        Event.Party.Buyer.EditedMeeting.some
      ),
      Party.Buyer,
      withRollback = true
    ),
    UpdateRequestSuccessTestCase(
      "Provide new subject info - Success rollback after approved docs",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyBySeller(
        PartySubjectInfoUpdateAction(PartySubjectAutoruInfo(ptsInfo.some, stsInfo.some, newCarInfo.some))
      ),
      UpdateRequestAction(
        withPhotos(dealWithNewSubjectDetailsRollbackAfterApprovedDocs, isDeleted = true).copy(isRollback = true),
        Event.Party.Seller.EditedCarData.some
      ),
      Party.Seller,
      withRollback = true
    ),
    UpdateRequestSuccessTestCase(
      "Provide new selling price - Success rollback after approved docs",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyByBuyer(PartySellingPriceUpdateAction(1100000L)),
      UpdateRequestAction(
        withPhotos(dealWithNewSellingPriceRollbackAfterApprovedDocs, isDeleted = true).copy(isRollback = true),
        Event.Party.EditedCarPrice(ParticipantType.BUYER).some
      ),
      Party.Buyer,
      withRollback = true
    ),
    UpdateRequestSuccessTestCase(
      "Provide seller passport - Success rollback after approved docs",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyBySeller(
        PartyProfileUpdateAction(
          sellerPersonProfile.name,
          newSellerPassportRf,
          sellerPersonProfile.phone,
          None
        )
      ),
      UpdateRequestAction(
        withPhotos(dealWithNewSellerPassportRollbackAfterApprovedDocs, isDeleted = true).copy(isRollback = true),
        Event.Party.EditedProfile(ParticipantType.SELLER).some
      ),
      Party.Seller,
      withRollback = true
    ),
    UpdateRequestSuccessTestCase(
      "Cancel deal by seller - Success before money transferred",
      dealWithAccountDetails,
      AutoruPayloadPartyBySeller(PartyCancelDealAction(CancellationReason.SELLER_DOESNT_WANT_TO_SELL, None)),
      UpdateRequestAction(
        cancelledDealBeforeMoneyTransferred.copy(isRollback = false),
        Event.Party.CanceledDeal(ParticipantType.SELLER).some
      ),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestSuccessTestCase(
      "Restore deal by seller - Success with offer and sellingPrice",
      cancelledDealBeforeMoneyTransferred,
      AutoruPayloadPartyBySeller(
        PartyRestoreDealAction(DealCreateRequest.AutoruSubject(AutoruSubjectValueByOffer(offer)).some, Some(900000L))
      ),
      UpdateRequestAction(
        restoredDeal.copy(isRollback = false),
        Event.Party.RestoredDeal(ParticipantType.SELLER).some
      ),
      Party.Seller,
      withRollback = false
    )
  )

  private val updateRequestFailureTestCases: Seq[UpdateRequestFailureTestCase] = Seq(
    UpdateRequestFailureTestCase(
      "Accept deal by buyer - Error: Illegal Access",
      newDeal,
      AutoruPayloadPartyBySeller(PartyNewDealApproveAction(true)),
      SafeDealIllegalAccess("bySeller can be updated only by seller"),
      Party.Buyer,
      withRollback = true
    ),
    UpdateRequestFailureTestCase(
      "Provide new selling price after deal is approved - Error: NotSuitableStatus",
      dealWithBuyerCodeApproved,
      AutoruPayloadPartyByBuyer(PartySellingPriceUpdateAction(1100000L)),
      NotSuitableStatus(
        "Allowed deal steps for this request: DEAL_CREATED, DEAL_INVITE_ACCEPTED. Current dealStep=DEAL_CONFIRMED",
        DealStep.DEAL_CONFIRMED
      ),
      Party.Buyer,
      withRollback = true
    ),
    UpdateRequestFailureTestCase(
      "Provide new selling price with with_rollback=false - Error: rollback is disabled",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyByBuyer(PartySellingPriceUpdateAction(1100000L)),
      NotSuitableStatus(
        "Rollback is disabled so allowed buyer_step: BUYER_INTRODUCING_SELLING_PRICE",
        DealStep.DEAL_INVITE_ACCEPTED
      ),
      Party.Buyer,
      withRollback = false
    ),
    UpdateRequestFailureTestCase(
      "Provide new subject info with with_rollback=false - Error: rollback is disabled",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyBySeller(
        PartySubjectInfoUpdateAction(PartySubjectAutoruInfo(ptsInfo.some, stsInfo.some, newCarInfo.some))
      ),
      NotSuitableStatus(
        "Rollback is disabled so allowed seller_step: SELLER_INTRODUCING_SUBJECT_DETAILS",
        DealStep.DEAL_INVITE_ACCEPTED
      ),
      Party.Seller,
      withRollback = false
    ),
    UpdateRequestFailureTestCase(
      "Provide account details - Error rollback after approved docs",
      dealWithSignedDocsApproved,
      AutoruPayloadPartyBySeller(PartySellerBankAccountUpdateAction(newBankingEntity)),
      NotSuitableStatus(
        "Allowed seller steps for this request: SELLER_INTRODUCING_ACCOUNT_DETAILS, SELLER_CHECKING_ACCOUNT_DETAILS, SELLER_INVALID_ACCOUNT_DETAILS. Current sellerStep=SELLER_APPROVING_DEAL",
        DealStep.DEAL_INVITE_ACCEPTED
      ),
      Party.Seller,
      withRollback = true
    )
  )

  private val updateRequestTests = updateRequestTestCases.map {
    case UpdateRequestSuccessTestCase(description, deal, request, expected, party, withRollback) =>
      testM(description) {
        val expectedCleared = clearInsignificantFields(expected)
        val updateAction = for {
          manager <- ZIO.service[ApiBusinessLogicManager.Service]
          updateRequestAction <- manager.applyUpdateRequest(deal, AutoruPayload(request), party, withRollback)
          cleared = clearInsignificantFields(updateRequestAction)
        } yield cleared
        assertM(updateAction)(equalTo(expectedCleared)).provideLayer(env)
      }
  }

  private val updateRequestFailureTests = updateRequestFailureTestCases.map {
    case UpdateRequestFailureTestCase(description, deal, request, expected, party, withRollback) =>
      testM(description) {
        val updateAction = for {
          manager <- ZIO.service[ApiBusinessLogicManager.Service]
          updateRequestAction <- manager.applyUpdateRequest(deal, AutoruPayload(request), party, withRollback)
          cleared = clearInsignificantFields(updateRequestAction)
        } yield cleared
        assertM(updateAction.run)(fails(exceptionEqualTo(expected))).provideLayer(env)
      }
  }

  final def exceptionEqualTo(expected: Throwable): Assertion[Throwable] =
    Assertion.assertion("exceptionEqualTo")(param(expected))(actual =>
      actual.getClass == expected.getClass && actual.getMessage == expected.getMessage
    )

  private def clearInsignificantFields(source: UpdateRequestAction): UpdateRequestAction = {
    val sourceDeal = source.deal.autoru.get
    UpdateRequestAction(
      sourceDeal
        .modify(_.sellerStep)
        .setTo(SellerStep.SELLER_STEP_UNKNOWN)
        .modify(_.buyerStep)
        .setTo(BuyerStep.BUYER_STEP_UNKNOWN)
        .modify(_.buyer.confirmationCode.each.requestedTime)
        .setTo(LocalTime.MIN)
        .modify(_.buyer.confirmationCode.each.code)
        .setTo("111111")
        .modify(_.seller.each.confirmationCode.each.requestedTime)
        .setTo(LocalTime.MIN)
        .modify(_.seller.each.confirmationCode.each.code)
        .setTo("222222")
        .modify(_.seller.each.personProfile.each.when[PersonProfileImpl].bankingEntity.each.additions)
        .setTo(None)
        .modify(_.buyer.personProfile.each.when[PersonProfileImpl].bankingEntity.each.additions)
        .setTo(None)
        .modify(_.buyer.personProfile.each.when[PersonProfileImpl].documentPhotos.each.isJustAdded)
        .setTo(false)
        .modify(_.buyer.personProfile.each.when[PersonProfileImpl].documentPhotos.each.documentId)
        .setTo("DOC_ID".taggedWith[Tag.DocumentId])
        .modify(_.notifications)
        .using(_.empty)
        .modify(_.meetingInfo.region)
        .setTo(None),
      source.event
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("applyUpdateRequest")(
      suite("success cases")(updateRequestTests: _*) @@ sequential,
      suite("failure cases")(updateRequestFailureTests: _*) @@ sequential
    )
}
