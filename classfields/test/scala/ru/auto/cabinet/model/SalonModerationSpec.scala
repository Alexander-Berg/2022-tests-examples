package ru.auto.cabinet.model

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import SalonModeration._

class SalonModerationSpec extends FlatSpec with Matchers {

  import SalonModerationSpec._

  it should "parse php SalonModeration data" in {

    val parsed = moderationValues.map(SalonModeration.fromPHP)

    parsed should contain theSameElementsAs expected.map(Right(_))

  }

}

object SalonModerationSpec {

  val moderationValues = List(
    """a:17:{s:9:"client_id";s:4:"2120";s:17:"sale_edit_contact";b:0;s:15:"generate_origin";b:0;s:11:"_version_id";s:4:"2323";s:11:"_resolution";a:10:{s:5:"title";s:0:"";s:3:"url";s:0:"";s:6:"phones";s:0:"";s:11:"hide_phones";s:0:"";s:9:"city_name";s:0:"";s:7:"address";s:371:"Для изменения адреса необходимо прислать скан договора аренды на использование площадки: …. вашей компанией (договор субаренды, комиссии, доп.соглашение .....)    После получения док-тов Вам изменят адрес  ";s:11:"description";s:0:"";s:6:"schema";s:0:"";s:5:"photo";s:0:"";s:7:"_global";s:0:"";}s:11:"description";s:108:"Продажа грузовой техники. Грузововой автосервис. Запчасти.";s:10:"everyday24";s:0:"";s:11:"hide_phones";b:1;s:6:"origin";s:7:"spb1234";s:6:"poi_id";s:4:"2596";s:6:"submit";s:18:"Сохранить";s:5:"title";s:5:"Title";s:3:"url";s:24:"http://www.autodealer.su";s:6:"phones";a:2:{i:0;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"912";s:5:"phone";s:7:"1234567";s:9:"extention";s:0:"";s:2:"id";s:6:"205632";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"менеджер";s:9:"call_from";s:1:"9";s:9:"call_till";s:2:"20";s:10:"delete_row";s:0:"";}i:1;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"812";s:5:"phone";s:7:"1234567";s:9:"extention";s:0:"";s:2:"id";s:6:"205633";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"менеджер";s:9:"call_from";s:1:"9";s:9:"call_till";s:2:"20";s:10:"delete_row";s:0:"";}}s:5:"photo";a:3:{s:6:"origin";s:0:"";s:3:"new";s:0:"";s:6:"delete";s:0:"";}s:3:"poi";a:7:{s:10:"country_id";s:1:"1";s:9:"region_id";s:2:"89";s:7:"city_id";s:4:"1566";s:7:"address";s:103:"промзона Шушары, Московское ш.,ул.Пушкина, д. Колотушкина";s:3:"lat";s:9:"13.807843";s:3:"lng";s:9:"25.431907";s:2:"id";s:0:"";}s:6:"schema";a:3:{s:6:"origin";s:12:"cbbb6c44.jpg";s:3:"new";s:0:"";s:6:"delete";s:0:"";}}""",
    """a:25:{s:9:"client_id";i:2638;s:15:"is_gold_partner";b:0;s:17:"sale_edit_contact";b:0;s:17:"sale_edit_address";b:0;s:12:"vin_required";b:0;s:13:"call_tracking";b:0;s:15:"generate_origin";b:0;s:11:"_version_id";s:5:"76597";s:11:"_resolution";a:18:{s:4:"logo";s:0:"";s:5:"title";s:0:"";s:3:"url";s:0:"";s:17:"dealership_0_data";s:0:"";s:6:"phones";s:0:"";s:11:"hide_phones";s:0:"";s:17:"sale_edit_contact";s:0:"";s:17:"sale_edit_address";s:0:"";s:12:"vin_required";s:0:"";s:9:"city_name";s:0:"";s:7:"address";s:268:"Для изменения адреса необходимо предоставить новый, актуальный договор аренды или свидетельство о собственности, в котором прописан новый адрес.";s:5:"place";s:0:"";s:16:"rent_certificate";s:0:"";s:6:"lessor";s:0:"";s:11:"description";s:0:"";s:6:"schema";s:0:"";s:5:"photo";s:0:"";s:7:"_global";s:0:"";}s:11:"description";s:65:"Автосалон, можете купить здесь авто";s:10:"everyday24";s:1:"0";s:11:"hide_phones";b:1;s:6:"lessor";s:33:"ООО "Рога и копыта"";s:6:"origin";s:7:"msk1234";s:6:"poi_id";s:4:"2604";s:6:"submit";s:18:"Сохранить";s:5:"title";s:23:"Гагарин Авто";s:3:"url";s:23:"http://www.autosalon.ru";s:10:"dealership";a:0:{}s:4:"logo";a:3:{s:6:"origin";s:42:"43206-scb4547a7f7e3a1b9e6sss8adf2286d8.jpg";s:3:"new";s:0:"";s:6:"delete";s:0:"";}s:6:"phones";a:2:{i:0;a:12:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"916";s:5:"phone";s:7:"1234567";s:9:"extention";s:0:"";s:2:"id";s:6:"418189";s:9:"marks_ids";s:1:"0";s:11:"marks_names";s:1:"0";s:5:"title";s:12:"Сергей";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"20";s:10:"delete_row";s:0:"";s:10:"is_virtual";s:0:"";}i:1;a:12:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"910";s:5:"phone";s:7:"1234567";s:9:"extention";s:0:"";s:2:"id";s:6:"418191";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:12:"Сергей";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"20";s:10:"delete_row";s:0:"";s:10:"is_virtual";s:0:"";}}s:5:"photo";a:3:{s:6:"origin";s:42:"28818-6547af3eab342805a84e3520f5cfb397.jpg";s:3:"new";s:0:"";s:6:"delete";s:0:"";}s:3:"poi";a:10:{s:10:"country_id";i:1;s:9:"region_id";i:38;s:7:"city_id";i:873;s:7:"address";s:76:"Россия, Московская область, ул. Улица, д.8543";s:3:"lat";s:9:"32.711241";s:3:"lng";s:9:"53.123464";s:2:"id";s:0:"";s:13:"ya_country_id";i:225;s:12:"ya_region_id";i:1;s:10:"ya_city_id";i:10735;}s:16:"rent_certificate";a:3:{s:6:"origin";s:417:"28818-a132f18234723f3bf4b30affbacece25.jpg,34135-6bcf49470b1b20cda007e7469e602065.jpg,34135-e9625ba58c0bd179ad92e69f0f8c1f7c.jpg,35716-9ab28ab235359c0348e08156648ee578.jpg,35716-ce7520ec95d6b6e74bef1ce554cc54d5.jpg,39885-2cb775a951184e6b0d35e2884af776dd.jpg,39885-b2c9e26f882e68b808016c12b71f4185.jpg,40553-d04eb9bab15baec53d6ca9f3371a1d51.jpg,dcd977c4b177e172f4c46bc29e5ecda6.jpg,fda9be89ca57b02a54851579526b1888.jpg";s:3:"new";s:0:"";s:6:"delete";s:0:"";}s:6:"schema";a:3:{s:6:"origin";s:42:"34135-7c41a16f575a0bc3fed90febe2cb8f57.jpg";s:3:"new";s:0:"";s:6:"delete";s:0:"";}}""",
    """a:14:{s:11:"_resolution";s:0:"";s:10:"everyday24";s:1:"0";s:11:"hide_phones";b:1;s:9:"client_id";s:4:"2778";s:11:"description";s:0:"";s:6:"poi_id";s:4:"2744";s:6:"submit";s:18:"Сохранить";s:5:"title";s:18:"Евроспорт";s:3:"url";s:0:"";s:8:"services";a:1:{i:0;a:6:{s:2:"id";s:0:"";s:4:"name";s:0:"";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:10:"mark_limit";s:0:"";s:10:"delete_row";s:0:"";}}s:6:"phones";a:3:{i:0;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"926";s:5:"phone";s:7:"0067255";s:9:"extention";s:0:"";s:2:"id";s:6:"221752";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"Менеджер";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"21";s:10:"delete_row";s:0:"";}i:1;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"916";s:5:"phone";s:7:"0019991";s:9:"extention";s:0:"";s:2:"id";s:6:"221753";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"Менеджер";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"21";s:10:"delete_row";s:0:"";}i:2;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"903";s:5:"phone";s:7:"1253607";s:9:"extention";s:0:"";s:2:"id";s:6:"221754";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"Менеджер";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"21";s:10:"delete_row";s:0:"";}}s:5:"photo";a:3:{s:6:"origin";s:0:"";s:3:"new";s:0:"";s:6:"delete";s:0:"";}s:3:"poi";a:7:{s:10:"country_id";s:1:"1";s:9:"region_id";s:2:"87";s:7:"city_id";s:4:"1123";s:7:"address";s:16:"ул. Улица";s:3:"lat";s:9:"53.732112";s:3:"lng";s:9:"36.726348";s:2:"id";s:4:"2744";}s:6:"schema";a:3:{s:6:"origin";s:0:"";s:3:"new";s:0:"";s:6:"delete";s:0:"";}}"""
  )

