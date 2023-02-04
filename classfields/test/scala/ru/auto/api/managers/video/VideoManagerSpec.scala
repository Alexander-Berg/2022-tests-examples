package ru.auto.api.managers.video

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.auto.api.BaseSpec
import ru.auto.api.BreadcrumbsModel._
import ru.auto.api.ResponseModel.VideoListingResponse
import ru.auto.api.auth.Application
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.model.CategorySelector.{Cars, Moto, Trucks}
import ru.auto.api.model.bunker.VideoSearchBlackList
import ru.auto.api.model.{CategorySelector, ModelGenerators, Paging, RequestParams}
import ru.auto.api.search.SearchModel.CatalogFilter
import ru.auto.api.services.video.VideoClient
import ru.auto.api.testkit.TestDataEngine
import ru.auto.api.util.RequestImpl
import ru.auto.catalog.model.api.ApiModel._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

class VideoManagerSpec extends BaseSpec with MockitoSupport with BeforeAndAfter {
  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req
  }

  private val videoSearchBlackList = VideoSearchBlackList.from(TestDataEngine)

  private val videoClient: VideoClient = mock[VideoClient]
  private val catalogManager: CatalogManager = mock[CatalogManager]
  private val videoManager: VideoManager = new VideoManager(videoClient, videoSearchBlackList, catalogManager)

  "VideoManager" should {
    "parse empty cars params" in {
      VideoManager.parseQuery(Cars, Map.empty) shouldBe "тест-драйв автомобили"
    }

    "parse cars params" in {
      val params = Map(
        "mark" -> "BMW",
        "model" -> "X5",
        "super_gen" -> "(ABC) Рестайлинг",
        "tech_param" -> "30d"
      )

      VideoManager.parseQuery(Cars, params) shouldBe "title:BMW title:X5 title:30d (ABC) Рестайлинг тест-драйв автомобили"
    }

    "respond with empty videos list on mark from black list" in {
      val mark = "alpina"

      val params = Map("mark" -> mark)

      val videos = Gen.listOfN(10, ModelGenerators.VideoGen).next.map(_.toBuilder.setTitle(mark).build())
      val resp = VideoListingResponse
        .newBuilder()
        .addAllVideos(videos.asJava)
        .build()
      when(videoClient.search(?, ?)(?)).thenReturnF(resp)

      val result = videoManager.search(CategorySelector.Cars, Paging(1, 2), params).futureValue

      result.getVideosCount shouldBe 0
    }

    "respond with videos list with catalog filter" in {
      val mark = "mercedes"

      val params = Map("mark" -> mark)

      val catalogFilter = CatalogFilter
        .newBuilder()
        .setMark("MERCEDES")
        .setModel("C_KLASSE")
        .setGeneration(2307688)
        .setTechParam(2307695)
        .build()

      val markCard = MarkCard
        .newBuilder()
        .setEntity(Entity.newBuilder().setName("Mercedes-Benz"))
        .putModel(
          catalogFilter.getModel,
          ModelCard
            .newBuilder()
            .setEntity(Entity.newBuilder().setName("C-Класс"))
            .build()
        )
        .build()

      val configurationCard = ConfigurationCard
        .newBuilder()
        .setEntity(
          Entity
            .newBuilder()
            .setConfiguration(
              ConfigurationEntity
                .newBuilder()
                .setHumanName("Седан")
                .build()
            )
        )
        .build()

      val superGenCard = SuperGenCard
        .newBuilder()
        .setEntity(
          Entity
            .newBuilder()
            .setSuperGen(
              SuperGenerationEntity
                .newBuilder()
                .setYearFrom(2006)
            )
        )
        .build()

      val techParamCard = TechParamCard
        .newBuilder()
        .setEntity(Entity.newBuilder().setName("320"))
        .build()

      val rawCatalog = RawCatalog
        .newBuilder()
        .putMark(catalogFilter.getMark, markCard)
        .putSuperGen(catalogFilter.getGeneration.toString, superGenCard)
        .putConfiguration(catalogFilter.getConfiguration.toString, configurationCard)
        .putTechParam(catalogFilter.getTechParam.toString, techParamCard)
        .build()

      val textCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val videos = Gen.listOfN(10, ModelGenerators.VideoGen).next.map(_.toBuilder.setTitle("Mercedes 320").build())
      val resp = VideoListingResponse
        .newBuilder()
        .addAllVideos(videos.asJava)
        .build()

      when(catalogManager.exactByCatalogFilter(?, ?, ?, ?, ?)(?)).thenReturnF(rawCatalog)
      when(videoClient.search(?, ?)(?)).thenReturnF(resp)

      val result = videoManager.search(CategorySelector.Cars, Paging(1, 2), params, Some(catalogFilter)).futureValue

      verify(videoClient).search(textCaptor.capture(), ?)(?)
      textCaptor.getValue shouldBe
        "title:Mercedes-Benz title:C-Класс title:320 2006 Седан тест-драйв " +
          "автомобили -unlimited -nfs -gameplay -gta -gaming " +
          "-game -turismo -project -косметика -кухня -гитара"

      verify(catalogManager).exactByCatalogFilter(
        eq(CategorySelector.Cars),
        eq(None),
        eq(Seq(catalogFilter)),
        eq(false),
        eq(false)
      )(?)

      result.getVideosCount shouldBe 2
    }

    "respond with empty videos list on case insensitive mark from black list" in {
      val mark = "ALPina"
      val params = Map("mark" -> mark)

      val videos = Gen.listOfN(10, ModelGenerators.VideoGen).next.map(_.toBuilder.setTitle(mark).build())
      val resp = VideoListingResponse
        .newBuilder()
        .addAllVideos(videos.asJava)
        .build()
      when(videoClient.search(?, ?)(?)).thenReturnF(resp)

      val result = videoManager.search(CategorySelector.Cars, Paging(1, 2), params).futureValue

      result.getVideosCount shouldBe 0
    }

    "parse vendor cars params" in {
      val params = Map(
        "vendor" -> "FOREIGN"
      )

      VideoManager.parseQuery(Cars, params) shouldBe "тест-драйв иномарки"
    }

    "parse moto only category params" in {
      val params = Map(
        "moto_category" -> "ATV"
      )

      VideoManager.parseQuery(Moto, params) shouldBe "тест-драйв мотовездеход"
    }

    "parse moto params" in {
      val params = Map(
        "mark" -> "BMW"
      )

      VideoManager.parseQuery(Moto, params) shouldBe "title:BMW тест-драйв мотоцикл"
    }

    "parse trucks only category params" in {
      val params = Map(
        "truck_category" -> "BUS"
      )

      VideoManager.parseQuery(Trucks, params) shouldBe "тест-драйв автобус"
    }

    "parse trucks params" in {
      val params = Map(
        "truck_category" -> "TRUCK",
        "mark" -> "МАЗ",
        "model" -> "IDK"
      )

      VideoManager.parseQuery(Trucks, params) shouldBe "title:МАЗ title:IDK тест-драйв грузовик"
    }
  }
}
