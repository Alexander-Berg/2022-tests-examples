package ru.auto.salesman.dao.impl.jdbc.user

import org.joda.time.DateTime
import org.scalatest.LoneElement
import ru.auto.salesman.dao.user.SubscriptionDao
import ru.auto.salesman.model.user.Prolongable
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ProductStatuses}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

class JdbcSubscriptionDaoSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate
    with LoneElement
    with UserModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val dao = new JdbcSubscriptionDao(database)

  "JdbcSubscriptionDao" should {
    "filter out subscriptions with counter < 0" in {

      val product = OffersHistoryReports(1)
      val user = "user:123"

      val subscriptionCreateRequest = SubscriptionDao.Request(
        user,
        product,
        1L,
        1200,
        ProductStatuses.Active,
        "transaction-id-0",
        DateTime.now(),
        DateTime.now(),
        SubscriptionContextGen.next,
        Prolongable(false)
      )

      dao
        .insertIfNotExists(
          subscriptionCreateRequest
            .copy(counter = 0, transactionId = "transaction-id-1")
        )
        .success

      dao
        .get(SubscriptionDao.Filter.ForUtilizedActiveProductUser(product, user))
        .success
        .value shouldBe Iterable()

      dao.insertIfNotExists(subscriptionCreateRequest).success

      val subscriptions = dao
        .get(SubscriptionDao.Filter.ForUtilizedActiveProductUser(product, user))
        .success
        .value
      subscriptions should have size 1
      subscriptions.loneElement.transactionId shouldEqual "transaction-id-0"
    }
  }
}
