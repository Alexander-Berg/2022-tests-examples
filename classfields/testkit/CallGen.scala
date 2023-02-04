package auto.dealers.calltracking.model.testkit

import java.time.Instant
import java.util.concurrent.TimeUnit

import common.zio.testkit.CommonGen._
import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.calltracking.model.Call._
import auto.dealers.calltracking.model.ExternalId._
import auto.dealers.calltracking.model.{Call, ExternalId}
import ru.auto.calltracking.proto.model.Call.CallResult
import ru.yandex.vertis.billing.model.Resolution.Status
import zio.random.Random
import zio.test.{Gen, Sized}

import scala.concurrent.duration.FiniteDuration

object CallGen {

  val anyExternalId: Gen[Random with Sized, ExternalId] =
    Gen.oneOf(
      anyString1.map(_.take(30)).map(TeleponyId),
      anyString1.map(_.take(30)).map(CallkeeperId)
    )

  val anyPhoneNumber: Gen[Random, String] =
    Gen.long(70000000000L, 79999999999L).map(number => s"+$number")

  val anyDuration: Gen[Random, FiniteDuration] =
    Gen.int(0, Int.MaxValue).map(FiniteDuration(_, TimeUnit.SECONDS))

  val anyCallResult: Gen[Any, CallResult] =
    Gen.fromIterable(CallResult.values)

  val anyTag: Gen[Random with Sized, String] = anyString.map(_.take(128))

  val anySection: Gen[Any, Section] = Gen.fromIterable(Section.values.filter(_ != Section.SECTION_UNKNOWN))
  val anyCategory: Gen[Any, Category] = Gen.fromIterable(Category.values.filter(_ != Category.CATEGORY_UNKNOWN))
  val anyOfferId: Gen[Random with Sized, String] = anyString1.map(_.take(32))
  val anyVinCode: Gen[Random with Sized, String] = Gen.alphaNumericString.map(_.take(17))
  val anyBodyType: Gen[Random with Sized, String] = anyString
  val anyTransmission: Gen[Random with Sized, String] = anyString
  val anyYear: Gen[Random, Int] = Gen.int(1950, 2020)
  val anyPrice: Gen[Random, Int] = Gen.int(0, Int.MaxValue)

  val anyCarInfo: Gen[Random with Sized, CarInfo] = for {
    mark <- anyString1
    model <- anyString1
    superGen <- Gen.anyLong
    bodyType <- anyString1
    transmission <- anyString1
  } yield CarInfo(mark, model, superGen, bodyType, transmission)

  val anyAutoInfo: Gen[Random with Sized, AutoInfo] = Gen.oneOf(
    anyCarInfo,
    anyString1.zipWith(anyString1)(MotoInfo),
    anyString1.zipWith(anyString1)(TruckInfo)
  )

  val anyOfferInfo: Gen[Random with Sized, OfferInfo] = {
    for {
      id <- anyOfferId
      section <- anySection
      autoInfo <- anyAutoInfo
      year <- anyYear
      price <- anyPrice
      vin <- Gen.option(anyVinCode)
    } yield OfferInfo(id, section, autoInfo, year, price, vin)
  }

  val anyBilling: Gen[Random with Sized, Billing] =
    Gen.anyLong.zipWith(Gen.anyLong)(Billing)

  def anyTelepony(
      domain: Gen[Random with Sized, String] = anyString1,
      objectId: Gen[Random with Sized, String] = anyString1,
      tag: Gen[Random with Sized, String] = anyString,
      hasRecord: Gen[Random with Sized, Boolean] = Gen.boolean): Gen[Random with Sized, Telepony] = {
    for {
      domain <- domain
      objectId <- objectId
      tag <- tag
      hasRecord <- hasRecord
    } yield Telepony(
      domain,
      objectId,
      tag,
      hasRecord
    )
  }

  val anyComplaint: Gen[Random with Sized, Complaint] =
    anyInstant.map(Complaint)

  val anyResolution: Gen[Random with Sized, Resolution] =
    anyString.zipWith(Gen.fromIterable(Status.values))(Resolution)

