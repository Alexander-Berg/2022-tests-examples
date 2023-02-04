package ru.yandex.vos2.autoru.dao.old

import java.sql.Timestamp
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicLong

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.baker.util.TracedUtils
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.model.AutoruCatalogModels.Modification
import ru.yandex.vos2.autoru.model.AutoruSale.{Badge, DiscountPrice, Phone, PhonesRedirect, SaleEmail, Setting, Video}
import ru.yandex.vos2.autoru.model._
import ru.yandex.vos2.autoru.services.SettingAliases
import ru.yandex.vos2.autoru.utils.testforms.TestFormsGenerator
import ru.yandex.vos2.dao.utils.{JdbcTemplateWrapper, SimpleRowMapper}

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 10/28/16.
  */
@RunWith(classOf[JUnitRunner])
class AutoruSalesDaoWriteTest extends AnyFunSuite with InitTestDbs with OptionValues with BeforeAndAfterAll {

  implicit private val trace: Traced = TracedUtils.empty

  override def beforeAll(): Unit = {
    initDbs()
  }

  private val newPrivateSale = AutoruSale(
    id = 0,
    hash = "G4GLTScd",
    createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
    anyUpdateTime = new DateTime(2016, 10, 28, 18, 31, 26, 0),
    expireDate = new DateTime(2016, 11, 28, 18, 31, 26, 0),
    freshDate = None,
    status = AutoruSaleStatus.STATUS_CREATED_BY_CLIENT,
    sectionId = 1,
    userId = 18318774,
    poiId = None,
    countryId = 0,
    regionId = 0,
    cityId = 0,
    yaCountryId = 225,
    yaRegionId = 1,
    yaCityId = 21652,
    clientId = 0,
    contactId = 0,
    newClientId = 0,
    salonId = 0,
    _salonContactId = 0,
    year = 2011,
    price = 500000.56,
    currency = "RUR",
    priceRur = 0.0,
    _markId = 109,
    modificationId = 58238,
    _folderId = 30907,
    description =
      "Своевременное обслуживание. Пройдены все ТО. Куплена не в кредит. Непрокуренный салон. Сервисная книжка. Не участвовала в ДТП.",
    settings = Some(
      List(
        Setting(saleId = 0, alias = SettingAliases.DRIVE, settingId = 10, value = "180"),
        Setting(saleId = 0, alias = SettingAliases.STATE, settingId = 19, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.COLOR, settingId = 21, value = "20"),
        Setting(saleId = 0, alias = SettingAliases.WHEEL, settingId = 22, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.CUSTOM, settingId = 27, value = "2"),
        Setting(saleId = 0, alias = SettingAliases.AVAILABILITY, settingId = 28, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.EXCHANGE, settingId = 29, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.METALLIC, settingId = 38, value = "0"),
        Setting(saleId = 0, alias = SettingAliases.GEARBOX, settingId = 58, value = "1413"),
        Setting(saleId = 0, alias = SettingAliases.OWNERS_NUMBER, settingId = 15, value = "2"),
        Setting(saleId = 0, alias = SettingAliases.RUN, settingId = 18, value = "200000"),
        Setting(saleId = 0, alias = SettingAliases.USERNAME, settingId = 32, value = "Христофор"),
        Setting(saleId = 0, alias = SettingAliases.PTS, settingId = 413, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.NOTDISTURB, settingId = 458, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.VIRTUAL_PHONE, settingId = 462, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.WARRANTY, settingId = 16, value = "1"),
        Setting(saleId = 0, alias = SettingAliases.VIN, settingId = 12, value = "ABCDEFGH1JKLM0123"),
        Setting(saleId = 0, alias = SettingAliases.STS, settingId = 441, value = "01АБ012345"),
        Setting(saleId = 0, alias = SettingAliases.WARRANTY_EXPIRE, settingId = 17, value = "2018-12"),
        Setting(saleId = 0, alias = SettingAliases.PURCHASE_DATE, settingId = 437, value = "2012-12"),
        Setting(saleId = 0, alias = SettingAliases.COMPLECTATION, settingId = 35, value = "20082323_20368960_20082325"),
        Setting(saleId = 0, alias = SettingAliases.CREDIT_DISCOUNT, settingId = 480, value = "100"),
        Setting(saleId = 0, alias = SettingAliases.INSURANCE_DISCOUNT, settingId = 482, value = "200"),
        Setting(saleId = 0, alias = SettingAliases.TRADEIN_DISCOUNT, settingId = 484, value = "300"),
        Setting(saleId = 0, alias = SettingAliases.MAX_DISCOUNT, settingId = 491, value = "600")
      )
    ),
    poi = Some(
      AutoruPoi(
        poiId = 0,
        countryId = None,
        regionId = None,
        cityId = None,
        yaCountryId = Some(225),
        yaRegionId = Some(1),
        yaCityId = Some(21652),
        address = Some("Рублёвское шоссе"),
        latitude = Some(43.905251),
        longitude = Some(30.261402)
      )
    ),
    user = Some(
      AutoruUser(
        id = 18318774,
        email = Some("superscalper@yandex.ru"),
        phones = Seq(
          UserPhone(
            id = 32624814,
            userId = Some(18318774),
            number = None,
            phone = 79161793608L,
            status = 1,
            isMain = false,
            code = 0,
            created = new DateTime(2016, 8, 21, 14, 36, 31, 0),
            updated = None
          )
        )
      )
    ),
    phones = Some(
      List(
        Phone(
          saleId = 0,
          phoneId = 32624814,
          phone = Some(
            UserPhone(
              id = 32624814,
              userId = Some(18318774),
              number = None,
              phone = 79161793608L,
              status = 1,
              isMain = false,
              code = 0,
              created = new DateTime(2016, 8, 21, 14, 36, 31, 0),
              updated = None
            )
          ),
          callFrom = 9,
          callTill = 23,
          contactName = "Христофор"
        )
      )
    ),
    salonPoiContacts = None,
    salonPoi = None,
    dealerMarks = None,
    images = Some(
      List(
        AutoruImage(
          id = 0,
          saleId = 0,
          main = true,
          order = 1,
          name = "101404-1e190a2f94f4f29a8eb7dc720d75ec51",
          originalName = Some("101404-1e190a2f94f4f29a8eb7dc720d75ec51"),
          created = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          cvHash = None,
          exifLat = None,
          exifLon = None,
          exifDate = None,
          state = ImageStates.Default
        ),
        AutoruImage(
          id = 0,
          saleId = 0,
          main = false,
          order = 2,
          name = "117946-b6d2fb88b2628038237af5c45ae1299a",
          originalName = Some("117946-b6d2fb88b2628038237af5c45ae1299a"),
          created = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          cvHash = None,
          exifLat = None,
          exifLon = None,
          exifDate = None,
          state = ImageStates.Default
        ),
        AutoruImage(
          id = 0,
          saleId = 0,
          main = false,
          order = 3,
          name = "136387-6df3831aea9cd6df09157c86b8f3d2a0",
          originalName = Some("236387-6df3831aea9cd6df09157c86b8f3d2a0"),
          created = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          cvHash = None,
          exifLat = None,
          exifLon = None,
          exifDate = None,
          state = ImageStates.Blur
        )
      )
    ),
    videos = Some(
      List(
        Video(
          id = 0,
          saleId = 0,
          provider = "Yandex",
          value = "",
          parseValue = "",
          videoId = 123620,
          createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          updateDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          yandexVideo = None
        )
      )
    ),
    modification = Some(
      Modification(
        id = 58238,
        label = None,
        markId = 109,
        folderId = 30907,
        techParamId = Some(6143500),
        configurationId = Some(6143425),
        startYear = 2010,
        endYear = Some(2013),
        properties = Map(
          "turbo_type" -> "1488",
          "drive" -> "1074",
          "body_size" -> "4410",
          "weight" -> "1525",
          "acceleration" -> "10,1",
          "consumption_city" -> "9,1",
          "clearance" -> "170",
          "tank_volume" -> "58",
          "front_suspension" -> "1514",
          "body_type" -> "1358",
          "engine_order" -> "1458",
          "consumption_mixed" -> "7,1",
          "gearbox_type" -> "1414",
          "power_system" -> "1480",
          "cylinders_value" -> "4",
          "front_brake" -> "1548",
          "compression" -> "16",
          "cylinders_order" -> "1446",
          "front_wheel_base" -> "1591",
          "gears_count" -> "6",
          "engine_volume" -> "1995",
          "power_kvt" -> "135",
          "height" -> "1660",
          "engine_type" -> "1260",
          "diametr" -> "84.0x90.0",
          "fuel" -> "1504",
          "engine_power" -> "184",
          "wheel_size" -> "225/60/R17",
          "max_speed" -> "195",
          "boot_volume_max" -> "1436",
          "back_suspension" -> "1534",
          "doors_count" -> "5",
          "moment_rpm" -> "1800",
          "moment" -> "392",
          "back_brake" -> "1558",
          "consumption_hiway" -> "6",
          "back_wheel_base" -> "1592",
          "valvetrain" -> "1466",
          "boot_volume_min" -> "591",
          "power_rpm" -> "4000",
          "seats" -> "134",
          "valves" -> "4",
          "width" -> "1820",
          "full_weight" -> "2140",
          "wheel_base" -> "2640"
        )
      )
    ),
    folders = None,
    services = None,
    badges = Some(
      List(
        Badge(
          id = 0,
          saleId = 0,
          categoryId = 15,
          createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          isActivated = false,
          badge = "Парктроник"
        ),
        Badge(
          id = 0,
          saleId = 0,
          categoryId = 15,
          createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          isActivated = false,
          badge = "Кожаный салон"
        ),
        Badge(
          id = 0,
          saleId = 0,
          categoryId = 15,
          createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
          isActivated = false,
          badge = "Коврики в подарок"
        )
      )
    ),
    phonesRedirect =
      Some(PhonesRedirect(id = 0, saleId = 0, active = true, updated = new DateTime(2016, 10, 28, 18, 31, 26, 0))),
    recallReason = None,
    discountPrice = Some(
      DiscountPrice(
        id = 0,
        saleId = 0,
        userId = 18318774,
        clientId = 0,
        price = 100500,
        status = "active"
      )
    )
  )

