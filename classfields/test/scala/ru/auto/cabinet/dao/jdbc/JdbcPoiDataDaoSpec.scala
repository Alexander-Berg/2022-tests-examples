package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.model.Poi
import ru.auto.cabinet.model.PoiPropertiesUpdate.{
  PoiPropertyName,
  PoiPropertyValue
}
import ru.auto.cabinet.test.JdbcSpecTemplate
import slick.jdbc.MySQLProfile.api.{offsetDateTimeColumnType => _, _}

import scala.concurrent.Future

class JdbcPoiDataDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  private val poiDao = new JdbcPoiDataDao(office7Database, office7Database)
  "JdbcPoiDataDao" should {

    "return none with unknown poi_id" in {
      poiDao.get(2L).map { v =>
        v shouldBe None
      }
    }

    "update salon properties" in {
      def getCurrentProperties(poiId: Long): Future[List[PoiPropertyValue]] =
        office7Database
          .run(sql"""select value, prop_name_id from poi_prop_values
              where poi_id = $poiId
              order by prop_value_id desc"""
            .as[(String, Int)])
          .map(_.map { case (value, nameId) =>
            PoiPropertyValue(value, PoiPropertyName.withValue(nameId))
          }.toList)

      val initial = Seq(
        PoiPropertyValue("0", PoiPropertyName.AllowPhotoReorder),
        PoiPropertyValue("0", PoiPropertyName.AutoActivateCarsOffers),
        PoiPropertyValue("0", PoiPropertyName.AutoActivateCommercialOffers),
        PoiPropertyValue("0", PoiPropertyName.AutoActivateMotoOffers),
        PoiPropertyValue("0", PoiPropertyName.ChatEnabled),
        PoiPropertyValue("0", PoiPropertyName.HideLicensePlate),
        PoiPropertyValue("123456789", PoiPropertyName.OverdraftBalancePersonId),
        PoiPropertyValue("0", PoiPropertyName.OverdraftEnabled)
      )

      val updated = Seq(
        PoiPropertyValue("1", PoiPropertyName.AutoActivateMotoOffers),
        PoiPropertyValue("1", PoiPropertyName.ChatEnabled),
        PoiPropertyValue("1", PoiPropertyName.HideLicensePlate),
        PoiPropertyValue("987654321", PoiPropertyName.OverdraftBalancePersonId),
        PoiPropertyValue("1", PoiPropertyName.OverdraftEnabled)
      )

      val expected = Seq(
        PoiPropertyValue("0", PoiPropertyName.AllowPhotoReorder),
        PoiPropertyValue("0", PoiPropertyName.AutoActivateCarsOffers),
        PoiPropertyValue("0", PoiPropertyName.AutoActivateCommercialOffers),
        PoiPropertyValue("1", PoiPropertyName.AutoActivateMotoOffers),
        PoiPropertyValue("1", PoiPropertyName.ChatEnabled),
        PoiPropertyValue("1", PoiPropertyName.HideLicensePlate),
        PoiPropertyValue("987654321", PoiPropertyName.OverdraftBalancePersonId),
        PoiPropertyValue("1", PoiPropertyName.OverdraftEnabled)
      )

      for {
        emptyProperties <- getCurrentProperties(3)
        _ <- poiDao.upsertProperties(3L, initial)
        initialProperties <- getCurrentProperties(3)
        _ <- poiDao.upsertProperties(3L, updated)
        currentProperties <- getCurrentProperties(3)
      } yield {
        emptyProperties.size shouldBe 0
        initialProperties.sortBy(_.name.value) shouldBe initial.sortBy(
          _.name.value)
        currentProperties.sortBy(_.name.value) shouldBe expected.sortBy(
          _.name.value)
      }
    }

    "return base info about poi" in {
      val expected = Poi(
        poiId = 1,
        yaCountryId = 225,
        yaRegionId = 1,
        yaCityId = Some(213),
        countryId = 1,
        regionId = 87,
        cityId = Some(1123),
        lat = 55.657055,
        lng = 37.732887,
        address =
          """ул.Нижние Поля 29Б (обязательно звоните) Метро "Марьино", "Братиславская""""
      )

      for {
        basePoi <- poiDao.getPoi(1)
      } yield basePoi.get shouldBe expected
    }
  }
}
