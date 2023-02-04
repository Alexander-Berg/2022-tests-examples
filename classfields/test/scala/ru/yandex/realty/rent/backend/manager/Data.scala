package ru.yandex.realty.rent.backend.manager

import com.google.protobuf.Empty
import org.joda.time.DateTime
import ru.yandex.realty.rent.backend.converter.ContractConverter
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.ContractStatus.ContractStatus
import ru.yandex.realty.rent.model.enums.{ContractStatus, OwnerRequestSettingStatus, PassportVerificationStatus}
import ru.yandex.realty.rent.model.{ContractParticipant, Flat, FlatShowing, OwnerRequest, RentContract, User}
import ru.yandex.realty.rent.proto.api.moderation.ContractStatusErrorNamespace.ContractStatusErrorCode
import ru.yandex.realty.rent.proto.api.moderation.{
  CreateFlatShowingsRequest,
  UpdateContractStatusError,
  UpdateContractStatusRequest,
  UpdateContractStatusResponse,
  UpdateContractStatusSuccessResponse
}
import ru.yandex.realty.rent.proto.model.contract.CalculationStrategyNamespace.CalculationStrategy
import ru.yandex.realty.rent.proto.model.contract.ContractData
import ru.yandex.realty.rent.proto.model.user.{TenantTermsOfUse, UserData}
import ru.yandex.vertis.protobuf.BasicProtoFormats
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

trait Data extends BasicProtoFormats with RentModelsGen {
  this: Wiring =>

  type Updater = (RentContract, Option[FlatShowing]) => Future[(RentContract, Option[FlatShowing])]

  val sampleFlatId = "flatId-1"
  val sampleContractId = "contractId-1"
  val sampleContractNumber = "22-03-12345"
  val sampleOwnerRequestId = "ownerRequestId-1"

  val sampleRequest: UpdateContractStatusRequest = UpdateContractStatusRequest
    .newBuilder()
    .build()

  val sampleException: RuntimeException = new RuntimeException("sample exception")

  val sampleOwner: ContractParticipant =
    ContractParticipant(uid = Some(10015), name = None, phone = None, email = None)

  val sampleTenant: ContractParticipant =
    ContractParticipant(uid = Some(10016), name = None, phone = None, email = None)

  lazy val sampleUserCorrectData = UserData.newBuilder
    .setTenantTermsOfUse(
      TenantTermsOfUse.newBuilder.setAgreementDate(DateTimeFormat.write(DateTime.now))
    )
    .build

  lazy val sampleUserErrorData =
    UserData.newBuilder
      .setTenantTermsOfUse(
        TenantTermsOfUse.newBuilder.setAgreementDate(DateTimeFormat.write(DateTime.now.withDate(1970, 1, 1)))
      )
      .build

  val sampleStatus: ContractStatus = ContractStatus.Active

  val sampleContractData: ContractData =
    ContractData
      .newBuilder()
      .build()

  val sampleRentContract: RentContract = RentContract(
    contractId = sampleContractId,
    contractNumber = sampleContractNumber,
    ownerRequestId = Some(sampleOwnerRequestId),
    flatId = sampleContractId,
    owner = sampleOwner,
    tenant = sampleTenant,
    terminationDate = None,
    status = sampleStatus,
    data = sampleContractData,
    createTime = DateTime.now(),
    updateTime = DateTime.now(),
    visitTime = None,
    shardKey = 0
  )

  val sampleUid = 321L
  val sampleUserId = "user-id"
  val sampleUser: User = userGen(recursive = false).next.copy(uid = sampleUid, userId = sampleUserId)

  val sampleTenantPhone = "12345"
  val sampleFlat: Flat = flatGen().next.copy(flatId = sampleFlatId)
  val sampleLeadId: Long = 123L

  val sampleCreateShowingsRequest: CreateFlatShowingsRequest = CreateFlatShowingsRequest
    .newBuilder()
    .setTenantPhone(sampleTenantPhone)
    .build()

