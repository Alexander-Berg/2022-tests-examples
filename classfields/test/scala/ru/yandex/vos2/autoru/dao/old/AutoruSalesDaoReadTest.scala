package ru.yandex.vos2.autoru.dao.old

import java.text.SimpleDateFormat

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.AutoruSale

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 26.09.16
  */
@RunWith(classOf[JUnitRunner])
class AutoruSalesDaoReadTest extends AnyFunSuite with OptionValues with InitTestDbs with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initOldSalesDbs()
  }

  /**
    * Убеждаемся, что salonPoi успешно загружается, проверяем также телефоны и клиента внутри него.
    */
  test("test load SalonPoi") {
    // объявление от имени автосалона
    val sale = components.autoruSalesDao.getOffer(1042409964)(Traced.empty).value
    val salonPoi = sale.salonPoi.value
    assert(salonPoi.id == 9542)
    assert(salonPoi.hash.isEmpty) // пустой хеш
    assert(salonPoi.address.value == "44 км МКАД (внешняя сторона), владение 1")
    assert(salonPoi.latitude.value == 55.628967)
    assert(salonPoi.longitude.value == 37.469810)

    val phonesMap = salonPoi.phones.map(phone => (phone.id, phone)).toMap

    val phone1 = phonesMap.get(440464).value
    assert(phone1.phone == "78001001017")
    assert(phone1.phoneMask == "1:3:7")
    assert(phone1.callFrom.contains(8))
    assert(phone1.callTill.contains(21))

    val phone2 = phonesMap.get(446316).value
    assert(phone2.phone == "79263918675")
    assert(phone2.phoneMask == "1:3:7")
    assert(phone2.callFrom.contains(8))
    assert(phone2.callTill.contains(23))

    val phone3 = phonesMap.get(450121).value
    assert(phone3.phone == "79257593664")
    assert(phone3.phoneMask == "1:3:7")
    assert(phone3.callFrom.contains(9))
    assert(phone3.callTill.contains(22))

    val client = salonPoi.client.value
    assert(client.id == 10086)
    assert(client.yaCountryId.value == 225)
    assert(client.yaRegionId.value == 1)
    assert(client.yaCityId.value == 213)
    assert(client.url == "")
    assert(client.email == "vbelov@imoneybank.ru")
    assert(client.phone.value == "74957778895")
    assert(client.phoneMask.value == "1:3:7")
    assert(client.fax.isEmpty)
    assert(client.faxMask.isEmpty)
    assert(client.description == "")
    assert(client.status == "active")
    assert(client.contactName == "менеджер 925")
    assert(!client.isGoldPartner)
    assert(!client.usePremiumTariff)
  }

  /**
    * проверяем, что корректно загружается из базы и конвертируется объявление с яндекс-видео
    */
  test("test load YandexVideo") {
    // объявление с активным яндекс-видео
    val sale = components.autoruSalesDao.getOffer(1043211458)(Traced.empty).value
    val videos = sale.videos.value
    assert(videos.lengthCompare(1) == 0)
    val video1 = videos.head
    assert(video1.id == 2404332)
    assert(video1.saleId == 1043211458)
    assert(video1.provider == "Yandex")
    assert(video1.value == "http://s3-eu-west-1.amazonaws.com/s3.ovidoapp/video/086fa236040c4698846dca3d48824500.mp4")
    assert(video1.parseValue == "m-63774-156a07376d0-87d395248cef988d")
    assert(video1.videoId == 156870)
    assert(video1.createDate == DateTime.parse("2016-08-22T04:31:25"))
    assert(video1.updateDate == DateTime.parse("2016-08-22T04:31:25"))

    val yandexVideo1 = video1.yandexVideo.value
    assert(yandexVideo1.id == 309190)
    assert(yandexVideo1.status == 4)
    assert(yandexVideo1.yandexVideoType == "file")
    assert(yandexVideo1.filmN == 156870)
    assert(yandexVideo1.videoIdent.value == "m-63774-156a07376d0-87d395248cef988d")
    assert(yandexVideo1.videoUrl.nonEmpty)
    assert(yandexVideo1.videoThumbs.nonEmpty)
    assert(yandexVideo1.targetUrl.isEmpty)
    assert(yandexVideo1.progressUrl.isEmpty)
    assert(yandexVideo1.createDate == DateTime.parse("2016-08-19T04:38:41"))
    assert(yandexVideo1.updateDate.value == DateTime.parse("2016-08-19T05:47:07"))
  }

  test("DiscountPrice") {
    val sale = components.autoruSalesDao.getOffer(1037186558)(Traced.empty).value
    assert(sale.discountPrice.value == AutoruSale.DiscountPrice(1, 1037186558, 16189485, 21029, 100000.00, "active"))
  }

  val sdf1: SimpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  test("paid services") {
    val sale = components.autoruSalesDao.getOffer(1043270830)(Traced.empty).value
    val services = sale.services.value
    assert(services.lengthCompare(5) == 0)
    val badges = sale.badges.value
    assert(badges.lengthCompare(3) == 0)
    val List(s0, s1, s2, s3, s4) = services
    val List(s5, s6, s7) = badges
    assert(s0.serviceType == "package_cart")
    assert(s0.createDate.getMillis == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(!s0.isActivated)
    assert(s0.offerBilling.isEmpty)
    assert(s0.offerBillingDeadline.isEmpty)
    assert(s0.expireDate.value.getMillis == sdf1.parse("2016-08-22 14:38:06").getTime)

    assert(s1.serviceType == "package_turbo")
    assert(s1.createDate.getMillis == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(s1.isActivated)
    assert(s1.offerBilling.isEmpty)
    assert(s1.offerBillingDeadline.isEmpty)

    assert(s2.serviceType == "all_sale_color")
    assert(s2.createDate.getMillis == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(s2.isActivated)
    assert(s2.offerBilling.isEmpty)
    assert(s2.offerBillingDeadline.isEmpty)

    assert(s3.serviceType == "all_sale_special")
    assert(s3.createDate.getMillis == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(s3.isActivated)
    assert(s3.offerBilling.isEmpty)
    assert(s3.offerBillingDeadline.isEmpty)

    assert(s4.serviceType == "all_sale_toplist")
    assert(s4.createDate.getMillis == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(s4.isActivated)
    assert(s4.offerBilling.isEmpty)
    assert(s4.offerBillingDeadline.isEmpty)

    assert(s5.createDate.getMillis == sdf1.parse("2016-08-21 14:37:15").getTime)
    assert(s5.isActivated)
    assert(s5.badge == "Камера заднего вида")

    assert(s6.createDate.getMillis == sdf1.parse("2016-08-21 14:37:15").getTime)
    assert(s6.isActivated)
    assert(s6.badge == "Два комплекта резины")

    assert(s7.createDate.getMillis == sdf1.parse("2016-08-21 14:37:15").getTime)
    assert(s7.isActivated)
    assert(s7.badge == "Кожаный салон")
  }

  test("original image names") {
    //VOS-2980 images no longer loaded
    val sale1 = components.autoruSalesDao.getOffer(1044216699L)(Traced.empty).value
    assert(sale1.images.isEmpty)

    val sale2 = components.autoruSalesDao.getOfferForMigration(1044216699L).value
    assert(sale1.images.isEmpty)
  }

  test("load ips") {
    val sale1 = components.autoruSalesDao.getOffer(1044214673L)(Traced.empty).value
    assert(sale1.ip.isEmpty)

    val sale2 = components.autoruSalesDao.getOfferForMigration(1044214673L).value
    assert(sale2.ip.contains("95.220.131.125"))

    val sale3 = components.autoruSalesDao.getOfferForMigration(1044216699L).value
    assert(sale3.ip.isEmpty)

    val sale4 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    assert(sale4.ip.contains("223.227.26.179"))
  }

  test("trucks services") {
    val sale1 = components.autoruTrucksDao.getOffer(6229746L)(Traced.empty).value
    assert(sale1.services.nonEmpty)
  }

  test("client region") {
    assert(components.autoruSalonsDao.getClientRegion(10600)(Traced.empty) == 967)
    assert(components.autoruSalonsDao.getClientRegion(10086)(Traced.empty) == 213)
    assert(components.autoruSalonsDao.getClientRegion(67)(Traced.empty) == 0)
  }

  test("load source platforms") {
    // client
    val sale1 = components.autoruSalesDao.getOfferForMigration(1037186558L).value
    assert(sale1.sourceInfo.nonEmpty)
    assert(sale1.sourceInfo.value.getPlatform == SourceInfo.Platform.DESKTOP)
    assert(sale1.sourceInfo.value.getSource == SourceInfo.Source.AUTO_RU)
    assert(sale1.sourceInfo.value.getUserRef == "ac_21029")
    assert(!sale1.sourceInfo.value.getIsCallcenter)
    assert(sale1.sourceInfo.value.getParseUrl.isEmpty)

    val sale2 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    assert(sale2.sourceInfo.nonEmpty)
    assert(sale2.sourceInfo.value.getPlatform == SourceInfo.Platform.DESKTOP)
    assert(sale2.sourceInfo.value.getSource == SourceInfo.Source.AUTO24)
    assert(sale2.sourceInfo.value.getUserRef == "a_10591660")

    val sale3 = components.autoruSalesDao.getOfferForMigration(1043270830L).value
    assert(sale3.sourceInfo.nonEmpty)
    assert(sale3.sourceInfo.value.getPlatform == SourceInfo.Platform.IOS)
    assert(sale3.sourceInfo.value.getSource == SourceInfo.Source.AUTO_RU)
    assert(sale3.sourceInfo.value.getUserRef == "a_18318774")

    val sale4 = components.autoruSalesDao.getOfferForMigration(1044214673L).value
    assert(sale4.sourceInfo.nonEmpty)
    assert(sale4.sourceInfo.value.getPlatform == SourceInfo.Platform.DESKTOP)
    assert(sale4.sourceInfo.value.getSource == SourceInfo.Source.AUTO_RU)
    assert(sale4.sourceInfo.value.getUserRef == "a_18605844")
    assert(sale4.sourceInfo.value.getIsCallcenter)
    assert(sale4.sourceInfo.value.getParseUrl == "http://krasnodar.drom.ru/lada/2115/19617057.html")

    val sale5 = components.autoruSalesDao.getOfferForMigration(1044216699L).value
    assert(sale5.sourceInfo.nonEmpty)
    assert(sale5.sourceInfo.value.getPlatform == SourceInfo.Platform.MOBILE)
    assert(sale5.sourceInfo.value.getSource == SourceInfo.Source.AUTO_RU)
    assert(sale5.sourceInfo.value.getUserRef == "a_18740415")

    val sale6 = components.autoruSalesDao.getOfferForMigration(1044159039L).value
    assert(sale6.sourceInfo.isEmpty)
  }
}
