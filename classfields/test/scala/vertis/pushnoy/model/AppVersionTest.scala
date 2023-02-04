package vertis.pushnoy.model

import org.scalatest.Assertion
import vertis.pushnoy.PushnoySuiteBase
import vertis.pushnoy.model.request.params.AppVersion

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 26/11/2017.
  */
class AppVersionTest extends PushnoySuiteBase {

  test("parse app version") {
    val versionString1 = "1.2.3"
    val version1 = Some(AppVersion(1, 2, 3, 0))
    val appVersion1 = AppVersion.parse(versionString1)

    appVersion1 shouldBe version1

    val versionString2 = "1.2"
    val version2 = None
    val appVersion2 = AppVersion.parse(versionString2)

    appVersion2 shouldBe version2

    val versionString3 = "a.b.c"
    val version3 = None
    val appVersion3 = AppVersion.parse(versionString3)

    appVersion3 shouldBe version3

    val versionString4 = "1.2.3.4"
    val version4 = Some(AppVersion(1, 2, 3, 4))
    val appVersion4 = AppVersion.parse(versionString4)

    appVersion4 shouldBe version4

    val versionString5 = "4.9.0_AUTORUAPPS-5760"
    val version5 = Some(AppVersion(4, 9, 0))
    val appVersion5 = AppVersion.parse(versionString5)

    appVersion5 shouldBe version5

    val versionString6 = "4.8.0_MEIZU"
    val version6 = Some(AppVersion(4, 8, 0))
    val appVersion6 = AppVersion.parse(versionString6)

    appVersion6 shouldBe version6
  }

  test("compare major app version") {
    val version1major = AppVersion(1, 2, 3)
    val version2major = AppVersion(2, 2, 3, 4)

    compare(version1major, version2major)
  }

  test("compare minor app version") {
    val version1minor = AppVersion(1, 1, 3)
    val version2minor = AppVersion(1, 2, 3)

    compare(version1minor, version2minor)
  }

  test("compare path app version") {
    val version1patch = AppVersion(1, 2, 2)
    val version2patch = AppVersion(1, 2, 3)

    compare(version1patch, version2patch)
  }

  test("compare build app version") {
    val version0build = AppVersion(1, 2, 3)
    val version1build = AppVersion(1, 2, 3, 4)
    val version2build = AppVersion(1, 2, 3, 5)

    compare(version0build, version1build)
    compare(version1build, version2build)
    compare(version0build, version2build)
  }

  private def compare(min: AppVersion, max: AppVersion): Assertion = {
    assert(min < max)
    assert(max > min)
    assert(min == min)
    assert(min <= min)
    assert(min >= min)
    assert(min <= max)
    assert(max >= min)
  }

}
