package ru.yandex.realty.rent.gen

import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.Timestamp
import org.joda.time.DateTime
import org.scalacheck.Gen
import realty.palma.rent_user.{PassportData => PalmaPassportData, RentUser => PalmaUser}
import ru.yandex.realty.cadastr.proto.event.EvaluatedObjectInfo
import ru.yandex.realty.rent.model.enums.ContractStatus.ContractStatus
import ru.yandex.realty.rent.model.enums.FlatShowingStatus.FlatShowingStatus
import ru.yandex.realty.rent.model.enums.MeterReadingsStatus.MeterReadingsStatus
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus.OwnerRequestStatus
import ru.yandex.realty.rent.model.enums.PaymentStatus.PaymentStatus
import ru.yandex.realty.rent.model.enums.PaymentType.PaymentType
import ru.yandex.realty.rent.model.enums.{
  AggregatedMeterReadingsStatus,
  BillStatus,
  CloseShowingCause,
  FlatShowingStatus,
  FlatShowingType,
  GroupStatus,
  HouseServiceType,
  KeysHandoverDirection,
  MeterReadingsStatus,
  OwnerRequestSettingStatus,
  OwnerRequestStatus,
  PassportVerificationStatus,
  PaymentConfirmationStatus,
  PaymentStatus,
  PaymentType,
  PeriodType,
  ReceiptStatus,
  ResponsibleForPayment,
  Role
}
import ru.yandex.realty.rent.model.feed.FeedEntry
import ru.yandex.realty.rent.model.house.services
import ru.yandex.realty.rent.model.house.services.{HouseService, MeterReadings, Period}
import ru.yandex.realty.rent.model.{
  ContractParticipant,
  Flat,
  FlatQuestionnaire,
  FlatShowing,
  FlatUtils,
  Inventory,
  KeysHandover,
  OwnerRequest,
  Payment,
  RentContract,
  User,
  UserFlat,
  UserShowing
}
import ru.yandex.realty.rent.proto.api.common.ContractClassNamespace.ContractClass
import ru.yandex.realty.rent.proto.api.common.FlatTypeNamespace
import ru.yandex.realty.rent.proto.api.common.PersonLegalStatusNamespace.PersonLegalStatus
import ru.yandex.realty.rent.proto.api.flats.Flat.{FlatInfo => ApiFlatInfo}
import ru.yandex.realty.rent.proto.api.flats.OwnerRequestNamespace
import ru.yandex.realty.rent.proto.api.house.service.{ResponsibleForPaymentNamespace, SettingsStatusNamespace}
import ru.yandex.realty.rent.proto.api.image.{ImageUrl, Image => ImageProto}
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Building.ParkingNamespace
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Flat.{
  Balcony,
  Bathroom,
  RenovationTypeNamespace,
  RoomsNamespace
}
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Payments
import ru.yandex.realty.rent.proto.api.moderation.{FlatQuestionnaire => FlatQuestionnaireProto}
import ru.yandex.realty.rent.proto.model.contract.ContractData
import ru.yandex.realty.rent.proto.model.diffevent.{FlatProtoView, OwnerRequestProtoView}
import ru.yandex.realty.rent.proto.model.flat.showing.FlatShowingData
import ru.yandex.realty.rent.proto.model.flat.{FlatData, FlatExcerptsRequest, FlatLocation}
import ru.yandex.realty.rent.proto.model.house.service.periods.{MeterReadingsData, PeriodData}
import ru.yandex.realty.rent.proto.model.house.service.{HouseServiceData, Meter}
import ru.yandex.realty.rent.proto.model.image.Image
import ru.yandex.realty.rent.proto.model.inventory.InventoryData
import ru.yandex.realty.rent.proto.model.keys.handovers.KeysHandoverData
import ru.yandex.realty.rent.proto.model.owner_request.OwnerRequestData
import ru.yandex.realty.rent.proto.model.payment.PaymentData
import ru.yandex.realty.rent.proto.model.user.UserData
import ru.yandex.realty.util.Mappings._
import ru.yandex.realty.util.TimeUtils
import ru.yandex.realty.util.TimeUtils.RichDateTime
import ru.yandex.realty.util.gen.IdGenerator
import ru.yandex.realty.util.protobuf.BuilderExt
import ru.yandex.vertis.generators.{BasicGenerators, DateTimeGenerators}
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.protobuf.ProtobufUtils
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.Instant
import scala.collection.JavaConverters._
import scala.util.Random