  val anyResolutionVector: Gen[Random with Sized, ResolutionVector] =
    Gen.option(anyResolution).zipWith(Gen.option(anyResolution))(ResolutionVector)

  val anyPlatform: Gen[Random with Sized, String] = anyString1.map(_.take(256))

  def anyCall(
      id: Gen[Random with Sized, Long] = Gen.anyLong,
      externalId: Gen[Random with Sized, ExternalId] = anyExternalId,
      clientId: Gen[Random with Sized, Long] = Gen.anyLong,
      created: Gen[Random with Sized, Instant] = anyInstant,
      callTime: Gen[Random with Sized, Instant] = anyInstant,
      callResult: Gen[Random with Sized, CallResult] = anyCallResult,
      isUniq: Gen[Random, Boolean] = Gen.boolean,
      isRelevant: Gen[Random, Option[Boolean]] = Gen.option(Gen.boolean),
      transcriptionAvailable: Gen[Random, Boolean] = Gen.boolean,
      isCallback: Gen[Random, Boolean] = Gen.boolean,
      redirectPhone: Gen[Random, Option[String]] = Gen.option(anyPhoneNumber),
      tags: Gen[Random with Sized, Set[String]] = Gen.listOf(anyTag).map(_.toSet),
      callDuration: Gen[Random, FiniteDuration] = anyDuration,
      talkDuration: Gen[Random, FiniteDuration] = anyDuration,
      sourcePhone: Gen[Random, Option[String]] = Gen.option(anyPhoneNumber),
      targetPhone: Gen[Random, String] = anyPhoneNumber,
      clusterHeadCall: Gen[Random with Sized, Option[ExternalId]] = Gen.option(anyExternalId),
      previousSourceCall: Gen[Random with Sized, Option[ExternalId]] = Gen.option(anyExternalId),
      nextSourceCall: Gen[Random with Sized, Option[ExternalId]] = Gen.option(anyExternalId),
      category: Gen[Random with Sized, Option[Category]] = Gen.option(anyCategory),
      section: Gen[Random with Sized, Option[Section]] = Gen.option(anySection),
      offerInfo: Gen[Random with Sized, Option[OfferInfo]] = Gen.option(anyOfferInfo),
      billing: Gen[Random with Sized, Option[Billing]] = Gen.option(anyBilling),
      telepony: Gen[Random with Sized, Option[Telepony]] = Gen.option(anyTelepony()),
      complaint: Gen[Random with Sized, Option[Complaint]] = Gen.option(anyComplaint),
      resolution: Gen[Random with Sized, Option[ResolutionVector]] = Gen.option(anyResolutionVector),
      platform: Gen[Random with Sized, Option[String]] = Gen.option(anyPlatform)): Gen[Random with Sized, Call] =
    for {
      id <- id
      externalId <- externalId
      clientId <- clientId
      created <- created
      callTime <- callTime
      callResult <- callResult
      isUniq <- isUniq
      isRelevant <- isRelevant
      transcriptionAvailable <- transcriptionAvailable
      isCallback <- isCallback
      tags <- tags
      callDuration <- callDuration
      talkDuration <- talkDuration
      redirectPhone <- redirectPhone
      sourcePhone <- sourcePhone
      targetPhone <- targetPhone
      clusterHeadCall <- clusterHeadCall
      previousSourceCall <- previousSourceCall
      nextSourceCall <- nextSourceCall
      category <- category
      section <- section
      offerInfo <- offerInfo
      billing <- billing
      telepony <- telepony
      complaint <- complaint
      resolution <- resolution
      platform <- platform
    } yield Call(
      id,
      externalId,
      clientId,
      created,
      callTime,
      callResult,
      isUniq,
      isRelevant,
      transcriptionAvailable,
      isCallback,
      tags,
      redirectPhone,
      callDuration,
      talkDuration,
      sourcePhone,
      targetPhone,
      clusterHeadCall,
      previousSourceCall,
      nextSourceCall,
      offerInfo.map(_.category).orElse(category),
      offerInfo.map(_.section).orElse(section),
      offerInfo,
      billing,
      telepony,
      complaint,
      resolution.filter(_.nonEmpty),
      platform
    )

  val anyCall: Gen[Random with Sized, Call] = anyCall()
}
