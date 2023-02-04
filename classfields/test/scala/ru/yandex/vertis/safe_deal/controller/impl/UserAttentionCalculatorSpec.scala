package ru.yandex.vertis.safe_deal.controller.impl

import cats.implicits.catsSyntaxOptionId
import ru.yandex.vertis.safe_deal.controller.UserAttentionCalculator
import ru.yandex.vertis.safe_deal.controller.UserAttentionCalculator.Attentions
import ru.yandex.vertis.safe_deal.controller.impl.AutoruTestEntities._
import ru.yandex.vertis.safe_deal.controller.impl.UserAttentionCalculatorImpl.RichGeneratedEnum
import ru.yandex.vertis.safe_deal.dictionary.NotificationTemplateDictionary
import ru.yandex.vertis.safe_deal.model.{Attention, Deal}
import ru.yandex.vertis.safe_deal.proto.common.ParticipantType
import ru.yandex.vertis.safe_deal.util.RichAutoruModel._
import ru.yandex.vertis.safe_deal.util.mock.NotificationTemplateDictionaryMock
import zio._
import zio.clock.Clock
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment

import java.time.Instant

object UserAttentionCalculatorSpec extends DefaultRunnableSpec {

  private lazy val now: Instant = Instant.now

  private val notificationTemplateDictionaryLayer: ULayer[NotificationTemplateDictionary] =
    ZLayer.succeed(new NotificationTemplateDictionaryMock())

  private val userAttentionCalculatorLayer =
    notificationTemplateDictionaryLayer ++ Clock.any >>> UserAttentionCalculator.live

  private case class UserAttentionTestCase(description: String, deal: Deal, expected: Attentions)

