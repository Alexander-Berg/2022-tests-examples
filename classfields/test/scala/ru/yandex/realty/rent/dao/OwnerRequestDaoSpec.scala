package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.model.enums.{ContractStatus, HouseServiceAction, OwnerRequestStatus}
import ru.yandex.realty.rent.model.OwnerRequest
import ru.yandex.realty.rent.model.enums.HouseServiceAction.HouseServiceAction
import ru.yandex.realty.rent.model.house.services.HouseServiceContext
import ru.yandex.realty.rent.proto.api.flats.OwnerRequestNamespace
import ru.yandex.realty.rent.proto.model.owner_request.OwnerRequestData
import ru.yandex.realty.rent.proto.model.sms.confirmation.SmsConfirmation
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class OwnerRequestDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "OwnerRequestDao" should {
    "create and find owner requests" in {
      val ownerRequests = generateOwnerRequest(10)

      ownerRequestDao.create(ownerRequests).futureValue

      val findOwnerRequests = ownerRequestDao.findByIds(ownerRequests.map(_.ownerRequestId)).futureValue
      ownerRequests.foreach { ownerRequest =>
        val findOwnerRequestOpt = findOwnerRequests.find(_.ownerRequestId == ownerRequest.ownerRequestId)
        assert(findOwnerRequestOpt.isDefined)
        assert(ownerRequest == findOwnerRequestOpt.get)
      }

    }

    "create update and find updated owner request" in {
      val ownerRequests = generateOwnerRequest(10)
      ownerRequestDao.create(ownerRequests).futureValue
      val oldOwnerRequest = ownerRequests.head
      val updatedOwnerRequest = ownerRequestGen.next.copy(
        ownerRequestId = oldOwnerRequest.ownerRequestId,
        flatId = oldOwnerRequest.flatId,
        data = OwnerRequestData
          .newBuilder()
          .addSmsConfirmations(
            SmsConfirmation
              .newBuilder()
              .setRequestId(readableString.next)
              .setStatus(SmsConfirmation.Status.CONFIRMED)
              .build()
          )
          .build()
      )
      implicit val ctx = prepareContext(HouseServiceAction.UpdateSettingsStatus)
      val ownerRequest = ownerRequestDao
        .updateWithAuditF(oldOwnerRequest.ownerRequestId)(_ => Future.successful(updatedOwnerRequest))
        .futureValue
      assert(ownerRequest == updatedOwnerRequest)

      val newUpdatedOwnerRequest = updatedOwnerRequest.updateStatus(status = OwnerRequestStatus.Confirmed)
      val newOwnerRequest = ownerRequestDao
        .update(updatedOwnerRequest.ownerRequestId)(_ => newUpdatedOwnerRequest)
        .futureValue
      assert(newOwnerRequest == newUpdatedOwnerRequest)

      val findOwnerRequest = ownerRequestDao.findByIds(Seq(oldOwnerRequest.ownerRequestId)).futureValue
      assert(findOwnerRequest.size == 1)
      assert(findOwnerRequest.head.copy(updateTime = updatedOwnerRequest.updateTime) == newUpdatedOwnerRequest)
      assert(
        findOwnerRequest.head.data.getStatusHistoryList.asScala
          .map(_.getStatus.getNumber)
          .contains(OwnerRequestNamespace.Status.CONFIRMED.getNumber)
      )
    }

    "create and delete owner requests" in {
      val ownerRequests = generateOwnerRequest(10)
      ownerRequestDao.create(ownerRequests).futureValue
      ownerRequests.foreach(ownerRequest => ownerRequestDao.delete(ownerRequest.ownerRequestId).futureValue)
      val findOwnerRequests = ownerRequestDao.findByIds(ownerRequests.map(_.ownerRequestId)).futureValue
      assert(findOwnerRequests.isEmpty)
    }

    "create and find owner requests by flat id" in {
      val flat = flatGen(false).next
      flatDao.create(flat).futureValue

      val firstOwnerRequest = ownerRequestGen.next.copy(flatId = flat.flatId)
      val secondOwnerRequest = ownerRequestGen.next.copy(flatId = flat.flatId)
      val thirdOwnerRequest = ownerRequestGen.next.copy(flatId = flat.flatId)
      ownerRequestDao.create(Seq(secondOwnerRequest, firstOwnerRequest, thirdOwnerRequest)).futureValue
      val ownerRequest = ownerRequestGen.next.copy(flatId = flat.flatId)
      ownerRequestDao.create(Seq(ownerRequest)).futureValue

      val findOwnerRequest = ownerRequestDao.findLastByFlatId(flat.flatId).futureValue
      assert(findOwnerRequest.isDefined)
      assert(ownerRequest == findOwnerRequest.get)
    }

    "find owner requests by flat id without creating" in {
      val findOwnerRequest = ownerRequestDao.findLastByFlatId(readableString.next).futureValue
      assert(findOwnerRequest.isEmpty)
    }

    "create contract with owner request" in {
      val ownerRequest = generateOwnerRequest(1).head
      ownerRequestDao.create(Seq(ownerRequest)).futureValue
      val contract = rentContractGen(ContractStatus.Active).next
        .copy(ownerRequestId = Some(ownerRequest.ownerRequestId))
      rentContractDao.create(Seq(contract)).futureValue
      val contractOpt = rentContractDao.findByIdOpt(contract.contractId).futureValue
      assert(contractOpt.isDefined)
      assert(contract == contractOpt.get)
    }
  }

  private def generateOwnerRequest(num: Int): Seq[OwnerRequest] = {
    val flat = flatGen(false).next
    flatDao.create(flat).futureValue
    val ownerRequests = ownerRequestGen.next(num).toSeq
    ownerRequests.map(_.copy(flatId = flat.flatId))
  }

  private def prepareContext(statusAction: HouseServiceAction): HouseServiceContext = {
    val user = userGen(false).next
    HouseServiceContext(user.uid, statusAction)
  }
}
