package ru.auto.cabinet.test.model

import ru.auto.cabinet.SalonModel._
import ru.auto.cabinet.model.SalonInfo
import ru.auto.cabinet.model.SalonInfo.FileUpdate

import scala.jdk.CollectionConverters._

object SalonInfoData {

  val premoderatedSerializedSalonInfo = """
a:10:{
    s:5:"title";
    s:9:"SV-Motors";
    s:3:"url";
    s:23:"http://www.svmotors.ru/";
    s:11:"description";
    s:3:"---";
    s:6:"lessor";
    s:6:"ДАС";
    s:4:"logo";
    a:3:{
        s:6:"origin";
        s:7:"123.jpg";
        s:3:"new";
        s:0:"";
        s:6:"delete";
        s:0:"";
    }
    s:5:"photo";
    a:3:{
        s:6:"origin";
        s:42:"40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg";
        s:3:"new";
        s:0:"";
        s:6:"delete";
        s:0:"";
    }
    s:3:"poi";
    a:7:{
        s:6:"geo_id";
        s:3:"213";
        s:7:"address";
        s:3:"---";
        s:3:"lat";
        s:9:"55.572231";
        s:3:"lng";
        s:9:"37.564014";
        s:10:"ya_city_id";
        s:3:"213";
        s:12:"ya_region_id";
        s:1:"1";
        s:13:"ya_country_id";
        s:3:"225";
    }
    s:16:"rent_certificate";
    a:3:{
        s:6:"origin";
        s:42:"39885-023830226bca3900fa5a3c0ccec35222.jpg";
        s:3:"new";
        s:0:"";
        s:6:"delete";
        s:0:"";
    }
    s:6:"phones";
    a:3:{
      i:0;
      a:9:{
          s:5:"title";
          s:10:"Дилер";
          s:2:"id";
          s:6:"461028";
          s:10:"delete_row";
          s:1:"1";
          s:12:"country_code";
          s:1:"7";
          s:9:"city_code";
          s:3:"926";
          s:5:"phone";
          s:7:"7178974";
          s:9:"extention";
          s:0:"";
          s:9:"call_from";
          s:1:"9";
          s:9:"call_till";
          s:2:"23";
      }
      i:1;
      a:9:{
          s:2:"id";
          s:6:"461029";
          s:10:"delete_row";
          s:0:"";
          s:12:"country_code";
          s:1:"7";
          s:9:"city_code";
          s:3:"499";
          s:5:"phone";
          s:7:"3947451";
          s:9:"extention";
          s:0:"";
          s:9:"call_from";
          s:1:"9";
          s:9:"call_till";
          s:2:"23";
          s:5:"title";
          s:10:"Дилер";
      }
      i:2;
      a:9:{
          s:2:"id";
          s:0:"";
          s:10:"delete_row";
          s:0:"";
          s:12:"country_code";
          s:1:"7";
          s:9:"city_code";
          s:3:"926";
          s:5:"phone";
          s:7:"7178974";
          s:9:"extention";
          s:0:"";
          s:9:"call_from";
          s:1:"9";
          s:9:"call_till";
          s:2:"23";
          s:5:"title";
          s:10:"Дилер";
      }
    }
    s:10:"dealership";
    a:3:{
      i:0;
      a:7:{
        s:2:"id";
        s:6:"101826";
        s:7:"mark_id";
        i:260;
        s:9:"mark_name";
        s:6:"Toyota";
        s:10:"delete_row";
        s:0:"";
        s:6:"origin";
        s:42:"40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg";
        s:6:"delete";
        s:0:"";
        s:3:"new";
        s:0:"";
      }
      i:1;
      a:7:{
        s:2:"id";
        s:0:"";
        s:7:"mark_id";
        i:260;
        s:9:"mark_name";
        s:6:"Toyota";
        s:10:"delete_row";
        s:0:"";
        s:6:"origin";
        s:0:"";
        s:6:"delete";
        s:0:"";
        s:3:"new";
        s:42:"40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg";
      }
      i:2;
      a:7:{
        s:2:"id";
        s:6:"101826";
        s:7:"mark_id";
        i:260;
        s:9:"mark_name";
        s:6:"Toyota";
        s:10:"delete_row";
        s:1:"1";
        s:6:"origin";
        s:0:"";
        s:6:"delete";
        s:42:"40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg";
        s:3:"new";
        s:0:"";
      }
    }
}"""