  private val privateSaleId = 1043270830L

  private val editPrivateSale = AutoruSale(
    id = privateSaleId,
    hash = "6b56a",
    createDate = new DateTime(2016, 8, 21, 14, 36, 59, 0),
    anyUpdateTime = new DateTime(2016, 10, 28, 18, 38, 44, 0),
    expireDate = new DateTime(2016, 10, 20, 14, 37, 2, 0),
    freshDate = None,
    status = AutoruSaleStatus.STATUS_HIDDEN,
    sectionId = 1,
    userId = 18318774,
    poiId = Some(1039076492),
    countryId = 0,
    regionId = 0,
    cityId = 0,
    yaCountryId = 225,
    yaRegionId = 1,
    yaCityId = 21652,
    clientId = 0,
    contactId = 0,
    newClientId = 0,
    salonId = 0,
    _salonContactId = 0,
    year = 2011,
    price = 500000.56,
    currency = "RUR",
    priceRur = 0.0,
    _markId = 109,
    modificationId = 58238,
    _folderId = 30907,
    description =
      "Своевременное обслуживание. Пройдены все ТО. Куплена не в кредит. Непрокуренный салон. Сервисная книжка. Не участвовала в ДТП.",
    settings = Some(
      List(
        Setting(saleId = privateSaleId, alias = SettingAliases.DRIVE, settingId = 10, value = "180"),
        Setting(saleId = privateSaleId, alias = SettingAliases.STATE, settingId = 19, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.COLOR, settingId = 21, value = "20"),
        Setting(saleId = privateSaleId, alias = SettingAliases.WHEEL, settingId = 22, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.CUSTOM, settingId = 27, value = "2"),
        Setting(saleId = privateSaleId, alias = SettingAliases.AVAILABILITY, settingId = 28, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.EXCHANGE, settingId = 29, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.METALLIC, settingId = 38, value = "0"),
        Setting(saleId = privateSaleId, alias = SettingAliases.GEARBOX, settingId = 58, value = "1413"),
        Setting(saleId = privateSaleId, alias = SettingAliases.OWNERS_NUMBER, settingId = 15, value = "2"),
        Setting(saleId = privateSaleId, alias = SettingAliases.RUN, settingId = 18, value = "200000"),
        Setting(saleId = privateSaleId, alias = SettingAliases.USERNAME, settingId = 32, value = "Христофор"),
        Setting(saleId = privateSaleId, alias = SettingAliases.PTS, settingId = 413, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.NOTDISTURB, settingId = 458, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.VIRTUAL_PHONE, settingId = 462, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.WARRANTY, settingId = 16, value = "1"),
        Setting(saleId = privateSaleId, alias = SettingAliases.VIN, settingId = 12, value = "ABCDEFGH1JKLM0123"),
        Setting(saleId = privateSaleId, alias = SettingAliases.STS, settingId = 441, value = "01АБ012345"),
        Setting(saleId = privateSaleId, alias = SettingAliases.WARRANTY_EXPIRE, settingId = 17, value = "2018-12"),
        Setting(saleId = privateSaleId, alias = SettingAliases.PURCHASE_DATE, settingId = 437, value = "2012-12"),
        Setting(
          saleId = privateSaleId,
          alias = SettingAliases.COMPLECTATION,
          settingId = 35,
          value = "20082323_20368960_20082325"
        )
      )
    ),
    poi = Some(
      AutoruPoi(
        poiId = 1039076492,
        countryId = None,
        regionId = None,
        cityId = None,
        yaCountryId = Some(225),
        yaRegionId = Some(1),
        yaCityId = Some(21652),
        address = Some("Рублёвское шоссе"),
        latitude = Some(43.905251),
        longitude = Some(30.261402)
      )
    ),
    user = Some(
      AutoruUser(
        id = 18318774,
        email = Some("superscalper@yandex.ru"),
        phones = Seq(
          UserPhone(
            id = 32624814,
            userId = Some(18318774),
            number = None,
            phone = 79161793608L,
            status = 1,
            isMain = false,
            code = 0,
            created = new DateTime(2016, 8, 21, 14, 36, 31, 0),
            updated = None
          )
        )
      )
    ),
    phones = Some(
      List(
        Phone(
          saleId = privateSaleId,
          phoneId = 32624814,
          phone = Some(
            UserPhone(
              id = 32624814,
              userId = Some(18318774),
              number = None,
              phone = 79161793608L,
              status = 1,
              isMain = false,
              code = 0,
              created = new DateTime(2016, 8, 21, 14, 36, 31, 0),
              updated = None
            )
          ),
          callFrom = 9,
          callTill = 23,
          contactName = "Христофор"
        )
      )
    ),
    salonPoiContacts = None,
    salonPoi = None,
    dealerMarks = None,
    images = Some(
      List(
        AutoruImage(
          id = 0,
          saleId = privateSaleId,
          main = true,
          order = 1,
          name = "101404-1e190a2f94f4f29a8eb7dc720d75ec51",
          originalName = None,
          created = new DateTime(2016, 10, 28, 18, 38, 44, 0),
          cvHash = None,
          exifLat = None,
          exifLon = None,
          exifDate = None,
          state = ImageStates.Default
        ),
        AutoruImage(
          id = 0,
          saleId = privateSaleId,
          main = false,
          order = 2,
          name = "117946-b6d2fb88b2628038237af5c45ae1299a",
          originalName = None,
          created = new DateTime(2016, 10, 28, 18, 38, 44, 0),
          cvHash = None,
          exifLat = None,
          exifLon = None,
          exifDate = None,
          state = ImageStates.Default
        ),
        AutoruImage(
          id = 681188,
          saleId = privateSaleId,
          main = false,
          order = 3,
          name = "136387-6df3831aea9cd6df09157c86b8f3d2a0",
          originalName = None,
          created = new DateTime(2016, 8, 21, 14, 37, 0, 0),
          cvHash = Some("M4FD7C3DA008EDCFB"),
          exifLat = None,
          exifLon = None,
          exifDate = Some(new DateTime(1980, 1, 1, 0, 0, 1, 0)),
          state = ImageStates.Default
        )
      )
    ),
    videos = Some(
      List(
        Video(
          id = 0,
          saleId = privateSaleId,
          provider = "Yandex",
          value = "",
          parseValue = "",
          videoId = 123620,
          createDate = new DateTime(2016, 10, 28, 18, 38, 44, 0),
          updateDate = new DateTime(2016, 10, 28, 18, 38, 44, 0),
          yandexVideo = None
        )
      )
    ),
    modification = Some(
      Modification(
        id = 58238,
        label = None,
        markId = 109,
        folderId = 30907,
        techParamId = Some(6143500),
        configurationId = Some(6143425),
        startYear = 2010,
        endYear = Some(2013),
        properties = Map(
          "turbo_type" -> "1488",
          "drive" -> "1074",
          "body_size" -> "4410",
          "weight" -> "1525",
          "acceleration" -> "10,1",
          "consumption_city" -> "9,1",
          "clearance" -> "170",
          "tank_volume" -> "58",
          "front_suspension" -> "1514",
          "body_type" -> "1358",
          "engine_order" -> "1458",
          "consumption_mixed" -> "7,1",
          "gearbox_type" -> "1414",
          "power_system" -> "1480",
          "cylinders_value" -> "4",
          "front_brake" -> "1548",
          "compression" -> "16",
          "cylinders_order" -> "1446",
          "front_wheel_base" -> "1591",
          "gears_count" -> "6",
          "engine_volume" -> "1995",
          "power_kvt" -> "135",
          "height" -> "1660",
          "engine_type" -> "1260",
          "diametr" -> "84.0x90.0",
          "fuel" -> "1504",
          "engine_power" -> "184",
          "wheel_size" -> "225/60/R17",
          "max_speed" -> "195",
          "boot_volume_max" -> "1436",
          "back_suspension" -> "1534",
          "doors_count" -> "5",
          "moment_rpm" -> "1800",
          "moment" -> "392",
          "back_brake" -> "1558",
          "consumption_hiway" -> "6",
          "back_wheel_base" -> "1592",
          "valvetrain" -> "1466",
          "boot_volume_min" -> "591",
          "power_rpm" -> "4000",
          "seats" -> "134",
          "valves" -> "4",
          "width" -> "1820",
          "full_weight" -> "2140",
          "wheel_base" -> "2640"
        )
      )
    ),
    folders = None,
    services = None,
    badges = Some(
      List(
        Badge(
          id = 0,
          saleId = privateSaleId,
          categoryId = 15,
          createDate = new DateTime(2016, 11, 1, 14, 35, 18, 0),
          isActivated = false,
          badge = "Парктроник"
        ),
        Badge(
          id = 4552,
          saleId = privateSaleId,
          categoryId = 15,
          createDate = new DateTime(2016, 8, 21, 14, 37, 15, 0),
          isActivated = true,
          badge = "Кожаный салон"
        ),
        Badge(
          id = 0,
          saleId = privateSaleId,
          categoryId = 15,
          createDate = new DateTime(2016, 11, 1, 14, 35, 18, 0),
          isActivated = false,
          badge = "Коврики в подарок"
        ),
        Badge(
          id = 4548,
          saleId = privateSaleId,
          categoryId = 15,
          createDate = new DateTime(2016, 8, 21, 14, 37, 15, 0),
          isActivated = true,
          badge = "Камера заднего вида"
        ),
        Badge(
          id = 4550,
          saleId = privateSaleId,
          categoryId = 15,
          createDate = new DateTime(2016, 8, 21, 14, 37, 15, 0),
          isActivated = true,
          badge = "Два комплекта резины"
        )
      )
    ),
    phonesRedirect = Some(
      PhonesRedirect(id = 0, saleId = privateSaleId, active = true, updated = new DateTime(2016, 10, 28, 18, 38, 44, 0))
    ),
    recallReason = Some(
      RecallReason(
        saleId = privateSaleId,
        userId = 18318774,
        reasonId = 5,
        manyCalls = true,
        updated = new DateTime(2016, 8, 30, 10, 24, 15, 0),
        price = None
      )
    ),
    discountPrice = Some(
      DiscountPrice(
        id = 0,
        saleId = 1043270830,
        userId = 18318774,
        clientId = 0,
        price = 200500,
        status = "inactive"
      )
    )
  )

