package ru.yandex.auto.searcher.http.action

import java.util.concurrent.TimeUnit

import org.junit.rules.Timeout
import org.junit.{Ignore, Test}
import org.springframework.beans.factory.annotation.Autowired
import ru.auto.api.ResponseModel
import ru.auto.api.ResponseModel.ListingGrouping
import ru.yandex.auto.searcher.core.CarSearchParams._
import ru.yandex.auto.searcher.core.CarSearchParamsImpl
import ru.yandex.auto.searcher.http.action.api.ApiCarsSearchAction

import scala.collection.JavaConverters._

@Ignore
class ApiCarsSearchActionTest extends AbstractSearcherTest {

  @Autowired var carsSearchAction: ApiCarsSearchAction = _

  @Test
  @Timeout(5, TimeUnit.MINUTES)
  def calculateGroupCount(): Unit = {
    val params: CarSearchParamsImpl = createSearchParams()
    params.setParam(OFFER_GROUPING, "true")
    params.setParam(GROUP_BY, "TECHPARAM")

    val fullResponse: ResponseModel.OfferListingResponse =
      carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    params.setParam(IS_ONLY_COUNT, "true")
    val onlyCount: ResponseModel.OfferListingResponse =
      carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    fullResponse.getPagination.getTotalOffersCount shouldEqual onlyCount.getPagination.getTotalOffersCount
    fullResponse.getGrouping shouldEqual onlyCount.getGrouping

    // другая группировка
    params.setParam(
      GROUPING_ID,
      "tech_param_id=%d,complectation_id=%d".format(
        fullResponse.getOffersList.asScala.head.getCarInfo.getTechParamId,
        fullResponse.getOffersList.asScala.head.getCarInfo.getComplectationId
      )
    )

    val fullResponse2: ResponseModel.OfferListingResponse =
      carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    params.setParam(IS_ONLY_COUNT, "true")
    val onlyCount2: ResponseModel.OfferListingResponse =
      carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    fullResponse2.getPagination.getTotalOffersCount shouldBe >(0)
    fullResponse2.getPagination.getTotalOffersCount shouldEqual onlyCount2.getPagination.getTotalOffersCount
    fullResponse2.getGrouping shouldEqual onlyCount2.getGrouping
  }

  @Test
  def plainCountIfNoGrouping(): Unit = {
    val params: CarSearchParamsImpl = createSearchParams()
    params.setParam(IS_ONLY_COUNT, "true")

    val onlyCount: ResponseModel.OfferListingResponse =
      carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    println(onlyCount)
    onlyCount.getPagination.getTotalOffersCount shouldBe >(0)
    onlyCount.getGrouping shouldBe ListingGrouping.getDefaultInstance
  }

}