  val sampleShowingAlreadyExistException = new IllegalArgumentException(
    s"Lead flatId=[$sampleFlatId], phone=[$sampleTenantPhone] already exist"
  )

  val sampleFlatShowing: FlatShowing = flatShowingGen.next.copy(ownerRequestId = sampleOwnerRequestId)

  trait Dataset {
    def request: UpdateContractStatusRequest

    def contracts: Seq[RentContract]

    def expectedResponse: UpdateContractStatusResponse
  }

  object Dataset_SET_ACTIVE_STATUS_ERROR extends Dataset {

    override lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setActivate(Empty.getDefaultInstance)
      .build()

    lazy val ownerRequest: OwnerRequest = ownerRequestGen.next.copy(settingsStatus = OwnerRequestSettingStatus.Draft)

    override lazy val contracts: Seq[RentContract] = Seq(
      sampleRentContract.copy(status = ContractStatus.Active),
      sampleRentContract.copy(status = ContractStatus.Unknown),
      sampleRentContract.copy(status = ContractStatus.Cancelled),
      sampleRentContract.copy(status = ContractStatus.Terminated)
    )

    override lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setError(
        UpdateContractStatusError
          .newBuilder()
          .setErrorCode(ContractStatusErrorCode.SET_ACTIVE_STATUS_ERROR)
          .setShortMessage("")
          .setDetails("")
          .build()
      )
      .build()
  }

  object Dataset_PAST_RENT_START_TIME_ERROR extends Dataset {

    override lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setActivate(Empty.getDefaultInstance)
      .build()

    override lazy val contracts = Seq(
      sampleRentContract.copy(
        status = ContractStatus.Draft,
        data = ContractData
          .newBuilder()
          .setRentStartDate(
            DateTimeFormat.write(DateTime.now().minusDays(1))
          )
          .build()
      )
    )

    override lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setError(
        UpdateContractStatusError
          .newBuilder()
          .setErrorCode(ContractStatusErrorCode.PAST_RENT_START_TIME_ERROR)
          .setShortMessage("")
          .setDetails("")
          .build()
      )
      .build()
  }

  object Dataset_SET_CANCELLED_STATUS_ERROR extends Dataset {

    override lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setCancel(Empty.getDefaultInstance)
      .build()

    override lazy val contracts: Seq[RentContract] = Seq(
      sampleRentContract.copy(status = ContractStatus.Active),
      sampleRentContract.copy(status = ContractStatus.Unknown),
      sampleRentContract.copy(status = ContractStatus.Cancelled),
      sampleRentContract.copy(status = ContractStatus.Terminated)
    )

    override lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setError(
        UpdateContractStatusError
          .newBuilder()
          .setErrorCode(ContractStatusErrorCode.SET_CANCELLED_STATUS_ERROR)
          .setShortMessage("")
          .setDetails("")
          .build()
      )
      .build()
  }

  object Dataset_PLAN_TERMINATION_ERROR extends Dataset {

    override lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setPlanTermination(UpdateContractStatusRequest.PlanTermination.newBuilder().build())
      .build()

    override lazy val contracts: Seq[RentContract] = Seq(
      sampleRentContract.copy(status = ContractStatus.Draft),
      sampleRentContract.copy(status = ContractStatus.Unknown),
      sampleRentContract.copy(status = ContractStatus.Cancelled),
      sampleRentContract.copy(status = ContractStatus.Terminated)
    )

    override lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setError(
        UpdateContractStatusError
          .newBuilder()
          .setErrorCode(ContractStatusErrorCode.PLAN_TERMINATION_ERROR)
          .setShortMessage("")
          .setDetails("")
          .build()
      )
      .build()
  }

  object Dataset_PAST_PLAN_TERMINATION_ERROR extends Dataset {

