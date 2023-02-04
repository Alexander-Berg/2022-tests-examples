package ru.yandex.realty.event

import org.scalacheck.Gen
import ru.yandex.realty.events._
import ru.yandex.realty.model.gen.RealtyGenerators
import ru.yandex.realty.proto.{Currency, Phone, PriceType}
import ru.yandex.realty.proto.offer._
import ru.yandex.realty.proto.unified.offer.price.PriceDetail

import scala.collection.JavaConverters._

/**
  * @author azakharov
  */
trait RealtyEventModelGen extends RealtyGenerators {

  val clientInfoGen: Gen[ClientInfo] = {
    for {
      clientVersion <- readableString
      clientType <- protoEnumWithUnknown(ClientTypeNamespace.ClientType.values())
    } yield {
      ClientInfo
        .newBuilder()
        .setClientVersion(clientVersion)
        .setClientType(clientType)
        .build()
    }
  }

  val roomInfoGen: Gen[RoomInfo] = {
    for {
      roomsTotal <- posNum[Int]
      roomsOffered <- Gen.chooseNum(1, roomsTotal)
    } yield {
      RoomInfo
        .newBuilder()
        .setRoomsTotal(roomsTotal)
        .setRoomsOffered(roomsOffered)
        .build()
    }
  }

  val apartmentInfoGen: Gen[ApartmentInfo] = {
    for (flatType <- protoEnumWithUnknown(FlatType.values())) yield {
      ApartmentInfo.newBuilder().setFlatType(flatType).build()
    }
  }

  val houseInfoGen: Gen[HouseInfo] = {
    for (houseType <- protoEnumWithUnknown(HouseType.values())) yield {
      HouseInfo.newBuilder().setHouseType(houseType).build()
    }
  }

  val lotInfoGen: Gen[LotInfo] = {
    for (lotType <- protoEnumWithUnknown(LotType.values())) yield {
      LotInfo.newBuilder().setLotType(lotType).build()
    }
  }

  val commercialInfoGen: Gen[CommercialInfo] = {
    for (commercialType <- protoEnumWithUnknown(CommercialType.values())) yield {
      CommercialInfo.newBuilder().addCommercialType(commercialType).build()
    }
  }

  val garageInfoGen: Gen[GarageInfo] = {
    for (parkingType <- protoEnumWithUnknown(ParkingType.values())) yield {
      GarageInfo.newBuilder().setParkingType(parkingType).build()
    }
  }

  val priceGen: Gen[PriceDetail] = {
    for {
      value <- Gen.posNum[Long]
      currency <- protoEnumWithUnknown(Currency.values())
      priceType <- protoEnumWithUnknown(PriceType.values())
      pricingPeriod <- protoEnumWithUnknown(PricingPeriod.values())
    } yield {
      PriceDetail
        .newBuilder()
        .setValue(value)
        .setCurrency(currency)
        .setPriceType(priceType)
        .setPricingPeriod(pricingPeriod)
        .build()
    }
  }

  val rentOfferGen: Gen[RentOffer] = {
    for {
      price <- priceGen
      rentTime <- protoEnumWithUnknown(RentTime.values())
    } yield {
      RentOffer
        .newBuilder()
        .setPrice(price)
        .setRentTime(rentTime)
        .build()
    }
  }

  val sellOfferGen: Gen[SellOffer] = {
    for {
      price <- priceGen
      primarySale <- bool
    } yield {
      SellOffer
        .newBuilder()
        .setPrice(price)
        .setPrimarySale(primarySale)
        .build()
    }
  }

  val offerServicesInfoGen: Gen[OfferServicesInfo] = {
    val offerServiceGen = protoEnumWithUnknown(OfferServiceNamespace.OfferService.values())
    for (offerServices <- Gen.listOf(offerServiceGen)) yield {
      OfferServicesInfo.newBuilder().addAllOfferServices(offerServices.asJava).build()
    }
  }

  val offerCategoryGen: Gen[Any] =
    Gen.oneOf(roomInfoGen, apartmentInfoGen, houseInfoGen, lotInfoGen, commercialInfoGen, garageInfoGen)

