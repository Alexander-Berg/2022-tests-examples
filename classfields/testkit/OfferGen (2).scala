package ru.yandex.vertis.general.gost.model.testkit

import cats.data.NonEmptyList
import common.geobase.model.RegionIds.RegionId
import general.bonsai.category_model.Category
import general.gost.offer_model.YaMarketInfo
import ru.yandex.vertis.general.common.model.delivery.{DeliveryInfo, SelfDelivery}
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.gost.model.Offer._
import ru.yandex.vertis.general.gost.model.Photo._
import ru.yandex.vertis.general.gost.model.Preset.Preset
import ru.yandex.vertis.general.gost.model.SellingAddress.{apply => _, _}
import ru.yandex.vertis.general.gost.model._
import ru.yandex.vertis.general.gost.model.attributes.{AttributeValue, Attributes}
import ru.yandex.vertis.general.gost.model.bans.BanInfo
import ru.yandex.vertis.general.gost.model.counters.CountersAggregate
import ru.yandex.vertis.general.gost.model.inactive.InactiveReason
import ru.yandex.vertis.general.gost.model.inactive.InactiveReason.SellerRecall
import ru.yandex.vertis.general.gost.model.inactive.recall.SellerRecallReason
import ru.yandex.vertis.general.gost.model.inactive.recall.SellerRecallReason.{
  Other,
  Rethink,
  SoldOnYandex,
  SoldSomewhere
}
import ru.yandex.vertis.general.gost.model.moderation.DisplayApplicable.{Approved, NotApproved, Unprocessed}
import ru.yandex.vertis.general.gost.model.moderation.{DisplayApplicable, ModerationInfo}
import zio.random.Random
import zio.test.{Gen, Sized}

import java.time.Instant
import java.time.temporal.ChronoUnit

object OfferGen {

  val predefCategory: Gen[Any, Category] = Gen.fromIterable(Data.categories)

  def anyMdsImage(
      name: Gen[Random with Sized, String] = Gen.alphaNumericString,
      namespace: Gen[Random with Sized, String] = Gen.alphaNumericString,
      groupId: Gen[Random, Int] = Gen.anyInt): Gen[Random with Sized, MdsImage] =
    for {
      name <- name
      namespace <- namespace
      groupId <- groupId
    } yield MdsImage(name, namespace, groupId)

  val anyMdsImage: Gen[Random with Sized, MdsImage] = anyMdsImage()

  val anyPhoto: Gen[Random with Sized, Photo] = Gen.oneOf(
    anyMdsImage.map(CompletePhoto(_, None, None, None)),
    Gen.anyASCIIString.map(IncompletePhoto(_, false))
  )

  val anyPhotos: Gen[Random with Sized, List[Photo]] = Gen.listOf(anyPhoto)

  def anyVideo(
      url: Gen[Random with Sized, String] = Gen.alphaNumericString): Gen[Random with Sized, Video] =
    for {
      url <- url
    } yield Video(url)

  val anyVideo: Gen[Random with Sized, Video] = anyVideo()

  def anyPrice(
      priceRur: Gen[Random, Long] = Gen.long(0, Long.MaxValue),
      salaryRur: Gen[Random, Long] = Gen.long(0, Long.MaxValue),
      salaryRurMax: Gen[Random, Option[Long]] = Gen.option(Gen.long(0, Long.MaxValue)),
      salaryProfit: Gen[Random, SalaryProfit] =
        Gen.elements(SalaryProfit.Gross, SalaryProfit.Net)): Gen[Random, Price] =
    for {
      priceKop <- priceRur
      inCurrency = Price.InCurrency(priceKop)
      salaryProfit <- salaryProfit
      salaryRur <- salaryRur
      salaryRurMax <- salaryRurMax
      salary = Price.Salary(salaryRur, salaryProfit)
      salaryRange = Price.SalaryRange(Some(salaryRur), salaryRurMax, salaryProfit)
      anyPrice <- Gen.elements(inCurrency, Price.Free, salary, salaryRange, Price.Unset)
    } yield anyPrice

  val anyPrice: Gen[Random, Price] = anyPrice()

  val anySellerRecallReason: Gen[Random, SellerRecallReason] = Gen.elements(SoldOnYandex, SoldSomewhere, Rethink, Other)

  val anySellerRecall: Gen[Random, InactiveReason] =
    for {
      instant <- Gen.anyInstant
      reason <- anySellerRecallReason
    } yield SellerRecall(instant, reason)

