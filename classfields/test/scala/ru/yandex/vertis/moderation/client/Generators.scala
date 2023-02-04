package ru.yandex.vertis.moderation.client

import java.net.URL

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.moderation.proto.Model._
import ru.yandex.vertis.moderation.proto.{Model, ModelFactory, RealtyLightFactory}
import ru.yandex.vertis.moderation.client.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.RealtyLight.PriceInfo.{Currency, PricingPeriod}
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials.{
  CategoryType,
  CommercialType,
  OfferType,
  PropertyType
}
import ru.yandex.vertis.moderation.proto.RealtyLight._

import scala.util.Random
import scala.collection.JavaConverters._

/**
  * @author semkagtn
  */
object Generators {

  val BooleanGen: Gen[Boolean] = Gen.oneOf(true, false)

  val ServiceGen: Gen[Service] = Gen.oneOf(Service.values.filter(_ != Service.UNKNOWN_SERVICE))

  val OfferTypeGen: Gen[OfferType] = Gen.oneOf(OfferType.values.toSeq)

  val CurrencyGen: Gen[Currency] = Gen.oneOf(Currency.values().toSeq)

  val PricingPeriodGen: Gen[PricingPeriod] = Gen.oneOf(PricingPeriod.values().toSeq)

  val PropertyTypeGen: Gen[PropertyType] = Gen.oneOf(PropertyType.values().toSeq)

  val CategoryTypeGen: Gen[CategoryType] = Gen.oneOf(CategoryType.values().toSeq)

  val AreaUnitGen: Gen[AreaUnit] = Gen.oneOf(AreaUnit.values().toSeq)

  val CommercialTypeGen: Gen[CommercialType] = Gen.oneOf(CommercialType.values().toSeq)

  val AuthorTypeGen: Gen[AuthorType] = Gen.oneOf(AuthorType.values().toSeq)

  val UrlGen: Gen[URL] =
    for {
      parts <- Gen.nonEmptyListOf(stringGen(1, 4))
    } yield new URL("http://" + parts.mkString("/") + ".ru")

  val ApplicationGen: Gen[Application] = Gen.oneOf(Application.values)

  val ComplaintSubTypeGen: Gen[String] =
    Gen.oneOf(
      "auto",
      "specific_reason",
      "ad_signal",
      "wrong_data",
      "wrong_photo",
      "copy",
      "wrong_price",
      "phone_unavailable",
      "commerce",
      "residential",
      "sold_out",
      "leased_out",
      "fraud",
      "spare_part",
      "wrong_year",
      "wrong_address",
      "wrong_place",
      "wrong_model",
      "reseller"
    )

  val ExternalUserGen: Gen[ExternalUser] =
    for {
      systemId <- stringGen(4, 4)
      userId   <- stringGen(4, 4)
    } yield ModelFactory.newExternalUserBuilder().setSystemId(systemId).setUserId(userId).build()

  val UserGen: Gen[User] =
    for {
      random       <- Gen.choose(0, 4)
      id           <- stringGen(4, 4)
      externalUser <- ExternalUserGen
      builder = ModelFactory.newUserBuilder
    } yield random match {
      case 0 => builder.setYandexUser(id).build
      case 1 => builder.setPartnerUser(id).build()
      case 2 => builder.setExternalUser(externalUser).build()
      case 3 => builder.setDealerUser(id).build()
      case 4 => builder.setAutoruUser(id).build()
    }

  val ExternalIdGen: Gen[ExternalId] =
    for {
      user     <- UserGen
      objectId <- stringGen(4, 4)
    } yield ModelFactory.newExternalIdBuilder.setUser(user).setObjectId(objectId).build

  val PriceInfoGen: Gen[PriceInfo] =
    for {
      currency <- CurrencyGen
      value    <- Gen.choose(1.0, 100000000.0)
      period   <- PricingPeriodGen
      unit     <- AreaUnitGen
    } yield RealtyLightFactory.newPriceBuidler
      .setCurrency(currency)
      .setValue(value)
      .setPeriod(period)
      .setUnit(unit)
      .build

