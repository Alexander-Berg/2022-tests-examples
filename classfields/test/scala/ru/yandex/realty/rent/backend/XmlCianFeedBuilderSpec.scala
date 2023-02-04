package ru.yandex.realty.rent.backend

import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.clients.maps.RentPolygon
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.feed.FeedEntry
import ru.yandex.realty.rent.model.{Flat, OwnerRequest}
import ru.yandex.realty.rent.proto.api.common.FlatTypeNamespace
import ru.yandex.realty.rent.proto.api.moderation
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Flat.RoomsNamespace
import ru.yandex.realty.rent.proto.api.moderation.{CianVasData, ClassifiedTypeNamespace}
import ru.yandex.realty.rent.proto.model.flat.ClassifiedPublicationVas
import ru.yandex.realty.rent.proto.model.flat.ClassifiedPublicationVas.CianVas
import ru.yandex.realty.rent.proto.model.flat.FlatData.NearestMetro
import ru.yandex.realty.util.protobuf.ProtobufFormats.{DateTimeFormat => ProtoDateTimeFormat}
import ru.yandex.realty.util.Mappings.MapAny

import scala.collection.JavaConverters._
import scala.xml.Utility.trim
import scala.xml.XML.loadString
import scala.xml.{Node, NodeSeq}

@RunWith(classOf[JUnitRunner])
class XmlCianFeedBuilderSpec extends WordSpec with Matchers with Logging {

  "XmlCianFeedBuilder.head" should {
    "return correct xml header" in new Wiring with Data {
      builder.head
        .replaceFirst("""<!-- Generated at.*-->""", "")
        .replaceFirst("\n", "") shouldEqual expectedHead
    }
  }