  val expected = List(
    SalonModeration(
      clientId = Some(2120),
      poiId = Some(2596),
      origin = Some("spb1234"),
      title = "Title",
      description =
        Some("Продажа грузовой техники. Грузововой автосервис. Запчасти."),
      url = Some("http://www.autodealer.su"),
      poi = Poi(
        id = None,
        address = "промзона Шушары, Московское ш.,ул.Пушкина, д. Колотушкина",
        geoId = None,
        cityId = Some(1566),
        regionId = Some(89),
        countryId = Some(1),
        yaCityId = None,
        yaRegionId = None,
        yaCountryId = None,
        lat = Some(13.807843),
        lng = Some(25.431907)
      ),
      lessor = None,
      resolution = Some(
        Resolution(
          None,
          "",
          "",
          None,
          "",
          None,
          None,
          None,
          None,
          "Для изменения адреса необходимо прислать скан договора аренды на использование площадки: …. вашей компанией (договор субаренды, комиссии, доп.соглашение .....)    После получения док-тов Вам изменят адрес  ",
          "",
          "",
          None,
          "",
          "",
          Map.empty[String, String],
          None
        )),
      logo = None,
      photo = Some(Files(List(""), List(""), List(""))),
      schema = Some(Files(List("cbbb6c44.jpg"), List(""), List(""))),
      rentCertificate = None,
      phones = List(
        Phone(
          id = Some("205632"),
          title = Some("менеджер"),
          countryCode = Some("7"),
          cityCode = Some("912"),
          phone = Some("1234567"),
          extention = None,
          callFrom = Some("9"),
          callTill = Some("20"),
          isVirtual = None,
          deleteRow = None,
          marksIds = None,
          marksNames = None
        ),
        Phone(
          id = Some("205633"),
          title = Some("менеджер"),
          countryCode = Some("7"),
          cityCode = Some("812"),
          phone = Some("1234567"),
          extention = None,
          callFrom = Some("9"),
          callTill = Some("20"),
          isVirtual = None,
          deleteRow = None,
          marksIds = None,
          marksNames = None
        )
      ),
      hidePhones = Some(true),
      vinRequired = None,
      generateOrigin = Some(false),
      isGoldPartner = None,
      saleEditContact = Some(false),
      saleEditAddress = None,
      callTracking = None,
      everyday24 = None,
      submit = Some("Сохранить"),
      dealership = None,
      versionId = Some(2323),
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
    ),
    SalonModeration(
      clientId = Some(2638),
      poiId = Some(2604),
      origin = Some("msk1234"),
      title = "Гагарин Авто",
      description = Some("Автосалон, можете купить здесь авто"),
      url = Some("http://www.autosalon.ru"),
      poi = Poi(
        id = None,
        address = "Россия, Московская область, ул. Улица, д.8543",
        geoId = None,
        cityId = Some(873),
        regionId = Some(38),
        countryId = Some(1),
        yaCityId = Some(10735),
        yaRegionId = Some(1),
        yaCountryId = Some(225),
        lat = Some(32.711241),
        lng = Some(53.123464)
      ),
      lessor = Some("""ООО "Рога и копыта""""),
      resolution = Some(
        Resolution(
          None,
          "",
          "",
          None,
          "",
          None,
          None,
          None,
          None,
          "Для изменения адреса необходимо предоставить новый, актуальный договор аренды или свидетельство о собственности, в котором прописан новый адрес.",
          "",
          "",
          None,
          "",
          "",
          Map("dealership_0_data" -> ""),
          None
        )),
      logo = Some(
        Files(
          List("43206-scb4547a7f7e3a1b9e6sss8adf2286d8.jpg"),
          List(""),
          List(""))),
      photo = Some(
        Files(
          List("28818-6547af3eab342805a84e3520f5cfb397.jpg"),
          List(""),
          List(""))),
      schema = Some(
        Files(
          List("34135-7c41a16f575a0bc3fed90febe2cb8f57.jpg"),
          List(""),
          List(""))),
      rentCertificate = Some(
        Files(
          List(
            "28818-a132f18234723f3bf4b30affbacece25.jpg",
            "34135-6bcf49470b1b20cda007e7469e602065.jpg",
            "34135-e9625ba58c0bd179ad92e69f0f8c1f7c.jpg",
            "35716-9ab28ab235359c0348e08156648ee578.jpg",
            "35716-ce7520ec95d6b6e74bef1ce554cc54d5.jpg",
            "39885-2cb775a951184e6b0d35e2884af776dd.jpg",
            "39885-b2c9e26f882e68b808016c12b71f4185.jpg",
            "40553-d04eb9bab15baec53d6ca9f3371a1d51.jpg",
            "dcd977c4b177e172f4c46bc29e5ecda6.jpg",
            "fda9be89ca57b02a54851579526b1888.jpg"
          ),
          List(""),
          List("")
        )),
      phones = List(
        Phone(
          id = Some("418189"),
          title = Some("Сергей"),
          countryCode = Some("7"),
          cityCode = Some("916"),
          phone = Some("1234567"),
          extention = None,
          callFrom = Some("10"),
          callTill = Some("20"),
          isVirtual = None,
          deleteRow = None,
          marksIds = Some("0"),
          marksNames = Some("0")
        ),
        Phone(
          id = Some("418191"),
          title = Some("Сергей"),
          countryCode = Some("7"),
          cityCode = Some("910"),
          phone = Some("1234567"),
          extention = None,
          callFrom = Some("10"),
          callTill = Some("20"),
          isVirtual = None,
          deleteRow = None,
          marksIds = None,
          marksNames = None
        )
      ),
      hidePhones = Some(true),
      vinRequired = Some(false),
      generateOrigin = Some(false),
      isGoldPartner = Some(false),
      saleEditContact = Some(false),
      saleEditAddress = Some(false),
      callTracking = Some(false),
      everyday24 = Some(false),
      submit = Some("Сохранить"),
      dealership = Some(Map()),
      versionId = Some(76597),
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
    ),
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
  )

}
