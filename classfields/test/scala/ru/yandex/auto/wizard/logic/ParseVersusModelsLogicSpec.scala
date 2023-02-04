package ru.yandex.auto.wizard.logic

import java.util.Comparator

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.searcher.query.auto.{MarkType, ModelType, NameplateType}
import ru.yandex.auto.wizard.model.{SimpleVersusParsedModel, VersusModels}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ParseVersusModelsLogicSpec extends WordSpecLike with Matchers with MockitoSupport {

  private val ModelsComparatorAlphabetic: Comparator[ModelType] = new Comparator[ModelType] {
    override def compare(o1: ModelType, o2: ModelType): Int = o1.getCode.compareTo(o2.getCode)
  }

  import ru.yandex.auto.wizard.data.TestCatalog._

  private def runSpec(
      models: Map[MarkType, Set[ModelType]],
      nameplates: Map[MarkType, Map[ModelType, Set[NameplateType]]]
  ): Option[VersusModels] = {
    val modelsJava = models.mapValues(_.asJava).asJava
    val nameplatesJava = nameplates.mapValues(_.mapValues(_.asJava).asJava).asJava

    val logic = new ParseVersusModelsLogic(catalogService)
    val res = logic.parseVersusModelsOpt(
      modelsJava,
      nameplatesJava,
      ModelsComparatorAlphabetic
    )

    if (res.isPresent) Some(res.get)
    else None
  }

  private def parsedFrom(modelType: ModelType): SimpleVersusParsedModel = SimpleVersusParsedModel(
    markCode = modelType.getMark.getCode,
    modelCode = modelType.getCode,
    nameplateUrl = None,
    nameplateName = None
  )

  private def parsedFrom(nameplateType: NameplateType): SimpleVersusParsedModel = SimpleVersusParsedModel(
    markCode = nameplateType.mark.getCode,
    modelCode = nameplateType.model.getCode,
    nameplateUrl = Some(nameplateType.getCode),
    nameplateName = Some(nameplateType.getCode)
  )

  "ParseVersusModelsLogic" should {
    "return parsed" when {

      "comes two different marks without nameplates" in {
        val models = Map(
          Hyundai -> Set(Solaris),
          Kia -> Set(Rio)
        )

        runSpec(models, nameplates = Map.empty) shouldBe Some(
          VersusModels(parsedFrom(Rio), parsedFrom(Solaris))
        )
      }

      "comes two different marks, one with nameplate" in {
        val models = Map(
          Kia -> Set(Rio),
          Audi -> Set(A3)
        )

        val nameplates = Map(
          Audi -> Map(
            A3 -> Set(GTron)
          )
        )

        runSpec(models, nameplates) shouldBe Some(
          VersusModels(parsedFrom(GTron), parsedFrom(Rio))
        )
      }

      "comes two different marks with nameplates" in {
        val models = Map(
          Kia -> Set(Rio),
          Audi -> Set(A3)
        )

        val nameplates = Map(
          Audi -> Map(
            A3 -> Set(GTron)
          ),
          Kia -> Map(
            Rio -> Set(XLine)
          )
        )

        runSpec(models, nameplates) shouldBe Some(
          VersusModels(parsedFrom(GTron), parsedFrom(XLine))
        )
      }

      "comes single mark with two nameplates" in {
        val models = Map(
          Bmw -> Set(Series3)
        )

        val nameplates = Map(
          Bmw -> Map(
            Series3 -> Set(I320, I335)
          )
        )

        val result = runSpec(models, nameplates)
        result should not be empty

        Seq(result.get.first, result.get.second) should contain theSameElementsAs Seq(
          parsedFrom(I335),
          parsedFrom(I320)
        )
      }
    }

    "return empty" when {

      "comes single model without nameplate" in {
        val models = Map(Hyundai -> Set(Solaris))

        runSpec(models, Map.empty) shouldBe empty
      }

      // Что бы не отвечать на запросы вида `audi a3 g-tron vs` сравнением,
      // мы жертвуем запросами вида  `audi a3 g-tron vs audi a3`
      "comes single mark with only 1 nameplate" in {

        val models = Map(
          Audi -> Set(A3)
        )

        val nameplates = Map(
          Audi -> Map(
            A3 -> Set(GTron)
          )
        )

        runSpec(models, nameplates) shouldBe empty
      }

    }
  }

}