trait RentModelsGen extends BasicGenerators with DateTimeGenerators {

  def userGen(recursive: Boolean = true): Gen[User] =
    for {
      uid <- posNum[Long]
      userId <- readableString
      phone <- Gen.option(readableString)
      name <- Gen.option(readableString)
      surname <- Gen.option(readableString)
      patronymic <- Gen.option(readableString)
      fullName <- Gen.option(readableString)
      email <- Gen.option(readableString)
      passportVerificationStatus <- Gen.oneOf(Seq(PassportVerificationStatus.Absent, PassportVerificationStatus.Saved))
      roommateLinkId = IdGenerator.generateLongNumber(19)
      flatsAsOwner <- genFlats(recursive)
      flatsAsConfidant <- genFlats(recursive)
      flatsAsTenant <- genFlats(recursive)
      flatsAsAdditionalTenant <- genFlats(recursive)
    } yield User(
      uid,
      userId,
      phone,
      name,
      surname,
      patronymic,
      fullName,
      email,
      passportVerificationStatus,
      Some(roommateLinkId),
      Some(DateTimeUtil.now.plusDays(1)),
      Map(
        Role.Owner -> flatsAsOwner,
        Role.Confidant -> flatsAsConfidant,
        Role.Tenant -> flatsAsTenant,
        Role.AdditionalTenant -> flatsAsAdditionalTenant
      ).filter(_._2.nonEmpty),
      UserData.getDefaultInstance,
      DateTimeUtil.now,
      DateTimeUtil.now,
      Some(DateTimeUtil.now)
    )

  def palmaUserGen: Gen[PalmaUser] =
    for {
      uid <- readableString
      passportData <- palmaPassportDataGen
    } yield PalmaUser(uid, passportData = passportData.some)

  def palmaPassportDataGen: Gen[PalmaPassportData] =
    for {
      passportSeries <- readableString
      passportNumber <- readableString
      passportIssuedBy <- readableString
      departmentCode <- readableString
      birthPlace <- readableString
      registrationAddress <- readableString
    } yield PalmaPassportData(
      None,
      passportSeries,
      passportNumber,
      None,
      passportIssuedBy,
      departmentCode,
      birthPlace,
      registrationAddress
    )

  // scalastyle:off
  def flatGen(recursive: Boolean = true): Gen[Flat] =
    for {
      flatId <- readableString
      address <- readableString
      code <- Gen.option(readableString(6, 6))
      unifiedAddress = Some(address)
      flatNumber <- Gen.numStr
      nameFromRequest <- Gen.option(readableString)
      phoneFromRequest <- Gen.option(readableString)
      owners <- genUsers(recursive)
      confidants <- genUsers(recursive)
      tenants <- genUsers(recursive)
      additionalTenants <- genUsers(recursive)
      shardKey = FlatUtils.evaluateShardKey(flatId)
      data <- flatDataGen
    } yield Flat(
      flatId = flatId,
      code = code,
      data = data,
      address = address,
      unifiedAddress = unifiedAddress,
      flatNumber = flatNumber,
      nameFromRequest = nameFromRequest,
      phoneFromRequest = phoneFromRequest,
      isRented = false,
      keyCode = None,
      ownerRequests = Seq.empty,
      assignedUsers = Map(
        Role.Owner -> owners,
        Role.Confidant -> confidants,
        Role.Tenant -> tenants,
        Role.AdditionalTenant -> additionalTenants
      ).filter(_._2.nonEmpty),
      createTime = DateTimeUtil.now,
      updateTime = DateTimeUtil.now,
      visitTime = Some(DateTimeUtil.now),
      shardKey = shardKey
    )
  // scalastyle:on