  private val salesShard = components.oldSalesDatabase
  private val autoruSalesDao = components.autoruSalesDao
  private val autoruTrucksDao = components.autoruTrucksDao

  test("saveNewOffer") {
    val insertedSales = autoruSalesDao.saveOffers(Seq(newPrivateSale), isNew = true)(Traced.empty)
    assert(insertedSales.head.id == 1044216700)
    checkInserted(insertedSales.head, autoruSalesDao.getOfferForMigration(1044216700).value)
  }

  private val salesJdbc: JdbcTemplateWrapper = components.oldSalesDatabase.master.jdbc
  private val officeJdbc: JdbcTemplateWrapper = components.oldOfficeDatabase.master.jdbc

  test("transaction write") {
    val start = System.currentTimeMillis + 2000

    def createWorker(i: Int, data: Array[ArrayBlockingQueue[Long]]) = {
      new Thread(new Runnable {
        override def run(): Unit = {
          Thread.sleep(start - System.currentTimeMillis)
          val workerData = data(i)
          var workerTries = 1000
          while (workerTries > 0) {
            val nextId = autoruSalesDao.getNextSaleId()
            workerData.add(nextId)
            Thread.sleep(10)
            workerTries = workerTries - 1
          }
        }
      })
    }
    val NumThreads = 16
    val data: Array[ArrayBlockingQueue[Long]] =
      (0 until NumThreads).map(i => new ArrayBlockingQueue[Long](2000)).toArray

    val workers = (0 until NumThreads).map { i =>
      val t = createWorker(i, data)
      t.start()
      t
    }

    workers.foreach(_.join())

    val res = data.flatMap(_.iterator().asScala.toSeq)
    val length = res.length
    val setLength = res.toSet.size
    log.info(s"got length $length")
    log.info(s"got length set $setLength")
    assert(length > 0)
    assert(length == setLength)
  }

