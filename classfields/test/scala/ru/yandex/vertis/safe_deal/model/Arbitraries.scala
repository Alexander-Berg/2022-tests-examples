package ru.yandex.vertis.safe_deal.model

import java.net.URL
import java.time.{Instant, LocalDate, LocalTime, ZoneOffset}
import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.ByteString
import com.softwaremill.tagging._
import org.scalacheck.Gen.{alphaChar, alphaNumChar, asciiChar, choose, frequency, numChar, oneOf}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.rng.Seed
import ru.auto.api.api_offer_model.Category
import ru.auto.api.common_model.PhotoClass
import ru.yandex.vertis.zio_baker.app
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.{AutoUser, GeobaseId, OfferId, User, UserId}
import ru.yandex.vertis.common.{Domain, Platform}
import ru.yandex.vertis.safe_deal.client.bank.model.Operation
import ru.yandex.vertis.safe_deal.model.Api.{CommissionTariffResponse, CurrentCommissionTariffResponse}
import ru.yandex.vertis.safe_deal.model.AutoruDeal.Offer
import ru.yandex.vertis.safe_deal.proto.model.Api.PaymentStatusResponse.PaymentStatus
import ru.yandex.vertis.safe_deal.proto.common.{
  BuyerStep,
  CancellationReason,
  CancelledBy,
  DealState,
  DealStep,
  ParticipantType,
  PaymentType,
  SellerStep
}
import ru.yandex.vertis.safe_deal.model.event.DealEvent
import ru.yandex.vertis.safe_deal.model.tinkoff.{TinkoffDeal, TinkoffRefillBankDetails}
import ru.yandex.vertis.safe_deal.proto.{model => proto}
import ru.yandex.vertis.safe_deal.proto.{tinkoff => proto_tinkoff}
import ru.yandex.vertis.safe_deal.util.RichAutoruModel.RichDealBankType.DefaultDealBankType

import scala.concurrent.duration._
import scala.language.implicitConversions

object Arbitraries {

  import org.scalacheck.magnolia._

  def generate[T](implicit arb: Arbitrary[T]): Gen[T] = arb.arbitrary

  implicit private def wrap[A](g: Gen[A]): Arbitrary[A] = Arbitrary(g)

  implicit class RichGen[T](val gen: Gen[T]) extends AnyVal {
    def tagged[U]: Arbitrary[T @@ U] = gen.map(_.taggedWith[U])

    def ? : Gen[Option[T]] = Gen.option(gen)
  }

  implicit class RichArbitrary[T](val arb: Arbitrary[T]) extends AnyVal {
    def tagged[U]: Arbitrary[T @@ U] = Arbitrary(arb.arbitrary.map(_.taggedWith[U]))

    def withPrefix(prefix: String): Arbitrary[String] = Arbitrary(arb.arbitrary.map(v => s"$prefix$v"))
  }

  /** Generates russian upper-case alpha character */
  def cyrillicUpperChar: Gen[Char] = frequency((32, choose(1040.toChar, 1071.toChar)), (1, 1025.toChar))

  /** Generates russian lower-case alpha character */
  def cyrillicLowerChar: Gen[Char] = frequency((32, choose(1072.toChar, 1103.toChar)), (1, 1105.toChar))

  /** Generates russian  alpha character */
  def cyrillicChar: Gen[Char] = frequency((1, cyrillicUpperChar), (9, cyrillicLowerChar))

  def cyrillicNumChar: Gen[Char] = frequency((1, numChar), (9, cyrillicChar))

  def generateSeq[T](n: Int, filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): Seq[T] =
    Seq.fill(n)(generate[T](filter))

  def generate[T](filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): T =
    arb.arbitrary.suchThat(filter).pureApply(Gen.Parameters.default, Seed.random())

  val alphaNumStr: Arbitrary[String] = makeStr(alphaNumChar)

  val alphaStr: Arbitrary[String] = makeStr(alphaChar)

  val cyrillicNumStr: Arbitrary[String] = makeStr(cyrillicNumChar)

  val cyrillicStr: Arbitrary[String] = makeStr(cyrillicChar)

  val shortCyrillicNumStr: Arbitrary[String] = makeStr(1, 5, cyrillicNumChar)

  val byteString: Arbitrary[ByteString] =
    makeStr(5, 500, asciiChar).arbitrary.map(ByteString.copyFromUtf8)

  private def makeStr(min: Int, max: Int, gen: Gen[Char]): Arbitrary[String] =
    for {
      n <- Gen.chooseNum(min, max)
      str <- Gen.listOfN(n, gen).map(_.mkString)
    } yield str

  private def makeStr(gen: Gen[Char]): Arbitrary[String] = makeStr(3, 15, gen)

  implicit lazy val InstantArb: Arbitrary[Instant] = {
    val maxDelta = 500.days.toSeconds
    Gen.chooseNum(-maxDelta, maxDelta).map(Instant.now().plusSeconds)
  }

  val dlSecondPartGenAlpha: Gen[String] = Gen.listOfN(2, oneOf("авекмнорстухabekmhopctyx")).map(_.mkString)
  val dlSecondPartGenNumber: Gen[String] = Gen.listOfN(2, numChar).map(_.mkString)
  val dlSecondPartGen: Arbitrary[String] = frequency((1, dlSecondPartGenAlpha), (1, dlSecondPartGenNumber))

  implicit lazy val BooleanArb: Arbitrary[Boolean] = Gen.oneOf(true, false)
  implicit lazy val DomainArb: Arbitrary[Domain] = Gen.oneOf(Domain.values.filterNot(_.isDomainUnknown))

