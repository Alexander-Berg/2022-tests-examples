package ru.yandex.realty.rent.backend.manager

import cats.implicits.catsSyntaxOptionId
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.rent.backend.validator.ContractValidator.UpdateContractStatusException
import ru.yandex.realty.rent.model.RentContract
import ru.yandex.realty.rent.model.enums.PaymentType.PaymentType
import ru.yandex.realty.rent.proto.api.moderation.{UpdateContractStatusRequest, UpdateContractStatusResponse}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

import scala.concurrent.Future
import scala.language.higherKinds

@RunWith(classOf[JUnitRunner])
class ContractStatusManagerSpec
  extends SpecBase
  with AsyncSpecBase
  with RequestAware
  with ScalaCheckPropertyChecks
  with SearcherResponseModelGenerators
  with Logging {

  "ContractStatusManager.updateContractStatus" should {

    // failed scenarios

    "throw Exception when contractDao fails" in new Wiring with Data {
      (mockPaymentDao
        .getLastContractsPayments(_: Set[String], _: DateTime, _: PaymentType)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Nil))
        .anyNumberOfTimes()
      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(CorrectDataset_Activate.ownerRequest)))
      (mockContractDao
        .updateWithFlatShowingF(_: String)(_: Updater)(_: Traced))
        .expects(sampleContractId, *, *)
        .returning(Future.failed(sampleException))

      val err: RuntimeException = interceptCause[RuntimeException] {
        contractStatusManager.updateContractStatus(sampleFlatId, sampleContractId, sampleRequest)(traced).futureValue
      }
      err.getMessage shouldEqual sampleException.getMessage
    }

    // succeed scenarios: business errors

    "returns expected UpdateContractStatusErrorResponse for error datasets" in new Wiring with Data {

      errorDatasets.foreach { ds =>
        log.info(s"check expected [${ds.expectedResponse.getError.getErrorCode}]")

        ds.contracts.foreach { wrongContract =>
          log.info(s"check expected [${ds.expectedResponse.getError.getErrorCode}] for contract [$wrongContract]")

          (mockPaymentDao
            .getLastContractsPayments(_: Set[String], _: DateTime, _: PaymentType)(_: Traced))
            .expects(*, *, *, *)
            .returning(Future.successful(Nil))
            .anyNumberOfTimes()
          (ownerRequestDao
            .findLastByFlatId(_: String)(_: Traced))
            .expects(*, *)
            .returning(Future.successful(Some(CorrectDataset_Activate.ownerRequest)))

          (mockContractDao
            .updateWithFlatShowingF(_: String)(_: Updater)(_: Traced))
            .expects(where {
              case (contractId, updater, _) =>
                updater(wrongContract, flatShowingGen.next.some)
                contractId == wrongContract.contractId
            })
            .throws(UpdateContractStatusException(ds.expectedResponse.getError.getErrorCode))

          (mockUserDao
            .findByUid(_: Long)(_: Traced))
            .expects(*, *)
            .returning(Future.successful(toUser(sampleTenant).copy(data = sampleUserErrorData)))

          val response: UpdateContractStatusResponse = contractStatusManager
            .updateContractStatus(wrongContract.flatId, wrongContract.contractId, ds.request)(traced)
            .futureValue

          response shouldEqual ds.expectedResponse
        }

      }
    }

    // succeed scenarios: succeed response
    "returns expected UpdateContractStatusSuccessResponse for correct Activate status" in new Wiring with Data {
      val correctContract: RentContract = CorrectDataset_Activate.contract
      val correctRequest: UpdateContractStatusRequest = CorrectDataset_Activate.request

      (mockPaymentDao
        .getLastContractsPayments(_: Set[String], _: DateTime, _: PaymentType)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Nil))
        .anyNumberOfTimes()
      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(CorrectDataset_Activate.ownerRequest)))

      (mockContractDao
        .updateWithFlatShowingF(_: String)(_: Updater)(_: Traced))
        .expects(where {
          case (contractId, updater, _) =>
            updater(correctContract, flatShowingGen.next.some)
            contractId == correctContract.contractId
        })
        .returning(Future.successful((CorrectDataset_Activate.expectedUpdatedContract)))

      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(toUser(sampleTenant).copy(data = sampleUserCorrectData)))

      (mockUserDao
        .findByUids(_: Set[Long])(_: Traced))
        .expects(CorrectDataset_Activate.expectedAssignedUsers, *)
        .returning(Future.successful(CorrectDataset_Activate.sampleAssignedUsers))

      val response: UpdateContractStatusResponse = contractStatusManager
        .updateContractStatus(correctContract.flatId, correctContract.contractId, correctRequest)(traced)
        .futureValue

      // check results
      response shouldEqual CorrectDataset_Activate.expectedResponse
    }

    "returns expected UpdateContractStatusSuccessResponse for correct InsurancePolicy status" in new Wiring with Data {
      val correctContract: RentContract = CorrectDataset_InsurancePolicyRequested.contract
      val correctRequest: UpdateContractStatusRequest = CorrectDataset_InsurancePolicyRequested.request

      (mockPaymentDao
        .getLastContractsPayments(_: Set[String], _: DateTime, _: PaymentType)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Nil))
        .anyNumberOfTimes()
      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(CorrectDataset_Activate.ownerRequest)))

      (mockContractDao
        .updateWithFlatShowingF(_: String)(_: Updater)(_: Traced))
        .expects(where {
          case (contractId, updater, _) =>
            updater(correctContract, flatShowingGen.next.some)
            contractId == correctContract.contractId
        })
        .returning(Future.successful((CorrectDataset_InsurancePolicyRequested.expectedUpdatedContract)))

      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(toUser(sampleTenant).copy(data = sampleUserCorrectData)))

      (mockUserDao
        .findByUids(_: Set[Long])(_: Traced))
        .expects(CorrectDataset_InsurancePolicyRequested.expectedAssignedUsers, *)
        .returning(Future.successful(CorrectDataset_InsurancePolicyRequested.sampleAssignedUsers))

      val response: UpdateContractStatusResponse = contractStatusManager
        .updateContractStatus(correctContract.flatId, correctContract.contractId, correctRequest)(traced)
        .futureValue

      // check results
      response shouldEqual CorrectDataset_InsurancePolicyRequested.expectedResponse
    }

    "returns expected UpdateContractStatusSuccessResponse for correct SetNowMomentForTesting status" in new Wiring
    with Data {
      val correctContract: RentContract = CorrectDataset_SetNowMomentForTesting.contract
      val correctRequest: UpdateContractStatusRequest = CorrectDataset_SetNowMomentForTesting.request

      (mockPaymentDao
        .getLastContractsPayments(_: Set[String], _: DateTime, _: PaymentType)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Nil))
        .anyNumberOfTimes()
      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(CorrectDataset_Activate.ownerRequest)))

      (mockContractDao
        .updateWithFlatShowingF(_: String)(_: Updater)(_: Traced))
        .expects(where {
          case (contractId, updater, _) =>
            updater(correctContract, flatShowingGen.next.some)
            contractId == correctContract.contractId
        })
        .returning(Future.successful(CorrectDataset_SetNowMomentForTesting.expectedUpdatedContract))

      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(toUser(sampleTenant).copy(data = sampleUserCorrectData)))

      (mockUserDao
        .findByUids(_: Set[Long])(_: Traced))
        .expects(CorrectDataset_SetNowMomentForTesting.expectedAssignedUsers, *)
        .returning(Future.successful(CorrectDataset_SetNowMomentForTesting.sampleAssignedUsers))

      val response: UpdateContractStatusResponse = contractStatusManager
        .updateContractStatus(correctContract.flatId, correctContract.contractId, correctRequest)(traced)
        .futureValue

      // check results
      response shouldEqual CorrectDataset_SetNowMomentForTesting.expectedResponse
    }

    "returns expected UpdateContractStatusSuccessResponse for correct SendToOwner status" in new Wiring with Data {
      val correctContract: RentContract = CorrectDataset_SendToOwner.contract
      val correctRequest: UpdateContractStatusRequest = CorrectDataset_SendToOwner.request
      correctRequest.getActionCase shouldEqual (UpdateContractStatusRequest.ActionCase.SEND_TO_OWNER)
      (mockPaymentDao
        .getLastContractsPayments(_: Set[String], _: DateTime, _: PaymentType)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Nil))
        .anyNumberOfTimes()
      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(CorrectDataset_SendToOwner.ownerRequest)))
      (mockContractDao
        .updateWithFlatShowingF(_: String)(_: Updater)(_: Traced))
        .expects(where {
          case (contractId, updater, _) =>
            updater(correctContract, flatShowingGen.next.some)
            contractId == correctContract.contractId
        })
        .returning(Future.successful((CorrectDataset_SendToOwner.expectedUpdatedContract)))

      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(toUser(sampleTenant).copy(data = sampleUserCorrectData)))

      (mockUserDao
        .findByUids(_: Set[Long])(_: Traced))
        .expects(CorrectDataset_SendToOwner.expectedAssignedUsers, *)
        .returning(Future.successful(CorrectDataset_SendToOwner.sampleAssignedUsers))

      val response: UpdateContractStatusResponse = contractStatusManager
        .updateContractStatus(correctContract.flatId, correctContract.contractId, correctRequest)(traced)
        .futureValue

      // check results
      response shouldEqual CorrectDataset_SendToOwner.expectedResponse
    }
  }
}
