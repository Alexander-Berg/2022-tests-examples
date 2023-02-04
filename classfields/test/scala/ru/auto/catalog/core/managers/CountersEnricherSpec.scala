package ru.auto.catalog.core.managers

import org.mockito.Mockito._
import ru.auto.api.BreadcrumbsModel.Entity
import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.counters.{OfferCountersStatHolder, OfferStatHolder, ReviewCounterStatHolder}
import ru.auto.catalog.model.api.ApiModel._
import ru.yandex.vertis.baker.util.extdata.geo.RegionTree

class CountersEnricherSpec extends BaseSpec {

  trait mocks {
    val offerCounters = mock[OfferCountersStatHolder]
    val reviewCounters = mock[ReviewCounterStatHolder]
    val regionTree = mock[RegionTree]
    val enricher = new CountersEnricher(offerCounters, reviewCounters, regionTree)
    val testMarkId = "MERCEDES"
    val testModelId = "GL_KLASSE"
    val superGenId = "4986814"
    val configurationId = "4986815"
    val techParamId = "20494193"
    val testRegionId = 10
    val testNestedRegionId = 1
  }

  "CountersEnricher" should {

    "not enrich counters if not requested" in new mocks {
      val catalog = RawCatalog.getDefaultInstance
      val request = RawFilterRequest.getDefaultInstance
      val result = enricher.enrich(catalog, request)
      result shouldBe catalog
      verifyNoMoreInteractions(offerCounters, reviewCounters, regionTree)
    }

    "enrich mark-model counters" in new mocks {
      val catalogBuilder = RawCatalog.newBuilder()
      val markBuilder = MarkCard.newBuilder()
      val entity = Entity.newBuilder().setId(testMarkId).build()
      markBuilder.setEntity(entity)

      val modelEntity = Entity.newBuilder().setId(testModelId).build()
      val modelCard = ModelCard.newBuilder().setEntity(modelEntity).build()

      markBuilder.putModel(testModelId, modelCard)
      catalogBuilder.putMark(testMarkId, markBuilder.build())

      val catalog = catalogBuilder.build()

      val request = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(
          BreadcrumbsReturnMode
            .newBuilder()
            .addRegionId(testRegionId)
            .addRegionId(testNestedRegionId)
            .addState(State.USED)
        )
        .build()

      val markStatHolder = mock[OfferStatHolder]
      when(markStatHolder.getOffersCount(testMarkId, testRegionId, State.USED)).thenReturn(20)
      when(offerCounters.markStats).thenReturn(markStatHolder)
      when(reviewCounters.markStats).thenReturn(Map(testMarkId -> 10))

      val modelStatHolder = mock[OfferStatHolder]
      when(modelStatHolder.getOffersCount(testMarkId + "#" + testModelId, testRegionId, State.USED))
        .thenReturn(21)
      when(offerCounters.modelStats).thenReturn(modelStatHolder)
      when(reviewCounters.modelStats).thenReturn(Map(testMarkId + "#" + testModelId -> 22))

      when(regionTree.isInside(testRegionId, Set(testNestedRegionId.toLong))).thenReturn(false)
      when(regionTree.isInside(testNestedRegionId, Set(testRegionId.toLong))).thenReturn(true)
      val result = enricher.enrich(catalog, request)
      val mark = result.getMarkMap.get(testMarkId)
      val model = mark.getModelMap.get(testModelId)
      mark.getEntity.getOffersCount shouldBe 20
      mark.getEntity.getReviewsCount shouldBe 10
      model.getEntity.getOffersCount shouldBe 21
      model.getEntity.getReviewsCount shouldBe 22
    }

    "enrich generation counters" in new mocks {
      val catalogBuilder = RawCatalog.newBuilder()
      val superGenCard = SuperGenCard.newBuilder()
      val entity = Entity.newBuilder().setId(superGenId).build()
      superGenCard
        .setEntity(entity)
        .setParentMark(testMarkId)
        .setParentModel(testModelId)

      catalogBuilder.putSuperGen(superGenId, superGenCard.build())

      val catalog = catalogBuilder.build()

      val request = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(
          BreadcrumbsReturnMode
            .newBuilder()
            .addRegionId(testRegionId)
            .addRegionId(testNestedRegionId)
            .addState(State.USED)
        )
        .build()

      val offerStatHolder = mock[OfferStatHolder]
      when(offerStatHolder.getOffersCount(superGenId, testRegionId, State.USED)).thenReturn(20)
      when(offerCounters.generationStats).thenReturn(offerStatHolder)
      val reviewsKey = testMarkId + "#" + testModelId + "##" + superGenId
      when(reviewCounters.generationStats).thenReturn(Map(reviewsKey -> 10))
      when(regionTree.isInside(testRegionId, Set(testNestedRegionId.toLong))).thenReturn(false)
      when(regionTree.isInside(testNestedRegionId, Set(testRegionId.toLong))).thenReturn(true)
      val result = enricher.enrich(catalog, request)

      result.getSuperGenMap.get(superGenId).getEntity.getOffersCount shouldBe 20
      result.getSuperGenMap.get(superGenId).getEntity.getReviewsCount shouldBe 10
    }

    "enrich configuration counters" in new mocks {
      val catalogBuilder = RawCatalog.newBuilder()
      val configurationCard = ConfigurationCard.newBuilder()
      val entity = Entity.newBuilder().setId(configurationId).build()
      configurationCard.setEntity(entity)

      catalogBuilder.putConfiguration(configurationId, configurationCard.build())

      val catalog = catalogBuilder.build()

      val request = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(
          BreadcrumbsReturnMode
            .newBuilder()
            .addRegionId(testRegionId)
            .addRegionId(testNestedRegionId)
            .addState(State.USED)
        )
        .build()

      val offerStatHolder = mock[OfferStatHolder]
      when(offerStatHolder.getOffersCount(configurationId, testRegionId, State.USED)).thenReturn(20)
      when(offerCounters.configurationStats).thenReturn(offerStatHolder)
      when(regionTree.isInside(testRegionId, Set(testNestedRegionId.toLong))).thenReturn(false)
      when(regionTree.isInside(testNestedRegionId, Set(testRegionId.toLong))).thenReturn(true)
      val result = enricher.enrich(catalog, request)

      result.getConfigurationMap.get(configurationId).getEntity.getOffersCount shouldBe 20
    }

    "enrich tech param counters" in new mocks {
      val catalogBuilder = RawCatalog.newBuilder()
      val techParamCard = TechParamCard.newBuilder()
      val entity = Entity.newBuilder().setId(techParamId).build()
      techParamCard.setEntity(entity)

      catalogBuilder.putTechParam(techParamId, techParamCard.build())

      val catalog = catalogBuilder.build()

      val request = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(
          BreadcrumbsReturnMode
            .newBuilder()
            .addRegionId(testRegionId)
            .addRegionId(testNestedRegionId)
            .addState(State.USED)
        )
        .build()

      val offerStatHolder = mock[OfferStatHolder]
      when(offerStatHolder.getOffersCount(techParamId, testRegionId, State.USED)).thenReturn(20)
      when(offerCounters.techParamStats).thenReturn(offerStatHolder)
      when(regionTree.isInside(testRegionId, Set(testNestedRegionId.toLong))).thenReturn(false)
      when(regionTree.isInside(testNestedRegionId, Set(testRegionId.toLong))).thenReturn(true)
      val result = enricher.enrich(catalog, request)

      result.getTechParamMap.get(techParamId).getEntity.getOffersCount shouldBe 20
    }

  }

}
