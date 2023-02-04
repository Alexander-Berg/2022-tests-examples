package ru.yandex.vertis.json.formats

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsString, JsSuccess}
import ru.yandex.vertis.testdata.enums.{JavaEnum, PrefixedEnum}

/**
  * @author Natalia Ratskevich (reimai@yandex-team.ru)
  */
class EnumFormatBuilderTest extends WordSpec with Matchers {

  "java enum format" should {

    "do caseless read" in {
      val fmt = EnumFormatBuilder.format(classOf[JavaEnum])
      fmt.writes(JavaEnum.FST) shouldBe JsString("FST")
      fmt.reads(JsString("snd")) shouldBe JsSuccess(JavaEnum.SND)
    }

    "omit prefix" in {
      val fmt = EnumFormatBuilder.format(classOf[PrefixedEnum], "SOME_")
      fmt.writes(PrefixedEnum.SOME_FST) shouldBe JsString("FST")
      fmt.reads(JsString("snd")) shouldBe JsSuccess(PrefixedEnum.SOME_SND)
    }

    "not omit prefix" in {
      val fmt = EnumFormatBuilder.format(classOf[PrefixedEnum])
      fmt.writes(PrefixedEnum.SOME_FST) shouldBe JsString("SOME_FST")
      fmt.reads(JsString("SOME_SND")) shouldBe JsSuccess(PrefixedEnum.SOME_SND)
    }

    "read camelCase" in {
      val fmt = EnumFormatBuilder.format(classOf[PrefixedEnum], readCamelCase = true)
      fmt.writes(PrefixedEnum.SOME_FST) shouldBe JsString("SOME_FST")
      fmt.reads(JsString("someSnd")) shouldBe JsSuccess(PrefixedEnum.SOME_SND)
      fmt.reads(JsString("some_snd")).isError shouldBe true
    }

    "write lower case" in {
      val fmt = EnumFormatBuilder.format(classOf[PrefixedEnum], writeLowerCase = true)
      fmt.writes(PrefixedEnum.SOME_FST) shouldBe JsString("some_fst")
      fmt.reads(JsString("some_snd")) shouldBe JsSuccess(PrefixedEnum.SOME_SND)
      fmt.reads(JsString("someSnd")).isError shouldBe true
    }

    "read camelCase and write lower case" in {
      val fmt = EnumFormatBuilder.format(classOf[PrefixedEnum], readCamelCase = true, writeLowerCase = true)
      fmt.writes(PrefixedEnum.SOME_FST) shouldBe JsString("some_fst")
      fmt.reads(JsString("SomeSnd")) shouldBe JsSuccess(PrefixedEnum.SOME_SND)
    }

    "read camelCase and write lower case and omit prefix" in {
      val fmt = EnumFormatBuilder.format(classOf[PrefixedEnum], "SOME_",
        readCamelCase = true, writeLowerCase = true)
      fmt.writes(PrefixedEnum.SOME_FST) shouldBe JsString("fst")
      fmt.reads(JsString("snd")) shouldBe JsSuccess(PrefixedEnum.SOME_SND)
    }
  }

}