  def flatWithOwnerRequest(ownerRequestStatus: OwnerRequestStatus): Gen[Flat] =
    for {
      flat <- flatGen(recursive = false)
      ownerRequest <- ownerRequestGen
    } yield flat.copy(
      ownerRequests = Seq(ownerRequest.copy(flatId = flat.flatId).updateStatus(status = ownerRequestStatus))
    )

  private def getGenListOf[A](recursive: Boolean, generator: Boolean => Gen[A]): Gen[List[A]] = {
    val n = if (recursive) Random.nextInt(5) else 0
    Gen.listOfN(n, generator(false))
  }

  private def genFlats(recursive: Boolean): Gen[List[Flat]] =
    getGenListOf(recursive, flatGen)

  private def genUsers(recursive: Boolean): Gen[List[User]] =
    getGenListOf(recursive, userGen)

  def userFlatGen: Gen[UserFlat] =
    for {
      uid <- posNum[Long]
      flatId <- readableString
      role <- Gen.oneOf(Role.Owner, Role.Confidant, Role.Tenant, Role.AdditionalTenant)
    } yield UserFlat(uid, flatId, role)

  def contractParticipantGen: Gen[ContractParticipant] =
    for {
      uid <- posNum[Long]
      name <- Gen.option(readableString)
      phone <- Gen.option(readableString)
      email <- Gen.option(readableString)
    } yield ContractParticipant(Some(uid), name, phone, email)

  def rentContractGen(status: ContractStatus, nowMomentForTesting: DateTime = DateTimeUtil.now()): Gen[RentContract] =
    for {
      contractId <- readableString
      contractNumber <- readableString
      ownerRequestId <- readableString
      flatId <- readableString
      owner <- contractParticipantGen
      tenant <- contractParticipantGen
      shardKey <- Gen.oneOf(0, 1)
    } yield RentContract(
      contractId,
      contractNumber,
      ownerRequestId.some,
      flatId,
      owner,
      tenant,
      None,
      status,
      ContractData
        .newBuilder()
        .setNowMomentForTesting(DateTimeFormat.write(nowMomentForTesting))
        .setContractClass(ContractClass.AUTHORITY)
        .setOwnerStatus(PersonLegalStatus.JURIDICAL)
        .build(),
      DateTimeUtil.now,
      DateTimeUtil.now,
      Some(DateTimeUtil.now),
      shardKey
    )

  def flatShowingGen: Gen[FlatShowing] =
    flatShowingGen(None)

  def flatShowingGen(flatShowingStatus: Option[FlatShowingStatus]): Gen[FlatShowing] =
    for {
      showingId <- readableString
      ownerRequestId <- readableString
      status <- flatShowingStatus.map(Gen.const).getOrElse(Gen.oneOf(FlatShowingStatus.values.toSeq))
      closeShowingCause <- Gen.oneOf(CloseShowingCause.values.toSeq)
      groupStatus <- Gen.oneOf(GroupStatus.values.toSeq)
      showingType <- Gen.oneOf(FlatShowingType.values.toSeq)
      isActive <- bool
    } yield FlatShowing(
      showingId,
      ownerRequestId,
      status,
      Some(closeShowingCause),
      groupStatus,
      Some(showingType),
      isActive,
      FlatShowingData.getDefaultInstance,
      None,
      DateTimeUtil.now(),
      DateTimeUtil.now()
    )

  def userShowingGen: Gen[UserShowing] =
    for {
      showingId <- readableString
      userId <- readableString
      isMain <- bool
    } yield UserShowing(showingId, userId, isMain)

