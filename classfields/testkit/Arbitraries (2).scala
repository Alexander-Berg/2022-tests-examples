package ru.yandex.vertis.shark.model

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging._
import org.scalacheck.Gen.{alphaChar, alphaLowerChar, alphaNumChar, choose, frequency, numChar, oneOf}
import org.scalacheck.{Arbitrary, Gen}
import ru.auto.api.api_offer_model.{Category, Section, SellerType}
import ru.auto.api.common_model.PhotoClass
import ru.yandex.vertis.common.{Domain, Platform}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.{FiasId, GeobaseId, Inn, KladrId, OfferId, UserId}
import ru.yandex.vertis.shark.controller._
import ru.yandex.vertis.shark.model.Block.{ExpensesBlock, IncomeBlock, PhonesBlock}
import ru.yandex.vertis.shark.model.Entity.{DriverLicenseEntity, InsuranceNumberEntity, PhoneEntity}
import ru.yandex.vertis.shark.model.event._
import ru.yandex.vertis.shark.proto.model.Entity.PhoneEntity.PhoneType
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.app

import java.net.URL
import java.time.{Instant, LocalDate, ZoneOffset}
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
  }

  /** Generates russian upper-case alpha character */
  def cyrillicUpperChar: Gen[Char] = frequency((32, choose(1040.toChar, 1071.toChar)), (1, 1025.toChar))

  /** Generates russian lower-case alpha character */
  def cyrillicLowerChar: Gen[Char] = frequency((32, choose(1072.toChar, 1103.toChar)), (1, 1105.toChar))

  /** Generates russian  alpha character */
  def cyrillicChar: Gen[Char] = frequency((1, cyrillicUpperChar), (9, cyrillicLowerChar))

  def cyrillicNumChar: Gen[Char] = frequency((1, numChar), (9, cyrillicChar))

  def alphaNumLowerChar: Gen[Char] = frequency((1, numChar), (9, alphaLowerChar))

  def generateSeq[T](n: Int, filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): Seq[T] =
    Iterator
      .continually(arb.arbitrary.suchThat(filter).sample)
      .flatten
      .take(n)
      .toSeq

  def generate[T](filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): T =
    generateSeq[T](1, filter).head

  def generateId(ranges: Seq[Int], gen: Gen[Char], separator: String): Arbitrary[String] = {
    val gs = ranges.map(makeStr(_, gen).arbitrary)
    val str = gs
      .fold(Gen.const("")) { (prev, next) =>
        Gen.sequence[Seq[String], String](Seq(prev, next)).map(_.mkString(separator))
      }
      .map(_.drop(1))
    Arbitrary(str)
  }

  val alphaNumStr: Arbitrary[String] = makeStr(alphaNumChar)

  val alphaStr: Arbitrary[String] = makeStr(alphaChar)

  val cyrillicNumStr: Arbitrary[String] = makeStr(cyrillicNumChar)

  val cyrillicStr: Arbitrary[String] = makeStr(cyrillicChar)

  val shortCyrillicNumStr: Arbitrary[String] = makeStr(1, 5, cyrillicNumChar)

  private def makeStr(min: Int, max: Int, gen: Gen[Char]): Arbitrary[String] =
    for {
      n <- Gen.chooseNum(min, max)
      str <- Gen.listOfN(n, gen).map(_.mkString)
    } yield str

  private def makeStr(length: Int, gen: Gen[Char]): Arbitrary[String] = makeStr(length, length, gen)

  private def makeStr(gen: Gen[Char]): Arbitrary[String] = makeStr(3, 15, gen)

  implicit lazy val InstantArb: Arbitrary[Instant] = {
    val maxDelta = 500.days.toSeconds
    Gen.chooseNum(-maxDelta, maxDelta).map(Instant.now().minusSeconds(2 * maxDelta).plusSeconds)
  }

  val dlSecondPartGenAlpha: Gen[String] = Gen.listOfN(2, oneOf("авекмнорстухabekmhopctyx")).map(_.mkString)
  val dlSecondPartGenNumber: Gen[String] = Gen.listOfN(2, numChar).map(_.mkString)
  val dlSecondPartGen: Arbitrary[String] = frequency((1, dlSecondPartGenAlpha), (1, dlSecondPartGenNumber))

  implicit lazy val LocalDateArb: Arbitrary[LocalDate] = InstantArb.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalDate)

  implicit lazy val CreditProductIdArb: Arbitrary[CreditProductId] = alphaNumStr.tagged[Tag.CreditProductId]
  implicit lazy val BankIdArb: Arbitrary[BankId] = alphaNumStr.tagged[Tag.BankId]
  implicit lazy val CreditApplicationIdArb: Arbitrary[CreditApplicationId] = alphaNumStr.tagged[Tag.CreditApplicationId]

  implicit lazy val CreditApplicationClaimIdArb: Arbitrary[CreditApplicationClaimId] =
    alphaNumStr.tagged[Tag.CreditApplicationClaimId]

  implicit lazy val CreditApplicationBankClaimIdArb: Arbitrary[CreditApplicationBankClaimId] =
    alphaNumStr.tagged[Tag.CreditApplicationBankClaimId]
  implicit lazy val PersonProfileIdArb: Arbitrary[PersonProfileId] = alphaNumStr.tagged[Tag.PersonProfileId]
  implicit lazy val NotificationIdArb: Arbitrary[NotificationId] = alphaNumStr.tagged[Tag.NotificationId]
  implicit lazy val MoneyRubArb: Arbitrary[MoneyRub] = Gen.choose(1L, 100000000L).tagged[Tag.MoneyRub]
  implicit lazy val MonthAmountArb: Arbitrary[MonthAmount] = Gen.choose(1, 365).tagged[Tag.MonthAmount]
  implicit lazy val YearsAmountArb: Arbitrary[YearAmount] = Gen.choose(1, 99).tagged[Tag.YearAmount]
  implicit lazy val RateArb: Arbitrary[Rate] = Gen.choose(0f, 100f).tagged[Tag.Rate]
  implicit lazy val RequestRateArb: Arbitrary[RequestRate] = Gen.choose(0, 10000).tagged[Tag.RequestRate]

  implicit lazy val GeobaseIdArb: Arbitrary[GeobaseId] =
    Gen.choose(0, Long.MaxValue).tagged[zio_baker.Tag.GeobaseId]
  implicit lazy val UserIdArb: Arbitrary[UserId] = alphaNumStr.tagged[zio_baker.Tag.UserId]
  implicit lazy val RusPostCodeArb: Arbitrary[RusPostCode] = Gen.choose(1, 999999).tagged[Tag.RusPostCode]
  implicit lazy val OkvedArb: Arbitrary[Okved] = alphaNumStr.tagged[Tag.Okved]
  implicit lazy val OfferIdArb: Arbitrary[OfferId] = alphaNumStr.tagged[zio_baker.Tag.OfferId]
  implicit lazy val NameArb: Arbitrary[Name] = cyrillicStr.tagged[Tag.Name]
  implicit lazy val SurnameArb: Arbitrary[Surname] = cyrillicStr.tagged[Tag.Surname]
  implicit lazy val PatronymicArb: Arbitrary[Patronymic] = cyrillicStr.tagged[Tag.Patronymic]
  implicit lazy val DocSeriesArb: Arbitrary[DocSeries] = Gen.listOfN(4, numChar).map(_.mkString).tagged[Tag.DocSeries]
  implicit lazy val DocNumberArb: Arbitrary[DocNumber] = Gen.listOfN(6, numChar).map(_.mkString).tagged[Tag.DocNumber]
  implicit lazy val DepartNameArb: Arbitrary[DepartName] = cyrillicNumStr.tagged[Tag.DepartName]
  implicit lazy val VehicleMarkArb: Arbitrary[VehicleMark] = alphaStr.tagged[Tag.VehicleMark]
  implicit lazy val KladrIdArb: Arbitrary[KladrId] = alphaStr.tagged[zio_baker.Tag.KladrId]
  implicit lazy val FiasCodeArb: Arbitrary[FiasCode] = alphaStr.tagged[Tag.FiasCode]
  implicit lazy val YandexUidArb: Arbitrary[app.context.YandexUid] = alphaStr.tagged[app.context.Tag.YandexUid]
  implicit lazy val DeviceTypeArb: Arbitrary[app.context.DeviceType] = alphaStr.tagged[app.context.Tag.DeviceType]
  implicit lazy val ClientFeatureArb: Arbitrary[ClientFeature] = alphaStr.tagged[Tag.ClientFeature]
  implicit lazy val StepArb: Arbitrary[Step] = Gen.choose(0, 100).tagged[Tag.Step]
  implicit lazy val PriorityArb: Arbitrary[Priority] = Gen.choose(0, 3).tagged[Tag.Priority]
  implicit lazy val AffiliateUserIdArb: Arbitrary[AffiliateUserId] = alphaNumStr.tagged[Tag.AffiliateUserId]
  implicit lazy val UserTagArb: Arbitrary[UserTag] = alphaNumStr.tagged[Tag.UserTag]
  implicit lazy val DaysArb: Arbitrary[Days] = Gen.choose(1, 1825).tagged[Tag.Days]
  implicit lazy val HashStringArb: Arbitrary[HashString] = alphaNumStr.tagged[Tag.HashString]
  implicit lazy val PalmaKeyArb: Arbitrary[PalmaKey] = alphaNumStr.tagged[Tag.PalmaKey]
  implicit lazy val IdempotencyKeyArb: Arbitrary[IdempotencyKey] = alphaNumStr.tagged[Tag.IdempotencyKey]

  implicit lazy val FiasIdArb: Arbitrary[FiasId] =
    generateId(Seq(8, 4, 4, 4, 12), numChar, "-").tagged[zio_baker.Tag.FiasId]

  implicit lazy val HttpResponseCodeAtb: Arbitrary[HttpResponseCode] =
    Gen.oneOf(200, 404, 500).tagged[Tag.HttpResponseCode]

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  implicit lazy val InnArb: Arbitrary[Inn] =
    for {
      n <- Gen.oneOf(10, 12)
      str <- Gen.listOfN(n, numChar).map(_.mkString)
      tail = Inn
        .innControlSum(str)
        // We should get `Some` if str has correct length.
        .getOrElse(throw new IllegalStateException("tail must not be None"))
      res = Inn(str.dropRight(tail.length) + tail)
    } yield res

  implicit lazy val UrlArb: Arbitrary[URL] = for {
    path <- alphaStr.arbitrary
    host = "domain"
    domain = "com"
    schema = "http://"
  } yield new URL(s"$schema$host.$domain/$path")

  implicit lazy val DomainArb: Arbitrary[Domain] =
    Gen.oneOf(Domain.values.filter(CreditProduct.SupportedDomains.contains))

  implicit lazy val BankTypeArb: Arbitrary[proto.Bank.BankType] =
    Gen.oneOf(proto.Bank.BankType.values.filterNot(_.isUnknownBankType))

  implicit lazy val IdempotencyTypeArb: Arbitrary[proto.CreditProduct.IdempotencyType] =
    Gen.oneOf(proto.CreditProduct.IdempotencyType.values)

  implicit lazy val BlockTypeArb: Arbitrary[proto.Block.BlockType] =
    Gen.oneOf(proto.Block.BlockType.values.filterNot(_.isUnknownBlockType))

  implicit lazy val VisitForeignCountriesFrequencyArb: Arbitrary[proto.Block.ForeignPassportBlock.Yes.VisitForeignCountriesFrequency] =
    Gen.oneOf(
      proto.Block.ForeignPassportBlock.Yes.VisitForeignCountriesFrequency.values.filterNot(_.isFrequencyUnknown)
    )

  implicit lazy val EducationStateArb: Arbitrary[proto.Block.EducationBlock.State] =
    Gen.oneOf(proto.Block.EducationBlock.State.values.filterNot(_.isUnknownState))

  implicit lazy val MaritalStatusArb: Arbitrary[proto.Block.MaritalStatusBlock.State] =
    Gen.oneOf(proto.Block.MaritalStatusBlock.State.values.filterNot(_.isUnknownState))

  implicit lazy val IncomeProofArb: Arbitrary[proto.Block.IncomeBlock.IncomeProof] =
    Gen.oneOf(proto.Block.IncomeBlock.IncomeProof.values.filterNot(_.isUnknownIncomeProof))

  implicit lazy val NotEmployedReasonArb: Arbitrary[proto.Block.EmploymentBlock.NotEmployed.Reason] =
    Gen.oneOf(proto.Block.EmploymentBlock.NotEmployed.Reason.values.filterNot(_.isUnknownReason))

  implicit lazy val EmployeePositionTypeArb: Arbitrary[proto.Block.EmploymentBlock.Employed.Employee.PositionType] =
    Gen.oneOf(proto.Block.EmploymentBlock.Employed.Employee.PositionType.values.filterNot(_.isUnknownPositionType))

  implicit lazy val RelatedPersonTypeArb: Arbitrary[proto.Block.RelatedPersonsBlock.RelatedPerson.RelatedPersonType] =
    Gen.oneOf(proto.Block.RelatedPersonsBlock.RelatedPerson.RelatedPersonType.values.filterNot(_.isUnknownType))

  implicit lazy val GenderTypeArb: Arbitrary[proto.Block.GenderBlock.GenderType] =
    Gen.oneOf(proto.Block.GenderBlock.GenderType.values.filterNot(_.isUnknownGenderType))

  implicit lazy val NotificationTypeArb: Arbitrary[proto.Notification.NotificationType] =
    Gen.oneOf(proto.Notification.NotificationType.values)

  implicit lazy val NotificationStateArb: Arbitrary[proto.Notification.State] =
    Gen.oneOf(proto.Notification.State.values)

  implicit lazy val CategoryArb: Arbitrary[Category] = Gen.oneOf(Category.values.filterNot(_.isCategoryUnknown))

  implicit lazy val SectionArb: Arbitrary[Section] = Gen.oneOf(Section.values.filterNot(_.isSectionUnknown))

  implicit lazy val SellerTypeArb: Arbitrary[SellerType] = Gen.oneOf(SellerType.values)

  implicit lazy val ObjectStateArb: Arbitrary[proto.CreditApplication.Claim.ClaimPayload.ObjectState] =
    Gen.oneOf(proto.CreditApplication.Claim.ClaimPayload.ObjectState.values.filterNot(_.isUnknownObjectState))

  implicit lazy val ClaimStateArb: Arbitrary[proto.CreditApplication.Claim.ClaimState] =
    Gen.oneOf(proto.CreditApplication.Claim.ClaimState.values.filterNot(_.isUnknownClaimState))

  implicit lazy val CreditApplicationStateArb: Arbitrary[proto.CreditApplication.State] =
    Gen.oneOf(proto.CreditApplication.State.values.filterNot(_.isUnknownState))

  implicit lazy val BrandCountryArb: Arbitrary[proto.Block.VehicleOwnershipBlock.Vehicles.Vehicle.BrandCountry] =
    Gen.oneOf(proto.Block.VehicleOwnershipBlock.Vehicles.Vehicle.BrandCountry.values.filterNot(_.isUnknownBrandCountry))

  implicit lazy val CreditApplicationEventRequestTypeArb: Arbitrary[proto.CreditApplicationEvent.RequestType] =
    Gen.oneOf(proto.CreditApplicationEvent.RequestType.values.filterNot(_.isUnknownRequestType))

  implicit lazy val CreditApplicationClaimEventRequestTypeArb: Arbitrary[proto.CreditApplicationClaimEvent.RequestType] =
    Gen.oneOf(proto.CreditApplicationClaimEvent.RequestType.values.filterNot(_.isUnknownRequestType))

  implicit lazy val PhoneTypeArb: Arbitrary[proto.Entity.PhoneEntity.PhoneType] =
    Gen.oneOf(proto.Entity.PhoneEntity.PhoneType.values.filterNot(_.isUnknownPhoneType))

  implicit lazy val PlatformArb: Arbitrary[Platform] =
    Gen.oneOf(Platform.values.filterNot(_.isPlatformUnknown))

  implicit lazy val CreditProductBorrowerConditionsEmploymentTypeArb: Arbitrary[proto.CreditProduct.BorrowerConditions.EmploymentType] =
    Gen.oneOf(proto.CreditProduct.BorrowerConditions.EmploymentType.values.filterNot(_.isEmploymentTypeUnknown))

  implicit lazy val AutoruExternalCompletenessStateArb: Arbitrary[proto.CreditApplication.Communication.AutoruExternal.CompletenessState] =
    Gen.oneOf(
      proto.CreditApplication.Communication.AutoruExternal.CompletenessState.values
        .filterNot(_.isUnknownCompletenessState)
    )

  implicit lazy val AutoruExternalObjectCommunicationStateStateArb: Arbitrary[proto.CreditApplication.Communication.AutoruExternal.ObjectCommunicationState] =
    Gen.oneOf(
      proto.CreditApplication.Communication.AutoruExternal.ObjectCommunicationState.values
        .filterNot(_.isUnknownObjectCommunicationState)
    )

  implicit lazy val AutoruExternalClaimCommunicationStateStateArb: Arbitrary[proto.CreditApplication.Communication.AutoruExternal.ClaimCommunicationState] =
    Gen.oneOf(
      proto.CreditApplication.Communication.AutoruExternal.ClaimCommunicationState.values
        .filterNot(_.isUnknownClaimCommunicationState)
    )

  implicit lazy val SubscriptionChannelArb: Arbitrary[proto.Subscription.Channel] =
    Gen.oneOf(proto.Subscription.Channel.values.filterNot(_.isUnknownChannel))

  implicit lazy val DictionaryUpdateTypeArb: Arbitrary[proto.DictionaryUpdate.UpdateType] =
    Gen.oneOf(proto.DictionaryUpdate.UpdateType.values.filterNot(_.isUnknownUpdateType))

  implicit lazy val PhotoClassArb: Arbitrary[PhotoClass] =
    Gen.oneOf(PhotoClass.values.filterNot(_.isPhotoClassUndefined))

  implicit lazy val EcreditCalculationCreditSubtypeArb: Arbitrary[proto.Api.EcreditCalculationRequest.CreditSubtype] =
    Gen.oneOf(proto.Api.EcreditCalculationRequest.CreditSubtype.values.filterNot(_.isUnknownCreditSubtype))

  implicit lazy val EcreditCalculationGosSubsidyTypeArb: Arbitrary[proto.Api.EcreditCalculationRequest.GosSubsidyType] =
    Gen.oneOf(proto.Api.EcreditCalculationRequest.GosSubsidyType.values.filterNot(_.isUnknownGosSubtype))

  implicit lazy val EcreditCalculationIncomeProofArb: Arbitrary[proto.Api.EcreditCalculationRequest.IncomeProof] =
    Gen.oneOf(proto.Api.EcreditCalculationRequest.IncomeProof.values.filterNot(_.isUnknownIncomeProof))

  implicit lazy val BlockEntityArb: Arbitrary[proto.Block.BlockEntity] =
    Gen.lzy(proto.Block.BlockEntity().withName(proto.Block.NameBlock()))

  implicit lazy val AmountRangeArb: Arbitrary[CreditProduct.AmountRange] = for {
    from <- MoneyRubArb.arbitrary.filter(_ > 0L).?
    add <- MoneyRubArb.arbitrary.filter(_ > 0L).?
    to = add.map(a => (a + from.getOrElse(0L)).taggedWith[Tag.MoneyRub])
  } yield CreditProduct.AmountRange(from, to)

  implicit lazy val InterestRateRangeArb: Arbitrary[CreditProduct.InterestRateRange] = for {
    from <- RateArb.arbitrary.filter(_ > 0L).?
    add <- RateArb.arbitrary.filter(_ > 0L).?
    to = add.map(r => (r + from.getOrElse(0f)).taggedWith[Tag.Rate])
  } yield CreditProduct.InterestRateRange(from, to)

  implicit lazy val TermMonthsRangeArb: Arbitrary[CreditProduct.TermMonthsRange] = for {
    from <- MonthAmountArb.arbitrary.?
    add <- MonthAmountArb.arbitrary.?
    to = add.map(m => (from.getOrElse(0) + m).taggedWith[Tag.MonthAmount])
  } yield CreditProduct.TermMonthsRange(from, to)

  implicit lazy val BankAbr: Arbitrary[Bank] = for {
    bank <- gen[Bank].arbitrary
    colorInt <- Gen.choose(100000, 16777215).? // Int value from 100000 t0 ffffff
    colorHex = colorInt.map(_.toHexString)
  } yield bank.copy(colorHex = colorHex)

  implicit lazy val CreditProductBorrowerConditionsAgeArb: Arbitrary[CreditProduct.BorrowerConditions.AgeRange] = for {
    from <- YearsAmountArb.arbitrary
    add <- YearsAmountArb.arbitrary
    to = (from + add).taggedWith[Tag.YearAmount]
  } yield CreditProduct.BorrowerConditions.AgeRange(from.some, to.some)

  implicit lazy val CreditProductBorrowerConditionsScoreArb: Arbitrary[CreditProduct.BorrowerConditions.Score] = for {
    paymentSegment <- Gen.choose(1, 10).?
    approvalSegment <- Gen.choose(1, 10).?
    approvalAutoSegment <- Gen.choose(1, 10).?
    bnplSegment <- Gen.choose(1, 10).?
    minYandexProductScore <- Gen.choose(0f, 1f).?
    scoreRequired <- generate[Boolean]
  } yield CreditProduct.BorrowerConditions.Score(
    paymentSegment,
    approvalSegment,
    approvalAutoSegment,
    bnplSegment,
    minYandexProductScore,
    scoreRequired
  )

  implicit lazy val PhoneArb: Arbitrary[Phone] = for {
    phone <- Gen.listOfN(7, numChar).map(_.mkString)
  } yield Phone(s"7965$phone")

  implicit lazy val PhoneEntityArb: Arbitrary[PhoneEntity] = for {
    phone <- generate[Phone]
    phoneType <- generate[PhoneType]
  } yield PhoneEntity(phone, phoneType.some)

  implicit lazy val PhonesBlockArb: Arbitrary[PhonesBlock] = for {
    amountOfPhones <- Gen.choose(2, 5)
    personal <- Gen.listOfN(amountOfPhones, PhoneEntityArb.arbitrary.map(_.copy(phoneType = PhoneType.PERSONAL.some)))
    work <- Gen.listOfN(amountOfPhones, PhoneEntityArb.arbitrary.map(_.copy(phoneType = PhoneType.WORK.some)))
  } yield PhonesBlock(personal ++ work)

  implicit lazy val EmailArb: Arbitrary[Email] = for {
    firstChar <- alphaChar.arbitrary
    name <- alphaNumStr.arbitrary
    domainName <- alphaNumStr.arbitrary
    zone <- alphaStr.arbitrary
  } yield Email(s"$firstChar$name@$domainName.$zone")

  implicit lazy val FiasArb: Arbitrary[Entity.AddressEntity.Fias] = for {
    regionId <- FiasIdArb.arbitrary
    areaId <- FiasIdArb.arbitrary
    cityId <- FiasIdArb.arbitrary
    cityDistrictId <- FiasIdArb.arbitrary
    settlementId <- FiasIdArb.arbitrary
    streetId <- FiasIdArb.arbitrary
    houseId <- FiasIdArb.arbitrary
    id <- FiasIdArb.arbitrary
    code <- FiasCodeArb.arbitrary
    level <- FiasCodeArb.arbitrary
    actualityState <- FiasCodeArb.arbitrary
  } yield Entity.AddressEntity.Fias(
    regionId.some,
    areaId.some,
    cityId.some,
    cityDistrictId.some,
    settlementId.some,
    streetId.some,
    houseId.some,
    id.some,
    code.some,
    level.some,
    actualityState.some
  )

  implicit lazy val AddressEntityArb: Arbitrary[Entity.AddressEntity] = for {
    region <- cyrillicStr.arbitrary
    randomCity <- cyrillicStr.arbitrary.?
    fallbackCity <- cyrillicStr.arbitrary
    settlement <- cyrillicStr.arbitrary.?
    city = if (randomCity.isEmpty && settlement.isEmpty) fallbackCity.some else randomCity
    district <- cyrillicStr.arbitrary.?
    street <- cyrillicStr.arbitrary.?
    building <- shortCyrillicNumStr.arbitrary.?
    corpus <- shortCyrillicNumStr.arbitrary.?
    construction <- shortCyrillicNumStr.arbitrary.?
    flat <- shortCyrillicNumStr.arbitrary.?
    postCode <- RusPostCodeArb.arbitrary
    kladr <- gen[Entity.AddressEntity.Kladr].arbitrary
    fias <- FiasArb.arbitrary
  } yield Entity.AddressEntity(
    region,
    city,
    settlement,
    district,
    street,
    building,
    corpus,
    construction,
    flat,
    postCode,
    kladr,
    fias
  )

  implicit lazy val BirthDateBlockArb: Arbitrary[Block.BirthDateBlock] = {
    val maxDelta = 500.days.toSeconds
    val age = 365.days.toSeconds * 30
    Gen
      .chooseNum(-maxDelta, maxDelta)
      .map(Instant.now().minusSeconds(age).plusSeconds)
      .arbitrary
      .map(_.atZone(ZoneOffset.UTC).toLocalDate)
      .map(Block.BirthDateBlock.apply)
  }

  implicit lazy val BirthPlaceBlockArb: Arbitrary[Block.BirthPlaceBlock] = for {
    country <- cyrillicStr.arbitrary
    city <- cyrillicStr.arbitrary
  } yield Block.BirthPlaceBlock(country, city)

  implicit lazy val DependentsBlockArb: Arbitrary[Block.DependentsBlock] = for {
    amountOfChildren <- Gen.choose(0, 10)
  } yield Block.DependentsBlock(amountOfChildren)

  implicit lazy val NotEmployedEmploymentBlockArb: Arbitrary[Block.NotEmployedEmploymentBlock] = for {
    reason <- NotEmployedReasonArb.arbitrary
    otherReason <- if (reason.isUnknownReason) cyrillicStr.arbitrary.map(_.some) else cyrillicStr.arbitrary.?
  } yield Block.NotEmployedEmploymentBlock(reason, otherReason)

  implicit lazy val EmployeeEmploymentBlockArb: Arbitrary[Block.EmployeeEmploymentBlock] = for {
    orgName <- cyrillicStr.arbitrary
    inn <- InnArb.arbitrary
    headCount <- Gen.choose(1, 100)
    okvedsCount <- Gen.choose(1, 3)
    okveds <- Gen.listOfN(okvedsCount, OkvedArb.arbitrary)
    phonesCount <- Gen.choose(1, 3)
    phones <- Gen.listOfN(phonesCount, PhoneArb.arbitrary)
    addressEntity <- AddressEntityArb.arbitrary
    positionType <- EmployeePositionTypeArb.arbitrary
    lastExperienceMonths <- MonthAmountArb.arbitrary
  } yield Block.EmployeeEmploymentBlock(
    orgName,
    inn,
    headCount,
    okveds,
    addressEntity,
    phones,
    positionType,
    lastExperienceMonths
  )

  implicit lazy val SelfEmployedEmploymentBlockArb: Arbitrary[Block.SelfEmployedEmploymentBlock] = for {
    orgName <- alphaStr.arbitrary
    inn <- InnArb.arbitrary.?
    headCount <- Gen.choose(1, 100)
    okvedsNum <- Gen.choose(0, 3)
    okveds <- Gen.listOfN(okvedsNum, OkvedArb.arbitrary)
    addressEntity <- AddressEntityArb.arbitrary
  } yield Block.SelfEmployedEmploymentBlock(orgName, inn, headCount, okveds, addressEntity)

  implicit lazy val ControlWordBlockArb: Arbitrary[Block.ControlWordBlock] = for {
    wordLength <- Gen.choose(4, 10)
    word <- Gen.listOfN(wordLength, numChar).map(_.mkString)
  } yield Block.ControlWordBlock(word)

  implicit lazy val YandexScoreArb: Arbitrary[YandexScore] = for {
    payment <- Gen.choose(1, 10)
    approval <- Gen.choose(1, 10)
    approvalAuto <- Gen.choose(1, 10)
    bnpl <- Gen.choose(1, 10)

    timestamp <- InstantArb.arbitrary
    sourceHash <- HashStringArb.arbitrary.?
    paymentWeight <- Gen.oneOf(1_000_000d, 9_999_999d).arbitrary.?
    approvalWeight <- Gen.oneOf(1_000_000d, 9_999_999d).arbitrary.?
    approvalAutoWeight <- Gen.oneOf(1_000_000d, 9_999_999d).arbitrary.?
    bnplWeight <- Gen.oneOf(1_000_000d, 9_999_999d).arbitrary.?
    hasScore <- generate[Boolean]
    result =
      if (hasScore) {
        YandexScore(
          paymentSegment = payment.some,
          approvalSegment = approval.some,
          approvalAutoSegment = approvalAuto.some,
          bnplSegment = bnpl.some,
          paymentWeight = paymentWeight,
          approvalWeight = approvalWeight,
          approvalAutoWeight = approvalAutoWeight,
          bnplWeight = bnplWeight,
          timestamp = timestamp,
          sourceHash = sourceHash
        )
      } else {
        YandexScore(
          paymentSegment = None,
          approvalSegment = None,
          approvalAutoSegment = None,
          bnplSegment = None,
          paymentWeight = None,
          approvalWeight = None,
          approvalAutoWeight = None,
          bnplWeight = None,
          timestamp = timestamp,
          sourceHash = sourceHash
        )
      }
  } yield result

  implicit lazy val ProductEntityOfYandexProductScoreArb: Arbitrary[YandexProductScore.ProductEntity] = for {
    productEntity <- gen[YandexProductScore.ProductEntity].arbitrary
    score <- Gen.choose(0f, 1f)
  } yield productEntity.copy(score = score)

  implicit lazy val ApiCreditApplicationByBankClaimRequestArb: Arbitrary[Api.CreditApplicationByBankClaimRequest] =
    for {
      bankClaimId <- CreditApplicationBankClaimIdArb.arbitrary
      creditProductId <- CreditProductIdArb.arbitrary.?
    } yield Api.CreditApplicationByBankClaimRequest(bankClaimId, creditProductId)

  implicit lazy val ApiCreditApplicationsResponseArb: Arbitrary[Api.CreditApplicationsResponse] = for {
    result <- gen[Api.Result].arbitrary
    num <- Gen.choose(0, 3)
    creditApplications <- Gen.listOfN(num, gen[CreditApplication].arbitrary)
    totalEntities <- Gen.choose(1L, 100L).?
  } yield Api.CreditApplicationsResponse(result, creditApplications, totalEntities)

  implicit lazy val ApiBanksResponseArb: Arbitrary[Api.BanksResponse] =
    for {
      result <- gen[Api.Result].arbitrary
      num <- Gen.choose(0, 3)
      banks <- Gen.listOfN(num, BankAbr.arbitrary)
    } yield Api.BanksResponse(result, banks)

  implicit lazy val DepartCodeArb: Arbitrary[DepartCode] = for {
    first <- Gen.listOfN(3, numChar).map(_.mkString)
    last <- Gen.listOfN(3, numChar).map(_.mkString)
  } yield "%s-%s".format(first, last).taggedWith[Tag.DepartCode]

  implicit lazy val SnilsNumberArb: Arbitrary[SnilsNumber] = for {
    snils <- Gen.listOfN(9, numChar).map(_.mkString)
    cs = InsuranceNumberEntity.controlSum(snils)
  } yield "%s%02d".format(snils, cs).taggedWith[Tag.SnilsNumber]

  implicit lazy val DriveEntityNumberArb: Arbitrary[DriverLicenceNumber] = for {
    first <- Gen.listOfN(2, numChar).map(_.mkString)
    second <- dlSecondPartGen.arbitrary
    third <- Gen.listOfN(6, numChar).map(_.mkString)
  } yield "%s%s%s".format(first, second, third).taggedWith[Tag.DriverLicenceNumber]

  implicit lazy val ExpensesBlockArb: Arbitrary[ExpensesBlock] = for {
    first <- Gen.choose(1L, 9999999L)
  } yield ExpensesBlock(first.taggedWith[Tag.MoneyRub])

  implicit lazy val IncomeBlockArb: Arbitrary[IncomeBlock] = for {
    first <- Gen.choose(1L, 9999999L)
    second <- generate[proto.Block.IncomeBlock.IncomeProof]
  } yield IncomeBlock(first.taggedWith[Tag.MoneyRub], second)

  implicit lazy val DriverLicenseEntityArb: Arbitrary[DriverLicenseEntity] = for {
    generated <- gen[DriverLicenseEntity].arbitrary
    issuerName <- alphaStr.arbitrary.?
  } yield generated.copy(issuerName = issuerName)

  implicit lazy val ClaimBankPayloadArb: Arbitrary[CreditApplication.Claim.BankPayload] =
    gen[CreditApplication.Claim.BankPayload.Vtb].arbitrary

  implicit lazy val AutoruClaimArb: Arbitrary[CreditApplication.AutoruClaim] = for {
    generated <- gen[CreditApplication.AutoruClaim].arbitrary
    bankState <- alphaStr.arbitrary.?
  } yield generated.copy(bankState = bankState)

  implicit lazy val CreditApplicationClaimSourceArb: Arbitrary[CreditApplicationClaimSource] = for {
    generated <- gen[CreditApplicationClaimSource].arbitrary
    bankState <- alphaStr.arbitrary.?
  } yield generated.copy(bankState = bankState)

  implicit lazy val PersonProfileImplArb: Arbitrary[PersonProfileImpl] = for {
    personProfile <- gen[PersonProfileImpl].arbitrary
    nameBlock <- gen[Block.NameBlock].arbitrary
    relatedPersons = personProfile.relatedPersons.filter(_.relatedPersons.nonEmpty)
  } yield personProfile.copy(name = nameBlock.some, relatedPersons = relatedPersons)

  implicit lazy val PersonProfileArb: Arbitrary[PersonProfile] =
    PersonProfileImplArb.arbitrary.map(_.asInstanceOf[PersonProfile])

  implicit lazy val CreditApplicationClaimEventArb: Arbitrary[CreditApplicationClaimEvent] = for {
    event <- gen[CreditApplicationClaimEvent].arbitrary
    requestId <- alphaStr.arbitrary.?
    objectId <- alphaStr.arbitrary.?
    bankState <- alphaStr.arbitrary.?
    name <- alphaStr.arbitrary.?
  } yield event.copy(requestId = requestId, objectId = objectId, bankState = bankState, name = name)

  implicit lazy val CreditApplicationClaimSentEventArb: Arbitrary[CreditApplicationClaimSentEvent] = for {
    event <- gen[CreditApplicationClaimSentEvent].arbitrary
    requestId <- alphaStr.arbitrary.?
    objectId <- alphaStr.arbitrary.?
    requestLabel <- alphaStr.arbitrary
    requestData <- alphaStr.arbitrary.?
    responseData <- alphaStr.arbitrary.?
  } yield event.copy(
    requestId = requestId,
    objectId = objectId,
    requestLabel = requestLabel,
    requestData = requestData,
    responseData = responseData
  )

  implicit lazy val CreditApplicationEventArb: Arbitrary[CreditApplicationEvent] = for {
    event <- gen[CreditApplicationEvent].arbitrary
    requestId <- alphaStr.arbitrary.?
  } yield event.copy(requestId = requestId)

  implicit lazy val CreditApplicationAutoruExternalCommunicationEventArb: Arbitrary[CreditApplicationAutoruExternalCommunicationEvent] =
    for {
      event <- gen[CreditApplicationAutoruExternalCommunicationEvent].arbitrary
      requestId <- alphaStr.arbitrary.?
    } yield event.copy(requestId = requestId)

  implicit lazy val CreditApplicationAutoruExternalCommunicationEventObjectPayloadPhotoArb: Arbitrary[CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload.Photo] =
    for {
      sizesNum <- Gen.choose(1, 3)
      sizes <- Gen.listOfN(sizesNum, gen[(String, URL)].arbitrary)
      photoClass <- PhotoClassArb.arbitrary
    } yield CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload.Photo(sizes.toMap, photoClass)

  implicit lazy val CreditApplicationAutoruExternalCommunicationEventObjectPayloadArb: Arbitrary[CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload] =
    for {
      offerUrl <- UrlArb.arbitrary
      priceRub <- MoneyRubArb.arbitrary.filter(_ > 0L)
      mark <- alphaStr.arbitrary
      model <- alphaStr.arbitrary
      photosNum <- Gen.choose(1, 3)
      photos <- Gen.listOfN(photosNum, CreditApplicationAutoruExternalCommunicationEventObjectPayloadPhotoArb.arbitrary)
    } yield CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload(offerUrl, priceRub, mark, model, photos)

  implicit lazy val PlatformUrlArb: Arbitrary[PushNotification.PayloadContent.PlatformUrl] = for {
    web <- alphaStr.arbitrary.?
    android <- alphaStr.arbitrary.?
    iOs <- alphaStr.arbitrary.?
  } yield PushNotification.PayloadContent.PlatformUrl(web, android, iOs)

  implicit lazy val PushNotificationPayloadContentArb: Arbitrary[PushNotification.PayloadContent] = for {
    title <- cyrillicStr.arbitrary
    body <- cyrillicStr.arbitrary
  } yield PushNotification.PayloadContent(
    title = title,
    body = body,
    action = None
  )

  implicit lazy val PushNotificationContentArb: Arbitrary[PushNotification.Content] = for {
    payloadContent <- PushNotificationPayloadContentArb.arbitrary
  } yield payloadContent

  implicit lazy val PushNotificationArb: Arbitrary[PushNotification] = for {
    id <- NotificationIdArb.arbitrary
    created <- InstantArb.arbitrary
    updated <- InstantArb.arbitrary
    content <- PushNotificationContentArb.arbitrary
    idempotencyKey <- alphaStr.arbitrary.?
  } yield PushNotification(
    id = id,
    created = created,
    updated = updated,
    state = proto.Notification.State.NEW,
    content = content,
    idempotencyKey = idempotencyKey
  )

  implicit lazy val NotificationArb: Arbitrary[Notification] = for {
    pushNotification <- PushNotificationArb.arbitrary
  } yield pushNotification

  implicit lazy val PushNotificationSourceArb: Arbitrary[PushNotificationSource] = for {
    content <- PushNotificationContentArb.arbitrary
    idempotencyKey <- alphaStr.arbitrary.?
  } yield PushNotificationSource(content = content, idempotencyKey = idempotencyKey)

  implicit lazy val NotificationSourceArb: Arbitrary[NotificationSource] = for {
    pushNotificationSource <- PushNotificationSourceArb.arbitrary
  } yield pushNotificationSource

  implicit lazy val CreditApplicationCallCenterEventArb: Arbitrary[CreditApplicationCallCenterEvent] = for {
    event <- gen[CreditApplicationCallCenterEvent].arbitrary
    requestId <- alphaStr.arbitrary.?
    name <- alphaStr.arbitrary.?
  } yield event.copy(requestId = requestId, name = name)

  implicit lazy val CheckBorrowerAbr: Arbitrary[CheckBorrower] = for {
    checkBorrower <- gen[CheckBorrower].arbitrary
  } yield checkBorrower.copy(allExperienceMonths = true)

  implicit lazy val AutoSellerTypeArb: Arbitrary[proto.AutoSellerType] =
    Gen.oneOf(proto.AutoSellerType.values.filterNot(_.isSellerTypeUnknown))

  implicit lazy val MissingBlockArb: Arbitrary[MissingBlocks] = for {
    required <- Gen.listOf(BlockTypeArb.arbitrary)
    allowed <- Gen.listOf(BlockTypeArb.arbitrary)
  } yield MissingBlocks(required, allowed)

  implicit lazy val SuitableArb: Arbitrary[Suitable] = for {
    creditProductId <- CreditProductIdArb.arbitrary
    checkRequirements = CheckRequirements.Passed
    checkBorrower = CheckBorrower.Passed
    info <- MissingBlockArb.arbitrary
    borrowerPersonProfile <- MissingBlockArb.arbitrary
    checkRateLimit = CheckRateLimit.Passed
    checkObject = Seq(CheckObject.Auto(true))
  } yield Suitable(
    creditProductId,
    passed = true,
    checkRequirements,
    checkBorrower,
    info,
    borrowerPersonProfile,
    checkRateLimit,
    checkObject
  )

  implicit lazy val AddProductsResponseItemArb: Arbitrary[Api.AddProductsResponse.Item] = for {
    creditProductId <- CreditProductIdArb.arbitrary
    suitable <- SuitableArb.arbitrary
    value = Api.AddProductsResponse.Item.NotSuitable(suitable)
  } yield Api.AddProductsResponse.Item(creditProductId, value)

  implicit lazy val PriorityTagArb: Arbitrary[proto.CreditProduct.PriorityTag] =
    Gen.oneOf(proto.CreditProduct.PriorityTag.values.filterNot(_.isUnknownPriorityTag))

  implicit lazy val AutoruCreditApplicationArb: Arbitrary[AutoruCreditApplication] = gen[AutoruCreditApplication]
  implicit lazy val PersonProfileStubArb: Arbitrary[PersonProfileStub] = gen[PersonProfileStub]

  implicit lazy val DictionaryUpdateArb: Arbitrary[DictionaryUpdate] = for {
    generated <- gen[DictionaryUpdate].arbitrary
    comment <- alphaStr.arbitrary.?
  } yield generated.copy(comment = comment)

  implicit lazy val FiasGeobaseEntityArb: Arbitrary[FiasGeobaseEntity] = for {
    generated <- gen[FiasGeobaseEntity].arbitrary
    fiasLevel <- alphaStr.arbitrary.?
  } yield generated.copy(fiasLevel = fiasLevel)

  implicit lazy val AutoruUserRefArb: Arbitrary[UserRef.AutoruUser] = for {
    uid <- Gen.choose(1000000000L, 9999999999L)
  } yield UserRef.AutoruUser(uid)

  implicit lazy val AutoruDealerRefArb: Arbitrary[UserRef.AutoruDealer] = for {
    uid <- Gen.choose(1000000000L, 9999999999L)
  } yield UserRef.AutoruDealer(uid)

  implicit lazy val UserRefArb: Arbitrary[UserRef] =
    Gen.oneOf(AutoruUserRefArb.arbitrary, AutoruDealerRefArb.arbitrary)

  implicit lazy val ProductViewPayloadTagArb: Arbitrary[proto.Api.ProductViewPayload.ProductViewTag] =
    Gen.oneOf(proto.Api.ProductViewPayload.ProductViewTag.values.filterNot(_.isUnknownViewTag))

  implicit lazy val EcreditCalculationRequestArb: Arbitrary[Api.EcreditCalculationRequest] = for {
    dealerRef <- AutoruDealerRefArb.arbitrary
    carName <- alphaStr.arbitrary
    used <- generate[Boolean]
    carAge <- Gen.choose(1, Int.MaxValue).arbitrary.?
    price <- Gen.choose(1L, Long.MaxValue).arbitrary
    period <- Gen.listOfN(1, Gen.choose(1, Int.MaxValue))
    initialFeeMoney <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    initialFee <- Gen.choose(1f, Float.MaxValue).arbitrary.?
    buybackPayment <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    creditSubtype <- Gen.listOfN(1, EcreditCalculationCreditSubtypeArb.arbitrary)
    creditSubtypeHide <- Gen.listOfN(1, EcreditCalculationCreditSubtypeArb.arbitrary)
    bankCreditsClients <- generate[Boolean].?
    gosSubsideType <- EcreditCalculationGosSubsidyTypeArb.arbitrary
    dohodConfirmType <- EcreditCalculationIncomeProofArb.arbitrary
    kaskoInsurancePrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    kaskoInsuranceTerm <- Gen.choose(1, Int.MaxValue).arbitrary.?
    kaskoInsuranceInCredit <- generate[Boolean].?
    requiredKasko <- generate[Boolean].?
    lifeInsurancePrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    lifeInsuranceTerm <- Gen.choose(1, Int.MaxValue).arbitrary.?
    lifeInsuranceInCredit <- generate[Boolean].?
    requiredLife <- generate[Boolean].?
    gapInsurancePrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    gapInsuranceTerm <- Gen.choose(1, Int.MaxValue).arbitrary.?
    gapInsuranceInCredit <- generate[Boolean].?
    requiredGap <- generate[Boolean].?
    otherPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    otherPriceInCredit <- generate[Boolean].?
    equipmentPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    equipmentPriceInCredit <- generate[Boolean].?
  } yield Api.EcreditCalculationRequest(
    dealerRef = dealerRef,
    carName = carName,
    used = used,
    carAge = carAge,
    price = price,
    period = period,
    initialFeeMoney = initialFeeMoney,
    initialFee = initialFee,
    buybackPayment = buybackPayment,
    creditSubtype = creditSubtype,
    creditSubtypeHide = creditSubtypeHide,
    bankCreditsClients = bankCreditsClients,
    gosSubsideType = gosSubsideType,
    dohodConfirmType = dohodConfirmType,
    kaskoInsurancePrice = kaskoInsurancePrice,
    kaskoInsuranceTerm = kaskoInsuranceTerm,
    kaskoInsuranceInCredit = kaskoInsuranceInCredit,
    requiredKasko = requiredKasko,
    lifeInsurancePrice = lifeInsurancePrice,
    lifeInsuranceTerm = lifeInsuranceTerm,
    lifeInsuranceInCredit = lifeInsuranceInCredit,
    requiredLife = requiredLife,
    gapInsurancePrice = gapInsurancePrice,
    gapInsuranceTerm = gapInsuranceTerm,
    gapInsuranceInCredit = gapInsuranceInCredit,
    requiredGap = requiredGap,
    otherPrice = otherPrice,
    otherPriceInCredit = otherPriceInCredit,
    equipmentPrice = equipmentPrice,
    equipmentPriceInCredit = equipmentPriceInCredit
  )

  implicit lazy val EcreditCalculationResponseItemArb: Arbitrary[Api.EcreditCalculationResponse.Item] = for {
    id <- alphaStr.arbitrary
    title <- alphaStr.arbitrary
    bankTitle <- alphaStr.arbitrary
    interestRate <- Gen.choose(1f, Float.MaxValue).arbitrary.?
    marketingInterestRate <- Gen.choose(1f, Float.MaxValue).arbitrary.?
    interestRatePriviliage <- Gen.choose(1f, Float.MaxValue).arbitrary.?
    periodPriviliage <- Gen.choose(1, Int.MaxValue).arbitrary.?
    buybackPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    carDiscount <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    cashBack <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    payment <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    payment2 <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    creditAmmount <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    paymentAll <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    overpayment <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    kaskoPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    lifePrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    gapPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    bankDopsPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    otherPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    equipmentPrice <- Gen.choose(1L, Long.MaxValue).arbitrary.?
    periodMin <- Gen.choose(1, Int.MaxValue).arbitrary.?
    periodMax <- Gen.choose(1, Int.MaxValue).arbitrary.?
    buybackPaymentMin <- Gen.choose(1, Int.MaxValue).arbitrary.?
    buybackPaymentMax <- Gen.choose(1, Int.MaxValue).arbitrary.?
    requiredKasko <- generate[Boolean].?
    requiredLife <- generate[Boolean].?
    requiredGap <- generate[Boolean].?
    legalIssue <- alphaStr.arbitrary.?
    bankLogoUrl = None
  } yield Api.EcreditCalculationResponse.Item(
    id = id,
    title = title,
    bankTitle = bankTitle,
    interestRate = interestRate,
    marketingInterestRate = marketingInterestRate,
    interestRatePriviliage = interestRatePriviliage,
    periodPriviliage = periodPriviliage,
    buybackPrice = buybackPrice,
    carDiscount = carDiscount,
    cashBack = cashBack,
    payment = payment,
    payment2 = payment2,
    creditAmmount = creditAmmount,
    paymentAll = paymentAll,
    overpayment = overpayment,
    kaskoPrice = kaskoPrice,
    lifePrice = lifePrice,
    gapPrice = gapPrice,
    bankDopsPrice = bankDopsPrice,
    otherPrice = otherPrice,
    equipmentPrice = equipmentPrice,
    periodMin = periodMin,
    periodMax = periodMax,
    buybackPaymentMin = buybackPaymentMin,
    buybackPaymentMax = buybackPaymentMax,
    requiredKasko = requiredKasko,
    requiredLife = requiredLife,
    requiredGap = requiredGap,
    legalIssue = legalIssue,
    bankLogoUrl = bankLogoUrl
  )

  implicit lazy val EcreditCalculationResponseArb: Arbitrary[Api.EcreditCalculationResponse] =
    Gen
      .listOfN(1, EcreditCalculationResponseItemArb.arbitrary)
      .map(items => Api.EcreditCalculationResponse(items = items))
}
