package ru.auto.cabinet.service.moderation

import org.scalatest.time.{Seconds, Span}
import ru.auto.cabinet.KafkaProducerDealerModerationEvents
import ru.auto.cabinet.Moderation.DealerModerationEvent
import ru.auto.cabinet.converter.DealerToModerationConverter
import ru.auto.cabinet.dao.jdbc.JdbcPremoderationBufferDao.{
  ModerationStatus,
  PremoderationDataResponse
}
import ru.auto.cabinet.dao.jdbc.{
  JdbcClientDao,
  JdbcKeyValueDao,
  JdbcPremoderationBufferDao
}
import ru.auto.cabinet.environment._
import ru.auto.cabinet.model.SalonInfo.FileUpdate
import ru.auto.cabinet.model.dealer._
import ru.auto.cabinet.model.{
  Agency,
  ClientId,
  ClientStatuses,
  Company,
  Mark,
  SalonInfo
}
import ru.auto.cabinet.service.S3FileUrlService.S3File
import ru.auto.cabinet.service.dealer.{
  DealerBuilderService,
  SalonBuilderService,
  SalonUpdaterService
}
import ru.auto.cabinet.service.{DateTimeService, S3FileUrlService}
import ru.auto.cabinet.test.BaseSpec
import ru.auto.cabinet.trace.Context

import java.time.temporal.ChronoUnit
import java.time.{Instant, OffsetDateTime}
import scala.jdk.CollectionConverters._

class ModerationServiceSpec extends BaseSpec {
  implicit private val rc = Context.unknown

  private val s3FileUrlService = mock[S3FileUrlService]
  private val dateTimeService = mock[DateTimeService]

