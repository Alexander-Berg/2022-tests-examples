package ru.auto.salesman.service.goods

import org.joda.time.DateTime
import org.scalatest.concurrent.IntegrationPatience
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.api.akkahttp.ApiException.ConflictException
import ru.auto.salesman.dao.impl.jdbc.{
  JdbcCarsGoodsQueries,
  JdbcCategorizedGoodsQueries,
  JdbcGoodsDao
}
import ru.auto.salesman.model.AdsRequestTypes.CarsUsed
import ru.auto.salesman.model.ProductId.Fresh
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{CityId, Client, ClientStatuses, OfferCategories, RegionId}
import ru.auto.salesman.service.goods.domain.GoodsRequest
import ru.auto.salesman.service.goods.provider.FreshProvider
import ru.auto.salesman.service.impl.GoodsDaoProviderImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.test.template.SalesJdbcSpecTemplate

class FreshProviderSpec
    extends BaseSpec
    with IntegrationPatience
    with SalesJdbcSpecTemplate {

  val carsGoodsDao = new JdbcGoodsDao(JdbcCarsGoodsQueries, database)
  val categorizedGoodsDao = new JdbcGoodsDao(JdbcCategorizedGoodsQueries, database)

  val goodsDaoProvider =
    new GoodsDaoProviderImpl(carsDao = carsGoodsDao, categorizedDao = categorizedGoodsDao)

  val paymentModelFactory = TestPaymentModelFactory.withoutSingleWithCalls()

  val freshProvider = new FreshProvider(goodsDaoProvider, database, paymentModelFactory)

  val TestClient = Client(
    clientId = 20101,
    agencyId = None,
    categorizedClientId = None,
    companyId = None,
    regionId = RegionId(1L),
    cityId = CityId(21735L),
    status = ClientStatuses.Active,
    singlePayment = Set(CarsUsed),
    firstModerated = true,
    name = Some("Test client"),
    paidCallsAvailable = false,
    priorityPlacement = false
  )

  val TestRequest = GoodsRequest(
    offerId = AutoruOfferId(2, None),
    category = OfferCategories.Cars,
    section = Section.NEW,
    product = Fresh,
    badges = Set.empty[String]
  )

  "Fresh provider" when {

    "create a new fresh" in {
      val request = TestRequest.copy(offerId = AutoruOfferId(3, None))
      val addingResult = freshProvider.add(TestClient, request).success.value
      addingResult.size shouldBe 1
    }

    "fail to create another one with recently added" in {
      val request = TestRequest.copy(offerId = AutoruOfferId(4, None))
      val now = DateTime.now()
      freshProvider
        .add(TestClient, request)
        .provideConstantClock(now)
        .success
        .value

      val addingException = freshProvider
        .add(TestClient, request)
        .provideConstantClock(now.plusMinutes(1))
        .failure
        .exception

      addingException shouldBe a[ConflictException]
    }

    "replace existing old service" in {
      val request = TestRequest.copy(offerId = AutoruOfferId(5, None))
      val now = DateTime.now()
      freshProvider
        .add(TestClient, request)
        .provideConstantClock(now)
        .success
        .value

      val secondAdd = freshProvider
        .add(TestClient, request)
        .provideConstantClock(now.plusMinutes(11))
        .success
        .value
      secondAdd.size shouldBe 1
    }

  }
}
