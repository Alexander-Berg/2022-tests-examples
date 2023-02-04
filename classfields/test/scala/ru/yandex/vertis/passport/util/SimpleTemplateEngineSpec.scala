package ru.yandex.vertis.passport.util

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.passport.util.template.SimpleTemplateEngine

class SimpleTemplateEngineSpec extends WordSpec with Matchers {
  private val engine = SimpleTemplateEngine

  "SimpleTemplateEngineSpec" should {
    "should process templates" in {
      val template = "AAA {aa} BBB {bb}"
      val tagMap = Map(
        "aa" -> "123",
        "bb" -> "456"
      )
      val expectedResult = "AAA 123 BBB 456"

      engine.replaceTags(template, tagMap) shouldBe expectedResult
    }

    "should clean unknown tags" in {
      val template = "AAA {aa}"
      val tagMap = Map.empty[String, String]
      val expectedResult = "AAA "

      engine.replaceTags(template, tagMap) shouldBe expectedResult
    }

    "should ignore spaces in tags" in {
      val templates = Seq("A: { ddd}", "A: {sadasd }", "A: { sdfsdfs }")
      val tagMap = Map.empty[String, String]
      val expectedResult = "A: "

      templates.foreach { template =>
        engine.replaceTags(template, tagMap) shouldBe expectedResult
      }
    }
  }
}
