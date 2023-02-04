package ru.yandex.auto.searcher.http.action

import java.util.concurrent.TimeUnit

import org.junit.rules.Timeout
import org.junit.{Ignore, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Assert
import ru.yandex.auto.searcher.core.CarSearchParams._
import ru.yandex.auto.searcher.core.CarSearchParamsImpl
import ru.yandex.auto.searcher.http.action.api.{ApiAutoruBreadcrumbsAction, ApiCarsSearchAction}

import scala.collection.JavaConverters._

@Ignore
class OfferPositionActionTest extends AbstractSearcherTest {

  @Autowired var offerPositionAction: OfferPositionAction = _
  @Autowired var carsSearchAction: ApiCarsSearchAction = _
  @Autowired var apiAutoruBreadcrumbsAction: ApiAutoruBreadcrumbsAction = _

  @Test
  @Timeout(15, TimeUnit.MINUTES)
  def calculatePosition(): Unit = {
    Assert.notNull(offerPositionAction)
    val params: CarSearchParamsImpl = searchParams

    val searchResults = carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    val carIds = searchResults.getOffersList.asScala.map(_.getId).map(_.split("-")(0))
    Assert.isTrue(carIds.nonEmpty)
    for ((carId, position) <- carIds.zip(Stream.from(1))) {
      assertOrder(params, position, carId)
    }
  }

  @Test
  @Timeout(15, TimeUnit.MINUTES)
  def calculateTailPosition(): Unit = {
    Assert.notNull(offerPositionAction)
    val params = new CarSearchParamsImpl(regionService.getRegionTree, vendorManager, langsProvider)
    params.setParam("image", "true")
    params.setParam(PAGE_SIZE_OFFERS, "100")
    params.setParam(PAGE_NUM_OFFERS, "3")

    val searchResults = carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    val carIds = searchResults.getOffersList.asScala.map(_.getId).map(_.split("-")(0)).take(10)
    Assert.isTrue(carIds.nonEmpty)
    for ((carId, position) <- carIds.zip(Stream.from(201))) {
      assertOrder(params, position, carId)
    }
  }

  @Test
  @Timeout(15, TimeUnit.MINUTES)
  def findOffer(): Unit = {
    val params = new CarSearchParamsImpl(regionService.getRegionTree, vendorManager, langsProvider)

    params.setParam(PAGE_SIZE_OFFERS, "500")
    params.setParam(PAGE_NUM_OFFERS, "1")

    params.setParam("mark-model-nameplate", "RENAULT#LOGAN#stepway")

    val searchResults = carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)
    val carIds = searchResults.getOffersList.asScala

//    println(carIds)
    println(carIds.size)
  }

  @Test def assertSemanticUrl(): Unit = {
    val params = new CarSearchParamsImpl(regionService.getRegionTree, vendorManager, langsProvider)

    val lookup = "RENAULT#LOGAN#stepway"
    params.setParam(PAGE_SIZE_OFFERS, "500")
    params.setParam(BREADCRUMB_LOOKUP, lookup)

    params.setParam("mark-model-nameplate", lookup)

    val searchResults = carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)
    val carIds = searchResults.getOffersList.asScala

    searchResults.getOffersList.asScala
      .map(_.getCarInfo.getModelInfo.getNameplate.getSemanticUrl)
      .foreach(
        url =>
          Assert.isTrue(
            url == "stepway"
          )
      )

    println(carIds.size)

    val searchResults2 = apiAutoruBreadcrumbsAction.buildResult(params)
    println(searchResults2)
//    todo assert searchResults2
  }

  private def assertOrder(
      params: CarSearchParamsImpl,
      index: Int,
      carId: String
  ): Unit = {
    params.setParam(OFFER_ID_FOR_POSITION_REQUEST, "autoru-" + carId)
    val calculatedPosition = offerPositionAction.processInternal(params, null, null)
    println("!carId: " + carId)
    println("!expectedPosition: " + index)
    println("!calculatedPosition: " + calculatedPosition.getPosition)
    println("!getTotalCount: " + calculatedPosition.getTotalCount)
    Assert.isTrue(calculatedPosition.getPosition == index)
  }

  private def searchParams: CarSearchParamsImpl = {
    val params = createSearchParams()
    params.setParam("image", "true")
    params.setParam("mark-model-nameplate", "VAZ#GRANTA")
    params.setParam("sort_offers", "fresh_relevance_1-desc")
    params.setParam("with_hot_offers", "")
    params.setParam("currency", "RUR")
    params.setParam("state", "NEW")
    params
  }

}