  val anyInactiveReason: Gen[Random, InactiveReason] = Gen.oneOf(anySellerRecall).noShrink

  val anyInactiveStatus: Gen[Random, OfferStatus] =
    anyInactiveReason.map(OfferStatus.Inactive)

  val anyBanInfo: Gen[Random with Sized, BanInfo] =
    for {
      banReasons <- Gen.listOfBounded(1, 3)(Gen.alphaNumericString)
      banPreviousStatus <- Gen.oneOf(Gen.const(OfferStatus.Active), anyInactiveStatus)
      isInherited <- Gen.boolean
    } yield BanInfo(isInherited, NonEmptyList.fromListUnsafe(banReasons), banPreviousStatus)

  val anyBannedStatus: Gen[Random with Sized, OfferStatus] =
    anyBanInfo.map(OfferStatus.Banned)

  val anyRemovedStatus: Gen[Random with Sized, OfferStatus] =
    Gen
      .oneOf(
        Gen.const(OfferStatus.Active),
        anyInactiveStatus,
        anyBannedStatus
      )
      .map(OfferStatus.Removed)

  val anyStatus: Gen[Random with Sized, OfferStatus] =
    Gen.oneOf(
      Gen.const(OfferStatus.Active),
      anyInactiveStatus,
      anyBannedStatus,
      anyRemovedStatus
    )

  val anyNonRemovedStatus: Gen[Random, OfferStatus] =
    Gen.oneOf(
      Gen.const(OfferStatus.Active),
      anyInactiveStatus
    )

  val anyNonActiveStatus: Gen[Random with Sized, OfferStatus] =
    Gen.oneOf(
      anyBannedStatus,
      anyInactiveStatus,
      anyRemovedStatus
    )

  val anyPreset: Gen[Random, Preset] =
    Gen.elements(Preset.All, Preset.Active, Preset.Expired)

  def anyPresets(count: Int): Gen[Random, Set[Preset]] =
    Gen.setOfN(count)(anyPreset)

  val anyWayToContact: Gen[Random, WayToContact] =
    Gen.elements(WayToContact.Any, WayToContact.Chat, WayToContact.PhoneCall)

  val anyOfferOrigin: Gen[Random, OfferOrigin] =
    Gen.elements(OfferOrigin.Feed, OfferOrigin.Form)

  val anyDeliveryInfo: Gen[Random, DeliveryInfo] = {
    for {
      sendByCourier <- Gen.boolean
      sendWithinRussia <- Gen.boolean
    } yield DeliveryInfo(selfDelivery =
      Some(SelfDelivery(sendByCourier = sendByCourier, sendWithinRussia = sendWithinRussia))
    )
  }

  val anyMordaApplicable: Gen[Random, DisplayApplicable] =
    Gen.elements(Approved, Unprocessed, NotApproved)

  def anyGeoPoint(): Gen[Random, GeoPoint] = {
    for {
      latitude <- Gen.anyDouble
      longitude <- Gen.anyDouble
    } yield GeoPoint(latitude = latitude, longitude = longitude)
  }

  def anyAddressInfo(): Gen[Random with Sized, AddressInfo] =
    for {
      name <- Gen.anyString
    } yield AddressInfo(name)

  def anyMetroInfo(): Gen[Random with Sized, MetroStationInfo] =
    for {
      id <- Gen.anyLong
      name <- Gen.anyString
      isEnriched <- Gen.anyShort.map(_ % 2 == 0)
    } yield MetroStationInfo(id, Seq(), isEnriched, name, Seq())

  def anyDistrictInfo(): Gen[Random with Sized, DistrictInfo] =
    for {
      id <- Gen.anyLong
      name <- Gen.anyString
      isEnriched <- Gen.anyShort.map(_ % 2 == 0)
    } yield DistrictInfo(id, isEnriched, name)

  def anyRegionInfo(): Gen[Random with Sized, RegionInfo] =
    for {
      id <- Gen.anyLong
      name <- Gen.anyString
      isEnriched <- Gen.anyShort.map(_ % 2 == 0)
      isTown <- Gen.option(Gen.boolean)
    } yield RegionInfo(RegionId(id), isEnriched, name, isTown)