  private val userAttentionTestCases: Seq[UserAttentionTestCase] = Seq(
    UserAttentionTestCase(
      "new deal",
      newDeal,
      Attentions(
        newDeal.buyerAttention,
        newDeal.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal",
      acceptedDeal,
      Attentions(
        acceptedDeal.buyerAttention,
        acceptedDeal.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with checking seller passport",
      acceptedDealWithCheckingSellerPassport,
      Attentions(
        acceptedDealWithCheckingSellerPassport.buyerAttention,
        acceptedDealWithCheckingSellerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal validated with seller passport",
      acceptedDealValidatedWithSellerPassport,
      Attentions(
        acceptedDealValidatedWithSellerPassport.buyerAttention,
        acceptedDealValidatedWithSellerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with checking buyer passport",
      acceptedDealWithCheckingBuyerPassport,
      Attentions(
        acceptedDealWithCheckingBuyerPassport.buyerAttention,
        acceptedDealWithCheckingBuyerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with invalid buyer passport",
      acceptedDealWithInvalidBuyerPassport,
      Attentions(
        acceptedDealWithInvalidBuyerPassport.buyerAttention,
        acceptedDealWithInvalidBuyerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with validated buyer passport",
      acceptedDealWithValidatedBuyerPassport,
      Attentions(
        acceptedDealWithValidatedBuyerPassport.buyerAttention,
        acceptedDealWithValidatedBuyerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with seller and checking buyer passport",
      acceptedDealWithSellerAndCheckingBuyerPassport,
      Attentions(
        acceptedDealWithSellerAndCheckingBuyerPassport.buyerAttention,
        acceptedDealWithSellerAndCheckingBuyerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with checking seller and buyer passport",
      acceptedDealWithCheckingSellerAndBuyerPassport,
      Attentions(
        acceptedDealWithCheckingSellerAndBuyerPassport.buyerAttention,
        acceptedDealWithCheckingSellerAndBuyerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with seller and buyer passport",
      acceptedDealWithSellerAndBuyerPassport,
      Attentions(
        acceptedDealWithSellerAndBuyerPassport.buyerAttention,
        acceptedDealWithSellerAndBuyerPassport.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "accepted deal with seller passport and subject details",
      acceptedDealWithSellerPassportAndSubjectDetails,
      Attentions(
        acceptedDealWithSellerPassportAndSubjectDetails.buyerAttention,
        acceptedDealWithSellerPassportAndSubjectDetails.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with passports and subject details",
      dealWithPassportsAndSubjectDetails,
      Attentions(
        dealWithPassportsAndSubjectDetails.buyerAttention,
        dealWithPassportsAndSubjectDetails.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with selling price provided",
      dealWithSellingPriceProvided,
      Attentions(
        dealWithSellingPriceProvided.buyerAttention,
        dealWithSellingPriceProvided.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with selling price approved",
      dealWithSellingPriceApproved,
      Attentions(
        dealWithSellingPriceApproved.buyerAttention,
        dealWithSellingPriceApproved.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with account details",
      dealWithAccountDetails,
      Attentions(
        dealWithAccountDetails.buyerAttention,
        dealWithAccountDetails.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with money provided",
      dealWithMoneyProvided,
      Attentions(
        dealWithMoneyProvided.buyerAttention,
        dealWithMoneyProvided.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with meeting details provided",
      dealWithMeetingDetailsProvided,
      Attentions(
        dealWithMeetingDetailsProvided.buyerAttention,
        dealWithMeetingDetailsProvided.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with new meeting details provided",
      dealWithNewMeetingDetailsProvided,
      Attentions(
        dealWithNewMeetingDetailsProvided.buyerAttention,
        dealWithNewMeetingDetailsProvided.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with signed document uploaded first photo",
      dealWithSignedDocumentUploadedFirstPhoto,
      Attentions(
        dealWithSignedDocumentUploadedFirstPhoto.buyerAttention,
        dealWithSignedDocumentUploadedFirstPhoto.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with signed docs complete upload",
      dealWithSignedDocsCompleteUpload,
      Attentions(
        dealWithSignedDocsCompleteUpload.buyerAttention,
        dealWithSignedDocsCompleteUpload.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with signed docs approved",
      dealWithSignedDocsApproved,
      Attentions(
        dealWithSignedDocsApproved.buyerAttention,
        dealWithSignedDocsApproved.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with seller code requested",
      dealWithSellerCodeRequested,
      Attentions(
        dealWithSellerCodeRequested.buyerAttention,
        dealWithSellerCodeRequested.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with seller code approved",
      dealWithSellerCodeApproved,
      Attentions(
        dealWithSellerCodeApproved.buyerAttention,
        None
      )
    ),
    UserAttentionTestCase(
      "deal with buyer code requested",
      dealWithBuyerCodeRequested,
      Attentions(
        dealWithSellerCodeApproved.buyerAttention,
        None
      )
    ),
    UserAttentionTestCase(
      "deal with buyer code approved",
      dealWithBuyerCodeApproved,
      Attentions(
        dealWithBuyerCodeApproved.buyerAttention,
        dealWithBuyerCodeApproved.sellerAttention.map(
          _.copy(
            template = dealWithBuyerCodeApproved.buyerStep.asNotificationTemplateId(ParticipantType.SELLER)
          )
        )
      )
    ),
    UserAttentionTestCase(
      "deal with new account details (rollback after approved docs)",
      dealWithNewAccountDetailsRollbackAfterApprovedDocs,
      Attentions(
        dealWithNewAccountDetailsRollbackAfterApprovedDocs.buyerAttention,
        dealWithNewAccountDetailsRollbackAfterApprovedDocs.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with new subject details (rollback after approved docs)",
      dealWithNewSubjectDetailsRollbackAfterApprovedDocs,
      Attentions(
        dealWithNewSubjectDetailsRollbackAfterApprovedDocs.buyerAttention,
        dealWithNewSubjectDetailsRollbackAfterApprovedDocs.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with new selling price (rollback after approved docs)",
      dealWithNewSellingPriceRollbackAfterApprovedDocs,
      Attentions(
        dealWithNewSellingPriceRollbackAfterApprovedDocs.buyerAttention,
        dealWithNewSellingPriceRollbackAfterApprovedDocs.sellerAttention
      )
    ),
    UserAttentionTestCase(
      "deal with new seller passport (rollback after approved docs)",
      dealWithNewSellerPassportRollbackAfterApprovedDocs,
      Attentions(
        dealWithNewSellerPassportRollbackAfterApprovedDocs.buyerAttention,
        dealWithNewSellerPassportRollbackAfterApprovedDocs.sellerAttention
      )
    )
  )

  private def calculate(deal: Deal): RIO[UserAttentionCalculator, Attentions] =
    ZIO.accessM(
      _.get[UserAttentionCalculator.Service]
        .calculate(deal)
        .map(attentions =>
          attentions.copy(
            buyer = attentions.buyer.map(_.significant),
            seller = attentions.seller.map(_.significant)
          )
        )
    )

  private val userAttentionTests = userAttentionTestCases.map {
    case UserAttentionTestCase(description, deal, expected) =>
      testM(description) {
        assertM(calculate(deal))(equalTo(expected)).provideLayer(userAttentionCalculatorLayer)
      }
  }

  implicit class RichDeal(val value: Deal) extends AnyVal {

    def buyerAttention: Option[Attention] =
      Attention(
        userId = value.buyer.user.id,
        dealId = value.id,
        created = now,
        updated = now,
        read = false,
        template = value.buyerStep.asNotificationTemplateId(ParticipantType.BUYER),
        params = value.getParamsByParticipantType(ParticipantType.BUYER),
        subject = autoruSubject.some
      ).some

    def sellerAttention: Option[Attention] =
      value.seller.map { seller =>
        Attention(
          userId = seller.user.id,
          dealId = value.id,
          created = now,
          updated = now,
          read = false,
          template = value.sellerStep.asNotificationTemplateId(ParticipantType.SELLER),
          params = value.getParamsByParticipantType(ParticipantType.SELLER),
          subject = autoruSubject.some
        )
      }
  }

  implicit class RichAttention(val value: Attention) extends AnyVal {

    def significant: Attention =
      value.copy(
        created = now,
        updated = now,
        subject = autoruSubject.some
      )
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("UserAttentionCalculator")(userAttentionTests: _*)
}