  test("updatedOffer") {
    assert(
      salesJdbc
        .query(
          "select id from all7.video_sales_yandex where film_n = 100500",
          new SimpleRowMapper[java.lang.Long](rs => rs.getLong(1))
        )
        .asScala
        .nonEmpty
    )
    val updatedSales = autoruSalesDao.saveOffers(Seq(editPrivateSale), isNew = false)(Traced.empty)
    assert(updatedSales.head.id == editPrivateSale.id)
    checkInserted(updatedSales.head, autoruSalesDao.getOffer(editPrivateSale.id)(Traced.empty).value)
    // проверяем, что запись из связанной таблицы video_sales_yandex была удалена
    assert(
      salesJdbc
        .query(
          "select id from all7.video_sales_yandex where film_n = 100500",
          new SimpleRowMapper[java.lang.Long](rs => rs.getLong(1))
        )
        .isEmpty
    )
  }

  test("updateDiscount") {
    val updatedSale = autoruSalesDao.saveOffer(
      editPrivateSale.copy(discountPrice =
        Some(
          DiscountPrice(
            id = 0,
            saleId = editPrivateSale.id,
            userId = editPrivateSale.userId,
            clientId = 0,
            price = 300500,
            status = "inactive"
          )
        )
      ),
      isNew = false
    )(Traced.empty)

    checkInserted(updatedSale, autoruSalesDao.getOffer(editPrivateSale.id)(Traced.empty).value)
  }

