package ru.yandex.realty.rent.backend

import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.maps.RentPolygon
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.rent.backend.FeedEntryUtils._
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{Flat, OwnerRequest}
import ru.yandex.realty.rent.model.feed.FeedEntry
import ru.yandex.realty.rent.proto.api.moderation.{ClassifiedTypeNamespace, FlatQuestionnaire}
import ru.yandex.realty.rent.proto.api.moderation.ClassifiedTypeNamespace.ClassifiedType._
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Furniture.Internet.InternetTypeNamespace
import ru.yandex.realty.rent.proto.model.flat.ClassifiedRedirectNumber
import ru.yandex.realty.util.protobuf.ProtobufFormats.{DateTimeFormat => ProtoDateTimeFormat}
import ru.yandex.realty.util.BooleanUtils.booleanTable
import ru.yandex.realty.util.Mappings._
import java.time.Instant

import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Payments

import scala.jdk.CollectionConverters._
import scala.xml.{Node, NodeSeq}
import scala.xml.Utility.trim
import scala.xml.XML.loadString

@RunWith(classOf[JUnitRunner])
class XmlYandexFeedBuilderSpec extends WordSpec with Matchers {

  "XmlYandexFeedBuilder.head" should {
    "return correct xml header" in new Wiring with Data {
      builder.head.replaceFirst("""generation-date>.*<""", "generation-date>NONE<") shouldEqual expectedHead
    }
  }

