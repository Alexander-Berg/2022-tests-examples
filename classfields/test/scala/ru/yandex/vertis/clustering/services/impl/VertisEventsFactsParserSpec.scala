package ru.yandex.vertis.clustering.services.impl

import java.time.{Instant, ZonedDateTime}

import com.google.protobuf.Timestamp
import com.google.protobuf.util.JsonFormat
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.realty.proto.offer.Model.OfferStub
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.derivatives.DerivativeFactsBuilderImpl
import ru.yandex.vertis.clustering.derivatives.impl.BuilderDerivativeFeaturesBuilder
import ru.yandex.vertis.clustering.model._
import ru.yandex.vertis.clustering.services.impl.SourceFactsFilter._
import ru.yandex.vertis.clustering.services.impl.VertisEventsFactsParserSpec._
import ru.yandex.vertis.clustering.utils.DateTimeUtils._
import ru.yandex.vertis.clustering.utils.features.FeatureHelpers
import ru.yandex.vertis.events.{User => ProtoUser, _}
import ru.yandex.vertis.{Domain, RequestContext}

import scala.collection.JavaConverters._

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class VertisEventsFactsParserSpec extends BaseSpec {

  private val sourceFactsFilter: SourceFactsFilter[Event] = AutoruVertisEventsSourceFactsFilter

  private val derivativeFactsBuilder = new DerivativeFactsBuilderImpl(new BuilderDerivativeFeaturesBuilder)
  private val vertisEventsFactsParser: VertisEventsFactsParser =
    new VertisEventsFactsParser(derivativeFactsBuilder)

  private def parseJson(str: String): Event = {
    val builder = Event.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(str, builder)
    builder.build()
  }

  private val realtyTests: Seq[FactTest] = Seq(
    FactTest(title = "1 - Without userId",
             event = realtyEventOfferCreate(datetimeNow, suid = Some(suid), fingerprint = Some(fingerprint)),
             expected = Seq.empty),
    FactTest(
      title = "2 - Suid & Fingerprint",
      event = realtyEventOfferCreate(datetimeNow,
                                     userId = Some(realtyUser.userId),
                                     suid = Some(suid),
                                     fingerprint = Some(fingerprint)),
      expected = Seq(realtySuidFact, realtyFingerprintFact)
    ),
    FactTest(
      title = "3 - DeviceUid & Ip & IpNet",
      event = realtyEventOfferCreate(datetimeNow,
                                     userId = Some(realtyUser.userId),
                                     deviceUid = Some(deviceUid),
                                     ip = Some(ip)),
      expected = Seq(realtyIpFact, realtyDeviceUidFact, realtyIpNetFact)
    ),
    FactTest(
      title = "4 - UserAgent & YandexUid",
      event = realtyEventOfferCreate(datetimeNow,
                                     userId = Some(realtyUser.userId),
                                     userAgent = Some(userAgent),
                                     yandexUid = Some(yandexUid)),
      expected = Seq(realtyUserAgentFact, realtyYandexUidFact)
    ),
    FactTest(
      title = "5 - YandexUid & Phone & PhoneNet",
      event = realtyEventOfferCreate(datetimeNow,
                                     userId = Some(realtyUser.userId),
                                     phones = Seq(phone1, phone2, "fraud", phone3),
                                     yandexUid = Some(yandexUid)),
      expected = Seq(realtyPhone1Fact, realtyPhone2Fact, realtyYandexUidFact, realtyPhoneNet1Fact, realtyPhoneNet2Fact)
    )
  )

  private val autoruTests: Seq[FactTest] = Seq(
    FactTest(
      title = "1 - Impersonated user",
      event = autoruEventUserAuthorisation(datetimeNow,
                                           userId = Some(autoruUser.userId),
                                           yandexUid = Some(yandexUid),
                                           parentSessionUserId = Some("555")),
      expected = Seq.empty
    ),
    FactTest(
      title = "2 - From Yandex enterprise network",
      event = autoruEventUserAuthorisation(datetime20190101,
                                           userId = Some(autoruUser.userId),
                                           yandexUid = Some(yandexUid),
                                           ip = Some("2a02:6b8:0:2309:5085:8ec8:243e:15fc")),
      expected = Seq.empty
    ),
    FactTest(
      title = "3 - Ordinary user from external network",
      event = autoruEventUserAuthorisation(datetimeNow,
                                           userId = Some(autoruUser.userId),
                                           suid = Some(suid),
                                           ip = Some("8.8.8.8")),
      expected = Seq(
        Fact(autoruUser, Suid(suid), datetimeNow),
        Fact(autoruUser, FeatureHelpers.parseIp("8.8.8.8"), datetimeNow),
        Fact(autoruUser, FeatureHelpers.parseIpNet("8.8.8.255"), datetimeNow)
      )
    ),
    FactTest(
      title = "4 - Payment cards masks",
      event = autoruEventUserActualTiedCard(datetimeNow, autoruUser.userId, Iterable("MASK1", "MASK2")),
      expected = Seq(Fact(autoruUser, PaymentCardMask("MASK1"), datetimeNow),
                     Fact(autoruUser, PaymentCardMask("MASK2"), datetimeNow))
    ),
    FactTest(
      title = "5 - Event from call center",
      event = autoruEventUserAuthorisation(datetime20190101,
                                           userId = Some(autoruUser.userId),
                                           suid = Some(suid),
                                           isCallCenter = true),
      expected = Seq.empty
    )
  )

  "VertisEventsFactsParser" should {

    "Parse fact from json" in {
      val factsStrings = scala.io.Source.fromFile(ClassLoader.getSystemResource("fact.txt").toURI).getLines().toArray
      val event = parseJson(factsStrings(0))
      vertisEventsFactsParser.parse(event).nonEmpty shouldBe true
      (if (sourceFactsFilter(event)) vertisEventsFactsParser.parse(event) else Iterable()).nonEmpty shouldBe false

      val paymentCallcenterEvent = parseJson(factsStrings(1))
      vertisEventsFactsParser.parse(paymentCallcenterEvent).nonEmpty shouldBe true
      sourceFactsFilter(paymentCallcenterEvent) shouldBe false

      val eventWithExternalAuth = parseJson(factsStrings(2))
      val parsedFacts = vertisEventsFactsParser.parse(eventWithExternalAuth)
      val mosruFeatures = parsedFacts.filter(_.feature.`type` == FeatureTypes.MosruExternalAuthType).map(_.feature)
      (mosruFeatures should contain).allOf(MosruExternalAuthId("mosru_auth_111", None),
                                           MosruExternalAuthId("mosru_auth_222", None))
    }

    realtyTests.foreach { test =>
      s"Realty facts parse ${test.title}" in {
        val actual = if (RealtyVertisEventsSourceFactsFilter(test.event)) {
          vertisEventsFactsParser.parse(test.event)
        } else {
          Iterable.empty
        }
        actual.toSet shouldBe test.expected.toSet
      }
    }

    autoruTests.foreach { test =>
      s"Autoru facts parse ${test.title}" in {
        val actual = if (AutoruVertisEventsSourceFactsFilter(test.event)) {
          vertisEventsFactsParser.parse(test.event)
        } else {
          Iterable.empty
        }
        actual.toSet shouldBe test.expected.toSet
      }
    }

    "parseFact with userId in event" in {
      val requestBuilder = RequestContext
        .newBuilder()
        .setDomain(Domain.DOMAIN_REALTY)
        .setSuid(suid)
      val eventBuilder = Event
        .newBuilder()
        .setRequestContext(requestBuilder)
        .setTimestamp(Timestamp.newBuilder().setSeconds(datetimeNow.toEpochSecond))
      val userBuilder = ProtoUser.newBuilder().setId(realtyUser.userId)
      val userAuthorisation = UserAuthorisation.newBuilder().setUser(userBuilder)
      val userEvent = UserEvent.newBuilder().setAuthorisation(userAuthorisation)
      eventBuilder.setUserEvent(userEvent)
      val event = eventBuilder.build()
      val expected = Iterable(Fact(realtyUser, Suid(suid), datetimeNow))
      val actual = vertisEventsFactsParser.parse(event)
      actual shouldBe expected
    }
  }
}