  test("activate") {
    // сделаем типа мы создали объявление месяц назад, скрыли неделю назад
    val now = new DateTime()
    salesJdbc.update(
      "update all7.sales set create_date = ? where id = ?",
      new Timestamp(now.minusMonths(1).getMillis),
      Long.box(editPrivateSale.id)
    )
    officeJdbc.update(
      "update  users.reason_archive_users set date_update = ? where sale_id = ?",
      new Timestamp(now.minusDays(7).getMillis),
      Long.box(editPrivateSale.id)
    )
    autoruSalesDao.activate(
      editPrivateSale.id,
      editPrivateSale.hash,
      Some(editPrivateSale.userRef),
      needActivation = false
    )(Traced.empty)
    val offer = autoruSalesDao.getOffer(editPrivateSale.id)(Traced.empty).value
    assert(offer.status == AutoruSaleStatus.STATUS_SHOW)
    // expire_date должен продлиться на два месяца
    assert(
      AutoruCommonLogic.expireDate(new DateTime()).getMillis - offer.expireDate.getMillis < 1000
    )
  }

  private def checkInserted(insertedSale: AutoruSale, insertedSaleFromDb: AutoruSale): Unit = {
    assert(insertedSale.hash == insertedSaleFromDb.hash)
    assert(insertedSale.createDate == insertedSaleFromDb.createDate)
    assert(insertedSale.anyUpdateTime == insertedSaleFromDb.anyUpdateTime)
    assert(insertedSale.expireDate == insertedSaleFromDb.expireDate)
    assert(insertedSale.freshDate == insertedSaleFromDb.freshDate)
    assert(insertedSale.status == insertedSaleFromDb.status)
    assert(insertedSale.sectionId == insertedSaleFromDb.sectionId)
    assert(insertedSale.userId == insertedSaleFromDb.userId)
    assert(insertedSale.poiId == insertedSaleFromDb.poiId)
    assert(insertedSale.countryId == insertedSaleFromDb.countryId)
    assert(insertedSale.regionId == insertedSaleFromDb.regionId)
    assert(insertedSale.cityId == insertedSaleFromDb.cityId)
    assert(insertedSale.yaCountryId == insertedSaleFromDb.yaCountryId)
    assert(insertedSale.yaRegionId == insertedSaleFromDb.yaRegionId)
    assert(insertedSale.yaCityId == insertedSaleFromDb.yaCityId)
    assert(insertedSale.clientId == insertedSaleFromDb.clientId)
    assert(insertedSale.contactId == insertedSaleFromDb.contactId)
    assert(insertedSale.newClientId == insertedSaleFromDb.newClientId)
    assert(insertedSale.salonId == insertedSaleFromDb.salonId)
    assert(insertedSale.salonContactId == insertedSaleFromDb.salonContactId)
    assert(insertedSale.year == insertedSaleFromDb.year)
    assert(insertedSale.price == insertedSaleFromDb.price)
    assert(insertedSale.currency == insertedSaleFromDb.currency)
    assert(insertedSale.priceRur == insertedSaleFromDb.priceRur)
    assert(insertedSale._markId == insertedSaleFromDb._markId)
    assert(insertedSale.modificationId == insertedSaleFromDb.modificationId)
    assert(insertedSale._folderId == insertedSaleFromDb._folderId)

    assert(insertedSale.settings.value.sortBy(_.settingId) == insertedSaleFromDb.settings.value.sortBy(_.settingId))
    assert(insertedSale.poi.value == insertedSaleFromDb.poi.value)
    assert(insertedSale.user.value == insertedSaleFromDb.user.value)
    assert(
      insertedSale.phones.value.sortBy(_.phoneId).map(_.copy(contactName = "")) == insertedSaleFromDb.phones.value
        .sortBy(_.phoneId)
    )
    assert(insertedSaleFromDb.images.isEmpty) // VOS-2980 images no longer saved
    assert(insertedSale.videos.value.sortBy(_.id) == insertedSaleFromDb.videos.value.sortBy(_.id))
    assert(insertedSale.modification == insertedSaleFromDb.modification)
    assert(insertedSale.badges.value.sortBy(_.badge) == insertedSaleFromDb.badges.value.sortBy(_.badge))
    assert(insertedSale.phonesRedirect.value == insertedSaleFromDb.phonesRedirect.value)
    assert(insertedSale.discountPrice.value == insertedSaleFromDb.discountPrice.value)
  }