  val salonInfo = SalonInfo(
    title = "SV-Motors",
    url = Some("http://www.svmotors.ru/"),
    description = Some("---"),
    lessor = Some("ДАС"),
    logo = FileUpdate(List("123.jpg"), List.empty, List.empty),
    photo = FileUpdate(
      List("40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg"),
      List.empty,
      List.empty),
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
      List("39885-023830226bca3900fa5a3c0ccec35222.jpg"),
      List.empty,
      List.empty),
    phones = SalonInfo.Phone(
      title = Some("Дилер"),
      id = Some("461028"),
      deleteRow = true,
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
        id = Some("461029"),
        deleteRow = false,
        countryCode = "7",
        cityCode = "499",
        phone = "74993947451",
        localPhone = "3947451",
        phoneMask = "1:3:7",
        extention = "",
        callFrom = 9,
        callTill = 23,
        title = Some("Дилер")
      ) ::
      SalonInfo.Phone(
        id = None,
        deleteRow = false,
        countryCode = "7",
        cityCode = "926",
        phone = "79267178974",
        localPhone = "7178974",
        phoneMask = "1:3:7",
        extention = "",
        callFrom = 9,
        callTill = 23,
        title = Some("Дилер")
      ) :: Nil,
    dealership = List(
      SalonInfo.Dealership(
        id = Some("101826"),
        markId = "260",
        markName = "Toyota",
        deleteRow = false,
        origin = "40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg" :: Nil,
        `new` = Nil,
        delete = Nil
      ),
      SalonInfo.Dealership(
        id = None,
        markId = "260",
        markName = "Toyota",
        deleteRow = false,
        origin = Nil,
        `new` = "40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg" :: Nil,
        delete = Nil
      ),
      SalonInfo.Dealership(
        id = Some("101826"),
        markId = "260",
        markName = "Toyota",
        deleteRow = true,
        origin = Nil,
        `new` = Nil,
        delete = "40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg" :: Nil
      )
    )
  )

  val salonInfoUpdateProto = SalonInfoUpdate
    .newBuilder()
    .setTitle("SV-Motors")
    .setUrl("http://www.svmotors.ru/")
    .setDescription("---")
    .setLessor("ДАС")
    .setPoi(
      SalonPoi
        .newBuilder()
        .setGeoId(213)
        .setAddress("---")
        .setLat(55.572231)
        .setLng(37.564014)
        .setYaCityId(213)
        .setYaRegionId(1)
        .setYaCountryId(225))
    .addAllPhones(
      (SalonPhone
        .newBuilder()
        .setTitle("Дилер")
        .setId("461028")
        .setDeleteRow(true)
        .setCountryCode("7")
        .setCityCode("926")
        .setPhone("7178974")
        .setExtention("")
        .setCallFrom(9)
        .setCallTill(23)
        .build() ::
        SalonPhone
          .newBuilder()
          .setId("461029")
          .setTitle("Дилер")
          .setDeleteRow(false)
          .setCountryCode("7")
          .setCityCode("499")
          .setPhone("3947451")
          .setExtention("")
          .setCallFrom(9)
          .setCallTill(23)
          .build() ::
        SalonPhone
          .newBuilder()
          .setTitle("Дилер")
          .setDeleteRow(false)
          .setCountryCode("7")
          .setCityCode("926")
          .setPhone("7178974")
          .setExtention("")
          .setCallFrom(9)
          .setCallTill(23)
          .build() :: Nil).asJava)
    .setLogo(SalonFileUpdate
      .newBuilder()
      .addAllOrigin(List("123.jpg").asJava)
      .build())
    .setRentCertificate(SalonFileUpdate
      .newBuilder()
      .addAllOrigin(List("39885-023830226bca3900fa5a3c0ccec35222.jpg").asJava)
      .build())
    .setPhoto(SalonFileUpdate
      .newBuilder()
      .addAllOrigin(List("40553-3d4583bc9c0d4268b07cd1b3af208e4a.jpg").asJava)
      .build())
    .setDealership(
      SalonDealershipUpdate
        .newBuilder()
        .addAllOrigin(
          List(
            SalonDealershipInfo
              .newBuilder()
              .setId("101826")
              .setMarkId("260")
              .setMarkName("Toyota")
              .setFile("40553-3d4583bc9c0d4268b07cd1b3af208e4a")
              .setFileId("40553")
              .build()).asJava)
        .addAllDelete(
          List(
            SalonDealershipInfo
              .newBuilder()
              .setId("101826")
              .setMarkId("260")
              .setMarkName("Toyota")
              .setFile("40553-3d4583bc9c0d4268b07cd1b3af208e4a")
              .setFileId("40553")
              .setFileName("3d4583bc9c0d4268b07cd1b3af208e4a")
              .build()).asJava)
        .addAllNew(
          List(
            SalonDealershipNew
              .newBuilder()
              .setMarkId("260")
              .setMarkName("Toyota")
              .setFile("40553-3d4583bc9c0d4268b07cd1b3af208e4a")
              .build()).asJava))
    .build()
  val countryId = 1L
  val regionId = 213L
  val cityId = 0L

}