  def anyAddress(
      geopoint: Gen[Random, GeoPoint] = anyGeoPoint(),
      address: Gen[Random with Sized, Option[AddressInfo]] = Gen.option(anyAddressInfo()),
      nearestMetroStation: Gen[Random with Sized, Option[MetroStationInfo]] = Gen.option(anyMetroInfo()),
      district: Gen[Random with Sized, Option[DistrictInfo]] = Gen.option(anyDistrictInfo()),
      region: Gen[Random with Sized, Option[RegionInfo]] =
        Gen.option(anyRegionInfo())): Gen[Random with Sized, SellingAddress] =
    for {
      geopoint <- geopoint
      address <- address
      nearestMetroStation <- nearestMetroStation
      district <- district
      region <- region
    } yield SellingAddress(geopoint, address, nearestMetroStation, district, region)

  val anyAddresses: Gen[Random with Sized, List[SellingAddress]] = Gen.listOf(anyAddress())

  val anyPhoneNumber: Gen[Random, String] =
    Gen.long(70000000000L, 79999999999L).map(number => s"+$number")

  val anyEmail: Gen[Random with Sized, String] =
    Gen.alphaNumericString.zipWith(Gen.alphaNumericString)((s1, s2) => s"$s1@$s2.com")

  def anyContact(phone: Gen[Random, Option[String]] = Gen.option(anyPhoneNumber)): Gen[Random with Sized, Contact] =
    phone.map(Contact)

  val anyContact: Gen[Random with Sized, Contact] = anyContact()

  val anyContacts: Gen[Random with Sized, List[Contact]] = Gen.listOf(anyContact)

  val anyVendor: Gen[Random with Sized, Option[Vendor]] =
    for {
      name <- Gen.anyString
      logotype <- Gen.option(anyPhoto)
      hirer = Vendor.Hirer(name = name, logotype = logotype)
      vendor <- Gen.elements(None, Some(hirer))
    } yield vendor

  val anyShopInfo: Gen[Random with Sized, Option[ShopInfo]] = {
    val shopInfoGen = for {
      name <- Gen.anyString
      company <- Gen.anyString
      url <- Gen.anyString
      email <- anyEmail
      shopInfo = ShopInfo(name, company, url, email)
    } yield shopInfo
    Gen.option(shopInfoGen)
  }

