package ru.auto.api.model.searcher

import ru.auto.api.BaseSpec
import ru.auto.api.BreadcrumbsModel.{EntitiesList, Entity}
import ru.auto.api.ResponseModel.BreadcrumbsResponse
import ru.auto.catalog.model.api.ApiModel._

import scala.jdk.CollectionConverters._

class CatalogConverterSpec extends BaseSpec {

  trait mocks {
    val testMarkId = "MERCEDES"
    val testModelId = "GL_KLASSE"
    val superGenId = "4986814"
    val configurationId = "4986815"
    val firstTechParamId = "20494193"
    val secondTechParamId = "20494749"
  }

  "CatalogConverter" should {
    "run" in new mocks {
      val builder = RawCatalog.newBuilder()
      val modelCard = ModelCard
        .newBuilder()
        .setEntity(createDefaultEntity(testModelId))
        .addSuperGen(superGenId)
        .addSuperGen("nonExistentSuperGenId")
        .build()
      val mark = MarkCard
        .newBuilder()
        .setEntity(createDefaultEntity(testMarkId))
        .putModel(testModelId, modelCard)
        .build()
      val superGen = SuperGenCard
        .newBuilder()
        .setEntity(createDefaultEntity(superGenId))
        .addConfiguration(configurationId)
        .build()
      val configuration = ConfigurationCard
        .newBuilder()
        .setEntity(createDefaultEntity(configurationId))
        .addTechParam(firstTechParamId)
        .addTechParam(secondTechParamId)
        .build()
      List(firstTechParamId, secondTechParamId).foreach { techParamId =>
        val techParam = TechParamCard
          .newBuilder()
          .setEntity(createDefaultEntity(techParamId))
          .build()
        builder.putTechParam(techParamId, techParam)
      }
      builder
        .putMark(testMarkId, mark)
        .putSuperGen(superGenId, superGen)
        .putConfiguration(configurationId, configuration)
      val rawCatalog = builder.build()
      val result = CatalogConverter.rawCatalogToBreadcrumbsModel(rawCatalog)
      result.getBreadcrumbsCount shouldBe 5
      ensureCorrectParents(
        findByLevel(CatalogUtils.ModelLevel, result),
        mark = Option(testMarkId)
      )
      ensureCorrectParents(
        findByLevel(CatalogUtils.GenerationLevel, result),
        mark = Option(testMarkId),
        model = Option(testModelId)
      )
      ensureCorrectParents(
        findByLevel(CatalogUtils.ConfigurationLevel, result),
        mark = Option(testMarkId),
        model = Option(testModelId),
        superGen = Option(superGenId)
      )
      ensureCorrectParents(
        findByLevel(CatalogUtils.TechParamLevel, result),
        mark = Option(testMarkId),
        model = Option(testModelId),
        superGen = Option(superGenId),
        configuration = Option(configurationId)
      )
    }
  }

  private def findByLevel(level: String, response: BreadcrumbsResponse): EntitiesList = {
    response.getBreadcrumbsList.asScala.find(_.getMetaLevel == level).get
  }

  private def ensureCorrectParents(entitiesList: EntitiesList,
                                   mark: Option[String] = None,
                                   model: Option[String] = None,
                                   superGen: Option[String] = None,
                                   configuration: Option[String] = None): Unit = {
    mark.foreach(x => entitiesList.getMark.getId shouldBe x)
    model.foreach(x => entitiesList.getModel.getId shouldBe x)
    superGen.foreach(x => entitiesList.getSuperGeneration.getId shouldBe x)
    configuration.foreach(x => entitiesList.getConfiguration.getId shouldBe x)
  }

  private def createDefaultEntity(id: String): Entity = {
    Entity
      .newBuilder()
      .setId(id)
      .setName(id)
      .build()
  }

}
