package ru.auto.salesman.service.impl.user

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.SubscriptionDao.Filter
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ProductStatuses}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.user.{Prolongable, Subscription}
import ru.auto.salesman.service.user.UserProductService.Response
import ru.auto.salesman.service.user._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class UserProductServiceImplSpec extends BaseSpec with UserModelGenerators {

  private val goodsService = mock[GoodsService]
  private val bundleService = mock[BundleService]
  private val subscriptionService = mock[SubscriptionService]

  private val userProductServiceImpl = new UserProductServiceImpl(
    goodsService,
    bundleService,
    subscriptionService
  )

  "UserProductServiceImpl" should {
    "request subscriptions without zero counter" in {
      forAll(SubscriptionContextGen) { context =>
        val product = OffersHistoryReports(1)
        val user = "user:123"
        val subscription = Subscription(
          id = "id",
          user = user,
          product = product,
          counter = 1,
          amount = 1200,
          status = ProductStatuses.Active,
          transactionId = "transaction-id",
          context = context,
          activated = DateTime.now(),
          deadline = DateTime.now(),
          epoch = 123,
          unsafeProlongable = Prolongable(false)
        )
        val expectedResponse = Response(subscription)

        (subscriptionService.get _)
          .expects(Filter.ForUtilizedActiveProductUser(product, user))
          .returningZ(Iterable(subscription))

        userProductServiceImpl
          .get(
            UserProductService.Request(
              product,
              user,
              None
            )
          )
          .success
          .value shouldBe expectedResponse
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
