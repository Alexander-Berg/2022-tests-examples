package ru.auto.cabinet.converter

import org.scalatest.flatspec.{AnyFlatSpecLike => FlatSpecLike}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import ru.auto.cabinet.DealerAutoru
import ru.auto.cabinet.Moderation.DealerModerationEvent
import ru.auto.cabinet.dao.jdbc.ManagerRecord
import ru.auto.cabinet.mocks.SalonInfoMock
import ru.auto.cabinet.model.{S3Config, S3Files}
import ru.auto.cabinet.service.S3FileUrlService
import ru.auto.cabinet.util.Protobuf._

import java.time.Instant
import scala.jdk.CollectionConverters._

class DealerAutoruToModerationConverterSpec extends FlatSpecLike {

  "DealerModerationConverter.toDealerModerationEvent" should "successfully convert" in {
    val createdAt = Instant.now()

    val imagesS3Config = S3Files(
      "http://s3_images",
      "images_prefix"
    )

    val filesS3Config = S3Files(
      "http://s3_files",
      "files_prefix"
    )

    val s3Config = S3Config(imagesS3Config, filesS3Config)

    val s3FileUrlService = new S3FileUrlService(s3Config)

    val dealerModerationConverter =
      new DealerAutoruToModerationConverter(s3FileUrlService)

    val dealer = DealerAutoru.Dealer
      .newBuilder()
      .setName("name")
      .setId(123)
      .setStatus(DealerAutoru.Dealer.Status.NEW)
      .setEmail("test@test.net")
      .setPhone(12345678912L)
      .setAddress("address")
      .setAgency(
        DealerAutoru.Agency
          .newBuilder()
          .setId(123)
          .setName("agency")
      )
      .addMarks(
        DealerAutoru.Mark
          .newBuilder()
          .setId("VOLVO")
          .setName("Volvo")
      )
      .setCompanyGroup("company_group")
      .setCompanyGroupInfo(
        DealerAutoru.CompanyGroup
          .newBuilder()
          .setId(123)
          .setTitle("company_group_info")
      )
      .setCreateTime(createdAt.toEpochMilli)
      .setOrigin("msk_1234")
      .addUserIds(12)
      .build()

    val manager = ManagerRecord(
      1L,
      fio = Some("fio"),
      email = Some("email"),
      workPhoneNumber = Option(1234),
      mobilePhoneNumber = Some(12345L),
      telegram = Some("telegram"),
      viber = Some(1234),
      skype = Some("skype")
    )

    val expectedDealerModeration = DealerModerationEvent
      .newBuilder()
      .setId(123)
      .setDescription("salon_description")
      .setName("name")
      .setOrigin("msk_1234")
      .addUserIds(12)
      .setEmail("test@test.net")
      .setLogoUrl("http://s3_images/images_prefix/123/logo_new/full")
      .setCompanyGroup(
        DealerModerationEvent.CompanyGroup
          .newBuilder()
          .setId(123)
          .setName("company_group_info")
          .build()
      )
      .setRealEstateInfo(
        DealerModerationEvent.RealEstateInfo
          .newBuilder()
          .setName("lessor")
          .setAddress("poi_address")
          .setRegionId(5)
          .addAllDocumentsUrls(
            List(
              "http://s3_images/images_prefix/1345/rent_certificate_origin1/full",
              "http://s3_files/files_prefix/532/rent_certificate_origin2.pdf"
            ).asJava
          )
          .build()
      )
      .setAgency(
        DealerModerationEvent.Agency
          .newBuilder()
          .setId(123)
          .setName("agency")
          .build()
      )
      .addMarks(
        DealerModerationEvent.Mark
          .newBuilder()
          .setId("VOLVO")
          .setName("Volvo")
          .setDealershipDocUrl(
            "http://s3_files/files_prefix/1234/dealership_origin.pdf")
      )
      .setPersonalManager(
        DealerModerationEvent.ManagerInfo
          .newBuilder()
          .setName("fio")
          .setEmail("email")
          .setPhone("12345")
      )
      .setStatus(DealerModerationEvent.Status.NEW)
      .addPhones(
        DealerModerationEvent.Phone
          .newBuilder()
          .setId("phone_id")
          .setContactName("phone_title")
          .setCallFrom(1)
          .setCallTill(2)
          .setCityCode("city_code")
          .setCountryCode("country_code")
          .setPhone("phone")
          .setExtention("extention")
      )
      .setCreatedAt(createdAt.toProtobufTimestamp)
      .addPhotoUrls(
        "http://s3_images/images_prefix/234/photo_new/full"
      )
      .setWebsiteUrl("salon_url")
      .setEditorInfo(
        DealerModerationEvent.Editor
          .newBuilder()
          .setId(533L)
          .setEditedAt(Instant.now().toProtobufTimestamp)
      )

    val res = dealerModerationConverter.convert(
      dealer,
      managerOpt = Some(manager),
      SalonInfoMock.salonInfo,
      userId = 533L)

    res.getId shouldBe expectedDealerModeration.getId
    res.getLogoUrl shouldBe expectedDealerModeration.getLogoUrl
    res.getDescription shouldBe expectedDealerModeration.getDescription
    res.getName shouldBe expectedDealerModeration.getName
    res.getOrigin shouldBe expectedDealerModeration.getOrigin
    res.getUserIdsList shouldBe expectedDealerModeration.getUserIdsList
    res.getEmail shouldBe expectedDealerModeration.getEmail
    res.getCompanyGroup shouldBe expectedDealerModeration.getCompanyGroup
    res.getRealEstateInfo shouldBe expectedDealerModeration.getRealEstateInfo
    res.getAgency shouldBe expectedDealerModeration.getAgency
    res.getMarksList shouldBe expectedDealerModeration.getMarksList
    res.getPersonalManager shouldBe expectedDealerModeration.getPersonalManager
    res.getStatus shouldBe expectedDealerModeration.getStatus
    res.getPhonesList shouldBe expectedDealerModeration.getPhonesList
    res.getCreatedAt.getSeconds shouldBe expectedDealerModeration.getCreatedAt.getSeconds
    res.getPhotoUrlsList shouldBe expectedDealerModeration.getPhotoUrlsList
    res.getWebsiteUrl shouldBe expectedDealerModeration.getWebsiteUrl
    res.getEditorInfo.getId shouldBe expectedDealerModeration.getEditorInfo.getId
  }

}
