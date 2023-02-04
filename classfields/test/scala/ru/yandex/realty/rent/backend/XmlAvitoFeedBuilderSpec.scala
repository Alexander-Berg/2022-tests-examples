package ru.yandex.realty.rent.backend

import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.maps.RentPolygon
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.proto.offer.BuildingType
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{Flat, OwnerRequest}
import ru.yandex.realty.rent.model.feed.FeedEntry
import ru.yandex.realty.rent.proto.api.moderation.{AvitoVasData, ClassifiedTypeNamespace, FlatQuestionnaire}
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Building.ParkingNamespace
import ru.yandex.realty.rent.proto.model.flat.ClassifiedPublicationVas
import ru.yandex.realty.rent.proto.model.flat.ClassifiedPublicationVas.AvitoVas
import ru.yandex.realty.util.protobuf.ProtobufFormats.{DateTimeFormat => ProtoDateTimeFormat}
import ru.yandex.realty.util.Mappings.MapAny

import scala.collection.JavaConverters._
import scala.xml.{Node, NodeSeq}
import scala.xml.Utility.trim
import scala.xml.XML.loadString

@RunWith(classOf[JUnitRunner])
class XmlAvitoFeedBuilderSpec extends WordSpec with Matchers {

  "XmlAvitoFeedBuilder.validateHouseType" should {
    val supportedHouseTypes = Seq(
      BuildingType.BUILDING_TYPE_BRICK,
      BuildingType.BUILDING_TYPE_MONOLIT,
      BuildingType.BUILDING_TYPE_PANEL,
      BuildingType.BUILDING_TYPE_WOOD,
      BuildingType.BUILDING_TYPE_BLOCK
    )

    val unsupportedHouseTypes = Seq(
      BuildingType.BUILDING_TYPE_UNKNOWN,
      BuildingType.BUILDING_TYPE_MONOLIT_BRICK,
      BuildingType.BUILDING_TYPE_METAL,
      BuildingType.BUILDING_TYPE_FERROCONCRETE
    )

    "return None of error for correct houseType in questionnaire" in new Wiring with Data {
      val entries: Seq[FeedEntry] = supportedHouseTypes.map { houseType =>
        sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(data = sampleFeedEntry.flat.data.toBuilder.clearBuildingType().build()),
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setBuilding(
                sampleFeedEntry.questionnaire.data.getBuilding.toBuilder
                  .setHouseType(houseType)
                  .build()
              )
              .build()
          )
        )
      }
      entries.foreach { entry =>
        builder.validateHouseType(entry) shouldBe None
      }
    }

    "return None of error for correct houseType in flat data" in new Wiring with Data {
      val entries: Seq[FeedEntry] = supportedHouseTypes.map { houseType =>
        sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .setBuildingType(houseType)
              .build()
          ),
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setBuilding(
                sampleFeedEntry.questionnaire.data.getBuilding.toBuilder.clearHouseType().build()
              )
              .build()
          )
        )
      }
      entries.foreach { entry =>
        builder.validateHouseType(entry) shouldBe None
      }
    }

    // ---

    "return error for unsupported houseType in questionnaire" in new Wiring with Data {
      val entries: Seq[FeedEntry] = unsupportedHouseTypes.map { houseType =>
        sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(data = sampleFeedEntry.flat.data.toBuilder.clearBuildingType().build()),
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setBuilding(
                sampleFeedEntry.questionnaire.data.getBuilding.toBuilder
                  .setHouseType(houseType)
                  .build()
              )
              .build()
          )
        )
      }
      entries.foreach { entry =>
        builder.validateHouseType(entry) shouldEqual Some(XmlAvitoFeedValidatorCode.HouseTypeNotFound)
      }
    }

    "return error for unsupported houseType in flat data" in new Wiring with Data {
      val entries: Seq[FeedEntry] = unsupportedHouseTypes.map { houseType =>
        sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .setBuildingType(houseType)
              .build()
          ),
          questionnaire = sampleFeedEntry.questionnaire.copy(
            data = sampleFeedEntry.questionnaire.data.toBuilder
              .setBuilding(
                sampleFeedEntry.questionnaire.data.getBuilding.toBuilder.clearHouseType().build()
              )
              .build()
          )
        )
      }
      entries.foreach { entry =>
        builder.validateHouseType(entry) shouldEqual Some(XmlAvitoFeedValidatorCode.HouseTypeNotFound)
      }
    }

  }

  "XmlAvitoFeedBuilder.head" should {
    "return correct xml header" in new Wiring with Data {
      builder.head
        .replaceFirst("""<!-- Generated at.*-->""", "")
        .replaceAll("\n", "") shouldEqual expectedHead
    }
  }

  "XmlAvitoFeedBuilder.body" should {
    "return correct xml body" in new Wiring with Data {
      val resultBodyStr: String = builder.body(sampleFeedEntry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      resultBody shouldEqual expectedBody
    }

    "return unique options of parking for duplicated parking types" in new Wiring with Data {
      val entry: FeedEntry = sampleFeedEntry.copy(
        questionnaire = sampleFeedEntry.questionnaire.copy(
          data = sampleFeedEntry.questionnaire.data.toBuilder
            .setBuilding {
              val parkings = Seq(
                ParkingNamespace.Parking.BEHIND_BARRIER,
                ParkingNamespace.Parking.BEHIND_BARRIER,
                ParkingNamespace.Parking.IN_YARD,
                ParkingNamespace.Parking.IN_YARD,
                ParkingNamespace.Parking.MODERN,
                ParkingNamespace.Parking.MODERN,
                ParkingNamespace.Parking.MULTILEVEL,
                ParkingNamespace.Parking.MULTILEVEL,
                ParkingNamespace.Parking.OPEN,
                ParkingNamespace.Parking.OPEN,
                ParkingNamespace.Parking.ROOFTOP,
                ParkingNamespace.Parking.ROOFTOP,
                ParkingNamespace.Parking.UNDERGROUND,
                ParkingNamespace.Parking.UNDERGROUND
              ).asJava

              sampleFeedEntry.questionnaire.data.getBuilding.toBuilder
                .addAllParking(parkings)
                .build()
            }
            .build()
        )
      )

      val resultBodyStr: String = builder.body(entry)
      val resultBody: Node = trim(loadString(resultBodyStr))
      val parkingOptions: NodeSeq = resultBody \ "Parking" \ "Option"

      val expectedOptions: Seq[Node] = Seq(
        "<Option>Открытая во дворе</Option>",
        "<Option>За шлагбаумом во дворе</Option>",
        "<Option>Наземная многоуровневая</Option>",
        "<Option>Подземная</Option>"
      ).map(str => trim(loadString(str)))

      parkingOptions.theSeq should contain theSameElementsAs expectedOptions
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
      val phoneTag: NodeSeq = resultBody \ "ContactPhone"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.avito
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
      val phoneTag: NodeSeq = resultBody \ "ContactPhone"
      phoneTag.text shouldEqual RentPolygon.SPB.phone.avito
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
      val phoneTag: NodeSeq = resultBody \ "ContactPhone"
      phoneTag.text shouldEqual RentPolygon.MSK.phone.avito
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
        val phoneTag: NodeSeq = resultBody \ "ContactPhone"
        phoneTag.text shouldEqual p.phone.avito
      }
    }

    "REALTYBACK-6773 set VAS Type" in new Wiring with Data {
      val vasAvito = AvitoVas
        .newBuilder()
        .setMainVasType(AvitoVasData.VasType.X5_7)
      val entry: FeedEntry =
        sampleFeedEntry.copy(
          flat = sampleFeedEntry.flat.copy(
            data = sampleFeedEntry.flat.data.toBuilder
              .clearClassifiedPublicationVas()
              .addClassifiedPublicationVas(ClassifiedPublicationVas.newBuilder().setAvitoVas(vasAvito))
              .build()
          )
        )
      val resultBodyStr = builder.body(entry)
      val resultBody = trim(loadString(resultBodyStr))
      (resultBody \ "ListingFee").text shouldEqual "Package"
      (resultBody \ "AdStatus").text shouldEqual "x5_7"
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

    "generate description with possible check in date and temp rent value" in new Wiring with Data {
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
  }

  "XmlAvitoFeedBuilder.tail" should {
    "return correct xml tail" in new Wiring with Data {
      builder.tail shouldEqual expectedTail
    }
  }

  "XmlAvitoFeedBuilder.entryDocument" should {
    "return correct feed of one element" in new Wiring with Data {
      val resultStr: String = builder.entryDocument(sampleFeedEntry)
      val result: Node = trim(loadString(resultStr))
      result shouldEqual expectedDocument
    }
  }

  "XmlAvitoFeedBuilder.classifiedType" should {
    "return Avito classified type" in new Wiring with Data {
      builder.classifiedType shouldEqual ClassifiedTypeNamespace.ClassifiedType.AVITO
    }
  }

  trait Wiring extends FeaturesStubComponent {
    features.REALTYBACK_7187_WatermarkForAvito.setNewState(true)
    val mdsBuilder = new MdsUrlBuilder("//localhost:80")
    val builder = new XmlAvitoFeedBuilder(mdsBuilder, features)
  }

  trait Data extends RentModelsGen {
    this: Wiring =>

    val MSK_PHONE: String = RentPolygon.MSK.phone.avito

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

    val expectedHead: String = """<Ads formatVersion="3" target="Avito.ru">"""

    val expectedBodyStr: String = {
      s"""
         |<Ad>
         |    <Id>${or.ownerRequestId}</Id>
         |    <Category>Квартиры</Category>
         |    <OperationType>Сдам</OperationType>
         |    <Address>${f.unifiedAddress.getOrElse(f.address)}</Address>
         |    <Latitude>${f.data.getLocation.getLatitude}</Latitude>
         |    <Longitude>${f.data.getLocation.getLongitude}</Longitude>
         |    <Description>${q.getOfferCopyright}</Description>
         |    <Price>${q.getPayments.getAdValue / 100}</Price>
         |    <Rooms>2</Rooms>
         |    <Square>${q.getFlat.getArea}</Square>
         |    <Floor>${q.getFlat.getFloor}</Floor>
         |    <Floors>${q.getBuilding.getFloors}</Floors>
         |    <ViewFromWindows>На улицу</ViewFromWindows>
         |    <Parking>
         |        <Option>Открытая во дворе</Option>
         |    </Parking>
         |    <RoomType>
         |        <Option>Изолированные</Option>
         |    </RoomType>
         |    <Renovation>Евро</Renovation>
         |    <Bathroom>
         |        <Option>Несколько</Option>
         |    </Bathroom>
         |    <SSAdditionally>
         |        <Option>Мебель</Option>
         |        <Option>Бытовая техника</Option>
         |        <Option>Кондиционер</Option>
         |    </SSAdditionally>
         |    <LeaseType>На длительный срок</LeaseType>
         |    <LeaseMultimedia>
         |        <Option>Телевизор</Option>
         |    </LeaseMultimedia>
         |    <LeaseAppliances>
         |        <Option>Стиральная машина</Option>
         |        <Option>Холодильник</Option>
         |    </LeaseAppliances>
         |    <PetsAllowed>Нет</PetsAllowed>
         |    <ChildrenAllowed>Да</ChildrenAllowed>
         |    <LeaseCommissionSize>0</LeaseCommissionSize>
         |    <LeaseDeposit>Без Залога</LeaseDeposit>
         |    <Images>
         |        <image url="https://localhost:80/get-namespace1/1001/name1/avito_1024x1024"/>
         |        <image url="https://localhost:80/get-namespace2/1002/name2/avito_1024x1024"/>
         |        <image url="https://localhost:80/get-namespace3/1003/name3/avito_1024x1024"/>
         |        <image url="https://localhost:80/get-namespace4/1004/name4/avito_1024x1024"/>
         |    </Images>
         |    <CompanyName>Яндекс.Аренда</CompanyName>
         |    <AllowEmail>Нет</AllowEmail>
         |    <PropertyRights>Посредник</PropertyRights>
         |
         |    <ContactPhone>$MSK_PHONE</ContactPhone>
         |    <KitchenSpace>${q.getFlat.getKitchenSpace}</KitchenSpace>
         |
         |    <BalconyOrLoggia>Лоджия</BalconyOrLoggia>
         |    <PassengerElevator>${q.getBuilding.getElevators.getPassengerAmount}</PassengerElevator>
         |    <InHouse>
         |      <Option>Консьерж</Option>
         |      <Option>Мусоропровод</Option>
         |    </InHouse>
         |    <Courtyard>
         |      <Option>Закрытая территория</Option>
         |    </Courtyard>
         |
         |    <ListingFee>Package</ListingFee>
         |    <AdStatus>Free</AdStatus>
         |</Ad>
         |""".stripMargin
    }
    lazy val expectedBody: Node = trim(loadString(expectedBodyStr))

    val expectedTail = "</Ads>"

    val expectedDocumentStr: String =
      s"""$expectedHead
         |$expectedBodyStr
         |$expectedTail
         |""".stripMargin

    lazy val expectedDocument: Node = trim(loadString(expectedDocumentStr))
  }
}
