package ru.yandex.realty.storage

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import java.io.ByteArrayInputStream

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.search.{PresetDict, PresetName, Title}

@RunWith(classOf[JUnitRunner])
class PresetDictStorageSpec extends WordSpec with Matchers {

  "PresetDictStorageSpec" should {

    "parse empty Dictionary" in {
      val presetDictBuilder =
        PresetDict.newBuilder()
      PresetDictStorage.parse(new ByteArrayInputStream(presetDictBuilder.build().toByteArray))
    }

    "parse and read Dictionary with value" in {
      val presetDictBuilder =
        PresetDict.newBuilder()
      val dummyName = PresetName
        .newBuilder()
        .setFieldName("roomsTotal")
        .setValue("1")
        .addTitle(
          Title
            .newBuilder()
            .setLocale("ru")
            .setText("Однокомнатная")
        )

      presetDictBuilder.addPresetName(dummyName)
      val storage =
        PresetDictStorage.parse(new ByteArrayInputStream(presetDictBuilder.build().toByteArray))

      storage.getPresetName("roomsTotal", "1", Some("ru")).get shouldBe "Однокомнатная"
      storage.getPresetName("roomsTotal", "1", None).get shouldBe "Однокомнатная"
    }

  }
}
