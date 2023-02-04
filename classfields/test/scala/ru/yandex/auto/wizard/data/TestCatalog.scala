package ru.yandex.auto.wizard.data

import java.{lang, util}
import java.util.Optional

import com.yandex.yoctodb.query.Query
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import ru.yandex.auto.core.catalog.grouping.{
  CatalogCardGroupingService,
  GroupByComplectation,
  GroupByConfiguration,
  GroupByGeneration,
  GroupByMark,
  GroupByModel,
  GroupBySuperGeneration,
  GroupByTechParameter
}
import ru.yandex.auto.core.catalog.model.{CatalogCard, Complectation, Configuration, Model, ModelImpl, TechParameter}
import ru.yandex.auto.core.dictionary.Type
import ru.yandex.auto.core.{AutoLang, AutoLocale, AutoSchemaVersions}
import ru.yandex.auto.message.AutoUtilsSchema.NamePlateMessage
import ru.yandex.auto.message.CatalogSchema
import ru.yandex.auto.searcher.query.auto.{MarkType, ModelType, NameplateType}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters._

object TestCatalog extends MockitoSupport {

  def typeFrom(code: String, nameEn: String): Type =
    new Type(
      code,
      Map(AutoLang.EN -> nameEn).asJava,
      Map.empty[AutoLang, String].asJava
    )

  val Audi: MarkType = new MarkType(typeFrom("AUDI", "audi"))
  val A3: ModelType = new ModelType(typeFrom("A3", "a3"), Audi)

  val GTron: NameplateType = new NameplateType(
    typeFrom("g-tron", "g-tron"),
    Audi,
    A3,
    NameplateType.Unique
  )

  val Kia: MarkType = new MarkType(typeFrom("KIA", "kia"))
  val Rio: ModelType = new ModelType(typeFrom("RIO", "rio"), Kia)
  val XLine: NameplateType = new NameplateType(typeFrom("x-line", "x-line"), Kia, Rio, NameplateType.Unique)

  val Bmw: MarkType = new MarkType(typeFrom("BMW", "bmw"))
  val Series3: ModelType = new ModelType(typeFrom("3ER", "3-series"), Bmw)
  val I320: NameplateType = new NameplateType(typeFrom("320", "320i"), Bmw, Series3, NameplateType.Unique)
  val I335: NameplateType = new NameplateType(typeFrom("335", "335"), Bmw, Series3, NameplateType.Unique)

  val Hyundai = new MarkType(typeFrom("HYUNDAI", "Hyundai"))
  val Solaris = new ModelType(typeFrom("SOLARIS", "Solaris"), Hyundai)

  Audi.setModels(
    util.Arrays.asList(
      A3
    )
  )

  Kia.setModels(
    util.Arrays.asList(
      Rio
    )
  )

  Bmw.setModels(
    util.Arrays.asList(
      Series3
    )
  )

  Hyundai.setModels(
    util.Arrays.asList(
      Solaris
    )
  )

  val AllMarks: Seq[MarkType] = Seq(Audi, Bmw, Kia, Hyundai)
  val AllModels: Seq[ModelType] = AllMarks.flatMap(_.getModels.asScala)
  val AllNameplates: Seq[NameplateType] = Seq(GTron, XLine, I320, I335)

  private def nameplateTypeToNameplateMessage(nameplateType: NameplateType): NamePlateMessage =
    NamePlateMessage
      .newBuilder()
      .setVersion(AutoSchemaVersions.CATALOG_CARDS_VERSION)
      .setName(nameplateType.getCode)
      .setSemanticUrl(nameplateType.getCode)
      .build()

  private def modelTypeToCoreModel(modelType: ModelType): Model = {
    val msg = CatalogSchema.ModelMessage
      .newBuilder()
      .setVersion(AutoSchemaVersions.CATALOG_CARDS_VERSION)
      .setCode(modelType.getCode)
      .addAllNameplateFront(
        AllNameplates
          .filter { np =>
            np.model == modelType && np.mark == modelType.getMark
          }
          .map(nameplateTypeToNameplateMessage)
          .asJava
      )
      .build()

    ModelImpl.fromMessage(msg)
  }

  private def groupByModelFromModel(model: Model): GroupByModel = {
    val group = mock[GroupByModel]
    when(group.getKey).thenReturn(model)

    group
  }

  val catalogService: CatalogCardGroupingService = {
    val s = mock[CatalogCardGroupingService]
    when(s.buildGroupByModel(?, ?))
      .thenAnswer(
        new Answer[GroupByModel] {
          override def answer(invocation: InvocationOnMock): GroupByModel = {
            val markCode = invocation.getArgument[String](0)
            val modelCode = invocation.getArgument[String](1)

            AllModels
              .find(mt => mt.getMark.getCode == markCode && mt.getCode == modelCode)
              .map(modelTypeToCoreModel)
              .map(groupByModelFromModel)
              .orNull
          }
        }
      )
    s
  }

}
