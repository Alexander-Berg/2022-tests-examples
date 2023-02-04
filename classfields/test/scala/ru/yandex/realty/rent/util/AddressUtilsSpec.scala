package ru.yandex.realty.rent.util

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.rent.model.Flat
import ru.yandex.realty.rent.proto.model.flat.FlatData
import ru.yandex.realty.util.protobuf.JsonFormat

@RunWith(classOf[JUnitRunner])
class AddressUtilsSpec extends SpecBase {

  lazy val getCases: Seq[(Flat, String, String)] = {
    val is = getClass.getResourceAsStream("addressCases.json")
    Json.parse(is).as[Seq[JsObject]].map { json =>
      val data = FlatData.newBuilder()
      val dataJson = Json.obj("location" -> (json \ "location").asOpt[JsObject]).toString()
      JsonFormat.parser().merge(dataJson, data)
      val flat = Flat(
        flatId = "000000",
        code = None,
        data = data.build(),
        address = (json \ "address").asOpt[String].getOrElse(""),
        unifiedAddress = (json \ "unifiedAddress").asOpt[String],
        flatNumber = (json \ "flatNumber").asOpt[String].getOrElse(""),
        isRented = false,
        keyCode = None,
        ownerRequests = Nil,
        assignedUsers = Map(),
        createTime = DateTime.now(),
        updateTime = DateTime.now(),
        visitTime = None,
        shardKey = 0,
        phoneFromRequest = None,
        nameFromRequest = None
      )
      (
        flat,
        (json \ "streetToFlatAddress").asOpt[String].getOrElse(""),
        (json \ "streetToHouseAddress").asOpt[String].getOrElse("")
      )
    }
  }

  "AddressUtilsSpec" should {
    "getStreetToFlatAddressWithFallbacks" in {
      getCases.foreach {
        case (flat, stf, _) =>
          AddressUtils.getStreetToFlatAddressWithFallbacks(flat) shouldBe stf
      }
    }

    "getStreetToHouseAddressWithFallbacks" in {
      getCases.foreach {
        case (flat, _, sth) =>
          AddressUtils.getStreetToHouseAddressWithFallbacks(flat) shouldBe sth
      }
    }
  }
}