    override lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setPlanTermination(
        UpdateContractStatusRequest.PlanTermination
          .newBuilder()
          .setTerminateDate(
            DateTimeFormat.write(DateTime.now().minusDays(1))
          )
          .build()
      )
      .build()

    override lazy val contracts: Seq[RentContract] = Seq(
      sampleRentContract.copy(status = ContractStatus.Active)
    )

    override lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setError(
        UpdateContractStatusError
          .newBuilder()
          .setErrorCode(ContractStatusErrorCode.PAST_PLAN_TERMINATION_ERROR)
          .setShortMessage("")
          .setDetails("")
          .build()
      )
      .build()
  }

  object Dataset_SET_ACTIVE_AGREEMENT_DATE_ERROR extends Dataset {

    override lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setActivate(Empty.getDefaultInstance)
      .build()

    override lazy val contracts: Seq[RentContract] = Seq(
      sampleRentContract.copy(status = ContractStatus.Active),
      sampleRentContract.copy(status = ContractStatus.Unknown),
      sampleRentContract.copy(status = ContractStatus.Cancelled),
      sampleRentContract.copy(status = ContractStatus.Terminated)
    )

    override lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setError(
        UpdateContractStatusError
          .newBuilder()
          .setErrorCode(ContractStatusErrorCode.SET_ACTIVE_AGREEMENT_DATE_ERROR)
          .setShortMessage("")
          .setDetails("")
          .build()
      )
      .build()
  }

  object Dataset_UNKNOWN_ERROR_CODE extends Dataset {

    override lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .build()

    override lazy val contracts: Seq[RentContract] = Seq(
      sampleRentContract.copy(status = ContractStatus.Active),
      sampleRentContract.copy(status = ContractStatus.Draft),
      sampleRentContract.copy(status = ContractStatus.Unknown),
      sampleRentContract.copy(status = ContractStatus.Cancelled),
      sampleRentContract.copy(status = ContractStatus.Terminated)
    )

    override lazy val expectedResponse: UpdateContractStatusResponse = null
  }

  def toUser(participant: ContractParticipant): User = {
    User(
      uid = participant.uid.get,
      userId = s"sampleUserId-${participant.uid.get}",
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
      createTime = null,
      updateTime = null,
      visitTime = None
    )
  }

  object CorrectDataset_Activate {
    lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setActivate(Empty.getDefaultInstance)
      .build()

    lazy val ownerRequest: OwnerRequest =
      ownerRequestGen.next.copy(settingsStatus = OwnerRequestSettingStatus.ConfirmedByTenant)

    lazy val contract: RentContract =
      sampleRentContract.copy(
        status = ContractStatus.Draft,
        data = ContractData
          .newBuilder()
          .setCalculationStrategy(CalculationStrategy.STRATEGY_1)
          .setRentStartDate(
            DateTimeFormat.write(DateTime.now().plusDays(1))
          )
          .build()
      )

    val expectedUpdatedContract: RentContract = contract.copy(
      status = ContractStatus.Active,
      updateTime = DateTimeUtil.now,
      visitTime = Some(DateTimeUtil.now)
    )

    val sampleAssignedUsers: Iterable[User] = Seq(
      toUser(contract.owner),
      toUser(contract.tenant)
    )

    val expectedAssignedUsers: Set[Long] = sampleAssignedUsers.map(_.uid).toSet

    lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setSuccess(
        UpdateContractStatusSuccessResponse
          .newBuilder()
          .setContract(
            ContractConverter.buildContract(expectedUpdatedContract, sampleAssignedUsers)
          )
          .build()
      )
      .build()

  }

  object CorrectDataset_SendToOwner {
    lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setSendToOwner(Empty.getDefaultInstance)
      .build()

    lazy val ownerRequest: OwnerRequest =
      ownerRequestGen.next.copy(settingsStatus = OwnerRequestSettingStatus.ConfirmedByTenant)