  def anyOffer(
      offerId: Gen[Random with Sized, OfferId] = anyOfferId,
      title: Gen[Random with Sized, String] = Gen.alphaNumericString,
      description: Gen[Random with Sized, String] = Gen.alphaNumericString,
      category: Gen[Random with Sized, Category] = predefCategory,
      attributes: Category => Gen[Random with Sized, Attributes[AttributeValue]] = AttributeGen.ofCategory,
      sellerId: Gen[Random with Sized, SellerId] = SellerGen.anySellerId,
      photos: Gen[Random with Sized, Seq[Photo]] = anyPhotos,
      video: Gen[Random with Sized, Option[Video]] = Gen.option(anyVideo),
      createdAt: Gen[Random, Instant] = Gen.instant(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now()),
      updatedAt: Gen[Random, Instant] = Gen.instant(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now()),
      actualizedAt: Gen[Random, Instant] = Gen.instant(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now()),
      publishedAt: Gen[Random, Option[Instant]] =
        Gen.option(Gen.instant(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now())),
      activatedAt: Gen[Random, Option[Instant]] =
        Gen.option(Gen.instant(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now())),
      expireAt: Gen[Random, Option[Instant]] = Gen.option(Gen.anyInstant),
      price: Gen[Random, Price] = anyPrice,
      status: Gen[Random with Sized, OfferStatus] = anyStatus,
      activeServices: Gen[Random with Sized, Seq[Service]] = ServiceGen.anyServices,
      addresses: Gen[Random with Sized, Seq[SellingAddress]] = anyAddresses,
      contacts: Gen[Random with Sized, Seq[Contact]] = anyContacts,
      preferredWayToContact: Gen[Random, WayToContact] = anyWayToContact,
      isPhoneRedirectEnabled: Gen[Random, Option[Boolean]] = Gen.boolean.map(Option(_)),
      offerOrigin: Gen[Random, OfferOrigin] = anyOfferOrigin,
      mordaApplicable: Gen[Random, DisplayApplicable] = anyMordaApplicable,
      potentiallyMordaApplicable: Gen[Random, DisplayApplicable] = anyMordaApplicable,
      yanApplicable: Gen[Random, DisplayApplicable] = anyMordaApplicable,
      condition: Gen[Random, Option[Condition]] = ConditionGen.anyCondition,
      version: Gen[Random, Long] = Gen.long(min = 0, max = 1000000).noShrink,
      formatVersion: Gen[Random, Long] = Gen.long(min = 0, max = 1000000).noShrink,
      deliveryInfo: Gen[Random, Option[DeliveryInfo]] = Gen.option(anyDeliveryInfo),
      marketModelId: Gen[Random with Sized, Option[String]] = Gen.option(Gen.anyString),
      externalUrl: Gen[Random with Sized, Option[String]] = Gen.option(Gen.anyString),
      vendor: Gen[Random with Sized, Option[Vendor]] = anyVendor,
      hideOnService: Gen[Random, Boolean] = Gen.boolean,
      shopInfo: Gen[Random with Sized, Option[ShopInfo]] = anyShopInfo): Gen[Random with Sized, Offer] =
    for {
      offerId <- offerId
      sellerId <- sellerId
      title <- title
      description <- description
      category <- category
      attributes <- attributes(category)
      photos <- photos
      video <- video
      createdAt <- createdAt
      updatedAt <- updatedAt
      actualizedAt <- actualizedAt
      publishedAt <- publishedAt
      expireAt <- expireAt
      activatedAt <- activatedAt
      price <- price
      status <- status
      activeServices <- activeServices
      addresses <- addresses
      contacts <- contacts
      preferredWayToContact <- preferredWayToContact
      isPhoneRedirectEnabled <- isPhoneRedirectEnabled
      condition <- condition
      version <- version
      formatVersion <- formatVersion
      mordaApplicable <- mordaApplicable
      potentiallyMordaApplicable <- potentiallyMordaApplicable
      yanApplicable <- yanApplicable
      offerOrigin <- offerOrigin
      deliveryInfo <- deliveryInfo
      marketModelId <- marketModelId
      externalUrl <- externalUrl
      vendor <- vendor
      hideOnService <- hideOnService
      shopInfo <- shopInfo
    } yield Offer(
      offerId = offerId,
      sellerId = sellerId,
      title = title,
      description = description,
      category = CategoryInfo(category.id, category.version),
      attributes = attributes,
      photos = photos,
      video = video,
      createdAt = createdAt,
      updatedAt = updatedAt,
      actualizedAt = actualizedAt,
      publishedAt = publishedAt,
      expireAt = expireAt,
      activatedAt = activatedAt,
      price = price,
      status = status,
      services = activeServices,
      addresses = addresses,
      contacts = contacts,
      preferredWayToContact = preferredWayToContact,
      isPhoneRedirectEnabled = isPhoneRedirectEnabled,
      condition = condition,
      presets = Set.empty,
      version = version,
      feedInfo = None,
      mordaApplicable = mordaApplicable,
      potentiallyMordaApplicable = potentiallyMordaApplicable,
      yanApplicable = yanApplicable,
      moderationInfo = ModerationInfo(lockedFields = Set(), None),
      origin = offerOrigin,
      formatVersion = formatVersion,
      delivery = deliveryInfo,
      counters = CountersAggregate.empty,
      externalUrl = externalUrl,
      vendor = vendor,
      hideOnService = hideOnService,
      shopInfo = shopInfo,
      yaMarketInfo = Some(YaMarketInfo(sku = marketModelId))
    )

  val anyOfferId: Gen[Random with Sized, OfferId] = Gen.anyUUID.map(uuid => OfferId.apply(uuid.toString)).noShrink
  val anyOffer: Gen[Random with Sized, Offer] = anyOffer()

  val anyNonRemovedOffer: Gen[Random with Sized, Offer] =
    anyOffer(status = anyNonRemovedStatus)

  val anyActiveOffer: Gen[Random with Sized, Offer] =
    anyOffer(status = Gen.const(OfferStatus.Active))

  val anyInactiveOffer: Gen[Random with Sized, Offer] =
    anyOffer(status = anyInactiveStatus)

  val anyNonActiveOffer: Gen[Random with Sized, Offer] =
    anyOffer(status = anyNonActiveStatus)

  val anyBannedOffer: Gen[Random with Sized, Offer] =
    anyOffer(status = anyBannedStatus)

  def anyRemovedOffer: Gen[Random with Sized, Offer] =
    anyOffer(status = anyRemovedStatus)

  def anyNonRemovedOffers(count: Int): Gen[Random with Sized, List[Offer]] =
    Gen.listOfN(count)(anyNonRemovedOffer.noShrink)

  def anyActiveOffers(count: Int): Gen[Random with Sized, List[Offer]] =
    Gen.listOfN(count)(anyActiveOffer.noShrink)

  def anyInactiveOffers(count: Int): Gen[Random with Sized, List[Offer]] =
    Gen.listOfN(count)(anyInactiveOffer.noShrink)
}