  def ownerRequestGen: Gen[OwnerRequest] =
    for {
      ownerRequestId <- readableString
      flatId <- readableString
      status <- Gen.oneOf(
        Seq(
          OwnerRequestStatus.Draft,
          OwnerRequestStatus.Confirmed,
          OwnerRequestStatus.WorkInProgress,
          OwnerRequestStatus.LookingForTenant,
          OwnerRequestStatus.Denied,
          OwnerRequestStatus.Completed
        )
      )
      firstRent <- bool
      responsibleForPayment <- Gen.oneOf(ResponsibleForPayment.Owner, ResponsibleForPayment.Tenant)
      shouldSendReceiptPhotos <- bool
      shouldSendMetrics <- bool
      shouldTenantRefund <- bool
      paymentConfirmation <- bool
      hasServicesPaidByTenant <- bool
      settingsStatus <- Gen.oneOf(
        OwnerRequestSettingStatus.FilledByOwner,
        OwnerRequestSettingStatus.New,
        OwnerRequestSettingStatus.ConfirmedByTenant
      )
    } yield OwnerRequest(
      ownerRequestId,
      flatId,
      status,
      OwnerRequestData.getDefaultInstance,
      firstRent,
      Some(DateTimeUtil.now),
      responsibleForPayment,
      shouldSendReceiptPhotos,
      shouldSendMetrics,
      shouldTenantRefund,
      paymentConfirmation,
      hasServicesPaidByTenant,
      settingsStatus,
      DateTimeUtil.now,
      DateTimeUtil.now
    )

  def paymentGen(paymentType: Option[PaymentType] = None, paymentStatus: Option[PaymentStatus] = None): Gen[Payment] =
    for {
      paymentId <- readableString
      contractId <- readableString
      payType <- paymentType.map(Gen.const).getOrElse(Gen.oneOf(Seq(PaymentType.Rent, PaymentType.HouseServices)))
      payStatus <- paymentStatus
        .map(Gen.const)
        .getOrElse(
          Gen.oneOf(
            Seq(PaymentStatus.FuturePayment, PaymentStatus.New, PaymentStatus.PaidByTenant, PaymentStatus.PaidToOwner)
          )
        )
    } yield Payment(
      paymentId,
      contractId,
      payType,
      isPaidOutUnderGuarantee = false,
      DateTimeUtil.now,
      DateTimeUtil.now.minusHours(1),
      DateTimeUtil.now.plusDays(1),
      payStatus,
      PaymentData.newBuilder().build(),
      DateTimeUtil.now(),
      DateTimeUtil.now()
    )

  //scalastyle:off method.length cyclomatic.complexity
  def flatQuestionnaireGen: Gen[FlatQuestionnaire] = {
    for {
      flatId <- readableString
      area <- posNum[Float]
      floor <- posNum[Int]
      balconyAmount <- posNum[Int]
      loggiasAmount <- posNum[Int]
      combinedBathroomsAmount <- posNum[Int]
      separatedBathroomsAmount <- posNum[Int]
      kitchenSpace <- posNum[Int]

      offerCopyright <- readableString

      furniture <- flatQuestionnaireFurnitureGen

      flatProto = FlatQuestionnaireProto.Flat
        .newBuilder()
        .setFlatType(FlatTypeNamespace.FlatType.FLAT)
        .setArea(area)
        .setFloor(floor)
        .setBalcony(
          Balcony
            .newBuilder()
            .setLoggiaAmount(loggiasAmount)
            .setBalconyAmount(balconyAmount)
            .build()
        )
        .setRooms(RoomsNamespace.Rooms.TWO)
        .addAllWindowSide(
          Seq(
            FlatQuestionnaireProto.Flat.WindowSideNamespace.WindowSideType.STREET_SIDE
          ).asJava
        )
        .setBathroom(
          Bathroom
            .newBuilder()
            .setCombinedAmount(combinedBathroomsAmount)
            .setSeparatedAmount(separatedBathroomsAmount)
            .build()
        )
        .setRenovation(RenovationTypeNamespace.RenovationType.EURO)
        .setKitchenSpace(kitchenSpace)
        .build()

      paymentsProto <- flatQuestionnairePaymentsGen
      tenantRequirements <- flatQuestionnaireTenantRequirementsGen
      building <- flatQuestionnaireBuildingGen

      questionnaireProto = FlatQuestionnaireProto
        .newBuilder()
        .setOfferCopyright(offerCopyright)
        .setFlat(flatProto)
        .setPayments(paymentsProto)
        .setFurniture(furniture)
        .setTenantRequirements(tenantRequirements)
        .setBuilding(building)
        .build()
    } yield FlatQuestionnaire(
      flatId = flatId,
      data = questionnaireProto,
      presets = Set.empty
    )
  }
  //scalastyle:on