  test("setStatus") {
    val saleId = 1044216699
    val result0 =
      components.autoruSalesDao.setStatus(saleId, Seq(AutoruSaleStatus.STATUS_SHOW), AutoruSaleStatus.STATUS_SHOW)
    assert(!result0)
    val offer0 = components.autoruSalesDao.getOffer(saleId)(Traced.empty).value
    assert(System.currentTimeMillis() - offer0.anyUpdateTime.getMillis > 1000)
    val result = components.autoruSalesDao.setStatus(saleId, Seq(offer0.status), AutoruSaleStatus.STATUS_SHOW)
    assert(result)
    val offer1 = components.autoruSalesDao.getOffer(saleId)(Traced.empty).value
    assert(offer1.status == AutoruSaleStatus.STATUS_SHOW)
    assert(System.currentTimeMillis() - offer1.anyUpdateTime.getMillis < 1000)
    val result2 = components.autoruSalesDao.setStatus(saleId, Seq(AutoruSaleStatus.STATUS_SHOW), offer0.status)
    assert(result2)
    val offer2 = components.autoruSalesDao.getOffer(saleId)(Traced.empty).value
    assert(offer2.status == offer0.status)
    assert(System.currentTimeMillis() - offer2.anyUpdateTime.getMillis < 1000)
  }

