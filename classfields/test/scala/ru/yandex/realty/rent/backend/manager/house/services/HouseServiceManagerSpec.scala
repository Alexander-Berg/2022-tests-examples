package ru.yandex.realty.rent.backend.manager.house.services

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.rent.backend.manager.{Data, Wiring}
import ru.yandex.realty.rent.backend.validator.PeriodStatusConsistency
import ru.yandex.realty.rent.dao.RentSpecBase
import ru.yandex.realty.rent.model.enums.{ContractStatus, OwnerRequestSettingStatus, Role}
import ru.yandex.realty.rent.model.{StatusAuditLog, User}
import ru.yandex.realty.rent.proto.api.house.service.UpdateHouseServiceSettingsStatusRequest
import ru.yandex.realty.rent.proto.api.internal.house.services.InternalHouseServiceSettingsStatusResponse
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.rent.proto.api.house.service.HouseServiceConflictErrorNamespace.{
  HouseServiceConflictError => ConflictErrorEnum
}

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class HouseServiceManagerSpec extends SpecBase with RentSpecBase {

  "House service manager" should {
    "return conflict error when meters are deleted" in new Wiring with Data {
      val owner: User = userGen(true).next

      val flat = flatGen().next.copy(flatId = sampleFlatId, assignedUsers = Map(Role.Owner -> Seq(owner)))
      val passportUser = PassportUser(owner.uid)

      val request = UpdateHouseServiceSettingsStatusRequest
        .newBuilder()
        .setSetFilledByOwner(UpdateHouseServiceSettingsStatusRequest.SetFilledByOwner.newBuilder().build())
        .build()

      val ownerRequest =
        ownerRequestGen.next.copy(
          flatId = flat.flatId,
          settingsStatus = OwnerRequestSettingStatus.New,
          shouldSendMetrics = true
        )

      (mockFlatDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(Some(flat)))

      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(Some(ownerRequest)))

      (mockHouseServiceDao
        .findByOwnerRequest(_: String)(_: Traced))
        .expects(ownerRequest.ownerRequestId, *)
        .returning(Future.successful(Seq(houseServiceGen(ownerRequest.ownerRequestId).next.copy(deleted = true))))

      (statusAuditLogDao
        .create(_: StatusAuditLog)(_: Traced))
        .expects(*, *)
        .returning(Future.unit)

      val expectedResponse = InternalHouseServiceSettingsStatusResponse
        .newBuilder()
        .setError(
          PeriodStatusConsistency
            .buildHouseServiceConflictError(ConflictErrorEnum.METERS_ARE_ABSENT, Some("Необходимо добавить счётчики"))
        )
        .build()

      val res = houseServiceManager
        .updateHouseServiceSettingsStatus(
          user = passportUser,
          id = sampleFlatId,
          request = request
        )(houseServiceManager.findLastOwnerRequest)
        .futureValue

      res shouldEqual expectedResponse

    }

    "return conflict error when contract is in wrong status" in new Wiring with Data {

      val owner: User = userGen(true).next
      val tenant: User = userGen(true).next

      val flat = flatGen().next
        .copy(flatId = sampleFlatId, assignedUsers = Map(Role.Owner -> Seq(owner), Role.Tenant -> Seq(tenant)))
      val passportUser = PassportUser(tenant.uid)

      val confirmedByTenant = UpdateHouseServiceSettingsStatusRequest
        .newBuilder()
        .setSetConfirmedByTenant(UpdateHouseServiceSettingsStatusRequest.SetConfirmedByTenant.newBuilder().build())
        .build()

      val ownerRequest =
        ownerRequestGen.next.copy(flatId = flat.flatId, settingsStatus = OwnerRequestSettingStatus.FilledByOwner)

      (mockFlatDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(Some(flat)))

      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(Some(ownerRequest)))

      (mockContractDao
        .findByOwnerRequestIdOpt(_: String)(_: Traced))
        .expects(ownerRequest.ownerRequestId, *)
        .returning(Future.successful(Some(rentContractGen(status = ContractStatus.Active).next)))

      val expectedResponse = InternalHouseServiceSettingsStatusResponse
        .newBuilder()
        .setError(
          PeriodStatusConsistency.buildHouseServiceConflictError(ConflictErrorEnum.INVALID_CONTRACT)
        )
        .build()

      val res = houseServiceManager
        .updateHouseServiceSettingsStatus(
          user = passportUser,
          id = sampleFlatId,
          request = confirmedByTenant
        )(houseServiceManager.findLastOwnerRequest)
        .futureValue

      res shouldEqual expectedResponse

    }

    "return conflict error when owner request with status FilledByOwner not found" in new Wiring with Data {
      val owner: User = userGen(true).next

      val flat = flatGen().next.copy(flatId = sampleFlatId, assignedUsers = Map(Role.Owner -> Seq(owner)))
      val passportUser = PassportUser(owner.uid)

      val request = UpdateHouseServiceSettingsStatusRequest
        .newBuilder()
        .setSetFilledByOwner(UpdateHouseServiceSettingsStatusRequest.SetFilledByOwner.newBuilder().build())
        .build()

      val ownerRequest =
        ownerRequestGen.next.copy(
          flatId = flat.flatId,
          settingsStatus = OwnerRequestSettingStatus.Unknown,
          shouldSendMetrics = true
        )

      (mockFlatDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(Some(flat)))

      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(Some(ownerRequest)))

      (mockHouseServiceDao
        .findByOwnerRequest(_: String)(_: Traced))
        .expects(ownerRequest.ownerRequestId, *)
        .returning(Future.successful(Seq(houseServiceGen(ownerRequest.ownerRequestId).next)))

      (statusAuditLogDao
        .create(_: StatusAuditLog)(_: Traced))
        .expects(*, *)
        .returning(Future.unit)

      val expectedResponse = InternalHouseServiceSettingsStatusResponse
        .newBuilder()
        .setError(
          PeriodStatusConsistency.buildHouseServiceConflictError(ConflictErrorEnum.INVALID_SETTINGS_STATUS)
        )
        .build()

      val res = houseServiceManager
        .updateHouseServiceSettingsStatus(
          user = passportUser,
          id = sampleFlatId,
          request = request
        )(houseServiceManager.findLastOwnerRequest)
        .futureValue

      res shouldEqual expectedResponse

    }
  }

}
