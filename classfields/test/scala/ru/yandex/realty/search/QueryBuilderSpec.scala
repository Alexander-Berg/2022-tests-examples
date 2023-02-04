package ru.yandex.realty.search

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.currency.Currency
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.context.street.StreetStorage
import ru.yandex.realty.geocoder.LocationUnifierService
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.handlers.UserInputContext
import ru.yandex.realty.handlers.search.SearchUserInput
import ru.yandex.realty.model.message.ExtDataSchema.Tour3dMessage.Tour3dType
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.{Range => SearchRange}

@RunWith(classOf[JUnitRunner])
class QueryBuilderSpec extends SpecBase {

  val queryBuilder = new QueryBuilderStub

  "queryBuilder" should {

    "just move floors total from user input" in {
      val floorsTotal: Option[SearchRange] = Some(SearchRange.create(5f, 10f))
      val context = UserInputContext(currency = Some(Currency.RUR))
      val input = SearchUserInput(floorsTotal = floorsTotal, siteContext = context)

      val searchQuery = queryBuilder.buildQuery(input, None)(Traced.empty)
      searchQuery.get.floorsTotal shouldBe floorsTotal
    }

    "read tour3dTypes from user input" in {
      val tours: Seq[Tour3dType] = Seq(Tour3dType.TOUR_3D)
      val context = UserInputContext(currency = Some(Currency.RUR))
      val input = SearchUserInput(tour3dType = tours, siteContext = context)

      val searchQuery = queryBuilder.buildQuery(input, None)(Traced.empty)
      searchQuery.get.tour3dTypes shouldBe tours
    }
  }

  class QueryBuilderStub extends QueryBuilder with MockFactory {

    override def regionGraphProvider: Provider[RegionGraph] = mock[Provider[RegionGraph]]

    override def locationUnifierService: LocationUnifierService = mock[LocationUnifierService]

    override def streetStorage: StreetStorage = mock[StreetStorage]

    override def geohubClient: GeohubClient = mock[GeohubClient]
  }
}
