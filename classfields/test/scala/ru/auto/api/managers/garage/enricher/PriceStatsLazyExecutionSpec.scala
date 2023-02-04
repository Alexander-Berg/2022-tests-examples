package ru.auto.api.managers.garage.enricher

import cats.Eval
import org.mockito.{ArgumentMatcher, InOrder, Mockito}
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.{times, verify}
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.PriceHistogramResponse
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.AutoPriceHistogramNotFound
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.garage.enricher.LazyExecution.{EnrichMap, LazyExecution}
import ru.auto.api.managers.geo.GeoManager
import ru.auto.api.managers.searcher.SearcherManager
import ru.auto.api.managers.stats.StatsManager
import ru.auto.api.model.{CategorySelector, RequestParams}
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.searcher.PriceHistogramParams
import ru.auto.api.search.SearchModel.{PriceHistogramGroup, SearchRequestParameters}
import ru.auto.api.ui.UiModel.BodyTypeGroup
import ru.auto.api.util.RequestImpl
import ru.auto.api.vin.garage.GarageApiModel.Card
import ru.auto.catalog.model.api.ApiModel.{ConfigurationCard, RawCatalog, TechParamCard}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.protobuf.ProtoMacro.opt
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class PriceStatsLazyExecutionSpec extends BaseSpec {

  private val trace: Traced = Traced.empty
  implicit private val request: RequestImpl = generateReq

  private def generateReq: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.web)
    r.setUser(PrivateUserRefGen.next)
    r.setTrace(trace)
    r
  }

  private val Rid = GeoManager.RussiaRid
  private val CardId = "1234567"

  private def generateCard(withTechParamId: Boolean = true): Card = {
    val builder = Card
      .newBuilder()
      .setId(CardId)

    builder.getVehicleInfoBuilder.getCarInfoBuilder
      .setBodyType("ALLROAD_5_DOORS")
      .setMark("BMW")
      .setModel("X3")
      .setSuperGenId(21029610)
      .setConfigurationId(21029647)

    if (withTechParamId) builder.getVehicleInfoBuilder.getCarInfoBuilder.setTechParamId(21029738)

    builder.build()
  }

  private def generateCatalogCards(card: Card): Map[String, LazyExecution[RawCatalog]] = {
    val builder = RawCatalog.newBuilder()

    opt(card.getVehicleInfo.getCarInfo.getConfigurationId).foreach { configurationId =>
      val card = ConfigurationCard.newBuilder()
      card.getEntityBuilder.getConfigurationBuilder.setBodyTypeGroup(BodyTypeGroup.ALLROAD_5_DOORS)
      builder.putConfiguration(configurationId.toString, card.build)
    }

    opt(card.getVehicleInfo.getCarInfo.getTechParamId).foreach { techParamId =>
      val card = TechParamCard.newBuilder()
      card.getEntityBuilder.getTechParamsBuilder.setTransmission("AUTOMATIC")
      card.getEntityBuilder.getTechParamsBuilder.setEngineType("DIESEL")
      card.getEntityBuilder.getTechParamsBuilder.setGearType("ALL_WHEEL_DRIVE")
      card.getEntityBuilder.getTechParamsBuilder.setDisplacement(1995)
      card.getEntityBuilder.getTechParamsBuilder.setPower(190)
      builder.putTechParam(techParamId.toString, card.build)
    }

    Map(CardId -> Eval.now(Future.successful(Some(builder.build()))))
  }

  "PriceStatsLazyExecution.buildPriceHistograms" should {

    def generateHistogramResponse(size: Int = 3): Future[PriceHistogramResponse] = {
      val builder = PriceHistogramResponse.newBuilder()
      LazyList.fill(size)(PriceHistogramGroup.getDefaultInstance).foreach(builder.addGroups)
      builder.setSearchParameters(SearchRequestParameters.getDefaultInstance)
      Future.successful(builder.build())
    }

    def withTechParamId: ArgumentMatcher[PriceHistogramParams] = { priceHistogramParams =>
      priceHistogramParams != null &&
      priceHistogramParams.techparamId.isDefined &&
      priceHistogramParams.mark.isEmpty &&
      priceHistogramParams.model.isEmpty &&
      priceHistogramParams.configurationId.isEmpty
    }

    def withMarkModel: ArgumentMatcher[PriceHistogramParams] = { priceHistogramParams =>
      priceHistogramParams != null &&
      priceHistogramParams.techparamId.isEmpty &&
      priceHistogramParams.mark.isDefined &&
      priceHistogramParams.model.isDefined &&
      priceHistogramParams.configurationId.isDefined
    }

    "return histogram with 3 groups" in new Fixture {
      val card: Card = generateCard()
      when(searcherManager.histogram(eq(CategorySelector.Cars), ?)(?)).thenAnswer { answer =>
        val params: PriceHistogramParams = answer.getArgument(1)
        generateHistogramResponse(params.nCount.get.toInt)
      }

      val result: PriceStatsLazyExecution.PriceHistogramData = buildPriceHistogram(card)

      result.groups.length shouldBe 3
    }

    "request histogram by tech_param_id when it is defined" in new Fixture {
      val card: Card = generateCard()
      val histogramResponse: Future[PriceHistogramResponse] = generateHistogramResponse()
      when(searcherManager.histogram(?, argThat(withTechParamId))(?)).thenReturn(histogramResponse)

      buildPriceHistogram(card)

      verify(searcherManager, times(1)).histogram(?, ?)(?)
    }

    "request histogram by mark/model when tech_param_id is not defined" in new Fixture {
      val card: Card = generateCard(withTechParamId = false)
      val histogramResponse: Future[PriceHistogramResponse] = generateHistogramResponse()
      when(searcherManager.histogram(?, argThat(withMarkModel))(?)).thenReturn(histogramResponse)

      buildPriceHistogram(card)

      verify(searcherManager, times(1)).histogram(?, ?)(?)
    }

    "request histogram by mark/model when request by tech_param_id fails" in new Fixture {
      val card: Card = generateCard()
      val histogramResponse: Future[PriceHistogramResponse] = generateHistogramResponse()
      when(searcherManager.histogram(?, argThat(withMarkModel))(?)).thenReturn(histogramResponse)
      when(searcherManager.histogram(?, argThat(withTechParamId))(?))
        .thenThrowF(new AutoPriceHistogramNotFound(Some("Supposed to fail in test")))

      buildPriceHistogram(card)

      val inOrder: InOrder = Mockito.inOrder(searcherManager)
      inOrder.verify(searcherManager, times(1)).histogram(?, argThat(withTechParamId))(?)
      inOrder.verify(searcherManager, times(1)).histogram(?, argThat(withMarkModel))(?)
    }

    "enrich tech params into search request params" in new Fixture {
      val card: Card = generateCard()
      val histogramResponse: Future[PriceHistogramResponse] = generateHistogramResponse()
      when(searcherManager.histogram(?, argThat(withTechParamId))(?)).thenReturn(histogramResponse)

      val catalogCards: Map[String, LazyExecution[RawCatalog]] = generateCatalogCards(card)

      val result: PriceStatsLazyExecution.PriceHistogramData = buildPriceHistogram(card, catalogCards)

      private val carsParams = result.searchRequestParameters.getCarsParams
      carsParams.getBodyTypeGroupList.asScala should not be empty
      carsParams.getTransmissionList.asScala should not be empty
      carsParams.getEngineGroupList.asScala should not be empty
      carsParams.getGearTypeList.asScala should not be empty
      result.searchRequestParameters.getDisplacementFrom should be > 0
      result.searchRequestParameters.getDisplacementTo should be > 0
      result.searchRequestParameters.getPowerFrom should be > 0
      result.searchRequestParameters.getPowerTo should be > 0
    }

    "not enrich tech params when feature is disabled" in new Fixture {
      when(featureFlag.value).thenReturn(false)

      val card: Card = generateCard()
      val histogramResponse: Future[PriceHistogramResponse] = generateHistogramResponse()
      when(searcherManager.histogram(?, argThat(withTechParamId))(?)).thenReturn(histogramResponse)

      val catalogCards: Map[String, LazyExecution[RawCatalog]] = generateCatalogCards(card)

      val result: PriceStatsLazyExecution.PriceHistogramData = buildPriceHistogram(card, catalogCards)

      private val carsParams = result.searchRequestParameters.getCarsParams
      carsParams.getBodyTypeGroupList.asScala shouldBe empty
      carsParams.getTransmissionList.asScala shouldBe empty
      carsParams.getEngineGroupList.asScala shouldBe empty
      carsParams.getGearTypeList.asScala shouldBe empty
      result.searchRequestParameters.getDisplacementFrom shouldBe 0
      result.searchRequestParameters.getDisplacementTo shouldBe 0
      result.searchRequestParameters.getPowerFrom shouldBe 0
      result.searchRequestParameters.getPowerTo shouldBe 0
    }
  }

  trait Fixture extends MockitoSupport {
    val statsManager: StatsManager = mock[StatsManager]
    val searcherManager: SearcherManager = mock[SearcherManager]
    val featureManager: FeatureManager = mock[FeatureManager]
    val featureFlag: Feature[Boolean] = mock[Feature[Boolean]]

    when(featureManager.addTechParamsToSearchParamsInPriceHistogram).thenReturn(featureFlag)
    when(featureFlag.value).thenReturn(true)

    val priceStatsLazyExecution: PriceStatsLazyExecution = new PriceStatsLazyExecution(
      statsManager,
      searcherManager,
      featureManager
    )

    def buildPriceHistogram(card: Card): PriceStatsLazyExecution.PriceHistogramData =
      buildPriceHistogram(
        card,
        Map(card.getId -> Eval.now(Future.successful(None)))
      )

    def buildPriceHistogram(
        card: Card,
        catalogCards: Map[String, LazyExecution[RawCatalog]]
    ): PriceStatsLazyExecution.PriceHistogramData =
      priceStatsLazyExecution
        .buildPriceHistograms(List(card), Rid, catalogCards)
        .getOrExecute(CardId)
        .await
        .get
  }
}
