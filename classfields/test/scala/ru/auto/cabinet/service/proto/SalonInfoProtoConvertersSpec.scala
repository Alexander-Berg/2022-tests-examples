package ru.auto.cabinet.service.proto

import org.scalamock.scalatest.MockFactory
import ru.auto.cabinet.ApiModel.ModeratedPhonesInfo
import ru.auto.cabinet.SalonModel.SalonPhone
import ru.auto.cabinet.model.{Moderation, PoiPhone, SalonModeration}
import ru.auto.cabinet.service.MarkCatalogService
import ru.auto.cabinet.test.WordSpecBase

import java.time.OffsetDateTime

class SalonInfoProtoConvertersSpec extends WordSpecBase with MockFactory {

  private val phones = List(
    SalonModeration.Phone(
      Some("10"),
      Some("changed_title"),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    ),
    SalonModeration.Phone(
      None,
      Some("title2"),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    ),
    SalonModeration.Phone(
      None,
      Some("title3"),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )
  )

  private val salonModeration = SalonModeration(
    clientId = None,
    poiId = None,
    origin = None,
    title = "",
    description = None,
    workdays = None,
    worktime = None,
    oldContactsId = None,
    setDays = None,
    hideVinNumbers = None,
    overrideRating = None,
    callTrackingOn = None,
    allowPhotoReorder = None,
    chatEnabled = None,
    autoActivateCarsOffers = None,
    autoActivateCommercialOffers = None,
    autoActivateMotoOffers = None,
    hideLicencePlate = None,
    overdraftEnabled = None,
    overdraftBalancePersonId = None,
    url = None,
    poi = SalonModeration.Poi(
      None,
      "",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    ),
    lessor = None,
    resolution = None,
    logo = None,
    photo = None,
    schema = None,
    rentCertificate = None,
    phones = phones,
    hidePhones = None,
    vinRequired = None,
    generateOrigin = None,
    isGoldPartner = None,
    saleEditContact = None,
    saleEditAddress = None,
    callTracking = None,
    everyday24 = None,
    submit = None,
    dealership = None,
    versionId = None
  )

  "SalonInfoProtoConverter" should {
    "we should not miss any phones without ids" in {
      val markCatalog = mock[MarkCatalogService]
      val service = new SalonInfoProtoConverters(markCatalog)

      val phone1 = PoiPhone(10, "title", 2132131, "1:1:1", 10, 12)
      val phone2 = PoiPhone(11, "title", 1231231, "1:1:1", 11, 13)

      val moderatedData = Moderation[SalonModeration](
        poiId = 1,
        status = "active",
        data = salonModeration,
        date = OffsetDateTime.MIN
      )

      val moderatedPhonesInfo = service.getModeratedPhonesInfo(
        poiPhones = List(phone1, phone2),
        moderationDataOpt = Some(moderatedData)
      )

      val expectedResult = List(
        ModeratedPhonesInfo
          .newBuilder()
          .setPublicValue(
            SalonPhone
              .newBuilder()
              .setId("10")
              .setTitle("title")
              .setCountryCode("2")
              .setCityCode("1")
              .setPhone("3")
              .setCallFrom(10)
              .setCallTill(12)
              .build())
          .setOnModerateValue(SalonPhone
            .newBuilder()
            .setId("10")
            .setTitle("changed_title")
            .build())
          .build(),
        ModeratedPhonesInfo
          .newBuilder()
          .setPublicValue(
            SalonPhone
              .newBuilder()
              .setId("11")
              .setTitle("title")
              .setCountryCode("1")
              .setCityCode("2")
              .setPhone("3")
              .setCallFrom(11)
              .setCallTill(13)
              .build())
          .build(),
        ModeratedPhonesInfo
          .newBuilder()
          .setOnModerateValue(
            SalonPhone
              .newBuilder()
              .setTitle("title2")
              .build())
          .build(),
        ModeratedPhonesInfo
          .newBuilder()
          .setOnModerateValue(
            SalonPhone
              .newBuilder()
              .setTitle("title3")
              .build())
          .build()
      )

      moderatedPhonesInfo shouldBe expectedResult
    }
  }
}