  val GeoInfoGen: Gen[GeoInfo] =
    for {
      regionId            <- Gen.choose(1L, 99L)
      geocoderId          <- Gen.choose(1, 99)
      subjectFederationId <- Gen.choose(1, 99)
      sublocalityId       <- Gen.choose(1, 99)
      metroGeoId          <- Gen.choose(1, 99)
      address             <- stringGen(2, 3)
      rawAddress          <- stringGen(2, 3)
    } yield RealtyLightFactory.newGeoInfoBuilder
      .setRegionId(regionId)
      .setGeocoderId(geocoderId)
      .setSubjectFederationId(subjectFederationId)
      .setSublocalityId(sublocalityId)
      .setMetroGeoId(metroGeoId)
      .setAddress(address)
      .setRawAddress(rawAddress)
      .build

  val AreaInfoGen: Gen[AreaInfo] =
    for {
      value <- Gen.choose(1.0f, 100.0f)
      unit  <- AreaUnitGen
    } yield RealtyLightFactory.newAreaInfoBuilder.setValue(value).setUnit(unit).build

  val ApartmentComplexInfoGen: Gen[ApartmentComplexInfo] =
    for {
      name          <- stringGen(2, 3)
      yearBuilt     <- Gen.choose(1, 2100)
      storyesAmount <- Gen.choose(0, 10)
    } yield RealtyLightFactory.newApartmentComplexInfoBuilder
      .setName(name)
      .setYearBuilt(yearBuilt)
      .setStoreysAmount(storyesAmount)
      .build()

  val AuthorInfoGen: Gen[AuthorInfo] =
    for {
      authorType   <- AuthorTypeGen
      uid          <- stringGen(2, 3)
      internalId   <- stringGen(2, 3)
      phonesAmount <- Gen.choose(1, 3)
      phones       <- Gen.listOfN(phonesAmount, Gen.chooseNum(1000000L, 100000000000L))
      agencyName   <- stringGen(2, 3)
      sellerName   <- stringGen(2, 3)
    } yield RealtyLightFactory.newAuthorInfoBuilder
      .setAuthorType(authorType)
      .setUid(uid)
      .setInternalId(internalId)
      .addAllPhones(phones.map(_.toString).asJava)
      .setAgencyName(agencyName)
      .setSellerName(sellerName)
      .build()

  val RealtyEssentialsGen: Gen[RealtyEssentials] =
    for {
      clusterId           <- Gen.choose(1, 100000000)
      clusterHead         <- BooleanGen
      clusterSize         <- Gen.choose(1, 10)
      partnerId           <- Gen.choose(1, 100000000)
      offerType           <- OfferTypeGen
      rawAddress          <- stringGen(2, 50)
      pricesAmount        <- Gen.choose(1, 3)
      price               <- PriceInfoGen
      pricePerM2          <- PriceInfoGen
      propertyType        <- PropertyTypeGen
      categoryType        <- CategoryTypeGen
      photosAmount        <- Gen.choose(1, 6)
      photoUrls           <- Gen.listOfN(photosAmount, UrlGen)
      phonesAmount        <- Gen.choose(1, 3)
      phones              <- Gen.listOfN(phonesAmount, Gen.chooseNum(1L, 100L))
      description         <- stringGen(1, 20)
      feedId              <- Gen.choose(1, 100000000)
      roomsAmount         <- Gen.choose(1, 5)
      floor               <- Gen.choose(1, 5)
      geoInfo             <- GeoInfoGen
      areaInfo            <- AreaInfoGen
      commercialTypes     <- CommercialTypeGen
      isStudio            <- BooleanGen
      isOpenPlan          <- BooleanGen
      apartmnetCompleInfo <- ApartmentComplexInfoGen
      livingAreaInfo      <- AreaInfoGen
      kitchenAreaInfo     <- AreaInfoGen
      authorInto          <- AuthorInfoGen
      cadastralNumber     <- stringGen(2, 3)
      commision           <- Gen.choose(1, 20)
      isVat               <- BooleanGen
      isPremium           <- BooleanGen
      isRaised            <- BooleanGen
      isPromoted          <- BooleanGen
      days                <- Gen.choose(0, 100)
      isTrusted           <- BooleanGen
      isVos               <- BooleanGen
      isPlacement         <- BooleanGen
      isCallCenter        <- BooleanGen
      partnerInternalId   <- stringGen(2, 3)
      ip                  <- stringGen(1, 20)
    } yield RealtyLightFactory.newRealtyEssentialsBuilder
      .setClusterId(clusterId)
      .setClusterHead(clusterHead)
      .setClusterSize(clusterSize)
      .setPartnerId(partnerId)
      .setOfferType(offerType)
      .setPriceInfo(price)
      .setPriceInfoPerM2(pricePerM2)
      .setPropertyType(propertyType)
      .setCategoryType(categoryType)
      .addAllPhotoUrl(photoUrls.map(_.toString).asJava)
      .addAllPhones(phones.map(_.toString).asJava)
      .setRoomsAmount(roomsAmount)
      .setFloor(floor)
      .setGeoInfo(geoInfo)
      .setAreaInfo(areaInfo)
      .setIsStudio(isStudio)
      .setIsOpenPlan(isOpenPlan)
      .setApartmentComplexInfo(apartmnetCompleInfo)
      .setLivingAreaInfo(livingAreaInfo)
      .setKitchenAreaInfo(kitchenAreaInfo)
      .setAuthorInfo(authorInto)
      .setCadastralNumber(cadastralNumber)
      .setCommission(commision)
      .setIsVat(isVat)
      .setIsPremium(isPremium)
      .setIsRaised(isRaised)
      .setIsPromoted(isPromoted)
      .setIsTrusted(isTrusted)
      .setIsVos(isVos)
      .setIsPlacement(isPlacement)
      .setIsCallCenter(isCallCenter)
      .setPartnerInternalId(partnerInternalId)
      .build()

