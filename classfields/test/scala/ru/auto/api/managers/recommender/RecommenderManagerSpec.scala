package ru.auto.api.managers.recommender

import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.BreadcrumbsModel.Entity
import ru.auto.api.RequestModel.ModelsCompareRequest
import ru.auto.api.extdata.DataService
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.compare.{CompareManager, DictionaryCodeConverters, ModelsCompareBuilder}
import ru.auto.api.managers.geo.GeoManager
import ru.auto.api.model.ModelGenerators.CompareModelGen
import ru.auto.api.model.{ModelGenerators, RequestParams}
import ru.auto.api.search.SearchModel.CatalogFilter
import ru.auto.api.services.recommender.DefaultRecommenderClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.catalog.model.api.ApiModel._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class RecommenderManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  trait mocks {
    val recommenderClient: DefaultRecommenderClient = mock[DefaultRecommenderClient]
    val modelsCompareBuilder: ModelsCompareBuilder = mock[ModelsCompareBuilder]
    val compareManager: CompareManager = mock[CompareManager]
    val catalogManager: CatalogManager = mock[CatalogManager]
    val dataService: DataService = mock[DataService]
    val geoManager: GeoManager = mock[GeoManager]
    val dictionaryCodeConverters = mock[DictionaryCodeConverters]

    when(dictionaryCodeConverters.photoType).thenReturn(
      Map(
        "side" -> "Сбоку",
        "door" -> "Дверь",
        "disk" -> "Колесный диск",
        "torpedo" -> "Передняя панель",
        "front" -> "Спереди",
        "boot" -> "Багажник",
        "hand" -> "Ручка КПП",
        "3_4_behind" -> "3/4 сзади",
        "light-back" -> "Задний фонарь",
        "main" -> "3/4 спереди",
        "inter1" -> "Интересная деталь 1",
        "wheel" -> "Руль",
        "saloon-front" -> "Салон спереди",
        "back" -> "Сзади",
        "pp" -> "Приборная панель",
        "inter2" -> "Интересная деталь 2",
        "mirror" -> "Боковое зеркало",
        "light-front" -> "Передняя фара",
        "saloon-back" -> "Салон сзади"
      )
    )

    val recommenderManager: RecommenderManager =
      new RecommenderManager(
        recommenderClient,
        compareManager,
        catalogManager,
        dictionaryCodeConverters
      )

    implicit val trace: Traced = Traced.empty

    implicit val request: Request = {
      val r = new RequestImpl
      r.setRequestParams(RequestParams.construct("1.1.1.1"))
      r.setTrace(trace)
      r.setUser(ModelGenerators.PersonalUserRefGen.next)
      r
    }
  }

  "RecommenderManager.getRecommendation" when {
    "distinctModelMarkGeneration is true" should {
      "return recommendations".which {
        "are unique with respect to models, marks and generations" in new mocks {
          val distinctModelMarkGeneration = true

          forAll(CompareModelGen, arbitrary[Boolean]) { (compareModel, excludeSame) =>
            // Since the recommendation logic is quite complicated and it would take much time to implement this test
            // in a cleaner way it was implemented to, at least, work. If there is a better way to write the test,
            // please do or let me know. (tymur-lysenko)

            val complectationEntity = Entity
              .newBuilder()
              .setId("1")
              .build()
            val complectationCard = ComplectationCard
              .newBuilder()
              .setEntity(complectationEntity)
              .build()

            val techParamEntity1 = Entity
              .newBuilder()
              .setId("1")
              .build()
            val techParamCard1 = TechParamCard
              .newBuilder()
              .setEntity(techParamEntity1)
              .build()

            val techParamEntity2 = Entity
              .newBuilder()
              .setId("2")
              .build()
//            val techParamCard2 = TechParamCard
//              .newBuilder()
//              .setEntity(techParamEntity2)
//              .build()

            val configurationEntity1 = Entity
              .newBuilder()
              .setId("1")
              .build()
            val configurationCard1 = ConfigurationCard
              .newBuilder()
              .setEntity(configurationEntity1)
              .setParentSuperGen("1")
              .build()

            val configurationEntity2 = Entity
              .newBuilder()
              .setId("2")
              .build()
            val configurationCard2 = ConfigurationCard
              .newBuilder()
              .setEntity(configurationEntity2)
              .setParentSuperGen("1")
              .addAllTechParam(List(techParamEntity1.getId, techParamEntity2.getId).asJava)
              .build()

            val superGenEntity = Entity
              .newBuilder()
              .setId("1")
              .build()
            val superGenCard = SuperGenCard
              .newBuilder()
              .setEntity(superGenEntity)
              .setParentMark("BMW")
              .setParentModel("X3")
              .addAllConfiguration(List(configurationEntity1.getId, configurationEntity2.getId).asJava)
              .build()

            val bmwModelEntity = Entity
              .newBuilder()
              .setId("X3")
              .setName("X3")
              .build()
            val bmwModelCard = ModelCard
              .newBuilder()
              .setEntity(bmwModelEntity)
              .addAllSuperGen(List("123").asJava)
              .build()

            val bmwMarkModelEntity = Entity
              .newBuilder()
              .setId("BMW")
              .setName("BMW")
              .build()
            val bmwMarkCard = MarkCard
              .newBuilder()
              .setEntity(bmwMarkModelEntity)
              .putAllModel(
                Map(
                  "X3" -> bmwModelCard
                ).asJava
              )
              .build()

            val rawCatalogBuilder = RawCatalog
              .newBuilder()
              .putAllMark(
                Map(
                  "BMW" -> bmwMarkCard
                ).asJava
              )
              .putAllConfiguration(
                Map(
                  compareModel.catalogFilter.getConfiguration.toString -> configurationCard1
                ).asJava
              )
              .putAllSuperGen(
                Map(
                  configurationCard1.getParentSuperGen -> superGenCard,
                  configurationCard2.getParentSuperGen -> superGenCard
                ).asJava
              )
              .putAllTechParam(
                Map(
                  compareModel.catalogFilter.getTechParam.toString -> techParamCard1
                ).asJava
              )

            if (compareModel.catalogFilter.hasComplectation) {
              rawCatalogBuilder.putAllComplectation(
                Map(
                  compareModel.catalogFilter.getComplectation.toString -> complectationCard
                ).asJava
              )
            }

            val rawCatalog = rawCatalogBuilder.build()

            when(compareManager.toCompareModel(?)(?)).thenReturn(Future.successful(compareModel))
            when(recommenderClient.getConfigurations(?)(?)).thenReturn(Future.successful(Seq(1L, 2L)))
            when(catalogManager.exactByCatalogFilter(?, ?, ?, ?, ?)(?))
              .thenReturn(Future.successful(rawCatalog))
            when(catalogManager.subtree(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(rawCatalog))
            when(catalogManager.subtreeByCatalogFilter(?, ?, ?, ?, ?, ?)(?))
              .thenReturn(Future.successful(rawCatalog))

            val catalogFilter: CatalogFilter = CatalogFilter
              .newBuilder()
              .setMark("BMW")
              .setModel("M5")
              .setGeneration(123L)
              .build()
            val catalogFilters: ModelsCompareRequest = ModelsCompareRequest
              .newBuilder()
              .addAllData(List(catalogFilter).asJava)
              .build()

            val result = recommenderManager
              .getRecommendation(
                catalogFilters,
                None,
                None,
                excludeSame,
                distinctModelMarkGeneration = distinctModelMarkGeneration
              )
              .await

            result.getData(0).getModels(0).getSpecifications(0).getEntitiesCount shouldBe 19

            val recommendations = (for {
              data <- result.getDataList.asScala
              model <- data.getModelsList.asScala
              summary = model.getSummary
              modelId = summary.getModel.getId
              markId = summary.getMark.getId
              generationId = summary.getSuperGen.getId
            } yield (modelId, markId, generationId)).toVector

            recommendations should contain theSameElementsAs recommendations.toSet
          }
        }

        // Since it is unlikely that we will need this case, there is no test for it as of now
//        "might have the same models, marks and generations, but different tech paramaters and configurations" when {
//          "distinctModelMarkGeneration is false" in new mocks {
//
//          }
//        }
      }
    }
  }

}
