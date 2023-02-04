package ru.yandex.auto.searcher.http.action

import org.junit.{Assert, Ignore, Test}
import org.springframework.beans.factory.annotation.Autowired
import ru.auto.api.search.SearchModel.{RegionFilter, SalonSuggestionsRequest}
import ru.yandex.auto.searcher.core.{CarSearchParams, CarSearchParamsFactory, CarSearchParamsImpl}
import ru.yandex.auto.searcher.http.action.api.dealer.ApiSalonSuggestions
import ru.yandex.auto.searcher.http.action.dealers.DealersSuggestAction

@Ignore
class ApiSalonSuggestionsTest extends AbstractSearcherTest {

  import ru.yandex.auto.searcher.http.action.api.dealer.Ops._

  @Autowired var apiSalonSuggestions: ApiSalonSuggestions = _
  @Autowired var dealersSuggestAction: DealersSuggestAction = _
  @Autowired var carSearchParamsFactory: CarSearchParamsFactory[CarSearchParamsImpl] = _

  @Test def salonSuggestionsResponseEqualsToDealersSuggestAction(): Unit = {

    val searcherParams: CarSearchParams = carSearchParamsFactory.create()
    searcherParams.addMark("AUDI")
    searcherParams.addRegionId("213")

    val result1 = dealersSuggestAction.processInternal(searcherParams, null, null)
    print("DealersSuggestAction result " + result1)

    val request = SalonSuggestionsRequest
      .newBuilder()
      .setCarMark("AUDI")
      .setRegionFilter(RegionFilter.newBuilder().setRid(213))
      .build()

    val result2 = apiSalonSuggestions.search(request)

    print("ApiSalonSuggestions result " + result2)

    Assert.assertEquals(
      result1.toProto.toBuilder
        .setRequest(request)
        .build(),
      result2
    )
  }
}