  val EssentialsGen: Gen[Essentials] =
    for {
      random           <- Gen.choose(0, 1)
      realtyEssentials <- RealtyEssentialsGen
      builder = ModelFactory.newEssentialsBuilder
    } yield builder.setRealty(realtyEssentials).build

  val SourceTypeGen: Gen[SourceMarker.Type] = Gen.oneOf(SourceMarker.Type.values())

  val NoMarkerGen: Gen[SourceMarker] =
    Gen.const(ModelFactory.newSourceMarkerBuilder().setType(SourceMarker.Type.NO_MARKER).build())

  val InheritedMarkerGen: Gen[SourceMarker] =
    for {
      service <- ServiceGen
    } yield ModelFactory.newSourceMarkerBuilder().setService(service).setType(SourceMarker.Type.INHERITED).build()

  val SourceMarkerGen: Gen[SourceMarker] = Gen.oneOf(NoMarkerGen, InheritedMarkerGen)

  val AutomaticSourceGen: Gen[AutomaticSource] =
    for {
      application  <- ApplicationGen
      sourceMarker <- SourceMarkerGen
    } yield ModelFactory.newAutomaticSourceBuilder.setApplication(application).setSourceMarker(sourceMarker).build

  val ManualSourceGen: Gen[ManualSource] =
    for {
      userId       <- stringGen(3, 3)
      sourceMarker <- SourceMarkerGen
    } yield ModelFactory.newManualSourceBuilder.setUserId(userId).setSourceMarker(sourceMarker).build

  val SourceGen: Gen[Source] =
    for {
      random          <- Gen.choose(0, 1)
      automaticSource <- AutomaticSourceGen
      manualSource    <- ManualSourceGen
      builder = ModelFactory.newSourceBuilder
    } yield random match {
      case 0 => builder.setAutomaticSource(automaticSource).build
      case 1 => builder.setManualSource(manualSource).build
    }

  val ReasonGen: Gen[Reason] = Gen.oneOf(Reason.values)

  //  val ComplaintsSignalGen: Gen[ComplaintsSignal] = for {
  //    source <- SourceGen
  //    days <- Gen.choose(1, 3)
  //    timestamp = DateTime.now.minusDays(days).getMillis
  //    weight <- Gen.choose(0.0, 10.0)
  //    subType <- ComplaintSubTypeGen
  //  } yield ModelFactory.newComplaintsSignalBuilder.
  //    setSource(source).
  //    setTimestamp(timestamp).
  //    setWeight(weight).
  //    setSubType(subType).
  //    build

