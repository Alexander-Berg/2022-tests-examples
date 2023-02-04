package ru.auto.api.services.searcher

import ru.auto.api.auth.Application
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.enrich.enrichers.PositionsEnricher
import ru.auto.api.model.CategorySelector.{Cars, Moto, Trucks}
import ru.auto.api.model._
import ru.auto.api.model.searcher.{GroupBy, SearcherRequest}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.web.MockedFeatureManager
import ru.auto.api.unification.Unification.{CarsUnificationCollection, CarsUnificationEntry}
import ru.auto.api.util.RequestImpl

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 13.02.17
  */
class DefaultSearcherClientIntTest extends HttpClientSuite with MockedFeatureManager {

  override protected def config: HttpClientConfig =
    HttpClientConfig("auto2-searcher-api.vrts-slb.test.vertis.yandex.net", 80)

  private val searcherClient = new DefaultSearcherClient(http, featureManager)

  test("get offer position") {
    val offer = ModelGenerators.OfferGen.next.toBuilder
      .setId("123-aabc")
      .build()
    searcherClient.offerPosition(offer, PositionsEnricher.RelevanceSorting, true).futureValue
  }

  implicit private val req = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.desktop)
    r
  }

  private def createEntry(rawMark: Option[String] = None,
                          rawModel: Option[String] = None,
                          rawYear: Option[String] = None,
                          rawYearFrom: Option[String] = None,
                          rawYearTo: Option[String] = None,
                          rawBodyType: Option[String] = None,
                          rawDoorsCount: Option[String] = None,
                          rawPower: Option[String] = None,
                          rawDisplacement: Option[String] = None,
                          rawTransmission: Option[String] = None): CarsUnificationEntry = {
    val builder = CarsUnificationEntry.newBuilder()

    rawMark.foreach(builder.setRawMark)
    rawModel.foreach(builder.setRawModel)
    rawYear.foreach(builder.setRawYear)
    rawYearFrom.foreach(builder.setRawYearFrom)
    rawYearTo.foreach(builder.setRawYearTo)
    rawBodyType.foreach(builder.setRawBodyType)
    rawDoorsCount.foreach(builder.setRawDoorsCount)
    rawPower.foreach(builder.setRawPower)
    rawDisplacement.foreach(builder.setRawDisplacement)
    rawTransmission.foreach(builder.setRawTransmission)

    builder.build()
  }

  test("unify mark and model") {
    val request = CarsUnificationCollection
      .newBuilder()
      .addEntries(createEntry(rawMark = Some("киа"), rawModel = Some("rio")))
      .build()

    val res = searcherClient.unifyCars(request).futureValue

    res.getEntries(0).getMark shouldBe "KIA"
    res.getEntries(0).getModel shouldBe "RIO"
  }

  test("search offers without params") {
    val res = searcherClient
      .searchOffers(
        SearcherRequest.empty(Cars),
        Paging.Default,
        NoSorting,
        GroupBy.NoGrouping,
        eq(None)
      )
      .futureValue

    res.getOffersCount should be > 0
    res.hasPagination shouldBe true
  }

  test("offers count should return count") {
    val res = searcherClient.offersCount(SearcherRequest.empty(Cars)).futureValue
    res.hasCount shouldBe true
    res.getCount should be > 0
  }

  test("breadcrumbs without params should be not empty") {
    pending
    val res = searcherClient.breadcrumbs(Cars, Map(): SearcherFilter).futureValue
    res.getBreadcrumbsCount should be > 0
  }

  test("related throws OfferNotFound on unknown offer") {
    searcherClient
      .related(Cars, OfferID(0, Option("")), Map(), Paging.Default)
      .failed
      .futureValue shouldBe an[OfferNotFoundException]
  }

  test("showcase should return non-empty lists with popular & new cars") {
    val res = searcherClient.showcase(SearcherRequest.empty(Cars)).futureValue
    res.getPopularCount shouldBe >(0)
    res.getNewCount shouldBe >(0)
  }

  test("OfferNotFoundException when get random generated carOffer from searcher client") {
    val id = ModelGenerators.OfferIDGen.next
    val res = searcherClient.getOffer(Cars, id).failed.futureValue
    res shouldBe an[OfferNotFoundException]
  }

  test("OfferNotFoundException when get random generated motoOffer from searcher client") {
    val id = ModelGenerators.OfferIDGen.next
    val res = searcherClient.getOffer(Moto, id).failed.futureValue
    res shouldBe an[OfferNotFoundException]
  }

  test("OfferNotFoundException when get random generated trucksOffer from searcher client") {
    val id = ModelGenerators.OfferIDGen.next
    val res = searcherClient.getOffer(Trucks, id).failed.futureValue
    res shouldBe an[OfferNotFoundException]
  }

}
