package ru.yandex.realty.event

import java.net.InetAddress

import com.google.common.net.InetAddresses
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalacheck.Gen.option
import ru.yandex.realty.event.Protobuf._
import ru.yandex.realty.event.model.VertisEvent._
import ru.yandex.realty.event.model.VertisRequestContext

/**
  * Created by Sergey Kozlov <slider5@yandex-team.ru> on 28.12.2018
  */
object ModelGen {

  val Ip4Gen: Gen[InetAddress] =
    Gen.choose(Int.MinValue, Int.MaxValue).map(InetAddresses.fromInteger)

  val Ip6Gen: Gen[InetAddress] =
    Gen
      .listOfN(16, Gen.choose(Byte.MinValue, Byte.MaxValue))
      .map(bytes => InetAddress.getByAddress(bytes.toArray))

  val SourceIpGen: Gen[InetAddress] = Gen.oneOf(Ip4Gen, Ip6Gen)

  val VertisRequestContextGen: Gen[VertisRequestContext] =
    for {
      sourceIp <- option(SourceIpGen)
      userAgent <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      deviceId <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      platform <- option(Gen.oneOf(AndroidPlatform, IosPlatform, DesktopPlatform, MobilePlatform))
      mobileAdvertisingId <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      (gaid, idfa) = platform match {
        case Some(AndroidPlatform) => (mobileAdvertisingId, None)
        case Some(IosPlatform) => (None, mobileAdvertisingId)
        case _ => (None, None)
      }
      userId <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      yandexUserId <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      yandexUid <- option(Gen.numStr.filter(_.nonEmpty))
      fingerprint <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      suid <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      metricaDeviceId <- option(Gen.alphaNumStr.filter(_.nonEmpty))
      androidId <- option(Gen.alphaNumStr.filter(_.nonEmpty))
    } yield VertisRequestContext(
      sourceIp = sourceIp,
      userAgent = userAgent,
      deviceId = deviceId,
      platform = platform,
      userId = userId,
      yandexUserId = yandexUserId,
      yandexUid = yandexUid,
      fingerprint = fingerprint,
      suid = suid,
      gaid = gaid,
      idfa = idfa,
      metricaDeviceId = metricaDeviceId,
      androidId = androidId
    )

  val OfferGen: Gen[Offer] = for {
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
    phones <- Gen.listOf(Gen.numStr)
  } yield Offer(id, phones)

  val UserGen: Gen[User] = for {
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
    phones <- Gen.listOf(Gen.numStr)
  } yield User(id, phones)

  val DateTimeGen: Gen[DateTime] = for {
    stepBack <- Gen.chooseNum(1000, 100000)
  } yield new DateTime(stepBack * Protobuf.MillisInSecond)

  val CreateOfferEventGen: Gen[CreateOfferEvent] = for {
    offer <- OfferGen
    vertisRequestContext <- VertisRequestContextGen
    dateTime <- DateTimeGen
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
  } yield CreateOfferEvent(offer = offer, vertisRequestContext = vertisRequestContext, dateTime = dateTime, id = id)

  val UpdateOfferEventGen: Gen[UpdateOfferEvent] = for {
    offer <- OfferGen
    vertisRequestContext <- VertisRequestContextGen
    dateTime <- DateTimeGen
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
  } yield UpdateOfferEvent(offer = offer, vertisRequestContext = vertisRequestContext, dateTime = dateTime, id = id)

  val DeleteOfferEventGen: Gen[DeleteOfferEvent] = for {
    offer <- OfferGen
    vertisRequestContext <- VertisRequestContextGen
    dateTime <- DateTimeGen
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
  } yield DeleteOfferEvent(offer = offer, vertisRequestContext = vertisRequestContext, dateTime = dateTime, id = id)

  val CreateUserEventGen: Gen[CreateUserEvent] = for {
    user <- UserGen
    vertisRequestContext <- VertisRequestContextGen
    dateTime <- DateTimeGen
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
  } yield CreateUserEvent(user = user, vertisRequestContext = vertisRequestContext, dateTime = dateTime, id = id)

  val UpdateUserEventGen: Gen[UpdateUserEvent] = for {
    user <- UserGen
    vertisRequestContext <- VertisRequestContextGen
    dateTime <- DateTimeGen
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
  } yield UpdateUserEvent(user = user, vertisRequestContext = vertisRequestContext, dateTime = dateTime, id = id)

  val AuthorisationUserEventGen: Gen[AuthorisationUserEvent] = for {
    user <- UserGen
    vertisRequestContext <- VertisRequestContextGen
    dateTime <- DateTimeGen
    id <- Gen.alphaNumStr.filter(_.nonEmpty)
  } yield AuthorisationUserEvent(user = user, vertisRequestContext = vertisRequestContext, dateTime = dateTime, id = id)
}