  val BanSignalGen: Gen[BanSignal] =
    for {
      source <- SourceGen
      days   <- Gen.choose(1, 3)
      timestamp = DateTime.now.minusDays(days).getMillis
      reason <- ReasonGen
    } yield ModelFactory.newBanSignalBuilder.setSource(source).setTimestamp(timestamp).setReason(reason).build

  val UnbanSignalGen: Gen[UnbanSignal] =
    for {
      source <- SourceGen
      days   <- Gen.choose(1, 3)
      timestamp = DateTime.now.minusDays(days).getMillis
    } yield ModelFactory.newUnbanSignalBuilder.setSource(source).setTimestamp(timestamp).build

  val SignalGen: Gen[Signal] =
    for {
      random <- Gen.choose(0, 1)
      builder = ModelFactory.newSignalBuilder
    } yield random match {
      case 0 => builder.setBanSignal(BanSignalGen.next).build
      case 1 => builder.setUnbanSignal(UnbanSignalGen.next).build
    }

  val BanSignalSourceGen: Gen[BanSignalSource] =
    for {
      source <- SourceGen
      reason <- ReasonGen
    } yield ModelFactory.newBanSignalSourceBuilder.setSource(source).setReason(reason).build

  //  val ComplaintsSignalSourceGen: Gen[ComplaintsSignalSource] = for {
  //    source <- SourceGen
  //    weight <- Gen.choose(0.0, 10.0)
  //    subType <- ComplaintSubTypeGen
  //  } yield ModelFactory.newComplaintsSignalSourceBuilder.
  //    setSource(source).
  //    setWeight(weight).
  //    setSubType(subType).
  //    build

  val UnbanSignalSourceGen: Gen[UnbanSignalSource] =
    for {
      source <- SourceGen
    } yield ModelFactory.newUnbanSignalSourceBuilder.setSource(source).build

  val SignalSourceGen: Gen[SignalSource] =
    for {
      random <- Gen.choose(0, 1)
      builder = ModelFactory.newSignalSourceBuilder
    } yield random match {
      case 0 => builder.setBanSignal(BanSignalSourceGen.next).build
      case 1 => builder.setUnbanSignal(UnbanSignalSourceGen.next).build
    }

  val ContextSourceGen: Gen[ContextSource] =
    for {
      random     <- Gen.choose(0, 2)
      visibility <- Gen.oneOf(Model.Visibility.values().toSeq)
      tag        <- Gen.alphaStr
    } yield random match {
      case 0 => ModelFactory.newContextSourceBuilder().build()
      case 1 => ModelFactory.newContextSourceBuilder().setVisibility(visibility).setTag(tag).build()
      case 2 => ModelFactory.newContextSourceBuilder().setVisibility(visibility).build()
    }

  val InstanceSourceGen: Gen[InstanceSource] =
    for {
      qualifierOpt <- Gen.option(stringGen(3, 3))
      externalId   <- ExternalIdGen
      essentials   <- EssentialsGen
      numSignals   <- Gen.choose(0, 2)
      signals      <- Gen.listOfN(numSignals, SignalSourceGen)
      context      <- ContextSourceGen
    } yield (qualifierOpt match {
      case Some(qualifier) =>
        ModelFactory.newInstanceSourceBuilder.setQualifier(qualifier)
      case None =>
        ModelFactory.newInstanceSourceBuilder
    }).setExternalId(externalId).setEssentials(essentials).addAllSignals(signals.asJava).setContext(context).build

  def instanceSourceGen(externalId: ExternalId): Gen[InstanceSource] =
    for {
      qualifierOpt <- Gen.option(stringGen(3, 3))
      essentials   <- EssentialsGen
      numSignals   <- Gen.choose(0, 2)
      signals      <- Gen.listOfN(numSignals, SignalSourceGen)
      context      <- ContextSourceGen
    } yield (qualifierOpt match {
      case Some(qualifier) =>
        ModelFactory.newInstanceSourceBuilder.setQualifier(qualifier)
      case None =>
        ModelFactory.newInstanceSourceBuilder
    }).setExternalId(externalId).setEssentials(essentials).addAllSignals(signals.asJava).setContext(context).build

  def stringGen(minLen: Int, maxLen: Int): Gen[String] =
    for {
      len <- Gen.choose(minLen, maxLen)
    } yield Random.alphanumeric.take(len).mkString
}