  def inventoryGen: Gen[Inventory] = {
    for {
      ownerRequestId <- readableString
      version <- posNum[Int]
      inventoryData <- inventoryDataGen
      shardKey <- posNum[Int]
    } yield Inventory(
      ownerRequestId = ownerRequestId,
      version = version,
      data = inventoryData,
      visitTime = None,
      shardKey = shardKey
    )
  }

  def inventoryDataGen: Gen[InventoryData] =
    for {
      managerComment <- readableString
      defects = inventoryDefectGen.next(2)
      rooms = inventoryRoomGen.next(2)
    } yield InventoryData
      .newBuilder()
      .addAllRooms(rooms.asJava)
      .addAllDefects(defects.asJava)
      .setManagerComment(managerComment)
      .setNeedApproveByManager(false)
      .build()

  def inventoryRoomGen: Gen[InventoryData.Room] =
    for {
      roomName <- readableString
      items = inventoryItemGen.next(2)
      roomClientId <- readableString
    } yield InventoryData.Room
      .newBuilder()
      .setRoomName(roomName)
      .addAllItems(items.asJava)
      .setRoomClientId(roomClientId)
      .build()

  def inventoryItemGen: Gen[InventoryData.Item] =
    for {
      itemName <- readableString
      photos = rentImageGen.next(2)
      count <- posNum[Int]
      defectId <- readableString
      itemClientId <- readableString
    } yield InventoryData.Item
      .newBuilder()
      .setItemName(itemName)
      .setCount(count)
      .addAllPhotos(photos.asJava)
      .setDefectId(defectId)
      .setItemClientId(itemClientId)
      .build()

  def inventoryDefectGen: Gen[InventoryData.Defect] =
    for {
      description <- readableString
      photos = rentImageGen.next(2)
      defectClientId <- readableString
    } yield InventoryData.Defect
      .newBuilder()
      .setDescription(description)
      .addAllPhotos(photos.asJava)
      .setDefectClientId(defectClientId)
      .build()

  def flatQuestionnairePaymentsGen: Gen[FlatQuestionnaireProto.Payments] = {
    for {
      rentalValue <- posNum[Int]
      adValue <- posNum[Int]
    } yield {
      FlatQuestionnaireProto.Payments
        .newBuilder()
        .setRentalValue(rentalValue)
        .setAdValue(adValue)
        .setElectricity(Payments.Electricity.newBuilder().setNeedPayment(true))
        .setWater(Payments.Water.newBuilder().setNeedPayment(true))
        .setSanitation(Payments.Sanitation.newBuilder().setNeedPayment(true))
        .setGas(Payments.Gas.newBuilder().setNeedPayment(true))
        .setHeating(Payments.Heating.newBuilder().setNeedPayment(true))
        .setInternet(Payments.Internet.newBuilder().setNeedPayment(true))
        .setBarrier(Payments.Barrier.newBuilder().setNeedPayment(true))
        .setParking(Payments.Parking.newBuilder().setNeedPayment(true))
        .setConcierge(Payments.Concierge.newBuilder().setNeedPayment(true))
        .setAllReceipt(Payments.AllReceipt.newBuilder().setNeedPayment(true))
        .build()
    }
  }

  def flatQuestionnaireBuildingGen: Gen[FlatQuestionnaireProto.Building] = {
    for {
      floors <- posNum[Int]
      passengerAmount <- posNum[Int]
      cargoAmount <- posNum[Int]
      option <- readableString
      transportAccessibility <- readableString
    } yield FlatQuestionnaireProto.Building
      .newBuilder()
      .setFloors(floors)
      .setElevators(
        FlatQuestionnaireProto.Building.Elevators
          .newBuilder()
          .setPassengerAmount(passengerAmount)
          .setCargoAmount(cargoAmount)
          .build()
      )
      .setHasConcierge(true)
      .setHasGarbageChute(true)
      .setHasWheelchairStorage(true)
      .setHasOption(option)
      .setHasBarrier(true)
      .addAllParking(Seq(ParkingNamespace.Parking.OPEN).asJava)
      .setTransportAccessibility(transportAccessibility)
      .setHasModernEntrance(true)
      .build()
  }

