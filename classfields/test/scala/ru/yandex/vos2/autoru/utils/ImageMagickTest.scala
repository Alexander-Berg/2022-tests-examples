package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.util.IO

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 17/12/2018
  */
@RunWith(classOf[JUnitRunner])
class ImageMagickTest extends AnyFunSuite {

  test("apply blur to image") {
    val source = IO.resourceToFile(this, "/test-images/test-1.jpeg", "source", ".jpg")
    val target = IO.newTempFile("target", ".jpg")
    println(target)
    ImageMagick.applyBlur(
      source,
      target,
      Array(
        Array(239, 592),
        Array(438, 606),
        Array(442, 640),
        Array(239, 630)
      )
    )
  }
  test("apply blur to image in outside points") {
    val source = IO.resourceToFile(this, "/test-images/test-1.jpeg", "source", ".jpg")
    val target = IO.newTempFile("target", ".jpg")
    println(target)
    ImageMagick.applyBlur(
      source,
      target,
      Array(
        Array(-200, 592),
        Array(238, 606),
        Array(242, 640),
        Array(-239, 630)
      )
    )
  }
}
