package ru.yandex.realty.unifiedoffer

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ImagesProtoConverterSpec extends FlatSpec with Matchers {

  "imageFromAvatarnitsaUrl" should "work" in {
    val i = ImagesProtoConverter.imageFromAvatarnitsaUrl("http://example.com/image/orig", false)
    i.getOrigin shouldBe "//example.com/image/orig"
    i.getAppSnippetMiddle shouldBe "//example.com/image/app_snippet_middle"
  }

  "imageFromAvatarnitsaUrl" should "remove //" in {
    val i = ImagesProtoConverter.imageFromAvatarnitsaUrl("//example.com/image", false)
    i.getOrigin shouldBe "//example.com/image/orig"
    i.getAppSnippetMiddle shouldBe "//example.com/image/app_snippet_middle"
  }

}