  test("deactivateServices") {
    val result1 = autoruSalesDao.deactivateServices(1043270830L)
    assert(result1)
    val sale = autoruSalesDao.getOffer(1043270830L).value
    assert(sale.services.value.forall(!_.isActivated))

    // для траксов тест!
    val result2 = autoruTrucksDao.deactivateServices(6229746L)
    assert(result2)
    val truckSale = autoruTrucksDao.getOffer(6229746L).value
    assert(truckSale.services.value.forall(!_.isActivated))
  }

  test("save ip") {
    // при сохранеии в старую базу сохраняем айпишник
    val insertedSale = autoruSalesDao.saveOffer(newPrivateSale.copy(ip = Some("223.227.26.179")), isNew = true)
    assert(insertedSale.id != 0)
    val saleFromDb = autoruSalesDao.getOfferForMigration(insertedSale.id).value
    assert(saleFromDb.ip.contains("223.227.26.179"))

    // при обновлении айпишник не меняется
    autoruSalesDao.saveOffer(saleFromDb.copy(ip = Some("9.9.9.9")), isNew = false)
    val saleFromDb2 = autoruSalesDao.getOfferForMigration(insertedSale.id).value
    assert(saleFromDb2.ip.contains("223.227.26.179"))

    // если при сохранении в качестве айпишника передана ересь, игнорируем ее и не падаем
    val insertedSale2 = autoruSalesDao.saveOffer(newPrivateSale.copy(ip = Some("fgsfds")), isNew = true)
    assert(insertedSale2.id != 0)
    val saleFromDb3 = autoruSalesDao.getOfferForMigration(insertedSale2.id).value
    assert(saleFromDb3.ip.isEmpty)
  }

  test("do not save description") {
    val insertedSale = autoruSalesDao.saveOffer(newPrivateSale.copy(description = "T\uD83D\uDE06"), isNew = true)
    assert(insertedSale.id != 0)
    val saleFromDb = autoruSalesDao.getOfferForMigration(insertedSale.id).value
    assert(saleFromDb.description == "")
  }

  test("save emails and tasks for them") {
    val sale = newPrivateSale.copy(email = Some(SaleEmail(0, newPrivateSale.id, "test@example.com", "hash1")))
    val insertedSale = autoruSalesDao.saveOffer(sale, isNew = true)
    assert(insertedSale.email.value.saleId == insertedSale.id)
    assert(insertedSale.email.value.id != 0)
    val saleEmail = salesShard.master.jdbc
      .query(
        "select * from all7.emails_sales where id = ?",
        SimpleRowMapper(rs => {
          SaleEmail(rs.getLong("id"), rs.getLong("sale_id"), rs.getString("email"), rs.getString("hash"))
        }),
        Long.box(insertedSale.email.value.id)
      )
      .asScala
      .headOption
    assert(saleEmail.value == insertedSale.email.value)
    val saleFromDb = autoruSalesDao.getOfferForMigration(insertedSale.id).value
    assert(saleFromDb.email == saleEmail)
  }