object VertisEventsFactsParserSpec {

  private def realtyEventOfferCreate(datetime: ZonedDateTime,
                                     userId: Option[String] = None,
                                     suid: Option[String] = None,
                                     fingerprint: Option[String] = None,
                                     deviceUid: Option[String] = None,
                                     ip: Option[String] = None,
                                     userAgent: Option[String] = None,
                                     yandexUid: Option[String] = None,
                                     phones: Seq[String] = Seq.empty): Event = {
    val requestBuilder = RequestContext
      .newBuilder()
      .setDomain(Domain.DOMAIN_REALTY)
    userId.foreach(requestBuilder.setUserId)
    suid.foreach(requestBuilder.setSuid)
    fingerprint.foreach(requestBuilder.setFingerprint)
    deviceUid.foreach(requestBuilder.setDeviceId)
    userAgent.foreach(requestBuilder.setUserAgent)
    ip.foreach(requestBuilder.setSourceIp)
    yandexUid.foreach(requestBuilder.setYandexUid)
    val event = Event
      .newBuilder()
      .setRequestContext(requestBuilder)
      .setTimestamp(Timestamp.newBuilder().setSeconds(datetime.toEpochSecond))
    val realtyOffer = OfferStub.newBuilder()
    phones.foreach(realtyOffer.addPhones)
    val offer = Offer.newBuilder().setRealty(realtyOffer)
    val offerCreate = OfferCreate.newBuilder().setOffer(offer)
    val offerEvent = OfferEvent.newBuilder().setCreate(offerCreate)
    event.setOfferEvent(offerEvent)
    event.build()
  }

