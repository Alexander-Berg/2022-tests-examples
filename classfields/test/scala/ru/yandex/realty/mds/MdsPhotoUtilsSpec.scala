package ru.yandex.realty.mds

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

@RunWith(classOf[JUnitRunner])
class MdsPhotoUtilsSpec extends SpecBase with ScalaCheckPropertyChecks {

  private val data = Table(
    (
      "url",
      "expected_id"
    ),
    (
      "//avatars.mds.yandex.net/get-verba/937147/2a00000166a1d5be6757ff1c41fec4b31f25/realty_large",
      Some("2a00000166a1d5be6757ff1c41fec4b31f25")
    ),
    (
      "//avatars.mds.yandex.net/get-verba/937147/2a00000166a1d5be6757ff1c41fec4b31f25/",
      Some("2a00000166a1d5be6757ff1c41fec4b31f25")
    ),
    (
      "//avatars.mds.yandex.net/get-verba/937147/2a00000166a1d5be6757ff1c41fec4b31f25",
      Some("2a00000166a1d5be6757ff1c41fec4b31f25")
    ),
    (
      "//avatars.mds.yandex.net/get-verba/937147/",
      None
    )
  )

  "SiteUtil" should {
    forAll(data) { (url: String, expectedId: Option[String]) =>
      s"extract id $expectedId from avatar url $url" in {
        MdsPhotoUtils.extractAvatarPhotoId(url) shouldEqual expectedId
      }
    }
  }
}