  test("set archive") {
    val sale = newPrivateSale.copy(status = AutoruSaleStatus.STATUS_SHOW)
    val insertedSale = autoruSalesDao.saveOffer(sale, isNew = true)

    val resultArchiveTrue =
      autoruSalesDao.setArchive(insertedSale.id, insertedSale.hash, Some(insertedSale.userRef), archive = true)
    val afterSetArchiveTrue = autoruSalesDao.getOffer(insertedSale.id)
    val resultArchiveFalse =
      autoruSalesDao.setArchive(insertedSale.id, insertedSale.hash, Some(insertedSale.userRef), archive = false)
    val afterSetArchiveFalse = autoruSalesDao.getOffer(insertedSale.id)

    assert(insertedSale.status == AutoruSaleStatus.STATUS_SHOW)
    assert(resultArchiveTrue)
    assert(afterSetArchiveTrue.nonEmpty)
    assert(afterSetArchiveTrue.get.status == AutoruSaleStatus.STATUS_DELETED_BY_USER)
    assert(resultArchiveFalse)
    assert(afterSetArchiveFalse.nonEmpty)
    assert(afterSetArchiveFalse.get.status == AutoruSaleStatus.STATUS_WAITING_ACTIVATION)
  }

  test("set archive batch") {
    val sales = (1 to 2).map(idx => newPrivateSale.copy(status = AutoruSaleStatus.STATUS_SHOW))
    val insertedSale = sales.map(sale => autoruSalesDao.saveOffer(sale, isNew = true))

    val ids = insertedSale.map(sale => AutoruOfferID(sale.id, Some(sale.hash)))
    autoruSalesDao.setArchiveBatch(ids, archive = true)

    val afterSetArchiveTrue = ids.map(id => autoruSalesDao.getOffer(id.id))
    autoruSalesDao.setArchiveBatch(ids, archive = false)
    val afterSetArchiveFalse = ids.map(id => autoruSalesDao.getOffer(id.id))

    assert(insertedSale.forall(_.status == AutoruSaleStatus.STATUS_SHOW))
    assert(afterSetArchiveTrue.nonEmpty)
    assert(afterSetArchiveTrue.forall(_.get.status == AutoruSaleStatus.STATUS_DELETED_BY_USER))
    assert(afterSetArchiveFalse.nonEmpty)
    assert(afterSetArchiveFalse.forall(_.get.status == AutoruSaleStatus.STATUS_WAITING_ACTIVATION))
  }

  private val testFormsGenerator = new TestFormsGenerator(components)

  test("ipv6") {
    val ip = "2a02:6b8:0:c3e::1:4b"
    val sale = testFormsGenerator.carTestForms.createAutoruSale(formWriteParams = FormWriteParams(ip = Some(ip)))
    val insertedSale = autoruSalesDao.saveOffer(sale, isNew = true)
    val sale2 = autoruSalesDao.getOfferForMigration(insertedSale.id).value
    assert(sale2.ip.value == ip)
    val (ipValue, ipPlainValue) = components.oldSalesDatabase.master.jdbc
      .query(
        "select ip, ip_plain from ips where sale_id = ?",
        SimpleRowMapper(rs => (rs.getInt(1), rs.getString(2))),
        Long.box(sale2.id)
      )
      .asScala
      .head
    assert(ipValue == 0)
    assert(ipPlainValue == ip)
  }

  test("save vin to all7.sales_vin") {
    val sale = testFormsGenerator.carTestForms.createAutoruSale()
    val insertedSale = autoruSalesDao.saveOffer(sale, isNew = true)
    val sale2 = autoruSalesDao.getOfferForMigration(insertedSale.id).value

    assert(sale2.getSetting(SettingAliases.VIN) === insertedSale.getSetting(SettingAliases.VIN))

    val (_, vin1) = components.oldSalesDatabase.master.jdbc
      .query(
        "select sale_id, vin from sales_vin where sale_id = ?",
        SimpleRowMapper(rs => (rs.getLong(1), rs.getString(2))),
        Long.box(sale2.id)
      )
      .asScala
      .head

    assert(vin1 === sale.getSetting(SettingAliases.VIN).get)

    val sale3 = insertedSale.copy(settings = Some(Seq(AutoruSale.Setting(sale.id, SettingAliases.VIN, 123, "abcd"))))
    autoruSalesDao.saveOffer(sale3, isNew = false)

    val (_, vin2) = components.oldSalesDatabase.master.jdbc
      .query(
        "select sale_id, vin from sales_vin where sale_id = ?",
        SimpleRowMapper(rs => (rs.getLong(1), rs.getString(2))),
        Long.box(sale2.id)
      )
      .asScala
      .head

    assert(vin2 === "abcd")
  }
}
