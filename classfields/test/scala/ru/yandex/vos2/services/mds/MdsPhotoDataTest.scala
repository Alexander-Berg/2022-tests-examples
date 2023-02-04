package ru.yandex.vos2.services.mds

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class MdsPhotoDataTest extends AnyFunSuite with Matchers {
  test("successful parse photoId with prefix") {
    val photoId = MdsPhotoData("autoru-test", "123-prefix_001_abcdef0123456789")
    photoId.groupId shouldBe 123
    photoId.toPlain shouldBe "autoru-test:123-prefix_001_abcdef0123456789"
    photoId.prefix shouldBe Some("prefix_001")
  }

  test("successful parse photoId without prefix") {
    val photoId = MdsPhotoData("autoru-test", "123-abcdef0123456789")
    photoId.groupId shouldBe 123
    photoId.toPlain shouldBe "autoru-test:123-abcdef0123456789"
    photoId.prefix shouldBe None
  }

  test("partial parse photoId with too short id") {
    val photoId = MdsPhotoData("autoru-test", "123-prefix_001_abcdef012")
    photoId.groupId shouldBe 123
    photoId.toPlain shouldBe "autoru-test:123-prefix_001_abcdef012"
    photoId.prefix shouldBe None
  }

  test("partial parse photoId with empty prefix") {
    val photoId = MdsPhotoData("autoru-test", "123-_abcdef0123456789")
    photoId.groupId shouldBe 123
    photoId.toPlain shouldBe "autoru-test:123-_abcdef0123456789"
    photoId.prefix shouldBe None
  }

  test("failed parse photoId without group-id") {
    val photoId = MdsPhotoData("autoru-test", "prefix_001_abcdef0123456789")
    val groupId = Try(photoId.groupId)
    groupId.isFailure shouldBe true
    groupId.failed.get.isInstanceOf[NumberFormatException]
  }
}