    lazy val contract: RentContract =
      sampleRentContract.copy(
        status = ContractStatus.Draft,
        data = ContractData
          .newBuilder()
          .setCalculationStrategy(CalculationStrategy.STRATEGY_1)
          .setRentStartDate(DateTimeFormat.write(DateTime.parse("2022-06-01")))
          .build()
      )

    val sampleAssignedUsers: Iterable[User] = Seq(
      toUser(contract.owner),
      toUser(contract.tenant)
    )

    val expectedAssignedUsers: Set[Long] = sampleAssignedUsers.map(_.uid).toSet

    val expectedUpdatedContract: RentContract = contract.copy(
      status = ContractStatus.Signing,
      updateTime = DateTimeUtil.now,
      visitTime = Some(DateTimeUtil.now)
    )

    lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setSuccess(
        UpdateContractStatusSuccessResponse
          .newBuilder()
          .setContract(
            ContractConverter.buildContract(expectedUpdatedContract, sampleAssignedUsers)
          )
          .build()
      )
      .build()
  }

  object CorrectDataset_InsurancePolicyRequested {
    lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setSetInsurancePolicyRequested(UpdateContractStatusRequest.SetInsurancePolicyRequested.newBuilder().build())
      .build()

    lazy val contract: RentContract =
      sampleRentContract.copy(
        status = ContractStatus.Draft,
        data = ContractData
          .newBuilder()
          .setCalculationStrategy(CalculationStrategy.STRATEGY_1)
          .build()
      )

    val expectedUpdatedContract: RentContract = contract.copy(
      status = ContractStatus.Active,
      updateTime = DateTimeUtil.now,
      visitTime = Some(DateTimeUtil.now)
    )

    val sampleAssignedUsers: Iterable[User] = Seq(
      toUser(contract.owner),
      toUser(contract.tenant)
    )

    val expectedAssignedUsers: Set[Long] = sampleAssignedUsers.map(_.uid).toSet

    lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setSuccess(
        UpdateContractStatusSuccessResponse
          .newBuilder()
          .setContract(
            ContractConverter.buildContract(expectedUpdatedContract, sampleAssignedUsers)
          )
          .build()
      )
      .build()

  }

  object CorrectDataset_SetNowMomentForTesting {
    lazy val request: UpdateContractStatusRequest = UpdateContractStatusRequest
      .newBuilder()
      .setSetNowMomentForTesting(UpdateContractStatusRequest.SetNowMomentForTesting.newBuilder().build())
      .build()

    lazy val contract: RentContract =
      sampleRentContract.copy(
        status = ContractStatus.Draft,
        data = ContractData
          .newBuilder()
          .setCalculationStrategy(CalculationStrategy.STRATEGY_1)
          .build()
      )

    val expectedUpdatedContract: RentContract = contract.copy(
      status = ContractStatus.Active,
      updateTime = DateTimeUtil.now,
      visitTime = Some(DateTimeUtil.now)
    )

    val sampleAssignedUsers: Iterable[User] = Seq(
      toUser(contract.owner),
      toUser(contract.tenant)
    )

    val expectedAssignedUsers: Set[Long] = sampleAssignedUsers.map(_.uid).toSet

    lazy val expectedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setSuccess(
        UpdateContractStatusSuccessResponse
          .newBuilder()
          .setContract(
            ContractConverter.buildContract(expectedUpdatedContract, sampleAssignedUsers)
          )
          .build()
      )
      .build()

  }

  val errorDatasets: Seq[Dataset] = Seq(
    Dataset_SET_ACTIVE_STATUS_ERROR,
    Dataset_PAST_RENT_START_TIME_ERROR,
    Dataset_SET_CANCELLED_STATUS_ERROR,
    Dataset_PLAN_TERMINATION_ERROR,
    Dataset_PAST_PLAN_TERMINATION_ERROR,
    Dataset_SET_ACTIVE_AGREEMENT_DATE_ERROR
  )
}