  def flatQuestionnaireTenantRequirementsGen: Gen[FlatQuestionnaireProto.TenantRequirements] = {
    for {
      maxTenantCount <- posNum[Int]
      preferencesForTenants <- readableString
    } yield FlatQuestionnaireProto.TenantRequirements
      .newBuilder()
      .setMaxTenantCount(maxTenantCount)
      .setHasWithChildrenRequirement(false)
      .setHasWithPetsRequirement(false)
      .setPreferencesForTenants(preferencesForTenants)
      .build()
  }

  def flatQuestionnaireFurnitureGen: Gen[FlatQuestionnaireProto.Furniture] = {
    import FlatQuestionnaireProto.Furniture._
    for {
      provider <- readableString
      price <- posNum[Int]
      dsc <- readableString

    } yield FlatQuestionnaireProto.Furniture
      .newBuilder()
      .setTv(Tv.newBuilder().setIsPresent(true).build())
      .setInternet(
        Internet
          .newBuilder()
          .setIsPresent(true)
          .setInternetProvider(provider)
          .setCanRenewProviderContract(true)
          .setPrice(price)
          .setInternetType(Internet.InternetTypeNamespace.InternetType.HAS_PROVIDER)
          .build()
      )
      .setOven(
        Oven
          .newBuilder()
          .setIsPresent(true)
          .setOvenType(Oven.OvenTypeNamespace.OvenType.INDUCTION)
          .build()
      )
      .setWashingMachine(WashingMachine.newBuilder().setIsPresent(true).build())
      .setDishWasher(DishWasher.newBuilder().setIsPresent(true).build())
      .setDryingMachine(DryingMachine.newBuilder().setIsPresent(true).build())
      .setFridge(Fridge.newBuilder().setIsPresent(true).build())
      .setConditioner(Conditioner.newBuilder().setIsPresent(true).build())
      .setBoiler(Boiler.newBuilder().setIsPresent(true).build())
      .setWarmFloor(WarmFloor.newBuilder().setIsPresent(true).build())
      .setOther(Other.newBuilder().setDescription(dsc).build())
      .build()
  }

  def keysHandoverGen: Gen[KeysHandover] =
    for {
      handoverId <- readableString
      flatId <- readableString
      contractId <- readableString
      direction <- Gen.oneOf(
        KeysHandoverDirection.FromTenantToManager,
        KeysHandoverDirection.FromOwnerToManager,
        KeysHandoverDirection.FromManagerToOwner,
        KeysHandoverDirection.FromManagerToTenant
      )
      documentId <- readableString
      data = KeysHandoverData.newBuilder().build()
    } yield KeysHandover(
      handoverId,
      flatId,
      Some(contractId),
      direction,
      Some(documentId),
      data,
      DateTimeUtil.now(),
      DateTimeUtil.now()
    )

  def meterReadingsGen(
    houseServiceId: String,
    periodId: String,
    status: MeterReadingsStatus = Gen
      .oneOf(
        MeterReadingsStatus.NotSent,
        MeterReadingsStatus.Sent,
        MeterReadingsStatus.ShouldBeSent,
        MeterReadingsStatus.Declined,
        MeterReadingsStatus.Sending
      )
      .next
  ): Gen[MeterReadings] =
    for {
      meterReadingsId <- readableString
      genShardKey <- Gen.oneOf(0, 1)
    } yield MeterReadings(
      meterReadingsId,
      periodId,
      houseServiceId,
      status,
      MeterReadingsData.getDefaultInstance,
      DateTimeUtil.now,
      DateTimeUtil.now,
      None,
      genShardKey
    )

