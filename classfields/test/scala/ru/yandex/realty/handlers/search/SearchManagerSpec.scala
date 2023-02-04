package ru.yandex.realty.handlers.search

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.Slice.Full
import ru.yandex.realty.api.Sort.ByPrice
import ru.yandex.realty.api.{Slice, Sort}
import ru.yandex.realty.clients.crypta.CryptaClient
import ru.yandex.realty.crypta.{BnbBigBEnricher, BnbBigBLogger}
import ru.yandex.realty.handlers.UserInputContext
import ru.yandex.realty.model.locale.RealtyLocale
import ru.yandex.realty.render.RenderableSearchResult
import ru.yandex.realty.render.search.query.RenderableSearchQuery
import ru.yandex.realty.render.search.{RenderablePager, RenderableSearchResponse}
import ru.yandex.realty.request.{Request, RequestImpl}
import ru.yandex.realty.search.SearchFacade
import ru.yandex.realty.tracing.Traced

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class SearchManagerSpec extends AsyncSpecBase {

  "SearchManager" should {
    "siteSearch should work fine" in new SearchManagerFixture {
      val searchResponse: RenderableSearchResponse =
        manager.siteSearch(input, ByPrice, Full)(new RequestImpl).futureValue
      searchResponse.result.pager.totalPages shouldBe 1
    }
    "siteSearch should not wait for bigB info logger" in new SearchManagerFixture {
      val searchResponseF: Future[RenderableSearchResponse] =
        manager.siteSearch(input, ByPrice, Full)(new RequestImpl)
      val searchResponse: RenderableSearchResponse = Await.result(searchResponseF, 1.second)
      searchResponse.result.pager.totalPages shouldBe 1
    }
  }

  trait SearchManagerFixture {
    val input: SearchUserInput = SearchUserInput(siteContext = UserInputContext(currency = None))

    val facade: SearchFacade = mock[SearchFacade]
    val cryptaClient: CryptaClient = mock[CryptaClient]
    val enricher: BnbBigBEnricher = new BnbBigBEnricher(cryptaClient)
    val logger: BnbBigBLogger = mock[BnbBigBLogger]
    var flag: Boolean = false

    (logger
      .logBigBInfo(_: SearchUserInput)(_: Traced))
      .expects(input, *)
      .returning(Future {
        Thread.sleep(2000)
      })

    (facade
      .search(_: SearchUserInput, _: Sort, _: Slice)(_: Request))
      .expects(input, ByPrice, Full, *)
      .returns(
        Try(
          RenderableSearchResponse(
            RenderableSearchQuery(ByPrice, locale = RealtyLocale.RU),
            RenderableSearchResult(RenderablePager(Full, 999))
          )
        )
      )

    val manager = new SearchManager(
      facade,
      enricher,
      logger
    )
  }
}
