package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.test.JdbcSpecTemplate
import ru.auto.cabinet.model.{Moderation, SalonModeration}
import ru.auto.cabinet.model.SalonModeration._
import JdbcModerationDaoSpec.expectedSalonModeration

class JdbcModerationDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  private val moderationDao =
    new JdbcModerationDao(office7Database, office7Database)

  "ModerationDao" should {
    "find client on moderaion" in {
      for {
        onModeration <- moderationDao.onModeration(1L)
      } yield onModeration shouldBe true
    }

    "not find client not on moderaion" in {
      for {
        onModeration <- moderationDao.onModeration(2L)
      } yield onModeration shouldBe false
    }

    "find ban reasons" in {
      moderationDao
        .banReasons(20101)
        .map { reasons =>
          reasons should contain theSameElementsAs List("Жулики")
        }
    }

    "not find ban reasons" in {
      moderationDao
        .banReasons(1)
        .map(reasons => reasons shouldBe empty)
    }

    "retrieve moderation data for salon" in {
      moderationDao
        .getModerationByPoi(2, Moderation.SalonType)
        .map { moderation =>
          val expected = (2, "changed", expectedSalonModeration)
          val result = moderation.map { entity =>
            (entity.poiId, entity.status, entity.data)
          }
          result shouldEqual Some(expected)
        }
    }
  }

}

object JdbcModerationDaoSpec {

  val expectedSalonModeration =
    SalonModeration(
      clientId = Some(2778),
      poiId = Some(2744),
      origin = None,
      title = "Евроспорт",
      description = None,
      url = None,
      poi = Poi(
        id = Some(2744),
        address = "ул. Улица",
        geoId = None,
        cityId = Some(1123),
        regionId = Some(87),
        countryId = Some(1),
        yaCityId = None,
        yaRegionId = None,
        yaCountryId = None,
        lat = Some(53.732112),
        lng = Some(36.726348)
      ),
      lessor = None,
      resolution = None,
      logo = None,
      photo = Some(Files(List(""), List(""), List(""))),
      schema = Some(Files(List(""), List(""), List(""))),
      rentCertificate = None,
      phones = List(
        Phone(
          id = Some("221752"),
          title = Some("Менеджер"),
          countryCode = Some("7"),
          cityCode = Some("926"),
          phone = Some("0067255"),
          extention = None,
          callFrom = Some("10"),
          callTill = Some("21"),
          isVirtual = None,
          deleteRow = None,
          marksIds = None,
          marksNames = None
        ),
        Phone(
          id = Some("221753"),
          title = Some("Менеджер"),
          countryCode = Some("7"),
          cityCode = Some("916"),
          phone = Some("0019991"),
          extention = None,
          callFrom = Some("10"),
          callTill = Some("21"),
          isVirtual = None,
          deleteRow = None,
          marksIds = None,
          marksNames = None
        ),
        Phone(
          id = Some("221754"),
          title = Some("Менеджер"),
          countryCode = Some("7"),
          cityCode = Some("903"),
          phone = Some("1253607"),
          extention = None,
          callFrom = Some("10"),
          callTill = Some("21"),
          isVirtual = None,
          deleteRow = None,
          marksIds = None,
          marksNames = None
        )
      ),
      hidePhones = Some(true),
      vinRequired = None,
      generateOrigin = None,
      isGoldPartner = None,
      saleEditContact = None,
      saleEditAddress = None,
      callTracking = None,
      everyday24 = Some(false),
      submit = Some("Сохранить"),
      dealership = None,
      versionId = None,
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
      overdraftBalancePersonId = None
    )

}
