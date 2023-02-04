package ru.yandex.auto.searcher.http.action

import java.util.concurrent.TimeUnit

import org.junit.rules.Timeout
import org.junit.{Ignore, Test}
import org.springframework.beans.factory.annotation.Autowired
import ru.auto.api.ResponseModel
import ru.yandex.auto.searcher.core.CarSearchParams._
import ru.yandex.auto.searcher.core.CarSearchParamsImpl
import ru.yandex.auto.searcher.http.action.api.{ApiCarsSearchAction, ApiRegionRadiusCounterAction}
import ru.yandex.auto.searcher.sort.SortType

@Ignore
class ApiRegionRadiusCounterActionTest extends AbstractSearcherTest {

  @Autowired var apiRegionRadiusCounterAction: ApiRegionRadiusCounterAction = _
  @Autowired var carsSearchAction: ApiCarsSearchAction = _

  @Test
  @Timeout(15, TimeUnit.MINUTES)
  def calculatePosition(): Unit = {
    val params: CarSearchParamsImpl = createSearchParams()
    params.setParam("mark-model-nameplate", "VAZ#GRANTA")
    params.setParam(OFFER_GROUPING, "true")
    params.setParam(GROUP_BY, "TECHPARAM")
    params.setParam(OFFERS_SORT, SortType.NO_SORTING.getParamSortName)

    val calculatedPosition: ResponseModel.OfferLocatorCounterResponse =
      apiRegionRadiusCounterAction.processInternal(params, emptyProfileData, emptyRequest)

    println(calculatedPosition)
  }
}