  def periodGen(
    contractId: String,
    period: DateTime = DateTimeUtil.now
  ): Gen[Period] =
    for {
      periodId <- readableString
      meterReadingsStatus <- Gen.oneOf(
        AggregatedMeterReadingsStatus.ShouldBeSent,
        AggregatedMeterReadingsStatus.Sent,
        AggregatedMeterReadingsStatus.Declined,
        AggregatedMeterReadingsStatus.Expired,
        AggregatedMeterReadingsStatus.NotSent
      )
      billStatus <- Gen.oneOf(
        BillStatus.Paid,
        BillStatus.NotSent,
        BillStatus.ShouldBeSent,
        BillStatus.Declined,
        BillStatus.ShouldBePaid
      )
      receiptStatus <- Gen.oneOf(
        ReceiptStatus.Sent,
        ReceiptStatus.Declined,
        ReceiptStatus.NotSent,
        ReceiptStatus.CanBeSent,
        ReceiptStatus.ShouldBeSent
      )
      confirmationStatus <- Gen.oneOf(
        PaymentConfirmationStatus.Sent,
        PaymentConfirmationStatus.Declined,
        PaymentConfirmationStatus.NotSent,
        PaymentConfirmationStatus.CanBeSent,
        PaymentConfirmationStatus.ShouldBeSent
      )
      paymentId <- Gen.option(readableString)
    } yield services.Period(
      periodId,
      contractId,
      PeriodType.Regular,
      period.withFirstDayOfMonth,
      meterReadingsStatus,
      billStatus,
      receiptStatus,
      confirmationStatus,
      paymentId,
      PeriodData.getDefaultInstance,
      DateTimeUtil.now,
      DateTimeUtil.now
    )

  def houseServiceGen(ownerRequestId: String): Gen[HouseService] =
    for {
      houseServiceId <- readableString
    } yield services.HouseService(
      houseServiceId,
      ownerRequestId,
      HouseServiceType.Meter,
      HouseServiceData.newBuilder().setMeter(Meter.getDefaultInstance).build(),
      deleted = false,
      DateTimeUtil.now,
      DateTimeUtil.now
    )

  def rentApiImageGen: Gen[ImageProto] =
    for {
      namespace <- readableString
      groupId <- posNum[Int]
      name <- readableString
      url <- readableString
      alias <- readableString
    } yield ImageProto
      .newBuilder()
      .setNamespace(namespace)
      .setGroupId(groupId)
      .setName(name)
      .addAllImageUrls(
        List(
          ImageUrl
            .newBuilder()
            .setUrl(url)
            .setAlias(alias)
            .build()
        ).asJava
      )
      .build()

  def rentImageGen: Gen[Image] = {
    for {
      namespace <- readableString
      groupId <- posNum[Int]
      name <- readableString
    } yield Image
      .newBuilder()
      .setNamespace(namespace)
      .setGroupId(groupId)
      .setName(name)
      .build()
  }

  def flatDataImage(namespace: String, groupId: Int, name: String): Image = {
    Image
      .newBuilder()
      .setNamespace(namespace)
      .setGroupId(groupId)
      .setName(name)
      .build()
  }

  def flatDataGen: Gen[FlatData] = {
    for {
      address <- readableString
      subjectFederationId <- Gen.oneOf(0, 100)
      img1 = flatDataImage("namespace1", 1001, "name1")
      img2 = flatDataImage("namespace2", 1002, "name2")
      img3 = flatDataImage("namespace3", 1003, "name3")
      img4 = flatDataImage("namespace4", 1004, "name4")

      area <- posNum[Float]
      floor <- posNum[Int]

    } yield {
      FlatData
        .newBuilder()
        .setLocation(
          FlatLocation
            .newBuilder()
            .setAddress(address)
            .setSubjectFederationGeoid(subjectFederationId)
            .setSubjectFederationRgid(subjectFederationId)
            .build()
        )
        .setNearestMetro(
          FlatData.NearestMetro
            .newBuilder()
            .addAllMetro(
              Seq(
                FlatData.NearestMetro.Metro
                  .newBuilder()
                  .setId(101)
                  .setGeoId(1011)
                  .setTimeOnFoot(10)
                  .setTimeOnTransport(5)
                  .setName("Авиамоторная")
                  .build()
              ).asJava
            )
            .build()
        )
        .addAllRetouchedPhotos(Seq(img1, img2, img3, img4).asJava)
        .addAllFlatExcerptRequests(
          Seq(
            FlatExcerptsRequest
              .newBuilder()
              .setEvaluatedObjectInfo(
                EvaluatedObjectInfo
                  .newBuilder()
                  .setArea(area)
                  .setFloor(String.valueOf(floor))
                  .build()
              )
              .build()
          ).asJava
        )
        .setFlatInfo(ApiFlatInfo.newBuilder().setFlatType(FlatTypeNamespace.FlatType.FLAT))
        .build()
    }
  }