  private val dealerToModerationConverter =
    new DealerToModerationConverter(dateTimeService, s3FileUrlService)
  private val keyValueDao = mock[JdbcKeyValueDao]
  private val clientDao = mock[JdbcClientDao]
  private val premoderationBufferDao = mock[JdbcPremoderationBufferDao]
  private val dealerService = mock[DealerBuilderService]
  private val salonBuilderService = mock[SalonBuilderService]
  private val salonUpdaterService = mock[SalonUpdaterService]
  private val producer = mock[KafkaProducerDealerModerationEvents]

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(1, Seconds))

  private val createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
  private val instantEpoch = Instant.now()

  private val service =
    new ModerationService(
      clientDao,
      dealerToModerationConverter,
      keyValueDao,
      premoderationBufferDao,
      dealerService,
      salonBuilderService,
      salonUpdaterService,
      producer)

  private val dealer = Dealer(
    20101,
    "email@ya.ru",
    phone = 79211234567L,
    origin = "spb2022",
    createTime = createdAt,
    "address",
    "name",
    ClientStatuses.Active,
    userIds = Set(123L, 321L),
    Some(Agency(1, "agency")),
    regionId = 1,
    companyGroup = Some(Company(2, "company", createdAt)),
    isAgent = false,
    epoch = instantEpoch,
    salon = Some(Salon(
      title = "salon",
      description = Some("descr"),
      url = Some("www.ya.ru"),
      phones = Set(
        Phone(
          title = "manager",
          countryCode = "7",
          cityCode = "921",
          phone = "79211234567",
          localPhone = "1234567",
          phoneMask = "1:3:7",
          callFrom = 7,
          callTill = 22,
          extension = ""
        )),
      logo = "logo.jpg",
      photo = Set("photo.jpg"),
      rentCertificate = Set("rent.jpg"),
      lessor = Some("lessor"),
      dealership = Set(
        Dealership(
          Mark(3, Some("VerbaId"), "Lada"),
          Set("40553-certificate.jpg"),
          instantEpoch
        )),
      id = 7,
      address = "address salon",
      lat = 1.1d,
      lng = 2.2d,
      cityId = Some(123),
      regionId = Some(321),
      countryId = Some(111),
      yaCityId = 1,
      yaRegionId = 2,
      yaCountryId = 3
    )),
    Some(Manager(5, email = Some("manager@ya.ru")))
  )

  private val dealerModerationEvent = DealerModerationEvent
    .newBuilder()
    .setId(20101)
    .setEmail("email@ya.ru")
    .setOrigin("spb2022")
    .setCreatedAt(createdAt.asProtoTimestamp())
    .setName("name")
    .setStatus(DealerModerationEvent.Status.ACTIVE)
    .addAllUserIds(List(123, 321).map(Long.box(_)).asJava)
    .setAgency(DealerModerationEvent.Agency
      .newBuilder()
      .setId(1)
      .setName("agency")
      .build())
    .setCompanyGroup(DealerModerationEvent.CompanyGroup
      .newBuilder()
      .setId(2)
      .setName("company")
      .build())
    .addAllMarks(
      List(
        DealerModerationEvent.Mark
          .newBuilder()
          .setId("VerbaId")
          .setName("Lada")
          .setDealershipDocUrl("www.ya.ru/image/40553-certificate.jpg")
          .build()).asJava)
    .addAllPhones(
      List(
        DealerModerationEvent.Phone
          .newBuilder()
          .setPhone("79211234567")
          .setExtention("")
          .setContactName("manager")
          .setCallFrom(7)
          .setCallTill(22)
          .setCityCode("921")
          .setCountryCode("7")
          .build()).asJava)
    .setDescription("descr")
    .setWebsiteUrl("www.ya.ru")
    .addAllPhotoUrls(List("www.ya.ru/image/photo.jpg").asJava)
    .setPersonalManager(
      DealerModerationEvent.ManagerInfo.newBuilder().setEmail("manager@ya.ru"))
    .setRealEstateInfo(DealerModerationEvent.RealEstateInfo
      .newBuilder()
      .setName("lessor")
      .setAddress("address salon")
      .setRegionId(2)
      .addAllDocumentsUrls(List("www.ya.ru/image/rent.jpg").asJava))
    .setLogoUrl("www.ya.ru/image/logo.jpg")
    .build()

  private val dealerModerationEventAfterUpdate = dealerModerationEvent.toBuilder
    .setName("name")
    .setWebsiteUrl("http://www.svmotors.ru/")
    .setDescription("---")
    .setRealEstateInfo(
      DealerModerationEvent.RealEstateInfo
        .newBuilder()
        .setName("ДАС")
        .setAddress("---")
        .setRegionId(1)
        .addAllDocumentsUrls(List(
          "www.ya.ru/image/add-rent.jpg",
          "www.ya.ru/image/rent.jpg"
        ).asJava))
    .setLogoUrl("www.ya.ru/image/123.jpg")
    .clearPhotoUrls()
    .addAllPhotoUrls(List("www.ya.ru/image/new-photo.jpg").asJava)
    .clearMarks()
    .addAllMarks(
      List(
        DealerModerationEvent.Mark
          .newBuilder()
          .setId("")
          .setName("Toyota")
          .setDealershipDocUrl("www.ya.ru/image/40553-Toyota.jpg")
          .build()).asJava)
    .clearPhones()
    .addAllPhones(
      List(
        DealerModerationEvent.Phone
          .newBuilder()
          .setPhone("79267178974")
          .setExtention("")
          .setContactName("Дилер")
          .setCallFrom(9)
          .setCallTill(23)
          .setCityCode("926")
          .setCountryCode("7")
          .build()).asJava)
    .setEditorInfo {
      import ru.auto.cabinet.util.Protobuf._
      DealerModerationEvent.Editor
        .newBuilder()
        .setId(123)
        .setEditedAt(instantEpoch.toProtobufTimestamp)
    }
    .setEventId("3")
    .build()

  private val salonInfo = SalonInfo(
    title = "SV-Motors",
    url = Some("http://www.svmotors.ru/"),
    description = Some("---"),
    lessor = Some("ДАС"),
    logo =
      FileUpdate(List("logo.jpg", "123.jpg"), List("logo.jpg"), List.empty),
    photo = FileUpdate(
      origin = List("photo.jpg"),
      delete = List("photo.jpg"),
      `new` = List("new-photo.jpg")),
    poi = SalonInfo.Poi(
      id = 1,
      geoId = 213,
      address = "---",
      lat = 55.572231,
      lng = 37.564014,
      cityId = Some(0),
      regionId = Some(213),
      countryId = Some(1),
      yaCityId = 213,
      yaRegionId = 1,
      yaCountryId = 225
    ),
    rentCertificate = FileUpdate(
      origin = List("rent.jpg"),
      delete = List.empty,
      `new` = List("add-rent.jpg")),
    phones = SalonInfo.Phone(
      title = Some("Дилер"),
      id = Some("461028"),
      deleteRow = false,
      countryCode = "7",
      cityCode = "926",
      phone = "79267178974",
      localPhone = "7178974",
      phoneMask = "1:3:7",
      extention = "",
      callFrom = 9,
      callTill = 23
    ) ::
      SalonInfo.Phone(
        id = Some("4"),
        deleteRow = true,
        countryCode = "7",
        cityCode = "921",
        phone = "79211234567",
        localPhone = "1234567",
        phoneMask = "1:3:7",
        extention = "",
        callFrom = 7,
        callTill = 22,
        title = Some("manager")
      ) :: Nil,
    dealership = List(
      SalonInfo.Dealership(
        id = Some("101826"),
        markId = "260",
        markName = "Toyota",
        deleteRow = false,
        origin = Nil,
        `new` = "40553-Toyota.jpg" :: Nil,
        delete = Nil
      ),
      SalonInfo.Dealership(
        id = Some("101826"),
        markId = "3",
        markName = "Lada",
        deleteRow = true,
        origin = "40553-certificate.jpg" :: Nil,
        `new` = Nil,
        delete = Nil
      )
    )
  )

  private val premoderationData = PremoderationDataResponse(
    id = 3,
    clientId = 20101,
    updateRequest = salonInfo,
    status = ModerationStatus.New,
    reasons = Set.empty,
    dealerEpoch = Instant.now(),
    created = Instant.now().minusSeconds(3600),
    userId = 123
  )

  "ModerationService" should {
    "send dealer moderation event without salon update info" in {
      (keyValueDao
        .valueByKey(_: String)(_: Context))
        .expects("client-moderation-to-kafka", *)
        .returningF(Some("1970-01-01T00:00:00.00Z"))

      (dealerService
        .getDealersSinceEpoch(_: Instant)(_: Context))
        .expects(Instant.parse("1970-01-01T00:00:00.00Z"), *)
        .returningF(Set(dealer))

      (premoderationBufferDao
        .getLastUserModerationData(_: Set[ClientId])(_: Context))
        .expects(Set(20101L), *)
        .returningF(Set.empty)

      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("logo.jpg"))
        .returning("www.ya.ru/image/logo.jpg")
      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("photo.jpg"))
        .returning("www.ya.ru/image/photo.jpg")
      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("rent.jpg"))
        .returning("www.ya.ru/image/rent.jpg")
      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("40553-certificate.jpg"))
        .returning("www.ya.ru/image/40553-certificate.jpg")

      (producer
        .sendEvent(_: DealerModerationEvent)(_: Context))
        .expects(dealerModerationEvent, *)
        .returningF(())

      (keyValueDao
        .upsert(_: String, _: String)(_: Context))
        .expects("client-moderation-to-kafka", instantEpoch.toString, *)
        .returningF(1)

      service.sendDealerModerationEvent().futureValue shouldBe a[Unit]
    }

    "send dealer moderation event with salon update info" in {
      (keyValueDao
        .valueByKey(_: String)(_: Context))
        .expects("client-moderation-to-kafka", *)
        .returningF(Some("1970-01-01T00:00:00.00Z"))

      (dealerService
        .getDealersSinceEpoch(_: Instant)(_: Context))
        .expects(Instant.parse("1970-01-01T00:00:00.00Z"), *)
        .returningF(Set(dealer))

      (premoderationBufferDao
        .getLastUserModerationData(_: Set[ClientId])(_: Context))
        .expects(Set(20101L), *)
        .returningF(Set(premoderationData))

      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("123.jpg"))
        .returning("www.ya.ru/image/123.jpg")
      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("new-photo.jpg"))
        .returning("www.ya.ru/image/new-photo.jpg")
      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("add-rent.jpg"))
        .returning("www.ya.ru/image/add-rent.jpg")
      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("rent.jpg"))
        .returning("www.ya.ru/image/rent.jpg")
      (s3FileUrlService
        .getFullPath(_: S3File))
        .expects(S3File("40553-Toyota.jpg"))
        .returning("www.ya.ru/image/40553-Toyota.jpg")

      (() => dateTimeService.getNow)
        .expects()
        .returning(instantEpoch)

      (producer
        .sendEvent(_: DealerModerationEvent)(_: Context))
        .expects(dealerModerationEventAfterUpdate, *)
        .returningF(())

      (premoderationBufferDao
        .changeStatuses(_: Map[Long, (ModerationStatus, Option[Set[String]])])(
          _: Context))
        .expects(Map(3L -> (ModerationStatus.OnModeration, None)), *)
        .returningF(())

      (keyValueDao
        .upsert(_: String, _: String)(_: Context))
        .expects("client-moderation-to-kafka", instantEpoch.toString, *)
        .returningF(1)

      service.sendDealerModerationEvent().futureValue shouldBe a[Unit]
    }

  }
}
