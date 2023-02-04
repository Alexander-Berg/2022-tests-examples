package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.{Platform, Source}

/**
  * Created by andrey on 5/16/17.
  */
@RunWith(classOf[JUnitRunner])
class SourceInfoUtilsTest extends AnyFunSuite {

  test("testPlatformFromString") {
    assert(SourceInfoUtils.platformFromString("m") == Platform.MOBILE)
    assert(SourceInfoUtils.platformFromString("mobile") == Platform.MOBILE)
    assert(SourceInfoUtils.platformFromString("MOBILE") == Platform.MOBILE)
    assert(SourceInfoUtils.platformFromString("ios") == Platform.IOS)
    assert(SourceInfoUtils.platformFromString("IOS") == Platform.IOS)
    assert(SourceInfoUtils.platformFromString("android") == Platform.ANDROID)
    assert(SourceInfoUtils.platformFromString("wp") == Platform.WINDOWS_PHONE)
    assert(SourceInfoUtils.platformFromString("windows_phone") == Platform.WINDOWS_PHONE)
    assert(SourceInfoUtils.platformFromString("other") == Platform.PLATFORM_UNKNOWN)
    assert(SourceInfoUtils.platformFromString("desktop") == Platform.DESKTOP)
    assert(SourceInfoUtils.platformFromString("feed") == Platform.FEED)
  }

  test("testPlatformToString") {
    assert(SourceInfoUtils.platformToString(Platform.MOBILE) == "m")
    assert(SourceInfoUtils.platformToString(Platform.IOS) == "ios")
    assert(SourceInfoUtils.platformToString(Platform.ANDROID) == "android")
    assert(SourceInfoUtils.platformToString(Platform.WINDOWS_PHONE) == "wp")
    assert(SourceInfoUtils.platformToString(Platform.DESKTOP) == "desktop")
    assert(SourceInfoUtils.platformToString(Platform.PLATFORM_UNKNOWN) == "other")
    assert(SourceInfoUtils.platformToString(Platform.FEED) == "feed")
  }

  test("testStringToSource") {
    assert(SourceInfoUtils.sourceFromString("auto_ru") == Source.AUTO_RU)
    assert(SourceInfoUtils.sourceFromString("avito") == Source.AVITO)
    assert(SourceInfoUtils.sourceFromString("drom") == Source.DROM)
    assert(SourceInfoUtils.sourceFromString("hsd") == Source.HSD)
    assert(SourceInfoUtils.sourceFromString("auto24") == Source.AUTO24)
    assert(SourceInfoUtils.sourceFromString("AUTO_RU") == Source.AUTO_RU)
    assert(SourceInfoUtils.sourceFromString("AVITO") == Source.AVITO)
    assert(SourceInfoUtils.sourceFromString("e1") == Source.E1)
  }

  test("testSourceToString") {
    assert(SourceInfoUtils.sourceToString(Source.AUTO_RU) == "auto_ru")
    assert(SourceInfoUtils.sourceToString(Source.AVITO) == "avito")
    assert(SourceInfoUtils.sourceToString(Source.DROM) == "drom")
    assert(SourceInfoUtils.sourceToString(Source.HSD) == "hsd")
    assert(SourceInfoUtils.sourceToString(Source.AUTO24) == "auto24")
  }

}