  def feedEntryGen: Gen[FeedEntry] = {
    for {
      flat <- flatGen()
      ownerRequest <- ownerRequestGen
      questionnaire <- flatQuestionnaireGen
    } yield FeedEntry(flat, ownerRequest.copy(flatId = flat.flatId), questionnaire)
  }

  def flatProtoViewGen: Gen[FlatProtoView] = {
    for {
      dbId <- posNum[Int]
      flat <- flatGen()
      createTime = Instant.ofEpochMilli(flat.createTime.getMillis)
      updateTime = Instant.ofEpochMilli(flat.updateTime.getMillis)
      visitTime = flat.visitTime.map(x => Instant.ofEpochMilli(x.getMillis))
    } yield FlatProtoView
      .newBuilder()
      .setId(dbId)
      .setFlatId(flat.flatId)
      .setOptString(flat.code, _ setCode _)
      .setData(flat.data)
      .setDataJson(ProtobufUtils.toJson(flat.data))
      .setAddress(flat.address)
      .setOptString(flat.unifiedAddress, _ setUnifiedAddress _)
      .setFlatNumber(flat.flatNumber)
      .setOptString(flat.nameFromRequest, _ setNameFromRequest _)
      .setOptString(flat.phoneFromRequest, _ setPhoneFromRequest _)
      .setCreateTime(TimeUtils.instantToProtoTimestamp(createTime))
      .setUpdateTime(TimeUtils.instantToProtoTimestamp(updateTime))
      .setOptValue[Timestamp](visitTime.map(TimeUtils.instantToProtoTimestamp), _ setVisitTime _)
      .setOptInt(flat.shardKey.wrapInOption(), _ setShardKey _)
      .setIsRented(flat.isRented)
      .build()
  }

  def ownerRequestProtoViewGen: Gen[OwnerRequestProtoView] = {
    for {
      dbId <- posNum[Int]
      ownerRequest <- ownerRequestGen
      protoStatus = OwnerRequestNamespace.Status
        .forNumber(ownerRequest.status.id)
        .wrapInOption()
        .getOrElse(OwnerRequestNamespace.Status.UNKNOWN)
      settingStatus = SettingsStatusNamespace.SettingsStatus
        .forNumber(ownerRequest.settingsStatus.id)
        .wrapInOption()
        .getOrElse(SettingsStatusNamespace.SettingsStatus.UNKNOWN)
      responsibleForPayment = ResponsibleForPaymentNamespace.ResponsibleForPayment
        .forNumber(ownerRequest.responsibleForPayment.id)
        .wrapInOption()
        .getOrElse(ResponsibleForPaymentNamespace.ResponsibleForPayment.UNKNOWN)
      createTime = Instant.ofEpochMilli(ownerRequest.createTime.getMillis)
      updateTime = Instant.ofEpochMilli(ownerRequest.updateTime.getMillis)
    } yield OwnerRequestProtoView
      .newBuilder()
      .setId(dbId)
      .setOwnerRequestId(ownerRequest.ownerRequestId)
      .setFlatId(ownerRequest.flatId)
      .setData(ownerRequest.data)
      .setDataJson(ProtobufUtils.toJson(ownerRequest.data))
      .setFirstRent(ownerRequest.firstRent)
      .setResponsibleForPayment(responsibleForPayment)
      .setShouldSendReceiptPhotos(ownerRequest.shouldSendReceiptPhotos)
      .setShouldSendMetrics(ownerRequest.shouldSendMetrics)
      .setShouldTenantRefund(ownerRequest.shouldTenantRefund)
      .setPaymentConfirmation(ownerRequest.paymentConfirmation)
      .setHasServicesPaidByTenant(ownerRequest.hasServicesPaidByTenant)
      .setStatus(protoStatus)
      .setSettingsStatus(settingStatus)
      .setCreateTime(TimeUtils.instantToProtoTimestamp(createTime))
      .setUpdateTime(TimeUtils.instantToProtoTimestamp(updateTime))
      .build()
  }
}