  private def autoruEventUserAuthorisation(datetime: ZonedDateTime,
                                           userId: Option[String] = None,
                                           suid: Option[String] = None,
                                           fingerprint: Option[String] = None,
                                           deviceUid: Option[String] = None,
                                           ip: Option[String] = None,
                                           userAgent: Option[String] = None,
                                           yandexUid: Option[String] = None,
                                           phones: Seq[String] = Seq.empty,
                                           parentSessionUserId: Option[UserId] = None,
                                           isCallCenter: Boolean = false): Event = {
    val application = if (isCallCenter) "callcenter" else "desktop"
    val requestBuilder = RequestContext
      .newBuilder()
      .setDomain(Domain.DOMAIN_AUTO)
      .setApplication(application)
    userId.foreach(requestBuilder.setUserId)
    suid.foreach(requestBuilder.setSuid)
    fingerprint.foreach(requestBuilder.setFingerprint)
    deviceUid.foreach(requestBuilder.setDeviceId)
    userAgent.foreach(requestBuilder.setUserAgent)
    ip.foreach(requestBuilder.setSourceIp)
    yandexUid.foreach(requestBuilder.setYandexUid)
    parentSessionUserId.foreach(requestBuilder.setParentSessionUserId)
    val event = Event
      .newBuilder()
      .setRequestContext(requestBuilder)
      .setTimestamp(Timestamp.newBuilder().setSeconds(datetime.toEpochSecond))
    val user = ProtoUser.newBuilder.setId(userId.getOrElse("5")).addAllPhones(phones.asJava)
    val userAuthorisation = UserAuthorisation.newBuilder().setUser(user)
    val userEvent = UserEvent.newBuilder().setAuthorisation(userAuthorisation)
    event.setUserEvent(userEvent)
    event.build()
  }

  private def autoruEventUserActualTiedCard(datetime: ZonedDateTime,
                                            userId: UserId,
                                            cardsMasks: Iterable[String]): Event = {
    val requestBuilder = RequestContext
      .newBuilder()
      .setDomain(Domain.DOMAIN_AUTO)
      .setUserId(userId)
    val event = Event
      .newBuilder()
      .setRequestContext(requestBuilder)
      .setTimestamp(Timestamp.newBuilder().setSeconds(datetime.toEpochSecond))
    val user = ProtoUser.newBuilder.setId(userId)
    val tiedCards = cardsMasks.map(TiedCard.newBuilder.setMask(_).setBankerDomain("autoru").build)
    val userActualTiedCard = UserActualTiedCard
      .newBuilder()
      .setUser(user)
      .addAllTiedCards(tiedCards.asJava)
    val userEvent = UserEvent.newBuilder().setActualTiedCard(userActualTiedCard)
    event.setUserEvent(userEvent)
    event.build()
  }

  private val datetimeNow: ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(now.toEpochSecond), DefaultZoneId)
  private val datetime20190101: ZonedDateTime =
    ZonedDateTime.parse("2019-01-01T00:00:00.000+03:00[Europe/Moscow]")

  sealed private case class FactTest(title: String, event: Event, expected: Seq[Fact])

  private val suid = "430e24fcd0bfb6f0518f32fe9cb2f9ac.40e59cacbb4b2d23bdaff6226f8e6afa"
  private val fingerprint = "fingerprintvalue"
  private val deviceUid = "deviceUid"
  private val ip = "10.10.10.10"
  private val userAgent = "yandex-browser"
  private val yandexUid = "4839277111555107652"
  private val phone1 = "+7(800)-1234567"
  private val phone2 = "+88007654321"
  private val phone3 = "+7800"
  private val realtyUser = RealtyUser("1")
  private val autoruUser = AutoruUser("5")

  private val realtySuidFact = Fact(realtyUser, Suid(suid), datetimeNow)
  private val realtyFingerprintFact = Fact(realtyUser, Fingerprint(fingerprint), datetimeNow)
  private val realtyDeviceUidFact = Fact(realtyUser, DeviceUid(deviceUid), datetimeNow)
  private val realtyIpFact = Fact(realtyUser, FeatureHelpers.parseIp(ip), datetimeNow)
  private val realtyIpNetFact = Fact(realtyUser, FeatureHelpers.parseIpNet(ip), datetimeNow)
  private val realtyUserAgentFact = Fact(realtyUser, UserAgent(userAgent), datetimeNow)
  private val realtyPhone1Fact = Fact(realtyUser, FeatureHelpers.parsePhone(phone1), datetimeNow)
  private val realtyPhone2Fact = Fact(realtyUser, FeatureHelpers.parsePhone(phone2), datetimeNow)
  private val realtyPhoneNet1Fact = Fact(realtyUser, FeatureHelpers.parsePhoneNet(phone1), datetimeNow)
  private val realtyPhoneNet2Fact = Fact(realtyUser, FeatureHelpers.parsePhoneNet(phone2), datetimeNow)
  private val realtyYandexUidFact = Fact(realtyUser, YandexUid(yandexUid), datetimeNow)
}
