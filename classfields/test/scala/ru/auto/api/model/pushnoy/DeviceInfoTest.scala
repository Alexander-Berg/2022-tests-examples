package ru.auto.api.model.pushnoy

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json._

class DeviceInfoTest extends AnyFunSuite {
  import JsonSupport._

  private val deviceInfo = DeviceInfo(
    "fingerprint",
    "manufacturer",
    "brand",
    "model",
    "device",
    "product",
    ClientOS.ANDROID,
    "name",
    "4.1.0",
    Some("gaid"),
    Some("oaid"),
    Some("idfa"),
    Some("id"),
    Some("Europe/Moscow"),
    Some("8.0"),
    Some("ANDROID_ID"),
    Some("IOS_CHECK_TOKEN")
  )

  private val json =
    """{"fingerprint":"fingerprint","manufacturer":"manufacturer","brand":"brand","model":"model",""" +
      """"device":"device","product":"product","client_os":"android","name":"name","app_version":"4.1.0",""" +
      """"gaid":"gaid","oaid":"oaid","idfa":"idfa","appmetrica_device_id":"id","timezone":"Europe/Moscow","os_version":"8.0",""" +
      """"android_id":"ANDROID_ID","ios_device_check_token":"IOS_CHECK_TOKEN"}"""

  test("json format") {
    assert(Json.toJson(deviceInfo).toString() == json)
    assert(Json.parse(json).as[DeviceInfo] == deviceInfo)
  }
}