  val offerInfoGen: Gen[OfferInfo] = {
    for {
      offerId <- Gen.posNum[Long]
      offerType <- Gen.oneOf(sellOfferGen, rentOfferGen)
      partnerId <- Gen.posNum[Long]
      offerServicesInfo <- offerServicesInfoGen
      offerCategory <- offerCategoryGen
      revoked <- Gen.oneOf(true, false)
    } yield {
      val b = OfferInfo.newBuilder()
      b.setOfferId(offerId.toString)

      offerType match {
        case sell: SellOffer => b.setSellOffer(sell)
        case rent: RentOffer => b.setRentOffer(rent)
        case _ => throw new IllegalStateException("Unknown offerType")
      }

      b.setPartnerId(partnerId.toString)
      b.setOfferServicesInfo(offerServicesInfo)

      offerCategory match {
        case roomInfo: RoomInfo => b.setRoomInfo(roomInfo)
        case apartmentInfo: ApartmentInfo => b.setApartmentInfo(apartmentInfo)
        case houseInfo: HouseInfo => b.setHouseInfo(houseInfo)
        case lotInfo: LotInfo => b.setLotInfo(lotInfo)
        case commercialInfo: CommercialInfo => b.setCommercialInfo(commercialInfo)
        case garageInfo: GarageInfo => b.setGarageInfo(garageInfo)
        case _ => throw new IllegalStateException("Unknown offerCategory")
      }

      b.setRevoked(revoked)

      b.build()
    }
  }

  val phoneInfoGen: Gen[PhoneInfo] = {
    for {
      phoneNumber <- Gen.chooseNum(1000000, 9999999)
      redirectId <- Gen.posNum[Long]
    } yield {
      PhoneInfo
        .newBuilder()
        .setPhone(Phone.newBuilder().setWholePhoneNumber(phoneNumber.toString))
        .setPhoneRedirectId(redirectId.toString)
        .build()
    }
  }

  val siteInfoGen: Gen[SiteInfo] = {
    for {
      siteId <- Gen.posNum[Long]
    } yield {
      SiteInfo
        .newBuilder()
        .setSiteId(siteId.toString)
        .build()
    }
  }

  val villageInfoGen: Gen[VillageInfo] = {
    for {
      villageId <- Gen.posNum[Long]
    } yield {
      VillageInfo
        .newBuilder()
        .setVillageId(villageId.toString)
        .build()
    }
  }

  val locationInfoGen: Gen[LocationInfo] = {
    for {
      regionGraphId <- Gen.posNum[Long]
      subjectFederationId <- Gen.posNum[Int]
    } yield {
      LocationInfo
        .newBuilder()
        .setRgid(regionGraphId)
        .setSubjectFederationId(subjectFederationId)
        .build()
    }
  }

  val objectInfoGen: Gen[ObjectInfo] = {
    for {
      offerInfo <- Gen.option(offerInfoGen)
      phoneInfo <- Gen.option(phoneInfoGen)
      commercialSegmentEntity <- Gen.option(Gen.oneOf(siteInfoGen, villageInfoGen))
      locationInfo <- locationInfoGen
    } yield {
      val b = ObjectInfo.newBuilder()
      offerInfo.foreach(b.setOfferInfo)
      phoneInfo.foreach(b.setPhoneInfo)
      commercialSegmentEntity match {
        case Some(siteInfo: SiteInfo) => b.setSiteInfo(siteInfo)
        case Some(villageInfo: VillageInfo) => b.setVillageInfo(villageInfo)
        case Some(_) => throw new IllegalStateException("Unsupported commercial object info")
        case None =>
      }
      b.setLocation(locationInfo)
      b.build
    }
  }

  val queryInfoGen: Gen[QueryInfo] = {
    for {
      queryId <- Gen.posNum[Long]
      queryText <- readableString
    } yield {
      QueryInfo
        .newBuilder()
        .setQueryId(queryId.toString)
        .setQueryText(queryText)
        .build()
    }
  }

  val appRefererGen: Gen[AppReferer] = {
    for (screen <- protoEnumWithUnknown(MobileScreenNamespace.MobileScreen.values())) yield {
      AppReferer
        .newBuilder()
        .setRefererScreen(screen)
        .build()
    }
  }

