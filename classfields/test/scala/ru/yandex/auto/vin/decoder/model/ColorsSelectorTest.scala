package ru.yandex.auto.vin.decoder.model

import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.TestExtData
import ru.yandex.auto.vin.decoder.verba.proto.ColorsSchema

import scala.io.Source
import scala.jdk.CollectionConverters.IterableHasAsJava

@Ignore
class ColorsSelectorTest extends AnyWordSpecLike with Matchers {

  private def buildColor(
      code: String,
      name: String,
      aliases: List[String] = List.empty,
      similarCodes: List[String] = List.empty) = {
    ColorsSchema.Color
      .newBuilder()
      .setHexCode(code)
      .setName(name)
      .addAliases(name)
      .addAllAliases(aliases.asJava)
      .addAllSimilarColorCodes(similarCodes.asJava)
      .build()
  }

  private lazy val colorsSelector = TestExtData.providers.colorsProvider.getSelector

  private def buildColorSelector: ColorsSelector = {

    ColorsSelector(
      List(
        buildColor("c49648", "бежевый", similarCodes = List("dea522")),
        buildColor("fafbfb", "белый"),
        buildColor("22a0f8", "голубой", List("синий")),
        buildColor("ffd600", "желтый"),
        buildColor("007f00", "зеленый"),
        buildColor("dea522", "золотой"),
        buildColor("200204", "коричневый"),
        buildColor("ee1d19", "красный", List("пупрпурный, вишневый, бордовый")),
        buildColor("ff8649", "оранжевый"),
        buildColor("660099", "пурпурный", List("красный, вишневый, бордовый")),
        buildColor("cacecb", "серебристый", List("серый")),
        buildColor("97948f", "серый", List("серебристый")),
        buildColor("0000cc", "синий"),
        buildColor("4a2197", "фиолетовый"),
        buildColor("040001", "черный"),
        buildColor("ffc0cb", "розовый")
      )
    )
  }

  "colors select" must {
    "construct valid alias map" in {
      val sel = ColorsSelector(
        List(
          buildColor("c49648", "бежевый", aliases = List("квелый"), similarCodes = List("dea522")),
          buildColor("dea522", "золотой", aliases = List("медовик"))
        )
      )
      sel.aliasesColorsMap("золотой").size shouldBe 2
      sel.aliasesColorsMap("медовик").size shouldBe 2
      sel.aliasesColorsMap("квелый").size shouldBe 1
      sel.aliasesColorsMap("бежевый").size shouldBe 1
    }

    "work with similar codes in one direction" in {
      val sel = buildColorSelector
      val unified = sel.unifyColor("золотой")
      unified.exists(_.name == "бежевый") shouldBe true
      unified.exists(_.name == "золотой") shouldBe true

      val unifiedBack = sel.unifyColor("бежевый")
      unifiedBack.exists(_.name == "бежевый") shouldBe true
      unifiedBack.exists(_.name == "золотой") shouldBe false
    }
    "should define valid autoru color" in {
      val source = Source.fromInputStream(getClass.getResourceAsStream("/color_mapping.csv"))
      source
        .getLines()
        .foreach(l => {
          val cols = l.split(";", 2)
          val color = cols(0)
          val expected_color = cols(1)
          val mapped = colorsSelector.resolveAutoruColor(color).map(_.name).getOrElse("")
          assert(mapped == expected_color)
        })
      source.close()
    }
  }

}
