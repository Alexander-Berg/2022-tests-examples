package ru.yandex.realty.rent.stage.contract

import cats.implicits.catsSyntaxOptionId
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.{Feature, Features, SimpleFeatures}
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.dao.{FlatDao, FlatShowingDao, OwnerRequestDao, UserDao, UserFlatDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.{ContractStatus, PaymentStatus}
import ru.yandex.realty.rent.model.{ContractWithPayments, Flat, FlatShowing, UserFlat}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ActivateOrDraftStageSpec extends AsyncSpecBase with RentModelsGen with RentPaymentsData {
  val flatDao: FlatDao = mock[FlatDao]
  val ownerRequestDao: OwnerRequestDao = mock[OwnerRequestDao]
  val userDao: UserDao = mock[UserDao]
  val userFlatDao: UserFlatDao = mock[UserFlatDao]
  val flatShowingDao: FlatShowingDao = mock[FlatShowingDao]

  val features: Features = new SimpleFeatures {
    override def getState(feature: Feature): State =
      State(enabled = true, 1, 1)
  }

  private def invokeStage(contract: ContractWithPayments): ProcessingState[ContractWithPayments] = {
    val stage = new ActivateOrDraftStage(flatDao, ownerRequestDao, userDao, userFlatDao, flatShowingDao, features)
    stage.process(ProcessingState(contract))(Traced.empty).futureValue
  }

  "CheckPaymentOfSignedContractStage" should {
    "activate contract" in {
      val now = DateTimeUtil.now()
      val contract = createContract(now, now.getDayOfMonth, Some(now), status = ContractStatus.Signed)
      val payment = createPayment(contract, now, now.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidToOwner)
      val contractWithPayments = ContractWithPayments(contract, List(payment))

      (flatDao
        .updateF(_: String, _: Boolean)(_: Flat => Future[Flat])(_: Traced))
        .expects(*, *, *, *)
        .returns(Future.successful(flatGen().next))
      (userDao.findByUserIdOpt(_: String)(_: Traced)).expects(*, *).returns(Future.successful(Some(userGen().next)))
      (userFlatDao.replace(_: Iterable[UserFlat])(_: Traced)).expects(*, *).returns(Future.unit)
      (flatShowingDao
        .findActualByOwnerRequestAndUser(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .returns(Future.successful(flatShowingGen.next.some))
      (flatShowingDao
        .update(_: String)(_: FlatShowing => FlatShowing)(_: Traced))
        .expects(*, *, *)
        .returns(Future.successful(flatShowingGen.next))

      val newState = invokeStage(contractWithPayments)
      val updatedContract = newState.state.entry.contract
      updatedContract.status shouldBe ContractStatus.Active
      updatedContract.data.getContractSigningDate.getSeconds shouldNot be(0)
    }

    "rollback to draft" in {
      val now = DateTimeUtil.now()
      val contract = createContract(now, now.getDayOfMonth, Some(now), status = ContractStatus.Signed)
      val payment = createPayment(contract, now, now.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.New)
      val contractWithPayments = ContractWithPayments(contract, List(payment))

      (ownerRequestDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returns(Future.successful(ownerRequestGen.next.some))
      (flatShowingDao
        .findActualByOwnerRequestAndUser(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .returns(Future.successful(flatShowingGen.next.some))
      (flatShowingDao
        .update(_: String)(_: FlatShowing => FlatShowing)(_: Traced))
        .expects(*, *, *)
        .returns(Future.successful(flatShowingGen.next))

      val newState = invokeStage(contractWithPayments)
      val updatedContract = newState.state.entry.contract
      updatedContract.status shouldBe ContractStatus.Draft
      updatedContract.data.hasOwnerSign shouldBe false
      updatedContract.data.hasTenantSign shouldBe false
      updatedContract.data.getContractDocumentsCount shouldBe 0
    }

    "wait payment" in {
      val now = DateTimeUtil.now()
      val contract = createContract(now, now.getDayOfMonth, Some(now), status = ContractStatus.Signed)
      val updatedContract =
        contract.copy(data = contract.data.toBuilder.applySideEffect(_.getTenantSignBuilder.setSigned(now)).build)
      val payment = createPayment(updatedContract, now, now.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.New)
      val contractWithPayments = ContractWithPayments(updatedContract, List(payment))
      val newState = invokeStage(contractWithPayments)
      newState.state.entry.contract.status shouldBe ContractStatus.Signed
    }
  }
}