  val requestContextGen: Gen[RequestContext] = {
    for {
      queryInfo <- queryInfoGen
      cardViewType <- protoEnumWithUnknown(CardViewTypeNamespace.CardViewType.values())
      pageNum <- posNum[Int]
      offerPosition <- posNum[Int]
      eventPlace <- protoEnumWithUnknown(EventPlaceNamespace.EventPlace.values())
      referer <- Gen.oneOf(readableString, appRefererGen)
    } yield {
      val b = RequestContext
        .newBuilder()
        .setQueryInfo(queryInfo)
        .setCardViewType(cardViewType)
        .setPageNumber(pageNum)
        .setAbsoluteOfferPosition(offerPosition)
        .setEventPlace(eventPlace)

      referer match {
        case webReferer: String => b.setWebReferer(webReferer)
        case appReferer: AppReferer => b.setMobileReferer(appReferer)
        case _ => throw new IllegalStateException("Unknown referer")
      }

      b.build()
    }
  }

  val webUserInfoGen: Gen[WebUserInfo] = {
    for {
      yandexUid <- readableString
      bucket <- Gen.posNum[Int]
      passportBucket <- Gen.posNum[Int]
      userAgent <- readableString
    } yield {
      WebUserInfo
        .newBuilder()
        .setUserYandexUid(yandexUid)
        .setYandexuidBucket(bucket)
        .setPassportBucket(passportBucket)
        .setUserAgent(userAgent)
        .build()
    }
  }

  val appUserInfoGen: Gen[AppUserInfo] = {
    for (mobileUuid <- readableString) yield {
      AppUserInfo
        .newBuilder()
        .setMobileUuid(mobileUuid)
        .build()
    }
  }

  val userInfoGen: Gen[UserInfo] = {
    for {
      rid <- Gen.posNum[Int]
      portalRid <- Gen.posNum[Int]
      userUid <- Gen.posNum[Int]
      userIp <- Gen.posNum[Int]
      userIsInYandex <- bool
      extClientInfo <- Gen.oneOf(webUserInfoGen, appUserInfoGen)
    } yield {
      val b = UserInfo
        .newBuilder()
        .setRid(rid)
        .setPortalRid(portalRid)
        .setUserUid(userUid.toString)
        .setUserIp(userIp.toString)
        .setUserIsInYandex(userIsInYandex)

      extClientInfo match {
        case webUserInfo: WebUserInfo => b.setWebUserInfo(webUserInfo)
        case appUserInfo: AppUserInfo => b.setAppUserInfo(appUserInfo)
        case _ => throw new IllegalStateException("Unknown ext_client_info")
      }

      b.build()
    }
  }

  val trafficSourceInfoGen: Gen[TrafficSourceInfo] = {
    for {
      trafficFrom <- readableString
      campaign <- readableString
      content <- readableString
      medium <- readableString
      source <- readableString
      term <- readableString
    } yield {
      TrafficSourceInfo
        .newBuilder()
        .setTrafficFrom(trafficFrom)
        .setUtmCampaign(campaign)
        .setUtmContent(content)
        .setUtmMedium(medium)
        .setUtmSource(source)
        .setUtmTerm(term)
        .build()
    }
  }

  val eventGen: Gen[Event] = {
    for {
      version <- Gen.posNum[Int]
      clientInfo <- clientInfoGen
      objectInfo <- objectInfoGen
      requestContext <- requestContextGen
      userInfo <- userInfoGen
      trafficSourceInfo <- trafficSourceInfoGen
    } yield {
      Event
        .newBuilder()
        .setVersion(version)
        .setClientInfo(clientInfo)
        .setObjectInfo(objectInfo)
        .setRequestContext(requestContext)
        .setUserInfo(userInfo)
        .setTrafficSource(trafficSourceInfo)
        .build()
    }
  }

  val eventBatchGen: Gen[EventBatch] = {
    for (events <- Gen.listOf(eventGen)) yield {
      EventBatch
        .newBuilder()
        .addAllEvents(events.asJava)
        .build()
    }
  }

  val rentFrontEvent: Gen[RentFrontEvent] = {
    for {
      clientInfo <- clientInfoGen
      userInfo <- userInfoGen
    } yield RentFrontEvent.newBuilder().setClientInfo(clientInfo).setUserInfo(userInfo).build()
  }

  val rentFrontEventBatch: Gen[RentFrontEventBatch] = {
    for (events <- Gen.listOf(rentFrontEvent))
      yield RentFrontEventBatch.newBuilder().addAllEvents(events.asJava).build()
  }
}

object RealtyEventModelGen extends RealtyEventModelGen
