package ru.auto.salesman.test.model

import cats.data.NonEmptySet
import org.joda.time.LocalDate
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.cabinet.ApiModel.{PoiProperties, PoiProperty}
import ru.auto.salesman.environment.now
import ru.auto.salesman.model
import ru.auto.salesman.model.OfferStatuses.OfferStatus
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model._
import ru.auto.salesman.model.autostrategies.AutostrategyStatus._
import ru.auto.salesman.model.autostrategies.AutostrategyTypes.{
  AlwaysAtFirstPage,
  AutostrategyType
}
import ru.auto.salesman.model.autostrategies._
import ru.auto.salesman.model.offer.{AutoruOfferId, OfferIdentity}
import ru.auto.salesman.util.Collections._
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model._
import ru.yandex.vertis.generators.BasicGenerators._
import ru.yandex.vertis.generators.DateTimeGenerators._
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.generators.ProtobufGenerators.protoEnum
import ru.yandex.vertis.generators.{BasicGenerators, DateTimeGenerators}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.DurationInt
import scala.util.Random

package object gens {

  val quotaEntitiesGen: Gen[QuotaEntities.Value] =
    Gen.oneOf(QuotaEntities.values.toSeq)

  val ClientIdGen: Gen[ClientId] = Gen.choose(1, 10000)

  val UserIdGen: Gen[ClientId] = Gen.choose(1, 10000)

  val EpochGen: Gen[Epoch] = BasicGenerators.posNum[Epoch]

  val OptEpochGen: Gen[Option[Epoch]] = Gen.option(EpochGen)

  val EpochInPastGen: Gen[Epoch] =
    DateTimeGenerators.dateTimeInPast().map(_.getMillis)

  val OptEpochInPastGen: Gen[Option[Epoch]] = Gen.option(EpochInPastGen)

  val GoodsNameGen: Gen[ProductId] =
    // there is no all_sale_add product for dealers actually. only for users
    Gen.oneOf(ProductId.values.filterNot(_ == ProductId.Add).toSeq)

  val GoodsStatusGen: Gen[GoodStatus] = for {
    status <- Gen.oneOf(GoodStatuses.Active, GoodStatuses.Inactive)
  } yield status

  val OfferCategoryGen: Gen[OfferCategory] = for {
    category <- Gen.oneOf(OfferCategories.values.toSeq)
  } yield category

  val ProtoCategoryGen: Gen[Category] = protoEnum(Category.values())

  val CommercialOfferCategoryGen: Gen[OfferCategory] =
    Gen.oneOf(Category.TRUCKS.flatChildrenWithParent.toList)

  val OfferSectionGen: Gen[Section] = protoEnum(Section.values())

  val OfferStatusGen: Gen[OfferStatus] = for {
    status <- Gen.oneOf(OfferStatuses.values.toSeq)
  } yield status

  val OfferCurrencyGen: Gen[OfferCurrency] = for {
    currency <- Gen.oneOf(OfferCurrencies.values.toSeq)
  } yield currency

  // old prefix to avoid clash with OfferModelGenerators
  def oldOfferGen(
      offerIdGen: Gen[model.OfferId] = Gen.posNum[model.OfferId],
      offerHashGen: Gen[OfferHash] = readableString,
      categoryIdGen: Gen[OfferCategory] = OfferCategoryGen,
      sectionIdGen: Gen[Section] = OfferSectionGen,
      statusGen: Gen[OfferStatus] = OfferStatusGen,
      clientIdGen: Gen[ClientId] = Gen.posNum[ClientId]
  ): Gen[Offer] =
    for {
      offerId <- offerIdGen
      offerHash <- offerHashGen
      categoryId <- categoryIdGen
      sectionId <- sectionIdGen
      price <- Gen.posNum[Long]
      currency <- OfferCurrencyGen
      status <- statusGen
      clientId <- clientIdGen
      createDate = DateTimeUtil.now
      expireDate = createDate.plusDays(1)
      setDate = createDate.plusDays(1)
      freshDate = Some(createDate.plusDays(1))
    } yield
      Offer(
        offerId,
        offerHash,
        categoryId,
        sectionId,
        BigDecimal(price),
        currency,
        status,
        clientId,
        createDate,
        expireDate,
        setDate,
        freshDate
      )

  val OfferGen: Gen[Offer] = oldOfferGen()

  def carsNewActiveOfferGen(
      offerIdGen: Gen[model.OfferId] = Gen.posNum[model.OfferId],
      offerHashGen: Gen[OfferHash] = readableString,
      clientIdGen: Gen[ClientId] = Gen.posNum[ClientId]
  ): Gen[Offer] =
    oldOfferGen(
      offerIdGen = offerIdGen,
      offerHashGen = offerHashGen,
      categoryIdGen = OfferCategories.Cars,
      sectionIdGen = Section.NEW,
      statusGen = OfferStatuses.Show,
      clientIdGen = clientIdGen
    )

  def carsNewInactiveOfferGen(
      offerIdGen: Gen[model.OfferId] = Gen.posNum[model.OfferId],
      offerHashGen: Gen[OfferHash] = readableString,
      clientIdGen: Gen[ClientId] = Gen.posNum[ClientId]
  ): Gen[Offer] =
    oldOfferGen(
      offerIdGen = offerIdGen,
      offerHashGen = offerHashGen,
      categoryIdGen = OfferCategories.Cars,
      sectionIdGen = Section.NEW,
      statusGen = OfferStatuses.Hidden,
      clientIdGen = clientIdGen
    )

  val ClientStatusGen: Gen[ClientStatus] = for {
    status <- Gen.oneOf(ClientStatuses.values.toSeq)
  } yield status

  def quotaSettingsGen(
      sizeGen: Gen[Int] = Gen.choose(50, 1000)
  ): Gen[QuotaRequest.Settings] =
    for {
      size <- sizeGen
      days <- Gen.oneOf(1, 7)
    } yield QuotaRequest.Settings(size, days, None)

  val QuotaSettingsGen: Gen[QuotaRequest.Settings] = quotaSettingsGen()

  val quotaTypeGen: Gen[QuotaType] =
    Gen.oneOf(
      QuotaTypes.values
        .filterNot(q =>
          //TODO support all quotas in gens
          ProductId.PartsProducts(q)
        )
        .toSeq
    )

  def quotaRequestGen(
      quotaTypeGen: Gen[QuotaType],
      quotaSizeGen: Gen[Int] = Gen.choose(50, 1000)
  ): Gen[QuotaRequest] =
    for {
      client <- ClientIdGen
      settings <- quotaSettingsGen(quotaSizeGen)
      from <- DateTimeGenerators.dateTimeInPast(maxDistance = 2.hours)
      quotaType <- quotaTypeGen
    } yield QuotaRequest(client, quotaType, settings, from)

  val QuotaRequestGen: Gen[QuotaRequest] = quotaRequestGen(quotaTypeGen)

  val PartsQuotaRequestGen: Gen[QuotaRequest] =
    QuotaRequestGen.map(r =>
      r.copy(settings = r.settings.copy(entity = QuotaEntities.Parts))
    )

  def quotaRequestGen(days: Int): Gen[QuotaRequest] =
    for {
      request <- QuotaRequestGen
      settings = request.settings.copy(days = days)
    } yield request.copy(settings = settings)

  val TariffGen: Gen[Tariff] = for {
    client <- ClientIdGen
    tariffType <- Gen.oneOf(TariffTypes.values.toSeq)
    from = now().minusDays(1)
    to = now().plusDays(3)
  } yield Tariff(client, tariffType, from, to)

  def quotaGen(quotaTypeGen: Gen[QuotaType]): Gen[Quota] =
    for {
      request <- quotaRequestGen(quotaTypeGen)
      client <- ClientIdGen
      deadline = request.from.plusSeconds(
        request.settings.duration.toSeconds.toInt
      )
      price <- Gen.choose[Long](100, 10000)
      revenue = price
    } yield {
      val builder = new OfferBillingBuilderImpl
      val detailedFunds = CommonDetailedFunds(
        price = ModifiedPrice(revenue),
        priceWithoutDiscount = revenue
      )
      builder.withActiveDeadline(deadline)
      builder.withCustomDynamicPrice(
        request.quotaType,
        detailedFunds,
        productTariff = None
      )
      Quota(
        client,
        request.quotaType,
        request.settings.size,
        revenue,
        price,
        request.from,
        deadline,
        builder.build.get
      )
    }

  val quotaIdGen: Gen[QuotaId] = Gen.posNum[Long].map(QuotaId(_))

  def storedQuotaGen(quotaTypeGen: Gen[QuotaType]): Gen[StoredQuota] =
    for {
      id <- quotaIdGen
      epoch <- DateTimeGenerators.dateTimeInPast(maxDistance = 2.hours)
      quota <- quotaGen(quotaTypeGen)
    } yield
      StoredQuota(
        id,
        quota.clientId,
        quota.quotaType,
        quota.size,
        quota.revenue,
        quota.price,
        quota.from,
        quota.to,
        quota.offerBilling,
        quota.regionId,
        epoch.getMillis,
        quota.entity
      )

  val QuotaGen: Gen[Quota] = quotaGen(quotaTypeGen)

  val expiredQuotaGen: Gen[Quota] = for {
    quota <- QuotaGen
    from <- DateTimeGenerators.dateTimeInPast(1.days + 1.minute, 5.days)
  } yield quota.copy(from = from, to = from.plusDays(1))

  def quotaGen(days: Int): Gen[Quota] =
    for {
      quota <- QuotaGen
    } yield {
      val to = quota.from.plusDays(days)
      quota.copy(to = to)
    }

  val storedQuotaGen: Gen[StoredQuota] = storedQuotaGen(quotaTypeGen)

  val ProductIdGen: Gen[ProductId] =
    Gen.oneOf(ProductId.values.toSeq)

  val nonPlacementProductIdGen: Gen[ProductId] =
    Gen.oneOf(ProductId.values.filter(_ != Placement).toSeq)

  def sizedAlphaStrGen(length: Int): Gen[String] =
    Gen.listOfN(length, Gen.alphaChar).map(_.mkString)

  def autoruOfferIdGen(
      offerIdGen: Gen[Long] = BasicGenerators.posNum[Long]
  ): Gen[AutoruOfferId] =
    for {
      offerId <- offerIdGen
      offerHash <- Gen.option(sizedAlphaStrGen(4))
    } yield AutoruOfferId(offerId, offerHash)

  val AutoruOfferIdGen: Gen[AutoruOfferId] = autoruOfferIdGen()

  def autoruOfferIdExcludeGen(
      exclude: Seq[AutoruOfferId] = Nil
  ): Gen[AutoruOfferId] =
    autoruOfferIdGen().filter(!exclude.contains(_))

  val OfferIdentityGen: Gen[OfferIdentity] = AutoruOfferIdGen

  val LocalDateIntervalInPastGen: Gen[(LocalDate, LocalDate)] = {
    val now = LocalDate.now()
    for {
      startOffset <- Gen.chooseNum(0, 100)
      endOffset <- Gen.chooseNum(1, 100)
    } yield {
      val end = now.minusDays(endOffset)
      val start = end.minusDays(startOffset)
      (start, end)
    }
  }

  val LocalDateIntervalGen: Gen[(LocalDate, LocalDate)] = {
    val now = LocalDate.now()
    for {
      startOffset <- Gen.chooseNum(0, 100)
      endOffset <- Gen.chooseNum(0, 100)
    } yield {
      val start = now.plusDays(startOffset)
      val end = start.plusDays(endOffset)
      (start, end)
    }
  }

  def alwaysAtFirstPagePayloadGen(
      forMarkModelListing: Option[Boolean] = None,
      forMarkModelGenerationListing: Option[Boolean] = None
  ): Gen[AlwaysAtFirstPagePayload] = {
    val tupleGen = (forMarkModelListing, forMarkModelGenerationListing) match {
      case (Some(false), Some(false)) =>
        throw new IllegalArgumentException(
          "At least one on first page payload param should be true"
        )
      case (Some(markModel), Some(markModelGen)) =>
        Gen.const((markModel, markModelGen))
      case (Some(markModel), None) =>
        Gen.oneOf(Seq((markModel, true), (markModel, false)))
      case (None, Some(markModelGen)) =>
        Gen.oneOf(Seq((true, markModelGen), (false, markModelGen)))
      case (None, None) =>
        Gen.oneOf((true, true), (true, false), (false, true))
    }
    tupleGen.map(AlwaysAtFirstPagePayload.tupled)
  }

  val AutostrategyPayloadGen: Gen[AutostrategyPayload] =
    // that may be changed to oneOf() after adding more payload types
    alwaysAtFirstPagePayloadGen()

  def autostrategyPayloadGen(
      autostrategyType: AutostrategyType
  ): Gen[AutostrategyPayload] =
    autostrategyType match {
      case AlwaysAtFirstPage => alwaysAtFirstPagePayloadGen()
    }

  private def autostrategyGen(
      localDateIntervalGen: Gen[(LocalDate, LocalDate)]
  ) =
    for {
      offerId <- AutoruOfferIdGen
      (fromDate, toDate) <- localDateIntervalGen
      maxApplicationsPerDay <- Gen.option(Gen.posNum[Int])
      payload <- AutostrategyPayloadGen
    } yield Autostrategy(offerId, fromDate, toDate, maxApplicationsPerDay, payload)

  val AutostrategyGen: Gen[Autostrategy] =
    autostrategyGen(LocalDateIntervalGen)

  val OldAutostrategyGen: Gen[Autostrategy] =
    autostrategyGen(LocalDateIntervalInPastGen)

  def autostrategyGen(id: AutostrategyId): Gen[Autostrategy] =
    AutostrategyGen.map { autostrategy =>
      autostrategy.copy(
        offerId = id.offerId,
        payload = autostrategyPayloadGen(id.autostrategyType).next
      )
    }

  private def autostrategyListGen(
      autostrategyGen: Gen[Autostrategy]
  ): Gen[List[Autostrategy]] =
    // filters redundant generated autostrategies
    Gen.nonEmptyListOf(autostrategyGen).map {
      _.groupBy { autostrategy =>
        (autostrategy.offerId, autostrategy.payload.getType)
      }.values
        .map(_.head)
        .toList
    }

  val AutostrategyListGen: Gen[List[Autostrategy]] =
    autostrategyListGen(AutostrategyGen)

  val OldAutostrategyListGen: Gen[List[Autostrategy]] =
    autostrategyListGen(OldAutostrategyGen)

  val OfferAutostrategiesGen: Gen[List[OfferAutostrategies]] =
    AutostrategyListGen.map { autostrategies =>
      autostrategies
        .groupBy(_.offerId)
        .map(OfferAutostrategies.tupled)
        .toList
    }

  val AutostrategyIdListGen: Gen[List[AutostrategyId]] =
    AutostrategyListGen.map(_.map(_.id))

  val AutostrategyStatusGen: Gen[AutostrategyStatus] =
    Gen.oneOf(Active, Inactive, Expired)

  def storedAutostrategyGen(
      statusGen: Gen[AutostrategyStatus]
  ): Gen[StoredAutostrategy] =
    for {
      id <- Gen.posNum[Long]
      status <- statusGen
      epoch <- EpochGen
      props <- AutostrategyGen
    } yield StoredAutostrategy(id, status, epoch, props)

  val ActiveStoredAutostrategyGen: Gen[StoredAutostrategy] =
    storedAutostrategyGen(Active)

  val NotActiveStoredAutostrategyGen: Gen[StoredAutostrategy] =
    storedAutostrategyGen(Gen.oneOf(Inactive, Expired))

  def storedAutostrategyListGen(
      storedAutostrategyGen: Gen[StoredAutostrategy]
  ): Gen[List[StoredAutostrategy]] =
    Gen.nonEmptyListOf(storedAutostrategyGen).map {
      _.distinctBy(_.id)
        .distinctBy(_.props.id)
        .toList
    }

  val ActiveStoredAutostrategyListGen: Gen[List[StoredAutostrategy]] =
    storedAutostrategyListGen(ActiveStoredAutostrategyGen)

  val NotActiveStoredAutostrategyListGen: Gen[List[StoredAutostrategy]] =
    storedAutostrategyListGen(NotActiveStoredAutostrategyGen)

  def randomAlphanumericString(length: Int): String =
    Random.alphanumeric.take(length).mkString

  def searcherPositionGen(onFirstPage: Boolean): Gen[Int] =
    if (onFirstPage) Gen.choose(1, 37) else Gen.choose(38, 500000)

  val userEssentialsGen: Gen[UserEssentials] = for {
    id <- readableString
    email <- readableString
  } yield
    UserEssentials
      .newBuilder()
      .setId(id)
      .setEmail(email)
      .build()

  private val customerIdGen: Gen[Model.CustomerId] =
    for {
      version <- Gen.posNum[Int]
      clientId <- Gen.posNum[Long]
      agencyId <- Gen.option(Gen.posNum[Long])
    } yield {
      val b = Model.CustomerId
        .newBuilder()
        .setVersion(version)
        .setClientId(clientId)
      agencyId.foreach(b.setAgencyId)
      b.build()
    }

  private val customerHeaderGen: Gen[CustomerHeader] =
    for {
      version <- Gen.posNum[Int]
      id <- Gen.option(customerIdGen)
    } yield {
      val b = CustomerHeader.newBuilder().setVersion(version)
      id.foreach(b.setId)
      b.build()
    }

  private val orderGen: Gen[Order] =
    for {
      version <- Gen.posNum[Int]
      id <- Gen.posNum[Long]
      owner <- customerIdGen
      text <- readableString
      memo <- Gen.option(readableString)
      commitAmount <- Gen.posNum[Long]
      approximateAmount <- Gen.posNum[Long]
      totalIncome <- Gen.option(Gen.posNum[Long])
      totalSpent <- Gen.option(Gen.posNum[Long])
      productKey <- Gen.option(readableString)
    } yield {
      val b = Order
        .newBuilder()
        .setVersion(version)
        .setId(id)
        .setOwner(owner)
        .setText(text)
        .setCommitAmount(commitAmount)
        .setApproximateAmount(approximateAmount)
      memo.foreach(b.setMemo)
      totalIncome.foreach(b.setTotalIncome)
      totalSpent.foreach(b.setTotalSpent)
      productKey.foreach(b.setProductKey)
      b.build()
    }

  private val billingProductGen: Gen[Model.Product] =
    for {
      version <- Gen.posNum[Int]
    } yield Model.Product.newBuilder().setVersion(version).build()

  private def campaignSettingsGen(
      isEnabled: Gen[Boolean]
  ): Gen[CampaignSettings] =
    for {
      version <- Gen.posNum[Int]
      isEnabled <- isEnabled
    } yield
      CampaignSettings
        .newBuilder()
        .setVersion(version)
        .setIsEnabled(isEnabled)
        .build()

  val inactiveReasonGen: Gen[InactiveReason] =
    protoEnum(InactiveReason.values())

  private val maybeInactiveReasonGen: Gen[Option[InactiveReason]] =
    Gen.option(inactiveReasonGen)

  def campaignHeaderGen(
      inactiveReasonGen: Gen[Option[InactiveReason]] = maybeInactiveReasonGen,
      isEnabled: Gen[Boolean] = bool
  ): Gen[CampaignHeader] =
    for {
      version <- Gen.posNum[Int]
      id <- readableString
      owner <- customerHeaderGen
      order <- orderGen
      product <- billingProductGen
      settings <- campaignSettingsGen(isEnabled)
      maybeInactiveReason <- inactiveReasonGen
    } yield {
      val b = CampaignHeader
        .newBuilder()
        .setVersion(version)
        .setId(id)
        .setOwner(owner)
        .setOrder(order)
        .setProduct(product)
        .setSettings(settings)
      maybeInactiveReason.foreach(b.setInactiveReason)
      b.build()
    }

  val activeCampaignHeaderGen: Gen[CampaignHeader] =
    campaignHeaderGen(inactiveReasonGen = None, isEnabled = true)

  val inactiveCampaignHeaderGen: Gen[CampaignHeader] =
    campaignHeaderGen(Gen.some(inactiveReasonGen), isEnabled = false)

  val adsRequestTypeGen: Gen[AdsRequestType] =
    Gen.oneOf(AdsRequestTypes.values.toList)

  val adsRequestTypesGen: Gen[Set[AdsRequestType]] =
    Gen.oneOf(
      AdsRequestTypes.values ::
        Set.empty[AdsRequestType] ::
        AdsRequestTypes.values.map(Set(_)).toList
    )

  def adsRequestTypesGenWith(
      adsRequestType: AdsRequestType
  ): Gen[Set[AdsRequestType]] =
    Gen.oneOf(AdsRequestTypes.values, Set(adsRequestType))

  def regionIdGen: Gen[RegionId] = Gen.posNum[Long].map(RegionId.apply)

  def cityIdGen: Gen[CityId] = Gen.posNum[Long].map(CityId.apply)

  def clientRecordGen(
      regionIdGen: Gen[RegionId] = regionIdGen,
      singlePaymentsGen: Gen[Set[AdsRequestType]] = adsRequestTypesGen,
      paidCallsAvailableGen: Gen[Boolean] = bool,
      priorityPlacementGen: Gen[Boolean] = bool
  ): Gen[Client] =
    for {
      clientId <- Gen.posNum[Long]
      agencyId <- Gen.option(posNum[AgencyId])
      categorizedClientId <- Gen.posNum[Long]
      companyId <- Gen.posNum[Long]
      regionId <- regionIdGen
      cityId <- cityIdGen
      status <- ClientStatusGen
      singlePayments <- singlePaymentsGen
      firstModerated <- bool
      paidCallsAvailable <- paidCallsAvailableGen
      priorityPlacement <- priorityPlacementGen
    } yield
      Client(
        clientId,
        agencyId,
        Some(categorizedClientId),
        Some(companyId),
        regionId,
        cityId,
        status,
        singlePayments,
        firstModerated,
        paidCallsAvailable = paidCallsAvailable,
        priorityPlacement = priorityPlacement
      )

  val ClientRecordGen: Gen[Client] = clientRecordGen()

  def clientPoiPropertiesGen(
      callTrackingByOfferGen: Gen[Boolean] = bool
  ): Gen[PoiProperties] =
    for {
      callTrackingByOffer <- callTrackingByOfferGen
    } yield
      PoiProperties
        .newBuilder()
        .addProperties {
          PoiProperty
            .newBuilder()
            .setName("call_tracking_by_offer")
            .setValue(if (callTrackingByOffer) "1" else "0")
        }
        .build()

  val balanceRecordGen: Gen[BalanceClient] =
    Gen
      .zip(
        posNum[ClientId],
        posNum[BalanceClientId],
        Gen.option(posNum[BalanceClientId]),
        posNum[AccountId],
        posNum[Long].map(BigDecimal.apply)
      )
      .map(BalanceClient.tupled)

  val AutoruDealerGen: Gen[AutoruDealer] = for {
    id <- Gen.posNum[Long]
  } yield AutoruDealer(id)

  case class DifferentDealers(dealer1: AutoruDealer, dealer2: AutoruDealer)

  private val set = (1 to 100).toSet

  val DifferentDealersGen: Gen[DifferentDealers] = for {
    listOf2 <- Gen.pick(2, set).map(_.toList)
  } yield DifferentDealers(AutoruDealer(listOf2.head), AutoruDealer(listOf2.last))

  val fundsGen: Gen[Funds] = Gen.posNum[Funds]

  val optFundsGen: Gen[Option[Funds]] = Gen.option(fundsGen)

  def nonEmptySetGen[A: Ordering](gen: Gen[A]): Gen[NonEmptySet[A]] =
    Gen.nonEmptyListOf(gen).map { elems =>
      NonEmptySet.fromSetUnsafe(elems.to)
    }

  val localDateInFutureGen: Gen[LocalDate] =
    dateTimeInFuture().map(_.toLocalDate)

  val activateDateGen: Gen[ActivateDate] =
    dateTime().map(ActivateDate)
}