  implicit lazy val LocalDateArb: Arbitrary[LocalDate] = InstantArb.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalDate)
  implicit lazy val LocalTimeArb: Arbitrary[LocalTime] = InstantArb.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalTime)

  implicit lazy val DealIdArb: Arbitrary[DealId] = alphaNumStr.tagged[Tag.DealId]
  implicit lazy val DealNumberArb: Arbitrary[DealNumber] = Gen.choose(10000L, 20000L).tagged[Tag.DealNumber]
  implicit lazy val DocumentIdArb: Arbitrary[DocumentId] = alphaNumStr.tagged[Tag.DocumentId]
  implicit lazy val DocumentNameArb: Arbitrary[DocumentName] = alphaNumStr.tagged[Tag.DocumentName]
  implicit lazy val StsSeriesNumberArb: Arbitrary[StsSeriesNumber] = alphaNumStr.tagged[Tag.StsSeriesNumber]
  implicit lazy val PtsSeriesNumberArb: Arbitrary[PtsSeriesNumber] = alphaNumStr.tagged[Tag.PtsSeriesNumber]
  implicit lazy val VinNumberArb: Arbitrary[VinNumber] = alphaNumStr.tagged[Tag.VinNumber]
  implicit lazy val BikArb: Arbitrary[Bic] = alphaNumStr.tagged[Tag.Bic]
  implicit lazy val AccountNumberArb: Arbitrary[AccountNumber] = alphaNumStr.tagged[Tag.AccountNumber]
  implicit lazy val CorrAccountNumberArb: Arbitrary[CorrAccountNumber] = alphaNumStr.tagged[Tag.CorrAccountNumber]

  implicit lazy val BankIdArb: Arbitrary[BankId] = alphaNumStr.tagged[Tag.BankId]

  implicit lazy val PersonProfileIdArb: Arbitrary[PersonProfileId] = alphaNumStr.tagged[Tag.PersonProfileId]
  implicit lazy val NotificationIdArb: Arbitrary[NotificationId] = alphaNumStr.tagged[Tag.NotificationId]

  implicit lazy val NotificationTemplateIdArb: Arbitrary[NotificationTemplateId] =
    alphaNumStr.tagged[Tag.NotificationTemplateId]
  implicit lazy val MoneyRubArb: Arbitrary[MoneyRub] = Gen.choose(1L, 100000000L).tagged[Tag.MoneyRub]
  implicit lazy val MonthAmountArb: Arbitrary[MonthAmount] = Gen.choose(1, 365).tagged[Tag.MonthAmount]
  implicit lazy val YearsAmountArb: Arbitrary[YearAmount] = Gen.choose(1, 99).tagged[Tag.YearAmount]
  implicit lazy val RateArb: Arbitrary[Rate] = Gen.choose(0f, 100f).tagged[Tag.Rate]

  implicit lazy val GeobaseIdArb: Arbitrary[GeobaseId] =
    Gen.choose(0, Long.MaxValue).tagged[zio_baker.Tag.GeobaseId]
  implicit lazy val UserIdArb: Arbitrary[UserId] = alphaNumStr.withPrefix("user:").tagged[zio_baker.Tag.UserId]
  implicit lazy val RusPostCodeArb: Arbitrary[RusPostCode] = Gen.choose(1, 999999).tagged[Tag.RusPostCode]
  implicit lazy val OfferIdArb: Arbitrary[OfferId] = alphaNumStr.tagged[zio_baker.Tag.OfferId]
  implicit lazy val NameArb: Arbitrary[Name] = cyrillicStr.tagged[Tag.Name]
  implicit lazy val SurnameArb: Arbitrary[Surname] = cyrillicStr.tagged[Tag.Surname]
  implicit lazy val PatronymicArb: Arbitrary[Patronymic] = cyrillicStr.tagged[Tag.Patronymic]
  implicit lazy val DocSeriesArb: Arbitrary[DocSeries] = Gen.listOfN(4, numChar).map(_.mkString).tagged[Tag.DocSeries]
  implicit lazy val DocNumberArb: Arbitrary[DocNumber] = Gen.listOfN(6, numChar).map(_.mkString).tagged[Tag.DocNumber]
  implicit lazy val DepartNameArb: Arbitrary[DepartName] = cyrillicNumStr.tagged[Tag.DepartName]
  implicit lazy val BirthPlaceArb: Arbitrary[BirthPlace] = cyrillicNumStr.tagged[Tag.BirthPlace]
  implicit lazy val AddressArb: Arbitrary[Address] = cyrillicNumStr.tagged[Tag.Address]
  implicit lazy val VehicleMarkArb: Arbitrary[VehicleMark] = alphaStr.tagged[Tag.VehicleMark]
  implicit lazy val YandexUidArb: Arbitrary[app.context.YandexUid] = alphaStr.tagged[app.context.Tag.YandexUid]
  implicit lazy val ClientFeatureArb: Arbitrary[ClientFeature] = alphaStr.tagged[Tag.ClientFeature]
  implicit lazy val StepArb: Arbitrary[Step] = Gen.choose(0, 100).tagged[Tag.Step]
  implicit lazy val PriorityArb: Arbitrary[Priority] = Gen.choose(0, 3).tagged[Tag.Priority]
  implicit lazy val StringDateArb: Arbitrary[StringDate] = Gen.const("02.01.2004").tagged[Tag.StringDate]
  implicit lazy val CommissionTariffIdArb: Arbitrary[CommissionTariffId] = alphaStr.tagged[Tag.CommissionTariffId]
  implicit lazy val CorrelationIdArb: Arbitrary[CorrelationId] = alphaStr.tagged[Tag.CorrelationId]

  implicit lazy val TinkoffRefillBankDetailsIdArb: Arbitrary[TinkoffRefillBankDetailsId] =
    alphaStr.tagged[Tag.TinkoffRefillBankDetailsId]
  implicit lazy val BankDetailsIdArb: Arbitrary[BankDetailsId] = alphaStr.tagged[Tag.BankDetailsId]
  implicit lazy val BankPaymentIdArb: Arbitrary[BankPaymentId] = alphaStr.tagged[Tag.BankPaymentId]
  implicit lazy val BankUserIdArb: Arbitrary[BankUserId] = alphaStr.tagged[Tag.BankUserId]
  implicit lazy val RecipientIdArb: Arbitrary[RecipientId] = alphaStr.tagged[Tag.RecipientId]
  implicit lazy val BankDealIdArb: Arbitrary[BankDealId] = alphaStr.tagged[Tag.BankDealId]
  implicit lazy val BankStepIdArb: Arbitrary[BankStepId] = alphaStr.tagged[Tag.BankStepId]

  implicit lazy val DealStateArb: Arbitrary[DealState] =
    Gen.oneOf(DealState.values.filterNot(_.isUnknownState))

  implicit lazy val DealStepArb: Arbitrary[DealStep] =
    Gen.oneOf(DealStep.values.filterNot(_.isDealStepUnknown))

  implicit lazy val BuyerStepArb: Arbitrary[BuyerStep] =
    Gen.oneOf(BuyerStep.values.filterNot(_.isBuyerStepUnknown))

  implicit lazy val SellerStepArb: Arbitrary[SellerStep] =
    Gen.oneOf(SellerStep.values.filterNot(_.isSellerStepUnknown))

  implicit lazy val ParticipantTypeArb: Arbitrary[ParticipantType] =
    Gen.oneOf(ParticipantType.values.filterNot(_.isUnknownRecipientType))

  implicit lazy val TinkoffPaymentStatusArb: Arbitrary[proto_tinkoff.TinkoffPayment.Status] =
    Gen.oneOf(proto_tinkoff.TinkoffPayment.Status.values.filterNot(_.isUnknownTinkoffPaymentStatus))

  implicit lazy val TinkoffDealStatusArb: Arbitrary[proto_tinkoff.TinkoffDeal.Status] =
    Gen.oneOf(proto_tinkoff.TinkoffDeal.Status.values.filterNot(_.isUnknownTinkoffDealStatus))

  implicit lazy val TinkoffDealStepStatusArb: Arbitrary[proto_tinkoff.TinkoffDealStep.Status] =
    Gen.oneOf(proto_tinkoff.TinkoffDealStep.Status.values.filterNot(_.isUnknownTinkoffDealStepStatus))

  implicit lazy val BankOperationIdArb: Arbitrary[BankOperationId] = alphaStr.tagged[Tag.BankOperationId]

  implicit lazy val AmountArb: Arbitrary[Amount] = Arbitrary(Gen.posNum[Int].map(BigDecimal(_))).tagged[Tag.Amount]
  implicit lazy val UinArb: Arbitrary[Uin] = alphaStr.tagged[Tag.Uin]

  implicit lazy val PurposeArb: Arbitrary[PaymentPurpose] =
    arbitrary[DealNumber].map(dn => s"№$dn").tagged[Tag.PaymentPurpose]

  implicit lazy val DocumentTypeArb: Arbitrary[proto.DocumentPhoto.DocumentType] =
    Gen.oneOf(proto.DocumentPhoto.DocumentType.values.filterNot(_.isUnknownDocumentType))

  private lazy val TotalProvidedRubArb: Arbitrary[Long] = Gen.choose(0L, 10050000L)

  implicit lazy val UrlArb: Arbitrary[URL] = for {
    path <- alphaStr.arbitrary
    host = "domain"
    domain = "com"
    schema = "http://"
  } yield new URL(s"$schema$host.$domain/$path")

  implicit lazy val NotificationTypeArb: Arbitrary[proto.Notification.NotificationType] =
    Gen.oneOf(proto.Notification.NotificationType.values.filterNot(_.isUnknownNotificationType))

  implicit lazy val NotificationStateArb: Arbitrary[proto.Notification.State] =
    Gen.oneOf(proto.Notification.State.values.filterNot(_.isUnknownState))

  implicit lazy val CategoryArb: Arbitrary[Category] = Gen.oneOf(Category.values.filterNot(_.isCategoryUnknown))

  implicit lazy val PlatformArb: Arbitrary[Platform] =
    Gen.oneOf(Platform.values.filterNot(_.isPlatformUnknown))

  implicit lazy val PhotoClassArb: Arbitrary[PhotoClass] =
    Gen.oneOf(PhotoClass.values.filterNot(_.isPhotoClassUndefined))

  implicit lazy val PhoneArb: Arbitrary[Phone] = for {
    phone <- Gen.listOfN(10, numChar).map(_.mkString)
  } yield Phone(s"7$phone")

  implicit lazy val EmailArb: Arbitrary[Email] = for {
    firstChar <- alphaChar.arbitrary
    name <- alphaNumStr.arbitrary
    domainName <- alphaNumStr.arbitrary
    zone <- alphaStr.arbitrary
  } yield Email(s"$firstChar$name@$domainName.$zone")

  implicit lazy val DepartCodeArb: Arbitrary[DepartCode] = for {
    first <- Gen.listOfN(3, numChar).map(_.mkString)
    last <- Gen.listOfN(3, numChar).map(_.mkString)
  } yield "%s-%s".format(first, last).taggedWith[Tag.DepartCode]

  implicit lazy val InnArb: Arbitrary[Inn] = Gen.listOfN(12, numChar).map(_.mkString.taggedWith[Tag.Inn])
  implicit lazy val KppArb: Arbitrary[Kpp] = Gen.listOfN(9, numChar).map(_.mkString.taggedWith[Tag.Kpp])

  implicit lazy val BankNameArb: Arbitrary[BankName] = Gen.alphaStr.map(_.taggedWith[Tag.BankName])

  implicit lazy val PlatformUrlArb: Arbitrary[PushNotification.PayloadContent.PlatformUrl] = for {
    web <- alphaStr.arbitrary.?
    android <- alphaStr.arbitrary.?
    iOs <- alphaStr.arbitrary.?
  } yield PushNotification.PayloadContent.PlatformUrl(web, android, iOs)

  implicit lazy val DocumentPhotoArb: Arbitrary[DocumentPhoto] = for {
    documentType <- DocumentTypeArb.arbitrary
    photo <- EncryptedImageArb.arbitrary
    isDeleted <- BooleanArb.arbitrary
    documentId <- DocumentIdArb.arbitrary
    documentName <- DocumentNameArb.arbitrary.?
    // Not present in protobuf
    isJustAdded = false
  } yield DocumentPhoto(
    documentType = documentType,
    photo = photo,
    isDeleted = isDeleted,
    documentId = documentId,
    documentName = documentName,
    isJustAdded = isJustAdded
  )

  implicit lazy val PersonProfileArb: Arbitrary[PersonProfile] = for {
    personProfile <- gen[PersonProfileImpl].arbitrary
    nameEntity <- gen[Entity.NameEntity].arbitrary
  } yield personProfile.copy(name = nameEntity.some)

  implicit lazy val PtsCarInfoArb: Arbitrary[AutoruSubject.PtsCarInfo] = for {
    issuer <- cyrillicStr.arbitrary
  } yield AutoruSubject.PtsCarInfo(
    displacement = 3000.some,
    engineModel = "qweqweqw".some,
    engineNumber = "2312321".some,
    chassisNumber = "321321".some,
    bodyNumber = "12345678901234567".some,
    seriesNumber = "23TF234523".taggedWith[Tag.PtsSeriesNumber].some,
    issueDate = "02.01.2004".taggedWith[Tag.StringDate].some,
    issuer = issuer.some,
    licensePlate = "К222КК136".some
  )

  implicit lazy val StsCarInfoArb: Arbitrary[AutoruSubject.StsCarInfo] = for {
    issuer <- cyrillicStr.arbitrary
  } yield AutoruSubject.StsCarInfo(
    seriesNumber = "23TF234523".taggedWith[Tag.StsSeriesNumber].some,
    issueDate = "02.01.2004".taggedWith[Tag.StringDate].some,
    issuer = issuer.some
  )

  implicit lazy val OfferCarInfoArb: Arbitrary[AutoruSubject.OfferCarInfo] = for {
    mileage <- Gen.choose(1, 1000000)
    year <- Gen.choose(1950, 2020)
    horsePower <- Gen.choose(10, 200)
    subcategory <- cyrillicStr.arbitrary
  } yield AutoruSubject.OfferCarInfo(
    year,
    mileage,
    horsePower,
    subcategory,
    "Красный"
  )

  implicit lazy val NotificationTemplatePushContentArb: Arbitrary[NotificationTemplate.PushContent] = for {
    title <- cyrillicStr.arbitrary
    body <- cyrillicStr.arbitrary
  } yield NotificationTemplate.PushContent(
    title = title,
    body = body,
    action = None
  )

  implicit lazy val NotificationTemplateChatContentArb: Arbitrary[NotificationTemplate.ChatContent] = for {
    body <- cyrillicStr.arbitrary
  } yield NotificationTemplate.ChatContent(body = body)

  implicit lazy val NotificationTemplateContentArb: Arbitrary[NotificationTemplate.Content] = for {
    pushContent <- NotificationTemplatePushContentArb.arbitrary
  } yield pushContent

  implicit lazy val NotificationTemplateArb: Arbitrary[NotificationTemplate] = for {
    id <- NotificationTemplateIdArb.arbitrary
    content <- NotificationTemplateContentArb.arbitrary
  } yield NotificationTemplate(
    id = id,
    domain = Domain.DOMAIN_AUTO,
    content = content
  )

  implicit lazy val UserArb: Arbitrary[User] = for {
    id <- UserIdArb.arbitrary
  } yield AutoUser(
    id = id
  )

  implicit lazy val PushNotificationByTemplateContentArb: Arbitrary[PushNotification.ByTemplateContent] = for {
    template <- NotificationTemplateArb.arbitrary
  } yield PushNotification.ByTemplateContent(
    template = template,
    params = Map.empty
  )

  implicit lazy val PushNotificationPayloadContentArb: Arbitrary[PushNotification.PayloadContent] = for {
    title <- cyrillicStr.arbitrary
    body <- cyrillicStr.arbitrary
  } yield PushNotification.PayloadContent(
    title = title,
    body = body,
    action = None
  )

  implicit lazy val PushNotificationContentArb: Arbitrary[PushNotification.Content] = for {
    byTemplateContent <- PushNotificationByTemplateContentArb.arbitrary
  } yield byTemplateContent

  implicit lazy val PushNotificationArb: Arbitrary[PushNotification] = for {
    id <- NotificationIdArb.arbitrary
    created <- InstantArb.arbitrary
    updated <- InstantArb.arbitrary
    recipient <- UserArb.arbitrary
    content <- PushNotificationContentArb.arbitrary
  } yield PushNotification(
    id = id,
    created = created,
    updated = updated,
    state = proto.Notification.State.NEW,
    recipient = recipient,
    idempotencyKey = None,
    content = content
  )

  implicit lazy val NotificationArb: Arbitrary[Notification] = for {
    pushNotification <- PushNotificationArb.arbitrary
  } yield pushNotification

  implicit lazy val PushNotificationSourceArb: Arbitrary[PushNotificationSource] = for {
    content <- PushNotificationContentArb.arbitrary
  } yield PushNotificationSource(
    participantType = ParticipantType.SELLER,
    content = content
  )

  implicit lazy val NotificationSourceArb: Arbitrary[NotificationSource] = for {
    pushNotificationSource <- PushNotificationSourceArb.arbitrary
  } yield pushNotificationSource

  implicit lazy val OfferArb: Arbitrary[Offer] = for {
    category <- CategoryArb.arbitrary
    offerId <- OfferIdArb.arbitrary
  } yield Offer(category, offerId)

  implicit lazy val AutoruDealSubjectArb: Arbitrary[AutoruSubject] = for {
    offer <- OfferArb.arbitrary.?
    vin <- VinNumberArb.arbitrary.?
    mark <- alphaNumStr.arbitrary.?
    model <- alphaNumStr.arbitrary.?
    carInfo <- OfferCarInfoArb.arbitrary.?
    ptsInfo <- PtsCarInfoArb.arbitrary.?
    stsInfo <- StsCarInfoArb.arbitrary.?
  } yield AutoruSubject(
    offer = offer,
    vin = vin,
    mark = mark,
    model = model,
    carInfo = carInfo,
    ptsInfo = ptsInfo,
    stsInfo = stsInfo
  )

  implicit lazy val DealSubjectArb: Arbitrary[DealSubject] = AutoruDealSubjectArb.arbitrary

  implicit lazy val DealUpdateInfoArb: Arbitrary[Deal.UpdateInfo] = for {
    created <- InstantArb.arbitrary
    updated <- InstantArb.arbitrary
    stepUpdated <- InstantArb.arbitrary
    buyerStepUpdated <- InstantArb.arbitrary
    sellerStepUpdated <- InstantArb.arbitrary
  } yield Deal.UpdateInfo(
    created = created,
    updated = updated,
    completed = None,
    stepUpdated = stepUpdated,
    buyerStepUpdated = buyerStepUpdated,
    sellerStepUpdated = sellerStepUpdated
  )

  implicit lazy val CancelledByArb: Arbitrary[CancelledBy] =
    Gen.oneOf(CancelledBy.values.filterNot(_.isCancelledByUnknown))

  implicit lazy val CancellationReasonArb: Arbitrary[CancellationReason] =
    Gen.oneOf(CancellationReason.values.filterNot(_.isCancellationReasonUnknown))

  implicit lazy val DealCancelInfoArb: Arbitrary[Deal.CancelInfo] = for {
    cancelledBy <- CancelledByArb.arbitrary
    cancelDescription <- alphaNumStr.arbitrary.?
    cancelReason <- CancellationReasonArb.arbitrary
  } yield Deal.CancelInfo(cancelledBy = cancelledBy, cancelReason = cancelReason, cancelDescription = cancelDescription)

  implicit lazy val DealMeetingInfoArb: Arbitrary[Deal.MeetingInfo] = for {
    geobaseId <- GeobaseIdArb.arbitrary.?
    regName <- alphaStr.arbitrary
    region = geobaseId.map(RegionInfo(_, regName))
    meetingDate <- StringDateArb.arbitrary.?
  } yield Deal.MeetingInfo(geobaseId = geobaseId, region = region, meetingDate = meetingDate)

  implicit lazy val DealPriceInfoArb: Arbitrary[Deal.PriceInfo] = for {
    sellingPriceRub <- Gen.choose(1000, 10000).map(_.toLong).?
    feeAmountRub <- Gen.choose(10, 100).map(_.toLong).?
    totalPriceRub = sellingPriceRub.map(_ + feeAmountRub.getOrElse(0L))
  } yield Deal.PriceInfo(sellingPriceRub = sellingPriceRub, feeAmountRub = feeAmountRub, totalPriceRub = totalPriceRub)

  implicit lazy val SafeDealAccountArb: Arbitrary[SafeDealAccount] = for {
    bic <- BikArb.arbitrary
    accountNumber <- AccountNumberArb.arbitrary
    inn <- InnArb.arbitrary
    fullName <- NameArb.arbitrary
    corrAccountNumber <- CorrAccountNumberArb.arbitrary
    bankName <- BankNameArb.arbitrary
    receiverName <- alphaStr.arbitrary
    shortReceiverName <- alphaStr.arbitrary
  } yield SafeDealAccount(
    bic = bic,
    accountNumber = accountNumber,
    inn = inn,
    fullName = fullName,
    corrAccountNumber = corrAccountNumber,
    bankName = bankName,
    receiverName = receiverName,
    shortReceiverName = shortReceiverName
  )

  implicit lazy val EscrowArb: Arbitrary[Escrow] = for {
    bic <- BikArb.arbitrary
    amountRub <- Gen.choose(1, 100).map(_.toLong).arbitrary.?
    credited <- LocalDateArb.arbitrary.?
    debited <- LocalDateArb.arbitrary.?
    recipient <- ParticipantTypeArb.arbitrary
    operationId <- BankOperationIdArb.arbitrary
  } yield Escrow(
    bic = bic,
    amountRub = amountRub,
    credited = credited,
    debited = debited,
    recipient = recipient,
    operationId = operationId
  )

  implicit lazy val BuyerArb: Arbitrary[Buyer] = for {
    user <- UserArb.arbitrary
    personProfile <- PersonProfileArb.arbitrary.?
    isPassportProvided <- BooleanArb.arbitrary
    passportCorrelationId <- CorrelationIdArb.arbitrary.?
    isPriceApproved <- BooleanArb.arbitrary
    isMoneyProvided <- BooleanArb.arbitrary
    isReadyForMeeting <- BooleanArb.arbitrary
    isDocumentsUploaded <- BooleanArb.arbitrary
    confirmationCode <- DealPartyConfirmationCodeArb.arbitrary.?
    isPassportValid <- BooleanArb.arbitrary.?
    userName <- alphaStr.arbitrary.?
    phoneConfirmationCode <- DealPartyConfirmationCodeArb.arbitrary.?
    isDealAccepted <- BooleanArb.arbitrary
  } yield Buyer(
    user = user,
    personProfile = personProfile,
    isPassportProvided = isPassportProvided,
    passportCorrelationId = passportCorrelationId,
    isPriceApproved = isPriceApproved,
    isMoneyProvided = isMoneyProvided,
    isReadyForMeeting = isReadyForMeeting,
    isDocumentsUploaded = isDocumentsUploaded,
    confirmationCode = confirmationCode,
    isPassportValid = isPassportValid,
    userName = userName,
    phoneConfirmationCode = phoneConfirmationCode,
    isDealAccepted = isDealAccepted
  )

  implicit lazy val SellerArb: Arbitrary[Seller] = for {
    user <- UserArb.arbitrary
    personProfile <- PersonProfileArb.arbitrary.?
    isDealAccepted <- BooleanArb.arbitrary
    isPassportProvided <- BooleanArb.arbitrary
    passportCorrelationId <- CorrelationIdArb.arbitrary.?
    isSubjectInfoProvided <- BooleanArb.arbitrary
    isPriceApproved <- BooleanArb.arbitrary
    isBankingDetailsProvided <- BooleanArb.arbitrary
    isDocumentsApproved <- BooleanArb.arbitrary
    confirmationCode <- DealPartyConfirmationCodeArb.arbitrary.?
    isPassportValid <- BooleanArb.arbitrary.?
    userName <- alphaStr.arbitrary.?
    isBankingEntityValid <- BooleanArb.arbitrary.?
    phoneConfirmationCode <- DealPartyConfirmationCodeArb.arbitrary.?
  } yield Seller(
    user = user,
    personProfile = personProfile,
    isDealAccepted = isDealAccepted,
    isPassportProvided = isPassportProvided,
    passportCorrelationId = passportCorrelationId,
    isSubjectInfoProvided = isSubjectInfoProvided,
    isPriceApproved = isPriceApproved,
    isBankingDetailsProvided = isBankingDetailsProvided,
    isDocumentsApproved = isDocumentsApproved,
    confirmationCode = confirmationCode,
    isPassportValid = isPassportValid,
    userName = userName,
    isBankingEntityValid = isBankingEntityValid,
    phoneConfirmationCode = phoneConfirmationCode
  )

  implicit lazy val ProtoImageArb: Arbitrary[ru.yandex.vertis.palma.images.images.Image] = for {
    name <- alphaStr.arbitrary
    value <- alphaStr.arbitrary
    aliases = Map[String, String](name -> value)
  } yield ru.yandex.vertis.palma.images.images
    .Image(aliases = aliases)

  implicit lazy val EncryptedImageArb: Arbitrary[ru.yandex.vertis.palma.encrypted.content.Image] = for {
    key <- alphaStr.arbitrary
    dictionaryId <- alphaStr.arbitrary
  } yield ru.yandex.vertis.palma.encrypted.content
    .Image(key = key, dictionaryId = dictionaryId)

  implicit lazy val DocumentPhotoSizesArb: Arbitrary[DocumentPhotoSizes] = for {
    documentType <- DocumentTypeArb.arbitrary
    photo <- ProtoImageArb.arbitrary
    documentId <- DocumentIdArb.arbitrary
    documentName <- DocumentNameArb.arbitrary.?
  } yield DocumentPhotoSizes(
    documentType = documentType,
    photo = photo,
    documentId = documentId,
    documentName = documentName
  )

  implicit lazy val PartySubjectAutoruInfoArb: Arbitrary[PartyAction.PartySubjectAutoruInfo] = for {
    pts <- PtsCarInfoArb.arbitrary.?
    sts <- StsCarInfoArb.arbitrary.?
    carInfo <- OfferCarInfoArb.arbitrary.?
  } yield PartyAction.PartySubjectAutoruInfo(pts = pts, sts = sts, carInfo = carInfo)

  implicit lazy val PassportRfEntityArb: Arbitrary[Entity.PassportRfEntity] = // TODO generate
    for {
      departName <- cyrillicStr.arbitrary.map(_.taggedWith[Tag.DepartName])
      birthPlace <- alphaNumStr.arbitrary.map(_.taggedWith[Tag.BirthPlace])
      address <- alphaNumStr.arbitrary.map(_.taggedWith[Tag.Address])
    } yield Entity.PassportRfEntity(
      series = "4444".taggedWith[Tag.DocSeries],
      number = "666666".taggedWith[Tag.DocNumber],
      issueDate = "01.01.2020".taggedWith[Tag.StringDate],
      departCode = "100-100".taggedWith[Tag.DepartCode],
      departName = departName,
      birthPlace = birthPlace,
      birthDate = "01.01.2000".taggedWith[Tag.StringDate],
      address = address
    )

  implicit lazy val NameEntityArb: Arbitrary[Entity.NameEntity] = // TODO generate
    for {
      name <- NameArb.arbitrary
      surname <- SurnameArb.arbitrary
      patronymic <- PatronymicArb.arbitrary.?
    } yield Entity.NameEntity(name = name, surname = surname, patronymic = patronymic)

  implicit lazy val PhoneEntityArb: Arbitrary[Entity.PhoneEntity] = for {
    phone <- PhoneArb.arbitrary
    isConfirmed <- BooleanArb.arbitrary
  } yield Entity.PhoneEntity(phone = phone, isConfirmed = isConfirmed)

  implicit lazy val EmailEntityArb: Arbitrary[Entity.EmailEntity] = for {
    email <- EmailArb.arbitrary
  } yield Entity.EmailEntity(email = email)

  implicit lazy val BankingEntityArb: Arbitrary[Entity.BankingEntity] = Gen.const(
    Entity.BankingEntity(
      "044525225".taggedWith[Tag.Bic], // Сбер
      "40817810938160925982".taggedWith[Tag.AccountNumber],
      "Аркадий Аркадиев Аркадиевич".taggedWith[Tag.Name],
      Entity
        .AdditionalBankingDetails(
          "30101810400000000225".taggedWith[Tag.CorrAccountNumber],
          "АО ТИНЬКОФФ БАНК".taggedWith[Tag.BankName]
        )
        .some
    )
  )

  implicit lazy val DealCreateRequestAutoruSubjectValueByOfferArb: Arbitrary[DealCreateRequest.AutoruSubjectValueByOffer] =
    for {
      offer <- OfferArb.arbitrary
    } yield DealCreateRequest.AutoruSubjectValueByOffer(offer = offer)

  implicit lazy val DealCreateRequestAutoruSubjectValueByVinArb: Arbitrary[DealCreateRequest.AutoruSubjectValueByVin] =
    for {
      vin <- VinNumberArb.arbitrary
      mark <- alphaStr.arbitrary
      model <- alphaStr.arbitrary
    } yield DealCreateRequest.AutoruSubjectValueByVin(vin = vin, mark = mark, model = model)

  implicit lazy val DealCreateRequestAutoruSubjectValueArb: Arbitrary[DealCreateRequest.AutoruSubjectValue] =
    Gen.oneOf(
      DealCreateRequestAutoruSubjectValueByOfferArb.arbitrary,
      DealCreateRequestAutoruSubjectValueByVinArb.arbitrary
    )

  implicit lazy val DealCreateRequestAutoruSubjectArb: Arbitrary[DealCreateRequest.AutoruSubject] = for {
    value <- DealCreateRequestAutoruSubjectValueArb.arbitrary
  } yield DealCreateRequest.AutoruSubject(value = value)

  implicit lazy val DealCreateRequestSubjectArb: Arbitrary[DealCreateRequest.Subject] =
    DealCreateRequestAutoruSubjectArb.arbitrary

  implicit lazy val DealCreateRequestAutoruTargetValueByBuyerIdArb: Arbitrary[DealCreateRequest.TargetByBuyerId] =
    for {
      user <- UserArb.arbitrary
    } yield DealCreateRequest.TargetByBuyerId(buyerId = user)

  implicit lazy val DealCreateRequestArb: Arbitrary[DealCreateRequest] = for {
    subject <- DealCreateRequestSubjectArb.arbitrary.?
    state = DealState.DRAFT.some // TODO нет в протошке AUTORUBACK-2167
    geobaseId <- GeobaseIdArb.arbitrary.?
    sellerPhone <- PhoneArb.arbitrary.?
    target <- DealCreateRequestAutoruTargetValueByBuyerIdArb.arbitrary.?
    sellingPriceRub <- Gen.choose(1L, 10050000L).arbitrary
  } yield DealCreateRequest(
    subject = subject,
    state = state,
    geobaseId = geobaseId,
    sellerPhone = sellerPhone,
    target = target,
    sellingPriceRub = sellingPriceRub.some
  )

  implicit lazy val PartyProfileUpdateActionArb: Arbitrary[PartyAction.PartyProfileUpdateAction] = for {
    name <- NameEntityArb.arbitrary.?
    passportRf <- PassportRfEntityArb.arbitrary.?
    phone <- PhoneEntityArb.arbitrary.?
    email <- EmailEntityArb.arbitrary.?
  } yield PartyAction.PartyProfileUpdateAction(name = name, passportRf = passportRf, phone = phone, email = email)

  implicit lazy val PartySellingPriceUpdateActionArb: Arbitrary[PartyAction.PartySellingPriceUpdateAction] = for {
    sellingPriceRub <- Gen.choose(1L, 10050000L).arbitrary
  } yield PartyAction.PartySellingPriceUpdateAction(sellingPriceRub = sellingPriceRub)

  implicit lazy val PartyPersonalMeetingUpdateActionArb: Arbitrary[PartyAction.PartyPersonalMeetingUpdateAction] = for {
    meetingDate <- StringDateArb.arbitrary
    geobaseId <- GeobaseIdArb.arbitrary
  } yield PartyAction.PartyPersonalMeetingUpdateAction(meetingDate = meetingDate, geobaseId = geobaseId)

  implicit lazy val PartyPartySmsCodeRequestActionArb: Arbitrary[PartyAction.PartySmsCodeRequestAction] =
    Gen.const(PartyAction.PartySmsCodeRequestAction())

  implicit lazy val PartySmsCodeConfirmActionArb: Arbitrary[PartyAction.PartySmsCodeConfirmAction] = for {
    code <- alphaStr.arbitrary
  } yield PartyAction.PartySmsCodeConfirmAction(code = code)

  implicit lazy val PartyCancelDealActionArb: Arbitrary[PartyAction.PartyCancelDealAction] = for {
    cancelDescription <- alphaNumStr.arbitrary.?
    cancelReason <- CancellationReasonArb.arbitrary
  } yield PartyAction.PartyCancelDealAction(cancelReason = cancelReason, cancelDescription = cancelDescription)

  implicit lazy val PartyRestoreDealActionArb: Arbitrary[PartyAction.PartyRestoreDealAction] = for {
    createRequest <- DealCreateRequestSubjectArb.arbitrary.?
    sellingPriceRub <- Gen.choose(1L, 10050000L).arbitrary
  } yield PartyAction.PartyRestoreDealAction(
    createRequest = createRequest,
    requestSellingPriceRub = sellingPriceRub.some
  )

  implicit lazy val PartyNewDealApproveActionArb: Arbitrary[PartyAction.PartyNewDealApproveAction] = for {
    approve <- BooleanArb.arbitrary
  } yield PartyAction.PartyNewDealApproveAction(approve = approve)

  implicit lazy val PartySubjectInfoUpdateActionArb: Arbitrary[PartyAction.PartySubjectInfoUpdateAction] = for {
    info <- PartySubjectInfoArb.arbitrary
  } yield PartyAction.PartySubjectInfoUpdateAction(info = info)

  implicit lazy val PartySellingPriceApproveActionArb: Arbitrary[PartyAction.PartySellingPriceApproveAction] = for {
    sellingPriceRub <- Gen.choose(1L, 10050000L).arbitrary
  } yield PartyAction.PartySellingPriceApproveAction(sellingPriceRub)

  implicit lazy val PartyUploadPhotoActionArb: Arbitrary[PartyAction.PartyUploadPhotoAction] = for {
    documentType <- DocumentTypeArb.arbitrary
    file <- byteString.arbitrary
  } yield PartyAction.PartyUploadPhotoAction(documentType = documentType, file = file)

  implicit lazy val PartySellerApprovePhotosActionArb: Arbitrary[PartyAction.PartySellerApprovePhotosAction] =
    Gen.const(PartyAction.PartySellerApprovePhotosAction())

  implicit lazy val PartyBuyerApprovePhotoActionArb: Arbitrary[PartyAction.PartyBuyerApprovePhotoAction] = for {
    id <- DocumentIdArb.arbitrary
    name <- DocumentNameArb.arbitrary
  } yield PartyAction.PartyBuyerApprovePhotoAction(id = id, name = name)

  implicit lazy val PartyRemoveDocumentActionArb: Arbitrary[PartyAction.PartyRemoveDocumentAction] = for {
    documentType <- DocumentTypeArb.arbitrary
    documentId <- DocumentIdArb.arbitrary.?
  } yield PartyAction.PartyRemoveDocumentAction(documentType = documentType, documentId = documentId)

  implicit lazy val PartySellerBankAccountUpdateActionArb: Arbitrary[PartyAction.PartySellerBankAccountUpdateAction] =
    for {
      bankingEntity <- BankingEntityArb.arbitrary
    } yield PartyAction.PartySellerBankAccountUpdateAction(bankingEntity = bankingEntity)

  implicit lazy val PartyBuyerAddMoneyTestUpdateActionArb: Arbitrary[PartyAction.PartyBuyerAddMoneyTestUpdateAction] =
    for {
      account <- BankingEntityArb.arbitrary
      amount <- Gen.choose(100, 1000).arbitrary
    } yield PartyAction.PartyBuyerAddMoneyTestUpdateAction(account = account, amount = amount)

  implicit lazy val PartySubjectInfoArb: Arbitrary[PartyAction.PartySubjectInfo] = PartySubjectAutoruInfoArb.arbitrary

  private lazy val DealUpdateRequestPartyActionBuyerArb: Arbitrary[PartyAction.ByBuyer] = Gen.oneOf(
    PartyProfileUpdateActionArb.arbitrary,
    PartySellingPriceUpdateActionArb.arbitrary,
    PartyPersonalMeetingUpdateActionArb.arbitrary,
    PartyPartySmsCodeRequestActionArb.arbitrary,
    PartySmsCodeConfirmActionArb.arbitrary,
    PartyBuyerApprovePhotoActionArb.arbitrary,
    PartyRemoveDocumentActionArb.arbitrary,
    PartyBuyerAddMoneyTestUpdateActionArb.arbitrary,
    PartyUploadPhotoActionArb.arbitrary
  )

  private lazy val DealUpdateRequestPartyActionSellerArb: Arbitrary[PartyAction.BySeller] = Gen.oneOf(
    PartyProfileUpdateActionArb.arbitrary,
    PartySellingPriceUpdateActionArb.arbitrary,
    PartyPartySmsCodeRequestActionArb.arbitrary,
    PartySmsCodeConfirmActionArb.arbitrary,
    PartyCancelDealActionArb.arbitrary,
    PartyRestoreDealActionArb.arbitrary,
    PartyNewDealApproveActionArb.arbitrary,
    PartySubjectInfoUpdateActionArb.arbitrary,
    PartySellingPriceApproveActionArb.arbitrary,
    PartySellerApprovePhotosActionArb.arbitrary,
    PartySellerBankAccountUpdateActionArb.arbitrary
  )

  implicit lazy val AutoruPayloadPartyByBuyerArb: Arbitrary[DealUpdateRequest.AutoruPayloadPartyByBuyer] = for {
    action <- DealUpdateRequestPartyActionBuyerArb.arbitrary
  } yield DealUpdateRequest.AutoruPayloadPartyByBuyer(action)

  implicit lazy val AutoruPayloadPartyBySellerArb: Arbitrary[DealUpdateRequest.AutoruPayloadPartyBySeller] = for {
    action <- DealUpdateRequestPartyActionSellerArb.arbitrary
  } yield DealUpdateRequest.AutoruPayloadPartyBySeller(action)

  implicit lazy val DealUpdateRequestAutoruPayloadPartyArb: Arbitrary[DealUpdateRequest.AutoruPayloadParty] =
    Gen.oneOf(AutoruPayloadPartyByBuyerArb.arbitrary, AutoruPayloadPartyBySellerArb.arbitrary)

  implicit lazy val DealUpdateRequestAutoruPayloadArb: Arbitrary[DealUpdateRequest.AutoruPayload] = for {
    party <- DealUpdateRequestAutoruPayloadPartyArb.arbitrary
  } yield DealUpdateRequest.AutoruPayload(party)

  implicit lazy val DealUpdateRequestPayloadArb: Arbitrary[DealUpdateRequest.Payload] =
    DealUpdateRequestAutoruPayloadArb.arbitrary

  implicit lazy val DealFlagsArb: Arbitrary[Deal.DealFlags] = for {
    isFeeTransferred <- BooleanArb.arbitrary
    isMoneyTransferred <- BooleanArb.arbitrary
    isEnrichedWithOtherDealsInfo <- BooleanArb.arbitrary
    isLocked <- BooleanArb.arbitrary
    isDealInitiatedEmailSent <- BooleanArb.arbitrary
    isDealAcceptedEmailSent <- BooleanArb.arbitrary
  } yield Deal.DealFlags(
    isFeeTransferred = isFeeTransferred,
    isMoneyTransferred = isMoneyTransferred,
    isEnrichedWithOtherDealsInfo = isEnrichedWithOtherDealsInfo,
    isLocked,
    isDealInitiatedEmailSent,
    isDealAcceptedEmailSent
  )

  implicit lazy val DealEventRequestTypeArb: Arbitrary[proto.DealEvent.RequestType] =
    Gen.oneOf(proto.DealEvent.RequestType.values.filterNot(_.isUnknownRequestType))

  implicit lazy val DealEventArb: Arbitrary[DealEvent] = for {
    timestamp <- InstantArb.arbitrary
    requestId <- alphaStr.arbitrary.?
    requestType <- DealEventRequestTypeArb.arbitrary
    current <- DealArb.arbitrary
    previous <- DealArb.arbitrary.?
    userId <- UserIdArb.arbitrary.?
    platform <- PlatformArb.arbitrary.?
    operator <- YandexUidArb.arbitrary.?
  } yield DealEvent(
    timestamp = timestamp,
    requestId = requestId,
    requestType = requestType,
    current = current,
    previous = previous,
    userId = userId,
    platform = platform,
    operator = operator
  )

  implicit lazy val AutoruDealArb: Arbitrary[AutoruDeal] = for {
    id <- DealIdArb.arbitrary
    dealNumber <- DealNumberArb.arbitrary
    updateInfo <- DealUpdateInfoArb.arbitrary
    scheduledAt <- InstantArb.arbitrary.?
    schedulerLastUpdate <- InstantArb.arbitrary.?
    state <- DealStateArb.arbitrary
    dealStep <- DealStepArb.arbitrary
    sellerStep <- SellerStepArb.arbitrary
    buyerStep <- BuyerStepArb.arbitrary
    cancelInfo <- DealCancelInfoArb.arbitrary.?
    meetingInfo <- DealMeetingInfoArb.arbitrary
    priceInfo <- DealPriceInfoArb.arbitrary
    totalProvidedRub <- TotalProvidedRubArb.arbitrary
    safeDealAccount = None // <- SafeDealAccountArb.arbitrary.?
    escrows <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, EscrowArb.arbitrary))
    buyer <- BuyerArb.arbitrary
    seller <- SellerArb.arbitrary.?
    notifications <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, NotificationArb.arbitrary))
    subject <- AutoruDealSubjectArb.arbitrary
    documents = Seq() // <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, DocumentPhotoSizesArb.arbitrary))
    flags <- DealFlagsArb.arbitrary
    isRollback = false // <- BooleanArb.arbitrary
    lastOverdueNotification <- InstantArb.arbitrary.?
    payments <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, PaymentArb.arbitrary))
    previousDealNumbers <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, DealNumberArb.arbitrary))
    chatRoomId <- ChatRoomIdArb.arbitrary.?
    tinkoffDeal <- arbitrary[Option[TinkoffDeal]]
  } yield AutoruDeal(
    id = id,
    dealNumber = dealNumber,
    updateInfo = updateInfo,
    scheduledAt = scheduledAt,
    schedulerLastUpdate = schedulerLastUpdate,
    dealBankType = DefaultDealBankType,
    state = state,
    dealStep = dealStep,
    sellerStep = sellerStep,
    buyerStep = buyerStep,
    cancelInfo = cancelInfo,
    meetingInfo = meetingInfo,
    priceInfo = priceInfo,
    totalProvidedRub = totalProvidedRub,
    safeDealAccount = safeDealAccount,
    escrows = escrows,
    buyer = buyer,
    seller = seller,
    notifications = notifications,
    subject = subject,
    documents = documents,
    flags = flags,
    isRollback = isRollback,
    lastOverdueNotification = lastOverdueNotification,
    payments = payments,
    previousDealNumbers = previousDealNumbers,
    chatRoomId = chatRoomId,
    tinkoffDeal = tinkoffDeal
  )

  implicit lazy val DealArb: Arbitrary[Deal] = for {
    autoruDealArbitrary <- AutoruDealArb.arbitrary
  } yield autoruDealArbitrary

  implicit lazy val DealUpdateRequestArb: Arbitrary[DealUpdateRequest] = for {
    payload <- DealUpdateRequestPayloadArb.arbitrary
  } yield DealUpdateRequest(payload)

  implicit lazy val DealPartyConfirmationCodeArb: Arbitrary[DealParty.ConfirmationCode] = for {
    requestedTime <- LocalTimeArb.arbitrary
    code <- alphaNumStr.arbitrary
    isConfirmed <- BooleanArb.arbitrary
  } yield DealParty.ConfirmationCode(
    requestedTime = requestedTime,
    code = code,
    isConfirmed = isConfirmed
  )

  implicit lazy val CommissionTariffArb: Arbitrary[CommissionTariff] = for {
    id <- CommissionTariffIdArb.arbitrary
    domain <- DomainArb.arbitrary
    enabled <- BooleanArb.arbitrary
    rangePrice <- MoneyRubArb.arbitrary
    ranges = List(CommissionTariff.RangeItem(None, None, rangePrice, rangePrice.some))
  } yield CommissionTariff(id = id, domain = domain, enabled = enabled, ranges = ranges)

  implicit lazy val CommissionTariffResponseArb: Arbitrary[CommissionTariffResponse] = for {
    result <- ApiResultArb.arbitrary
    commissionsTariffs <- Gen.choose(1, 100).flatMap(Gen.listOfN(_, CommissionTariffArb.arbitrary))
  } yield CommissionTariffResponse(result = result, commissionsTariffs = commissionsTariffs)

  implicit lazy val CurrentCommissionTariffResponseArb: Arbitrary[CurrentCommissionTariffResponse] = for {
    result <- ApiResultArb.arbitrary
    commissionTariff <- CommissionTariffArb.arbitrary.?
  } yield CurrentCommissionTariffResponse(result = result, commissionTariff = commissionTariff)

  implicit lazy val ApiServerErrorArbitrary: Arbitrary[Api.Result.ServerError] = for {
    str <- alphaStr.arbitrary
  } yield Api.Result.ServerError(str)

  implicit lazy val ApiNotSuitableCreationArbitrary: Arbitrary[Api.Result.NotSuitableCreation] = for {
    str <- alphaStr.arbitrary
  } yield Api.Result.NotSuitableCreation(str)

  implicit lazy val ApiBadRequestErrorArbitrary: Arbitrary[Api.Result.BadRequestError] = for {
    str <- alphaStr.arbitrary
  } yield Api.Result.BadRequestError(str)

  implicit lazy val ApiNotSuitableStatusArbitrary: Arbitrary[Api.Result.NotSuitableStatus] = for {
    str <- alphaStr.arbitrary
    step <- DealStepArb.arbitrary
  } yield Api.Result.NotSuitableStatus(str, step)

  implicit lazy val ApiFieldErrorsEntityArbitrary: Arbitrary[Api.Result.FieldErrors.Entity] = for {
    name <- alphaStr.arbitrary
    message <- alphaStr.arbitrary
  } yield Api.Result.FieldErrors.Entity(name, message)

  implicit lazy val ApiFieldErrorsArbitrary: Arbitrary[Api.Result.FieldErrors] = for {
    entities <- Gen.choose(1, 20).flatMap(Gen.listOfN(_, ApiFieldErrorsEntityArbitrary.arbitrary))
  } yield Api.Result.FieldErrors(entities)

  implicit lazy val ProtoConfirmationCodeErrorReasonArbitrary: Arbitrary[proto.Api.Result.Error.ConfirmationCodeErrorReason] =
    Gen.oneOf(proto.Api.Result.Error.ConfirmationCodeErrorReason.values.filterNot(_.isCodeReasonUnknown))

  implicit lazy val ApiConfirmationErrorArbitrary: Arbitrary[Api.Result.ConfirmationError] = for {
    message <- alphaStr.arbitrary
    timeout <- Gen.choose(1, 100).arbitrary.?
    reason <- ProtoConfirmationCodeErrorReasonArbitrary.arbitrary
  } yield Api.Result.ConfirmationError(message, timeout, reason)

  implicit lazy val ApiResultArb: Arbitrary[Api.Result] =
    Gen.frequency(
      (7, Gen.const(Api.Result.Ok)),
      (1, Gen.const(Api.Result.NotFound)),
      (1, ApiServerErrorArbitrary.arbitrary),
      (1, ApiNotSuitableCreationArbitrary.arbitrary),
      (1, ApiBadRequestErrorArbitrary.arbitrary),
      (1, ApiNotSuitableStatusArbitrary.arbitrary),
      (1, ApiFieldErrorsArbitrary.arbitrary),
      (1, ApiConfirmationErrorArbitrary.arbitrary)
    )

  implicit lazy val ApiResponseStatusArb: Arbitrary[proto.Api.ResponseStatus] =
    Gen.oneOf(proto.Api.ResponseStatus.values.filterNot(_.isUnknownResponseStatus))

  implicit lazy val ApiErrorCodeArb: Arbitrary[proto.Api.ErrorCode] =
    Gen.oneOf(proto.Api.ErrorCode.values.filterNot(_.isUnknownErrorCode))

  implicit lazy val RawDealsResponseArb: Arbitrary[Api.RawDealsResponse] = for {
    res <- ApiResultArb.arbitrary
    deals <- Gen.listOf(DealArb.arbitrary)
    size = deals.size.toLong
  } yield Api.RawDealsResponse(res, deals, size.some)

  implicit lazy val DealViewSellerInfoArb: Arbitrary[DealView.SellerInfo] = for {
    name <- NameEntityArb.arbitrary.?
    passportRf <- PassportRfEntityArb.arbitrary.?
    phone <- PhoneEntityArb.arbitrary.?
    banking <- Gen.choose(0, 3).flatMap(Gen.listOfN(_, BankingEntityArb.arbitrary))
    userName <- alphaNumStr.arbitrary.?
  } yield DealView.SellerInfo(
    name = name,
    passportRf = passportRf,
    phone = phone,
    banking = banking,
    userName = userName
  )

  implicit lazy val DealViewBuyerInfoArb: Arbitrary[DealView.BuyerInfo] = for {
    name <- NameEntityArb.arbitrary.?
    passportRf <- PassportRfEntityArb.arbitrary.?
    phone <- PhoneEntityArb.arbitrary.?
    banking <- Gen.choose(0, 3).flatMap(Gen.listOfN(_, BankingEntityArb.arbitrary))
    userName <- alphaNumStr.arbitrary.?
  } yield DealView.BuyerInfo(
    name = name,
    passportRf = passportRf,
    phone = phone,
    banking = banking,
    userName = userName
  )

  implicit lazy val DealViewPartyBuyerArb: Arbitrary[DealView.DealViewBuyerParty] = for {
    personProfile <- PersonProfileArb.arbitrary.?
    sellerInfo <- DealViewSellerInfoArb.arbitrary.?
    safeDealAccount <- SafeDealAccountArb.arbitrary.?
    totalProvidedRub <- TotalProvidedRubArb.arbitrary
  } yield DealView.DealViewBuyerParty(
    personProfile = personProfile,
    sellerInfo = sellerInfo,
    safeDealAccount = safeDealAccount,
    totalProvidedRub = totalProvidedRub
  )

  implicit lazy val DealViewPartySellerArb: Arbitrary[DealView.DealViewSellerParty] = for {
    personProfile <- PersonProfileArb.arbitrary.?
    buyerInfo <- DealViewBuyerInfoArb.arbitrary.?
  } yield DealView.DealViewSellerParty(personProfile = personProfile, buyerInfo = buyerInfo)

  implicit lazy val DealViewPartyArb: Arbitrary[DealView.DealViewParty] = Gen.oneOf(
    DealViewPartyBuyerArb.arbitrary,
    DealViewPartySellerArb.arbitrary
  )

  implicit lazy val AutoruDealViewArb: Arbitrary[AutoruDealView] = for {
    id <- DealIdArb.arbitrary
    dealNumber <- DealNumberArb.arbitrary
    updateInfo <- DealUpdateInfoArb.arbitrary
    state <- DealStateArb.arbitrary
    dealStep <- DealStepArb.arbitrary
    sellerStep <- SellerStepArb.arbitrary
    buyerStep <- BuyerStepArb.arbitrary
    cancelInfo <- DealCancelInfoArb.arbitrary.?
    meetingInfo <- DealMeetingInfoArb.arbitrary
    sellerId <- UserArb.arbitrary.?
    buyerId <- UserArb.arbitrary
    price <- DealPriceInfoArb.arbitrary
    participantType <- ParticipantTypeArb.arbitrary
    party <- DealViewPartyArb.arbitrary.?
    subject <- AutoruDealSubjectArb.arbitrary
    documents <- Gen.choose(0, 3).flatMap(Gen.listOfN(_, DocumentPhotoSizesArb.arbitrary))
    isRollback <- BooleanArb.arbitrary
    isLocked = false
  } yield AutoruDealView(
    id = id,
    dealNumber = dealNumber,
    updateInfo = updateInfo,
    state = state,
    dealStep = dealStep,
    sellerStep = sellerStep,
    buyerStep = buyerStep,
    cancelInfo = cancelInfo,
    meetingInfo = meetingInfo,
    sellerId = sellerId,
    buyerId = buyerId,
    price = price,
    participantType = participantType,
    party = party,
    subject = subject,
    documents = documents,
    isRollback = isRollback,
    isLocked = isLocked
  )

  implicit lazy val DealViewArb: Arbitrary[DealView] = AutoruDealViewArb.arbitrary

  implicit lazy val DealsResponseArb: Arbitrary[Api.DealsResponse] = for {
    res <- ApiResultArb.arbitrary
    deals <- Gen.listOf(DealViewArb.arbitrary)
    size = deals.size.toLong
  } yield Api.DealsResponse(res, deals, size.some)

  implicit lazy val DefaultResponseArb: Arbitrary[Api.DefaultResponse] = for {
    result <- ApiResultArb.arbitrary
    status <- ApiResponseStatusArb.arbitrary
    errorCode <- ApiErrorCodeArb.arbitrary
  } yield Api.DefaultResponse(result = result, status = status, errorCode = errorCode)

  implicit lazy val OperationArb: Arbitrary[Operation] =
    for {
      id <- alphaStr.arbitrary
      opId <- arbitrary[BankOperationId]
      amount <- arbitrary[Amount]
      date <- arbitrary[LocalDate]
      drawDate <- arbitrary[LocalDate]
      paymentPurpose <- arbitrary[PaymentPurpose]
      uin <- arbitrary[Option[Uin]]
      payerBic <- arbitrary[Bic]
      payerBank <- arbitrary[BankName]
      payerName <- arbitrary[Name]
      payerAccount <- arbitrary[Option[AccountNumber]]
      payerCorrAccount <- arbitrary[Option[CorrAccountNumber]]
    } yield Operation(
      id,
      opId,
      amount,
      date,
      drawDate,
      paymentPurpose,
      uin,
      payerBic,
      payerBank,
      payerName,
      payerAccount,
      payerCorrAccount
    )

  implicit lazy val PaymentArb: Arbitrary[Payment] =
    for {
      id <- alphaStr.arbitrary
      returnId <- alphaStr.arbitrary.?
      kind <- arbitrary[PaymentType]
      needCheck <- arbitrary[Boolean]
      status <- arbitrary[PaymentStatus]
    } yield Payment(id, returnId, kind, needCheck, status)

  implicit lazy val PaymentTypeArb: Arbitrary[PaymentType] =
    oneOf(PaymentType.values.filterNot(_.isUnknownPaymentType))

  implicit lazy val ChatRoomIdArb: Arbitrary[zio_baker.ChatRoomId] =
    alphaStr.arbitrary.tagged

  implicit lazy val PaymentStatusArb: Arbitrary[PaymentStatus] =
    oneOf(PaymentStatus.values.filterNot(_.isUnknownResponseStatus))
}
