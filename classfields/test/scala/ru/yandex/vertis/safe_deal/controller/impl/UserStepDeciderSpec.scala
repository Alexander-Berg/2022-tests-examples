package ru.yandex.vertis.safe_deal.controller.impl

import ru.yandex.vertis.safe_deal.controller.impl.AutoruTestEntities._
import ru.yandex.vertis.safe_deal.model._
import ru.yandex.vertis.safe_deal.proto.common.{BuyerStep, SellerStep}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object UserStepDeciderSpec extends DefaultRunnableSpec {

  private case class UserStepsTestCase(description: String, deal: AutoruDeal)

  private val userStepsTestCases: Seq[UserStepsTestCase] = Seq(
    UserStepsTestCase(
      "new deal",
      newDeal
    ),
    UserStepsTestCase(
      "accepted deal",
      acceptedDeal
    ),
    UserStepsTestCase(
      "accepted deal with checking seller passport",
      acceptedDealWithCheckingSellerPassport
    ),
    UserStepsTestCase(
      "accepted deal validated with seller passport",
      acceptedDealValidatedWithSellerPassport
    ),
    UserStepsTestCase(
      "accepted deal with checking buyer passport",
      acceptedDealWithCheckingBuyerPassport
    ),
    UserStepsTestCase(
      "accepted deal with invalid buyer passport",
      acceptedDealWithInvalidBuyerPassport
    ),
    UserStepsTestCase(
      "accepted deal with validated buyer passport",
      acceptedDealWithValidatedBuyerPassport
    ),
    UserStepsTestCase(
      "accepted deal with seller and checking buyer passport",
      acceptedDealWithSellerAndCheckingBuyerPassport
    ),
    UserStepsTestCase(
      "accepted deal with checking seller and buyer passport",
      acceptedDealWithCheckingSellerAndBuyerPassport
    ),
    UserStepsTestCase(
      "accepted deal with seller and buyer passport",
      acceptedDealWithSellerAndBuyerPassport
    ),
    UserStepsTestCase(
      "accepted deal with seller passport and subject details",
      acceptedDealWithSellerPassportAndSubjectDetails
    ),
    UserStepsTestCase(
      "deal with passports and subject details",
      dealWithPassportsAndSubjectDetails
    ),
    UserStepsTestCase(
      "deal with selling price provided",
      dealWithSellingPriceProvided
    ),
    UserStepsTestCase(
      "deal with selling price approved",
      dealWithSellingPriceApproved
    ),
    UserStepsTestCase(
      "deal with account details",
      dealWithAccountDetails
    ),
    UserStepsTestCase(
      "deal with money provided",
      dealWithMoneyProvided
    ),
    UserStepsTestCase(
      "deal with meeting details provided",
      dealWithMeetingDetailsProvided
    ),
    UserStepsTestCase(
      "deal with new meeting details provided",
      dealWithNewMeetingDetailsProvided
    ),
    UserStepsTestCase(
      "deal with signed document uploaded first photo",
      dealWithSignedDocumentUploadedFirstPhoto
    ),
    UserStepsTestCase(
      "deal with signed docs complete upload",
      dealWithSignedDocsCompleteUpload
    ),
    UserStepsTestCase(
      "deal with signed docs approved",
      dealWithSignedDocsApproved
    ),
    UserStepsTestCase(
      "deal with seller code requested",
      dealWithSellerCodeRequested
    ),
    UserStepsTestCase(
      "deal with seller code approved",
      dealWithSellerCodeApproved
    ),
    UserStepsTestCase(
      "deal with buyer code requested",
      dealWithBuyerCodeRequested
    ),
    UserStepsTestCase(
      "deal with buyer code approved",
      dealWithBuyerCodeApproved
    ),
    UserStepsTestCase(
      "deal with new account details (rollback after approved docs)",
      dealWithNewAccountDetailsRollbackAfterApprovedDocs
    ),
    UserStepsTestCase(
      "deal with new subject details (rollback after approved docs)",
      dealWithNewSubjectDetailsRollbackAfterApprovedDocs
    ),
    UserStepsTestCase(
      "deal with new selling price (rollback after approved docs)",
      dealWithNewSellingPriceRollbackAfterApprovedDocs
    ),
    UserStepsTestCase(
      "deal with new seller passport (rollback after approved docs)",
      dealWithNewSellerPassportRollbackAfterApprovedDocs
    )
  )

  private val userStepsTests = userStepsTestCases.map { case UserStepsTestCase(description, deal) =>
    test(description) {
      val buyerStepExpected = deal.buyerStep
      val sellerStepExpected = deal.sellerStep
      val clearedUserSteps =
        deal.copy(sellerStep = SellerStep.SELLER_STEP_UNKNOWN, buyerStep = BuyerStep.BUYER_STEP_UNKNOWN)
      val nextDeal = UserStepDecider.decideUserSteps(clearedUserSteps)
      assert((nextDeal.buyerStep, nextDeal.sellerStep))(equalTo((buyerStepExpected, sellerStepExpected)))
    }
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("UserStepDecider")(
      suite("userSteps")(userStepsTests: _*)
    )
}