  "XmlCianFeedBuilder.body" should {
    "return correct xml body" in new Wiring with Data {
      val resultBodyStr: String = builder.body(sampleFeedEntry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      resultBody shouldEqual expectedBody
    }

    "return correct body with address from proto if unifiedAddress is None" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(flat = sampleFeedEntry.flat.copy(unifiedAddress = None))
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val resultAddressTag: NodeSeq = resultBody \ "Address"
      resultAddressTag.text shouldEqual entry.flat.data.getLocation.getAddress
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
      val phoneTag: NodeSeq = resultBody \ "Phones" \ "PhoneSchema" \ "Number"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.cian.drop(2)
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
      val phoneTag: NodeSeq = resultBody \ "Phones" \ "PhoneSchema" \ "Number"
      phoneTag.text shouldEqual RentPolygon.SPB.phone.cian.drop(2)
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
      val phoneTag: NodeSeq = resultBody \ "Phones" \ "PhoneSchema" \ "Number"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.cian.drop(2)
    }

    "REALTYBACK-6349 set roomsCount=[9] for studios and [N] for others" in new Wiring with Data {
      Seq(
        (RoomsNamespace.Rooms.ONE, "1"),
        (RoomsNamespace.Rooms.TWO, "2"),
        (RoomsNamespace.Rooms.THREE, "3"),
        (RoomsNamespace.Rooms.FOUR, "4"),
        (RoomsNamespace.Rooms.FIVE, "5"),
        (RoomsNamespace.Rooms.STUDIO, "9")
      ) foreach {
        case (rooms, expected) =>
          val entry: FeedEntry =
            sampleFeedEntry.copy(
              questionnaire = sampleFeedEntry.questionnaire.copy(
                data = sampleFeedEntry.questionnaire.data.toBuilder
                  .setFlat(
                    sampleFeedEntry.questionnaire.data.getFlat.toBuilder
                      .setRooms(rooms)
                      .build()
                  )
                  .build()
              )
            )
          val resultBodyStr: String = builder.body(entry)
          val resultBody: Node = trim(loadString(resultBodyStr))
          val phoneTag: NodeSeq = resultBody \ "FlatRoomsCount"
          phoneTag.text shouldEqual expected
      }
    }

    "REALTYBACK-6349 set [IsApartments=true] tag for apartments" in new Wiring with Data {
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setFlat(
                sampleFeedEntry.questionnaire.data.getFlat.toBuilder
                  .setFlatType(FlatTypeNamespace.FlatType.APARTMENTS)
                  .build()
              )
              .build()
          )
        )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val phoneTag: NodeSeq = resultBody \ "IsApartments"
      phoneTag.text shouldEqual "true"
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
        val phoneTag: NodeSeq = resultBody \ "Phones" \ "PhoneSchema" \ "Number"
        val codeTag: NodeSeq = resultBody \ "Phones" \ "PhoneSchema" \ "CountryCode"
        phoneTag.text shouldEqual p.phone.cian.drop(2)
        codeTag.text shouldEqual p.phone.cian.take(2)
      }
    }

    "REALTYBACK-6773 set VAS, Highlight and Auction Bet" in new Wiring with Data {
      val vasCian = CianVas
        .newBuilder()
        .setMainVasType(CianVasData.VasType.TOP3)
        .setVasHighlight(true)
        .setAuctionBet(14.88)
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .clearClassifiedPublicationVas()
              .addClassifiedPublicationVas(ClassifiedPublicationVas.newBuilder().setCianVas(vasCian))
              .build()
          )
        )
      val resultBodyStr = builder.body(entry)
      val resultBody = trim(loadString(resultBodyStr))
      val auctionTag = resultBody \ "Auction" \ "Bet"
      val servicesEnumTags = resultBody \ "PublishTerms" \ "Terms" \ "PublishTermSchema" \ "Services" \ "ServicesEnum"
      auctionTag.text shouldEqual "14.88"
      servicesEnumTags.exists(_.text == "top3") should be(true)
      servicesEnumTags.exists(_.text == "highlight") should be(true)
    }

    "REALTYBACK-7028 select walk type from nearest metros" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat
          .copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .applySideEffect(
                _.getLocationBuilder
                  .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                  .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
              )
              .setNearestMetro(
                NearestMetro
                  .newBuilder()
                  .addAllMetro(
                    Seq(
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Авиамоторная")
                        .setTimeOnFoot(5)
                        .setTimeOnTransport(1)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Автозаводская")
                        .setTimeOnFoot(6)
                        .setTimeOnTransport(2)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Академическая")
                        .setTimeOnFoot(7)
                        .setTimeOnTransport(3)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Октябрьская")
                        .setTimeOnFoot(8)
                        .setTimeOnTransport(4)
                        .build()
                    ).asJava
                  )
                  .build()
              )
              .build
          )
      )

      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val undergroundsTag: NodeSeq = resultBody \ "Undergrounds" \ "UndergroundInfoSchema"
      val result: Seq[(String, String, String)] =
        undergroundsTag.map { u =>
          println("u = ", u)

          ((u \ "Id").text, (u \ "Time").text, (u \ "TransportType").text)
        }

      result should contain theSameElementsAs Seq(
        ("1", "5", "walk"),
        ("2", "6", "walk"),
        ("3", "7", "walk")
      )
    }

    "REALTYBACK-7028 select first two walk types from nearest metros and third transport type" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat
          .copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .applySideEffect(
                _.getLocationBuilder
                  .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                  .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
              )
              .setNearestMetro(
                NearestMetro
                  .newBuilder()
                  .addAllMetro(
                    Seq(
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Авиамоторная")
                        .setTimeOnFoot(5)
                        .setTimeOnTransport(1)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Автозаводская")
                        .setTimeOnFoot(6)
                        .setTimeOnTransport(2)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Академическая")
                        .setTimeOnFoot(21)
                        .setTimeOnTransport(3)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Октябрьская")
                        .setTimeOnFoot(30)
                        .setTimeOnTransport(4)
                        .build()
                    ).asJava
                  )
                  .build()
              )
              .build
          )
      )

      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val undergroundsTag: NodeSeq = resultBody \ "Undergrounds" \ "UndergroundInfoSchema"
      val result: Seq[(String, String, String)] =
        undergroundsTag.map { u =>
          println("u = ", u)

          ((u \ "Id").text, (u \ "Time").text, (u \ "TransportType").text)
        }

      result should contain theSameElementsAs Seq(
        ("1", "5", "walk"),
        ("2", "6", "walk"),
        ("3", "3", "transport")
      )
    }

    "REALTYBACK-7028 select only transport types if there are not nearest walk-types" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        flat = sampleFeedEntry.flat
          .copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .applySideEffect(
                _.getLocationBuilder
                  .setSubjectFederationGeoid(Regions.MSK_AND_MOS_OBLAST)
                  .setSubjectFederationRgid(NodeRgid.MOSCOW_AND_MOS_OBLAST)
              )
              .setNearestMetro(
                NearestMetro
                  .newBuilder()
                  .addAllMetro(
                    Seq(
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Авиамоторная")
                        .setTimeOnFoot(21)
                        .setTimeOnTransport(1)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Автозаводская")
                        .setTimeOnFoot(22)
                        .setTimeOnTransport(2)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Академическая")
                        .setTimeOnFoot(100)
                        .setTimeOnTransport(4)
                        .build(),
                      NearestMetro.Metro
                        .newBuilder()
                        .setName("Октябрьская")
                        .setTimeOnFoot(30)
                        .setTimeOnTransport(3)
                        .build()
                    ).asJava
                  )
                  .build()
              )
              .build
          )
      )

      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val undergroundsTag: NodeSeq = resultBody \ "Undergrounds" \ "UndergroundInfoSchema"
      val result: Seq[(String, String, String)] =
        undergroundsTag.map { u =>
          println("u = ", u)

          ((u \ "Id").text, (u \ "Time").text, (u \ "TransportType").text)
        }

      result should contain theSameElementsAs Seq(
        ("1", "1", "transport"),
        ("2", "2", "transport"),
        ("80", "3", "transport")
      )
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
      val description: NodeSeq = resultBody \ "Description"
      val offerCopyright = entry.questionnaire.data.getOfferCopyright
      val expectedDescription = s"Заезд возможен с $checkInDate\n \n$offerCopyright"
      description.head.text shouldEqual expectedDescription
        .replace("\n \n", " ")
        .replace("\n", " ")
        .trim
      resultBodyStr.contains(expectedDescription) shouldBe (true)
    }

    "generate description with possible check in date and temporary rent value" in new Wiring with Data {
      val checkInDate = "26.05.2050"
      val checkInDateTime = DateTimeFormat.forPattern("dd.MM.yyyy").parseDateTime(checkInDate)
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setOfferCopyright("1\n2\n")
              .setPossibleCheckInDate(ProtoDateTimeFormat.write(checkInDateTime))
              .setPayments(
                sampleFeedEntry.questionnaire.data.getPayments.toBuilder
                  .setTemporaryRentalValue(2000000L)
                  .setTemporaryAdValue(2200000L)
                  .setTemporaryPeriodMonths(2)
                  .build()
              )
              .build()
          )
        )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val description: NodeSeq = resultBody \ "Description"
      val offerCopyright = entry.questionnaire.data.getOfferCopyright
      val expectedDescription =
        s"Заезд возможен с $checkInDate\n \nПервые 2 мес. арендная плата 22000 руб.\n \n$offerCopyright"
      description.head.text shouldEqual expectedDescription
        .replace("\n \n", " ")
        .replace("\n", " ")
        .trim
      resultBodyStr.contains(expectedDescription) shouldBe (true)
    }

    "generate description with possible check in date and temporary rent value 1 month" in new Wiring with Data {
      val checkInDate = "26.05.2050"
      val checkInDateTime = DateTimeFormat.forPattern("dd.MM.yyyy").parseDateTime(checkInDate)
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setOfferCopyright("1\n2\n")
              .setPossibleCheckInDate(ProtoDateTimeFormat.write(checkInDateTime))
              .setPayments(
                sampleFeedEntry.questionnaire.data.getPayments.toBuilder
                  .setTemporaryRentalValue(2000000L)
                  .setTemporaryAdValue(2200000L)
                  .setTemporaryPeriodMonths(1)
                  .build()
              )
              .build()
          )
        )
      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val description: NodeSeq = resultBody \ "Description"
      val offerCopyright = entry.questionnaire.data.getOfferCopyright
      val expectedDescription =
        s"Заезд возможен с $checkInDate\n \nПервый месяц арендная плата 22000 руб.\n \n$offerCopyright"
      description.head.text shouldEqual expectedDescription
        .replace("\n \n", " ")
        .replace("\n", " ")
        .trim
      resultBodyStr.contains(expectedDescription) shouldBe (true)
    }
  }

  "XmlCianFeedBuilder.tail" should {
    "return correct xml tail" in new Wiring with Data {
      builder.tail shouldEqual expectedTail
    }
  }

  "XmlCianFeedBuilder.entryDocument" should {
    "return correct feed of one element" in new Wiring with Data {
      val resultStr: String = builder.entryDocument(sampleFeedEntry)
      val result: Node = trim(loadString(resultStr))
      result shouldEqual expectedDocument
    }
  }

  "XmlCianFeedBuilder.classifiedType" should {
    "return Cian classified type" in new Wiring with Data {
      builder.classifiedType shouldEqual ClassifiedTypeNamespace.ClassifiedType.CIAN
    }

    "REALTYBACK-7660 vas experiment" in new Wiring with Data {
      val vasList = Set("paid", "premium", "top3")
      val vasCian = CianVas
        .newBuilder()
        .setMainVasType(CianVasData.VasType.PAID)
        .setVasHighlight(true)
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .clearClassifiedPublicationVas()
              .addClassifiedPublicationVas(ClassifiedPublicationVas.newBuilder().setCianVas(vasCian))
              .build()
          )
        )
      features.CianFeedVasExperiment.setNewState(true)
      val resultBodyStr = builder.body(entry)
      val resultBody = trim(loadString(resultBodyStr))
      val auctionTag = resultBody \ "Auction" \ "Bet"
      val servicesEnumTags = resultBody \ "PublishTerms" \ "Terms" \ "PublishTermSchema" \ "Services" \ "ServicesEnum"
      servicesEnumTags.exists(t => vasList.contains(t.text)) should be(true)
      servicesEnumTags.exists(_.text == "highlight") should be(false)
    }
  }

  trait Wiring {
    val mdsBuilder = new MdsUrlBuilder("//localhost:80")
    val features: Features = new SimpleFeatures()
    val builder = new XmlCianFeedBuilder(mdsBuilder, features)
  }

  trait Data extends RentModelsGen {
    this: Wiring =>

    val MSK_PHONE: String = RentPolygon.MSK.phone.cian.drop(2)

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
    val sampleFlat: Flat = sampleFeedEntry.flat
    val ownerRequest: OwnerRequest = sampleFeedEntry.ownerRequest
    val q: moderation.FlatQuestionnaire = sampleFeedEntry.questionnaire.data

    val expectedHead: String =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<feed>
         |<feed_version>2</feed_version>
         |""".stripMargin

    val expectedBodyStr: String = {
      s"""
         |<object>
         |    <Category>flatRent</Category>
         |    <ExternalId>${ownerRequest.ownerRequestId}</ExternalId>
         |    <Description>${q.getOfferCopyright}</Description>
         |    <Address>${sampleFlat.unifiedAddress.get}</Address>
         |
         |    <FlatRoomsCount>2</FlatRoomsCount>
         |
         |    <Phones>
         |        <PhoneSchema>
         |            <CountryCode>+7</CountryCode>
         |            <Number>$MSK_PHONE</Number>
         |        </PhoneSchema>
         |    </Phones>
         |
         |    <TotalArea>${q.getFlat.getArea}</TotalArea>
         |    <FloorNumber>${q.getFlat.getFloor}</FloorNumber>
         |    <Photos>
         |        <PhotoSchema>
         |          <FullUrl>https://localhost:80/get-namespace1/1001/name1/cian_1024x1024</FullUrl>
         |          <IsDefault>true</IsDefault>
         |        </PhotoSchema>
         |
         |        <PhotoSchema>
         |          <FullUrl>https://localhost:80/get-namespace2/1002/name2/cian_1024x1024</FullUrl>
         |          <IsDefault>true</IsDefault>
         |        </PhotoSchema>
         |
         |        <PhotoSchema>
         |          <FullUrl>https://localhost:80/get-namespace3/1003/name3/cian_1024x1024</FullUrl>
         |          <IsDefault>true</IsDefault>
         |        </PhotoSchema>
         |
         |        <PhotoSchema>
         |          <FullUrl>https://localhost:80/get-namespace4/1004/name4/cian_1024x1024</FullUrl>
         |          <IsDefault>true</IsDefault>
         |        </PhotoSchema>
         |    </Photos>
         |    <BalconiesCount>${q.getFlat.getBalcony.getBalconyAmount}</BalconiesCount>
         |    <LoggiasCount>${q.getFlat.getBalcony.getLoggiaAmount}</LoggiasCount>
         |    <WindowsViewType>street</WindowsViewType>
         |    <SeparateWcsCount>${q.getFlat.getBathroom.getSeparatedAmount}</SeparateWcsCount>
         |    <CombinedWcsCount>${q.getFlat.getBathroom.getCombinedAmount}</CombinedWcsCount>
         |    <RepairType>euro</RepairType>
         |    <IsApartments>false</IsApartments>
         |
         |    <HasInternet>true</HasInternet>
         |    <HasTv>true</HasTv>
         |    <HasWasher>true</HasWasher>
         |    <HasConditioner>true</HasConditioner>
         |    <HasDishwasher>true</HasDishwasher>
         |    <HasFridge>true</HasFridge>
         |    <PetsAllowed>false</PetsAllowed>
         |
         |    <Building>
         |        <FloorsCount>${q.getBuilding.getFloors}</FloorsCount>
         |        <Parking>
         |            <Type>open</Type>
         |        </Parking>
         |    </Building>
         |    <PublishTerms>
         |        <Terms>
         |            <PublishTermSchema>
         |                <Services>
         |                    <ServicesEnum>paid</ServicesEnum>
         |                </Services>
         |            </PublishTermSchema>
         |        </Terms>
         |    </PublishTerms>
         |    <BargainTerms>
         |        <Price>${q.getPayments.getAdValue / 100}</Price>
         |        <UtilitiesTerms>
         |            <FlowMetersNotIncludedInPrice>true</FlowMetersNotIncludedInPrice>
         |        </UtilitiesTerms>
         |        <Currency>rub</Currency>
         |        <LeaseTermType>longTerm</LeaseTermType>
         |        <PrepayMonths>1</PrepayMonths>
         |        <Deposit>0</Deposit>
         |        <ClientFee>0</ClientFee>
         |        <AgentFee>0</AgentFee>
         |        <AgentBonus>
         |            <Value>0</Value>
         |            <PaymentType>percent</PaymentType>
         |            <Currency>eur</Currency>
         |        </AgentBonus>
         |    </BargainTerms>
         |    <Undergrounds>
         |      <UndergroundInfoSchema>
         |        <Id>1</Id>
         |        <Time>10</Time>
         |        <TransportType>walk</TransportType>
         |      </UndergroundInfoSchema>
         |    </Undergrounds>
         |</object>
         |""".stripMargin
    }
    val expectedBody: Node = trim(loadString(expectedBodyStr))

    val expectedTail = "</feed>"

    val expectedDocumentStr: String =
      s"""$expectedHead
         |$expectedBodyStr
         |$expectedTail
         |""".stripMargin

    val expectedDocument: Node = trim(loadString(expectedDocumentStr))
  }
}