  "XmlYandexFeedBuilder.body" should {
    "return correct xml body" in new Wiring with Data {
      val resultBodyStr: String = builder.body(sampleFeedEntry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      resultBody shouldEqual expectedBody
    }

    "REALTYBACK-6342 use phone=[MSK] for geoId=[MSK]" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat.copy(
          unifiedAddress = None,
          data = sampleFeedEntry.flat.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
            )
            .build()
        )
      )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val phoneTag: NodeSeq = resultBody \ "sales-agent" \ "phone"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.yandex
    }

    "REALTYBACK-6342 use phone=[SPB] for geoId=[SPB]" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat.copy(
          unifiedAddress = None,
          data = sampleFeedEntry.flat.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.SPB_AND_LEN_OBLAST)
                .setSubjectFederationRgid(NodeRgid.SPB_AND_LEN_OBLAST)
            )
            .build()
        )
      )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val phoneTag: NodeSeq = resultBody \ "sales-agent" \ "phone"
      phoneTag.text shouldEqual RentPolygon.SPB.phone.yandex
    }

    "REALTYBACK-6342 use default phone=[MSK] for unsupported geoId" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat.copy(
          unifiedAddress = None,
          data = sampleFeedEntry.flat.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(123)
                .setSubjectFederationRgid(123)
            )
            .build()
        )
      )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val phoneTag: NodeSeq = resultBody \ "sales-agent" \ "phone"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.yandex
    }

    "REALTYBACK-6250 returns [true] for [utilities-included] when the one of more selected (exclude all)" in new Wiring
    with Data {

      val payments: Iterable[FlatQuestionnaire.Payments] =
        booleanTable(9).tail // skip 0,0,0..,0 row of zeros
          .map {
            case Vector(electric, water, sanitation, gas, heating, inet, parking, barrier, concierge) =>
              FlatQuestionnaire.Payments
                .newBuilder()
                .setElectricity(Payments.Electricity.newBuilder().setNeedPayment(electric))
                .setWater(Payments.Water.newBuilder().setNeedPayment(water))
                .setSanitation(Payments.Sanitation.newBuilder().setNeedPayment(sanitation))
                .setGas(Payments.Gas.newBuilder().setNeedPayment(gas))
                .setHeating(Payments.Heating.newBuilder().setNeedPayment(heating))
                .setInternet(Payments.Internet.newBuilder().setNeedPayment(inet))
                .setBarrier(Payments.Barrier.newBuilder().setNeedPayment(parking))
                .setParking(Payments.Parking.newBuilder().setNeedPayment(barrier))
                .setConcierge(Payments.Concierge.newBuilder().setNeedPayment(concierge))
                .setAllReceipt(Payments.AllReceipt.newBuilder().setNeedPayment(false))
                .build()
          }

      val entries: Iterable[FeedEntry] = payments.map(sampleFeedEntry.setPayments)

      entries foreach { entry =>
        val resultBodyStr: String = builder.body(entry)
        val resultBody: Node = trim(loadString(resultBodyStr))
        val utilitiesIncludedTag: NodeSeq = resultBody \ "utilities-included"
        utilitiesIncludedTag.text shouldEqual "да"
      }

    }

    //scalastyle:off
    "REALTYBACK-6250 returns [false] for [utilities-included] when the one of more payments are selected (exclude all checkbox)" in new Wiring
    with Data {

      val payments: Iterable[FlatQuestionnaire.Payments] =
        booleanTable(9)
          .map {
            case Vector(electric, water, sanitation, gas, heating, inet, parking, barrier, concierge) =>
              FlatQuestionnaire.Payments
                .newBuilder()
                .setElectricity(Payments.Electricity.newBuilder().setNeedPayment(electric))
                .setWater(Payments.Water.newBuilder().setNeedPayment(water))
                .setSanitation(Payments.Sanitation.newBuilder().setNeedPayment(sanitation))
                .setGas(Payments.Gas.newBuilder().setNeedPayment(gas))
                .setHeating(Payments.Heating.newBuilder().setNeedPayment(heating))
                .setInternet(Payments.Internet.newBuilder().setNeedPayment(inet))
                .setBarrier(Payments.Barrier.newBuilder().setNeedPayment(parking))
                .setParking(Payments.Parking.newBuilder().setNeedPayment(barrier))
                .setConcierge(Payments.Concierge.newBuilder().setNeedPayment(concierge))
                .setAllReceipt(Payments.AllReceipt.newBuilder().setNeedPayment(true))
                .build()
          }

      val entries: Iterable[FeedEntry] = payments.map(sampleFeedEntry.setPayments)

      entries foreach { entry =>
        val resultBodyStr: String = builder.body(entry)
        val resultBody: Node = trim(loadString(resultBodyStr))
        val utilitiesIncludedTag: NodeSeq = resultBody \ "utilities-included"
        utilitiesIncludedTag.text shouldEqual "нет"
      }

    }
    //scalastyle:on

    "REALTYBACK-6250 returns [true] for [internet] type HAS_PROVIDER" in new Wiring with Data {
      val types = Seq(InternetTypeNamespace.InternetType.HAS_PROVIDER)
      val entries: Seq[FeedEntry] = types.map(sampleFeedEntry.setInternetType)

      entries foreach { entry =>
        val resultBodyStr: String = builder.body(entry)
        val resultBody: Node = trim(loadString(resultBodyStr))
        val internetTag: NodeSeq = resultBody \ "internet"
        internetTag.text shouldEqual "да"
      }
    }

    "REALTYBACK-6250 returns [false] for other [internet] types" in new Wiring with Data {
      val types = Seq(InternetTypeNamespace.InternetType.NO_CABLE, InternetTypeNamespace.InternetType.ONLY_CABLE)
      val entries: Seq[FeedEntry] = types.map(sampleFeedEntry.setInternetType)

      entries foreach { entry =>
        val resultBodyStr: String = builder.body(entry)
        val resultBody: Node = trim(loadString(resultBodyStr))
        val internetTag: NodeSeq = resultBody \ "internet"
        internetTag.text shouldEqual "нет"
      }
    }

    "REALTYBACK-6790 use different phones for different geo ids" in new Wiring with Data {
      RentPolygon.values.foreach { p =>
        val entry = sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .applySideEffect(
                _.getLocationBuilder
                  .setSubjectFederationGeoid(p.subjectFederationId)
                  .setSubjectFederationRgid(p.rgId)
              )
              .build()
          )
        )

        val resultBodyStr: String = builder.body(entry)
        val resultBody: Node = trim(loadString(resultBodyStr))
        val phoneTag: NodeSeq = resultBody \ "sales-agent" \ "phone"
        phoneTag.text shouldEqual p.phone.yandex
      }
    }

    "use redirect number if it is present in flat" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat.copy(
          data = sampleFeedEntry.flat.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
            )
            .addAllRedirectNumbers(validRedirectNumbers.asJava)
            .build()
        )
      )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val phoneTag: NodeSeq = resultBody \ "sales-agent" \ "phone"
      phoneTag.text shouldEqual yandexRedirectNumber
    }

    "do not use redirect if it is incorrect" in new Wiring with Data {
      val incorrectNumber = "+7111222334"
      val redirects = validRedirectNumbers.filter(_.getClassifiedType != YANDEX_REALTY) ++ validRedirectNumbers
        .find(_.getClassifiedType == YANDEX_REALTY)
        .map(wrapper => wrapper.toBuilder.setRedirect(wrapper.getRedirect.toBuilder.setSource(incorrectNumber)).build())

      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat.copy(
          data = sampleFeedEntry.flat.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
            )
            .addAllRedirectNumbers(redirects.asJava)
            .build()
        )
      )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val phoneTag: NodeSeq = resultBody \ "sales-agent" \ "phone"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.yandex
    }

    "do not use redirect if deadline is soon" in new Wiring with Data {
      val soon = Instant.now().plus(java.time.Duration.ofHours(1)).toEpochMilli
      val redirects = validRedirectNumbers.filter(_.getClassifiedType != YANDEX_REALTY) ++ validRedirectNumbers
        .find(_.getClassifiedType == YANDEX_REALTY)
        .map(wrapper => wrapper.toBuilder.setRedirect(wrapper.getRedirect.toBuilder.setDeadline(soon)).build())

      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat.copy(
          data = sampleFeedEntry.flat.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
            )
            .addAllRedirectNumbers(redirects.asJava)
            .build()
        )
      )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val phoneTag: NodeSeq = resultBody \ "sales-agent" \ "phone"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.yandex
    }

    "generate temporary price if it exists in questionnaire" in new Wiring with Data {
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .applySideEffect(_.getPaymentsBuilder.setTemporaryPeriodMonths(3).setTemporaryAdValue(2500000))
              .build()
          )
        )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val temporaryPriceTag: NodeSeq = resultBody \ "temporary-price" \ "value"
      val temporaryPriceDuration: NodeSeq = resultBody \ "temporary-price" \ "duration"
      temporaryPriceTag.text shouldEqual (entry.questionnaire.data.getPayments.getTemporaryAdValue / 100).toString
      temporaryPriceDuration.text shouldEqual entry.questionnaire.data.getPayments.getTemporaryPeriodMonths.toString
    }

    "generate description with possible check in date" in new Wiring with Data {
      val checkInDate = "26.05.2050"
      val checkInDateTime = DateTimeFormat.forPattern("dd.MM.yyyy").parseDateTime(checkInDate)
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setOfferCopyright("1\n2\n")
              .applySideEffect(_.setPossibleCheckInDate(ProtoDateTimeFormat.write(checkInDateTime)))
              .build()
          )
        )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val description: NodeSeq = resultBody \ "description"
      val offerCopyright = entry.questionnaire.data.getOfferCopyright
      val expectedDescription = s"Заезд возможен с $checkInDate\n \n$offerCopyright"
      description.head.text shouldEqual expectedDescription
        .replace("\n \n", " ")
        .replace("\n", " ")
        .trim
      resultBodyStr.contains(expectedDescription) shouldBe (true)
    }
  }

  "XmlYandexFeedBuilder.tail" should {
    "return correct xml tail" in new Wiring with Data {
      builder.tail shouldEqual expectedTail
    }
  }

  "XmlYandexFeedBuilder.entryDocument" should {
    "return correct feed of one element" in new Wiring with Data {
      val resultStr: String =
        builder.entryDocument(sampleFeedEntry).replaceFirst("""generation-date>.*<""", "generation-date>NONE<")
      val result: Node = trim(loadString(resultStr))
      result shouldEqual expectedDocument
    }
  }

  "XmlYandexFeedBuilder.classifiedType" should {
    "return Yandex classified type" in new Wiring with Data {
      builder.classifiedType shouldEqual ClassifiedTypeNamespace.ClassifiedType.YANDEX_REALTY
    }
  }

  trait Wiring {
    val mdsBuilder = new MdsUrlBuilder("//localhost:80")
    val builder = new XmlYandexFeedBuilder(mdsBuilder)
  }

  trait Data extends RentModelsGen {
    this: Wiring =>

    val MSK_PHONE: String = RentPolygon.MSK.phone.yandex

    val sampleFeedEntry: FeedEntry = feedEntryGen.next.applyTransform { e =>
      e.copy(
        flat = e.flat.copy(
          data = e.flat.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
            )
            .build()
        )
      )
    }

    val f: Flat = sampleFeedEntry.flat
    val or: OwnerRequest = sampleFeedEntry.ownerRequest
    val q: FlatQuestionnaire = sampleFeedEntry.questionnaire.data

    val expectedHead: String =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<realty-feed xmlns="http://webmaster.yandex.ru/schemas/feed/realty/2010-06">
        |  <generation-date>NONE</generation-date>
        |""".stripMargin

    val expectedBodyStr: String = {
      s"""
         |<offer internal-id="${or.ownerRequestId}">
         |    <type>аренда</type>
         |    <property-type>жилая</property-type>
         |    <category>квартира</category>
         |    <creation-date>${builder.dateTimeFormatter.print(f.createTime)}</creation-date>
         |    <url>https://arenda.realty.yandex.ru/management/manager/flat/${f.flatId}/</url>
         |    <location>
         |        <country></country>
         |        <region></region>
         |
         |        <address>${f.data.getLocation.getAddress}</address>
         |        <apartment>${f.flatNumber}</apartment>
         |        <latitude>${f.data.getLocation.getLatitude}</latitude>
         |        <longitude>${f.data.getLocation.getLongitude}</longitude>
         |    </location>
         |    <sales-agent>
         |        <phone>$MSK_PHONE</phone>
         |        <category>agency</category>
         |        <photo>https://avatars.mds.yandex.net/get-yapic/43473/VngUodMEwWGhaahbTiiYtaqFFfI-1/islands-retina-middle
         |        </photo>
         |        <organization>Яндекс.Аренда</organization>
         |    </sales-agent>
         |
         |
         |    <price>
         |        <value>${q.getPayments.getAdValue / 100}</value>
         |        <currency>RUB</currency>
         |        <period>месяц</period>
         |    </price>
         |
         |    <rent-pledge>0</rent-pledge>
         |    <prepayment>0</prepayment>
         |    <agent-fee>0</agent-fee>
         |    <utilities-included>нет</utilities-included>
         |    <area>
         |        <value>${q.getFlat.getArea}</value>
         |        <unit>кв. м</unit>
         |    </area>
         |
         |    <virtual-tour>
         |        <provider>matterport</provider>
         |        <model-url></model-url>
         |    </virtual-tour>
         |
         |    <image>https://localhost:80/get-namespace1/1001/name1/yandex_1024x1024?arenda=</image>
         |    <image>https://localhost:80/get-namespace2/1002/name2/yandex_1024x1024?arenda=</image>
         |    <image>https://localhost:80/get-namespace3/1003/name3/yandex_1024x1024?arenda=</image>
         |    <image>https://localhost:80/get-namespace4/1004/name4/yandex_1024x1024?arenda=</image>
         |    <is-image-order-change-allowed>0</is-image-order-change-allowed>
         |    <renovation>евроремонт</renovation>
         |    <rooms>2</rooms>
         |
         |    <floor>${q.getFlat.getFloor}</floor>
         |    <floors-total>${q.getBuilding.getFloors}</floors-total>
         |
         |
         |    <description>${q.getOfferCopyright}</description>
         |    <apartments>false</apartments>
         |    <room-furniture>да</room-furniture>
         |    <window-view>на улицу</window-view>
         |    <balcony>${builder.balconyDeclension(q.getFlat.getBalcony.getBalconyAmount)}</balcony>
         |    <balcony>${builder.loggiaDeclension(q.getFlat.getBalcony.getLoggiaAmount)}</balcony>
         |    <bathroom-unit>2</bathroom-unit>
         |    <air-conditioner>да</air-conditioner>
         |    <internet>да</internet>
         |
         |    <television>да</television>
         |    <washing-machine>да</washing-machine>
         |    <dishwasher>да</dishwasher>
         |    <refrigerator>да</refrigerator>
         |    <lift>да</lift>
         |    <rubbish-chute>да</rubbish-chute>
         |
         |    <with-pets>нет</with-pets>
         |
         |
         |    <vas>premium</vas>
         |    <vas>promotion</vas>
         |    <vas start-time="2021-08-01T08:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T10:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T11:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T12:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T13:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T14:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T16:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T18:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T19:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T20:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T21:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T22:00:00+03:00" schedule="everyday">raise</vas>
         |    <vas start-time="2021-08-01T00:00:00+03:00" schedule="everyday">raise</vas>
         |
         |</offer>
         |""".stripMargin
    }
    lazy val expectedBody: Node = trim(loadString(expectedBodyStr))

    val expectedTail = "</realty-feed>"

    val expectedDocumentStr: String =
      s"""$expectedHead
         |$expectedBodyStr
         |$expectedTail
         |""".stripMargin

    lazy val expectedDocument: Node = trim(loadString(expectedDocumentStr))

    val yandexRedirectNumber = "+70001110011"
    val cianRedirectNumber = "+70001110012"
    val avitoRedirectNumber = "+70001110013"
    private val notSoon = Instant.now().plus(java.time.Duration.ofDays(5)).toEpochMilli

    val validRedirectNumbers = Seq(
      ClassifiedRedirectNumber
        .newBuilder()
        .setClassifiedType(YANDEX_REALTY)
        .setRedirect(PhoneRedirectMessage.newBuilder().setSource(yandexRedirectNumber).setDeadline(notSoon))
        .build(),
      ClassifiedRedirectNumber
        .newBuilder()
        .setClassifiedType(CIAN)
        .setRedirect(PhoneRedirectMessage.newBuilder().setSource(cianRedirectNumber).setDeadline(notSoon))
        .build(),
      ClassifiedRedirectNumber
        .newBuilder()
        .setClassifiedType(AVITO)
        .setRedirect(PhoneRedirectMessage.newBuilder().setSource(cianRedirectNumber).setDeadline(notSoon))
        .build()
    )
  }

}
