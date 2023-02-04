package ru.yandex.vertis.shark.model

import baker.common.client.dadata.model.{DadataAddress, DadataOrganization}
import com.softwaremill.tagging._
import ru.auto.api.api_offer_model.{Offer => AutoOffer}
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.shark.model.AutoruCreditApplication.ExternalCommunication
import ru.yandex.vertis.shark.model.Block._
import ru.yandex.vertis.shark.model.CreditApplication.Claim.{BankPayload, NotSentReason}
import ru.yandex.vertis.shark.model.CreditApplication.{AutoruClaim, Claim}
import ru.yandex.vertis.shark.model.CreditApplicationSource.Payload
import ru.yandex.vertis.shark.model.CreditProduct._
import ru.yandex.vertis.shark.model.Entity.AddressEntity
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.shark.proto.model.CreditApplication.Communication.AutoruExternal
import ru.yandex.vertis.shark.proto.model.CreditProduct.IdempotencyType
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.geobase.Region
import ru.yandex.vertis.zio_baker.model.{FiasId, GeobaseId, KladrId, User}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object TestSyntax {

  implicit class RichSenderConverterContextCompanion(val self: SenderConverterContext.type) extends AnyVal {

    def forTest(converterContext: ConverterContext, suitable: Option[Suitable] = None) =
      self(converterContext, suitable)
  }

  implicit class RichPersonProfileImplCompanion(val self: PersonProfileImpl.type) extends AnyVal {
    import ru.yandex.vertis.shark.model.Block._

    def forTest(
        id: Option[PersonProfileId] = None,
        user: Option[User] = None,
        name: Option[NameBlock] = None,
        oldName: Option[OldNameBlock] = None,
        gender: Option[GenderBlock] = None,
        passportRf: Option[PassportRfBlock] = None,
        oldPassportRf: Option[OldPassportRfBlock] = None,
        foreignPassport: Option[ForeignPassportBlock] = None,
        insuranceNumber: Option[InsuranceNumberBlock] = None,
        driverLicense: Option[DriverLicenseBlock] = None,
        birthDate: Option[BirthDateBlock] = None,
        birthPlace: Option[BirthPlaceBlock] = None,
        residenceAddress: Option[ResidenceAddressBlock] = None,
        registrationAddress: Option[RegistrationAddressBlock] = None,
        education: Option[EducationBlock] = None,
        maritalStatus: Option[MaritalStatusBlock] = None,
        dependents: Option[DependentsBlock] = None,
        income: Option[IncomeBlock] = None,
        expenses: Option[ExpensesBlock] = None,
        propertyOwnership: Option[PropertyOwnershipBlock] = None,
        vehicleOwnership: Option[VehicleOwnershipBlock] = None,
        employment: Option[EmploymentBlock] = None,
        relatedPersons: Option[RelatedPersonsBlock] = None,
        phones: Option[PhonesBlock] = None,
        emails: Option[EmailsBlock] = None) =
      self(
        id = id,
        user = user,
        name = name,
        oldName = oldName,
        gender = gender,
        passportRf = passportRf,
        oldPassportRf = oldPassportRf,
        foreignPassport = foreignPassport,
        insuranceNumber = insuranceNumber,
        driverLicense = driverLicense,
        birthDate = birthDate,
        birthPlace = birthPlace,
        residenceAddress = residenceAddress,
        registrationAddress = registrationAddress,
        education = education,
        maritalStatus = maritalStatus,
        dependents = dependents,
        income = income,
        expenses = expenses,
        propertyOwnership = propertyOwnership,
        vehicleOwnership = vehicleOwnership,
        employment = employment,
        relatedPersons = relatedPersons,
        phones = phones,
        emails = emails
      )
  }

  implicit class RichPersonProfileStubCompanion(val self: PersonProfileStub.type) extends AnyVal {

    def forTest(id: Option[PersonProfileId] = None, user: Option[User] = None, blockTypes: Seq[proto.Block.BlockType]) =
      self(id = id, user = user, blockTypes = blockTypes)
  }

  implicit class RichPhoneEntityCompanion(val self: Entity.PhoneEntity.type) extends AnyVal {

    def forTest(phone: Phone, phoneType: Option[proto.Entity.PhoneEntity.PhoneType] = None) = self(phone, phoneType)
  }

  implicit class RichKladrCompanion(val self: Entity.AddressEntity.Kladr.type) extends AnyVal {

    def forTest(
        id: Option[KladrId] = None,
        regionId: Option[KladrId] = None,
        areaId: Option[KladrId] = None,
        cityId: Option[KladrId] = None,
        cityDistrictId: Option[KladrId] = None,
        settlementId: Option[KladrId] = None,
        streetId: Option[KladrId] = None,
        houseId: Option[KladrId] = None) =
      self(
        id = id,
        regionId = regionId,
        areaId = areaId,
        cityId = cityId,
        cityDistrictId = cityDistrictId,
        settlementId = settlementId,
        streetId = streetId,
        houseId = houseId
      )
  }

  implicit class RichFiasCompanion(val self: Entity.AddressEntity.Fias.type) extends AnyVal {

    def forTest(
        regionId: Option[FiasId] = None,
        areaId: Option[FiasId] = None,
        cityId: Option[FiasId] = None,
        cityDistrictId: Option[FiasId] = None,
        settlementId: Option[FiasId] = None,
        streetId: Option[FiasId] = None,
        houseId: Option[FiasId] = None,
        id: Option[FiasId] = None,
        code: Option[FiasCode] = None,
        level: Option[FiasCode] = None,
        actualityState: Option[FiasCode] = None) =
      self(
        regionId = regionId,
        areaId = areaId,
        cityId = cityId,
        cityDistrictId = cityDistrictId,
        settlementId = settlementId,
        streetId = streetId,
        houseId = houseId,
        id = id,
        code = code,
        level = level,
        actualityState = actualityState
      )
  }

  implicit class RichAutoConverterContextCompanion(val self: ConverterContext.AutoConverterContext.type)
    extends AnyVal {

    def forTest(
        timestamp: Instant,
        creditApplication: CreditApplication,
        creditProduct: Option[CreditProduct] = None,
        vosOffer: Option[AutoOffer] = None,
        parentRegions: Seq[Region] = Seq.empty,
        organization: Option[DadataOrganization] = None,
        gender: GenderType = GenderType.MALE,
        registrationAddress: Option[AddressEntity] = None,
        residenceAddress: Option[AddressEntity] = None,
        registrationDadataAddress: Option[DadataAddress] = None,
        residenceDadataAddress: Option[DadataAddress] = None) =
      self(
        timestamp = timestamp,
        creditApplication = creditApplication,
        creditProduct = creditProduct,
        vosOffer = vosOffer,
        parentRegions = parentRegions,
        organization = organization,
        gender = gender,
        registrationAddress = registrationAddress,
        residenceAddress = residenceAddress,
        registrationDadataAddress = registrationDadataAddress,
        residenceDadataAddress = residenceDadataAddress
      )
  }

  implicit class RichAutoCreditProductCompanion(val self: AutoCreditProduct.type) extends AnyVal {

    def forTest(
        id: CreditProductId,
        bankType: Option[proto.Bank.BankType],
        bankId: BankId,
        amountRange: AmountRange,
        interestRateRange: InterestRateRange,
        termMonthsRange: TermMonthsRange,
        minInitialFeeRate: Rate,
        geobaseIds: Seq[GeobaseId],
        creditApplicationInfoBlockDependencies: Seq[BlockDependency],
        borrowerPersonProfileBlockDependencies: Seq[BlockDependency],
        isActive: Boolean,
        borrowerConditions: Option[BorrowerConditions],
        clientFeatures: Seq[ClientFeature] = Seq.empty,
        priority: Priority = EmptyPriority,
        excludedGeobaseIds: Seq[GeobaseId] = Seq.empty,
        specificBorrowerConditions: Seq[BorrowerConditions] = Seq.empty,
        rateLimit: Option[RateLimit] = None,
        priorityTags: Set[proto.CreditProduct.PriorityTag] = Set.empty,
        creditProposalEntities: Seq[CreditProposal] = Seq.empty,
        objectPayload: Option[CreditProduct.ObjectPayload.Auto] = None,
        sendDelay: Option[FiniteDuration] = None,
        idempotencyType: IdempotencyType = IdempotencyType.UNKNOWN_IDEMPOTENCY_TYPE,
        useActualStateWhenSending: Boolean = false) =
      self(
        id = id,
        bankType = bankType,
        bankId = bankId,
        amountRange = amountRange,
        interestRateRange = interestRateRange,
        termMonthsRange = termMonthsRange,
        minInitialFeeRate = minInitialFeeRate,
        geobaseIds = geobaseIds,
        creditApplicationInfoBlockDependencies = creditApplicationInfoBlockDependencies,
        borrowerPersonProfileBlockDependencies = borrowerPersonProfileBlockDependencies,
        isActive = isActive,
        borrowerConditions = borrowerConditions,
        clientFeatures = clientFeatures,
        priority = priority,
        excludedGeobaseIds = excludedGeobaseIds,
        specificBorrowerConditions = specificBorrowerConditions,
        rateLimit = rateLimit,
        priorityTags = priorityTags,
        creditProposalEntities = creditProposalEntities,
        objectPayload = objectPayload,
        sendDelay = sendDelay,
        idempotencyType = idempotencyType,
        useActualStateWhenSending = useActualStateWhenSending
      )
  }

  implicit class RichCreditApplicationSourceCompanion(val self: CreditApplicationSource.type) extends AnyVal {

    def forTest(
        payload: Option[Payload] = None,
        state: Option[proto.CreditApplication.State] = None,
        requirements: Option[CreditApplication.Requirements] = None,
        info: Option[CreditApplication.Info] = None,
        borrowerPersonProfile: Option[PersonProfile] = None,
        userSettings: Option[CreditApplication.UserSettings] = None,
        forcedSending: Option[Boolean] = None) =
      self(
        payload = payload,
        state = state,
        requirements = requirements,
        info = info,
        borrowerPersonProfile = borrowerPersonProfile,
        userSettings = userSettings,
        forcedSending = forcedSending
      )
  }

  implicit class RichDealerCreditProductCompanion(val self: DealerCreditProduct.type) extends AnyVal {

    def forTest(
        id: CreditProductId,
        domain: Domain,
        bankType: Option[proto.Bank.BankType],
        bankId: BankId,
        amountRange: AmountRange,
        interestRateRange: InterestRateRange,
        termMonthsRange: TermMonthsRange,
        minInitialFeeRate: Rate,
        geobaseIds: Seq[GeobaseId],
        creditApplicationInfoBlockDependencies: Seq[BlockDependency],
        borrowerPersonProfileBlockDependencies: Seq[BlockDependency],
        isActive: Boolean,
        borrowerConditions: Option[BorrowerConditions],
        clientFeatures: Seq[ClientFeature] = Seq.empty,
        priority: Priority = EmptyPriority,
        excludedGeobaseIds: Seq[GeobaseId] = Seq.empty,
        specificBorrowerConditions: Seq[BorrowerConditions] = Seq.empty,
        rateLimit: Option[RateLimit] = None,
        priorityTags: Set[proto.CreditProduct.PriorityTag] = Set.empty,
        creditProposalEntities: Seq[CreditProposal] = Seq.empty,
        sendDelay: Option[FiniteDuration] = None,
        idempotencyType: IdempotencyType = IdempotencyType.BY_PRODUCT_N_DEALER,
        useActualStateWhenSending: Boolean = false) =
      self(
        id = id,
        domain = domain,
        bankType = bankType,
        bankId = bankId,
        amountRange = amountRange,
        interestRateRange = interestRateRange,
        termMonthsRange = termMonthsRange,
        minInitialFeeRate = minInitialFeeRate,
        geobaseIds = geobaseIds,
        creditApplicationInfoBlockDependencies = creditApplicationInfoBlockDependencies,
        borrowerPersonProfileBlockDependencies = borrowerPersonProfileBlockDependencies,
        isActive = isActive,
        borrowerConditions = borrowerConditions,
        clientFeatures = clientFeatures,
        priority = priority,
        excludedGeobaseIds = excludedGeobaseIds,
        specificBorrowerConditions = specificBorrowerConditions,
        rateLimit = rateLimit,
        priorityTags = priorityTags,
        creditProposalEntities = creditProposalEntities,
        sendDelay = sendDelay,
        idempotencyType = idempotencyType,
        useActualStateWhenSending = useActualStateWhenSending
      )
  }

  implicit class RichDealerCreditProductStubCompanion(val self: DealerCreditProductStub.type) extends AnyVal {

    def forTest(
        id: CreditProductId,
        domain: Domain,
        bankType: Option[proto.Bank.BankType],
        bankId: BankId,
        geobaseIds: Seq[GeobaseId],
        creditApplicationInfoBlockDependencies: Seq[BlockDependency],
        borrowerPersonProfileBlockDependencies: Seq[BlockDependency],
        isActive: Boolean,
        borrowerConditions: Option[BorrowerConditions],
        clientFeatures: Seq[ClientFeature] = Seq.empty,
        priority: Priority = EmptyPriority,
        excludedGeobaseIds: Seq[GeobaseId] = Seq.empty,
        specificBorrowerConditions: Seq[BorrowerConditions] = Seq.empty,
        rateLimit: Option[RateLimit] = None,
        priorityTags: Set[proto.CreditProduct.PriorityTag] = Set.empty,
        creditProposalEntities: Seq[CreditProposal] = Seq.empty,
        sendDelay: Option[FiniteDuration] = None,
        idempotencyType: IdempotencyType = IdempotencyType.BY_PRODUCT_N_DEALER,
        useActualStateWhenSending: Boolean = false) =
      self(
        id = id,
        domain = domain,
        bankType = bankType,
        bankId = bankId,
        geobaseIds = geobaseIds,
        creditApplicationInfoBlockDependencies = creditApplicationInfoBlockDependencies,
        borrowerPersonProfileBlockDependencies = borrowerPersonProfileBlockDependencies,
        isActive = isActive,
        borrowerConditions = borrowerConditions,
        clientFeatures = clientFeatures,
        priority = priority,
        excludedGeobaseIds = excludedGeobaseIds,
        specificBorrowerConditions = specificBorrowerConditions,
        rateLimit = rateLimit,
        priorityTags = priorityTags,
        creditProposalEntities = creditProposalEntities,
        sendDelay = sendDelay,
        idempotencyType = idempotencyType,
        useActualStateWhenSending = useActualStateWhenSending
      )
  }

  implicit class RichCreditApplicationInfoCompanion(val self: CreditApplication.Info.type) extends AnyVal {

    def forTest(
        controlWord: Option[ControlWordBlock] = None,
        okbStatementAgreement: Option[OkbStatementAgreementBlock] = None,
        advertStatementAgreementBlock: Option[AdvertStatementAgreementBlock] = None) =
      self(
        controlWord = controlWord,
        okbStatementAgreement = okbStatementAgreement,
        advertStatementAgreementBlock = advertStatementAgreementBlock
      )
  }

  implicit class RichCreditApplicationAutoruClaimCompanion(val self: CreditApplication.AutoruClaim.type)
    extends AnyVal {

    def forTest(
        id: CreditApplicationClaimId,
        bankClaimId: Option[CreditApplicationBankClaimId],
        created: Instant,
        updated: Instant,
        step: Step = 0.taggedWith[Tag.Step],
        processAfter: Option[Instant],
        creditProductId: CreditProductId,
        state: proto.CreditApplication.Claim.ClaimState,
        bankState: Option[String] = None,
        approvedMaxAmount: Option[MoneyRub] = None,
        approvedTermMonths: Option[MonthAmount] = None,
        approvedInterestRate: Option[Rate] = None,
        approvedMinInitialFeeRate: Option[Rate] = None,
        offerEntities: Seq[AutoruClaim.OfferEntity] = Seq.empty,
        bankPayload: Option[BankPayload] = None,
        sentSnapshot: Option[Claim.SentSnapshot] = None,
        decisionChecked: Option[Instant] = None,
        notSentReason: Option[NotSentReason] = None,
        idempotencyKey: Option[IdempotencyKey] = None) =
      self(
        id = id,
        bankClaimId = bankClaimId,
        created = created,
        updated = updated,
        step = step,
        processAfter = processAfter,
        creditProductId = creditProductId,
        state = state,
        bankState = bankState,
        approvedMaxAmount = approvedMaxAmount,
        approvedTermMonths = approvedTermMonths,
        approvedInterestRate = approvedInterestRate,
        approvedMinInitialFeeRate = approvedMinInitialFeeRate,
        offerEntities = offerEntities,
        bankPayload = bankPayload,
        sentSnapshot = sentSnapshot,
        decisionChecked = decisionChecked,
        notSentReason = notSentReason,
        idempotencyKey = idempotencyKey
      )
  }

  implicit class RichAutoruCreditApplicationExternalCommunicationCompanion(
      val self: AutoruCreditApplication.ExternalCommunication.type)
    extends AnyVal {

    def forTest(
        updated: Instant,
        lastEvent: Option[Instant] = None,
        eventScheduledAt: Option[Instant] = None,
        creditApplicationState: proto.CreditApplication.State = proto.CreditApplication.State.UNKNOWN_STATE,
        completenessState: AutoruExternal.CompletenessState =
          AutoruExternal.CompletenessState.UNKNOWN_COMPLETENESS_STATE,
        objectCommunicationState: AutoruExternal.ObjectCommunicationState =
          AutoruExternal.ObjectCommunicationState.UNKNOWN_OBJECT_COMMUNICATION_STATE,
        claimEntities: Seq[ExternalCommunication.ClaimEntity] = Seq.empty) =
      self(
        updated = updated,
        lastEvent = lastEvent,
        eventScheduledAt = eventScheduledAt,
        creditApplicationState = creditApplicationState,
        completenessState = completenessState,
        objectCommunicationState = objectCommunicationState,
        claimEntities = claimEntities
      )
  }

  implicit class RichAutoruCreditApplicationExternalCommunicationClaimEntityCompanion(
      val self: AutoruCreditApplication.ExternalCommunication.ClaimEntity.type)
    extends AnyVal {

    def forTest(
        creditProductId: CreditProductId,
        state: proto.CreditApplication.Communication.AutoruExternal.ClaimCommunicationState,
        claimState: Option[proto.CreditApplication.Claim.ClaimState] = None) =
      self(creditProductId = creditProductId, state = state, claimState = claimState)
  }
}
