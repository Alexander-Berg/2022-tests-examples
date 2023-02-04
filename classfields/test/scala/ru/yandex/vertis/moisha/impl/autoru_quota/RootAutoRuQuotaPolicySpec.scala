package ru.yandex.vertis.moisha.impl.autoru_quota

import org.joda.time.DateTime
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.moisha.environment.wholeDay
import ru.yandex.vertis.moisha.impl.autoru_quota.AutoRuQuotaPolicy.AutoRuQuotaRequest
import ru.yandex.vertis.moisha.impl.autoru_quota.model.AutoRuQuotaContext
import ru.yandex.vertis.moisha.impl.autoru_quota.model.Products.PlacementCarsNew
import ru.yandex.vertis.moisha.index.IndexProvider
import ru.yandex.vertis.moisha.util.GeoIds.RegSaratov

class RootAutoRuQuotaPolicySpec extends WordSpec with Matchers {

  private lazy val indexProvider = mock[IndexProvider]

  private val policy = new RootAutoRuQuotaPolicy(indexProvider)

  "RootAutoRuQuotaPolicy" should {

    "return 330 for Saratov in august 2019" in {
      val context = AutoRuQuotaContext(
        amount = Int.MaxValue,
        clientRegionId = RegSaratov,
        clientCityId = None,
        clientMarks = None,
        tariff = None
      )
      val interval = wholeDay(DateTime.parse("2019-08-30T00:00:00+03:00"))
      val quotaRequest = AutoRuQuotaRequest(PlacementCarsNew, context, interval)
      policy.estimate(quotaRequest).get.points.head.product.goods.head.price / 100 shouldBe 330
    }

    "return 330 for Saratov in september 2019" in {
      val context = AutoRuQuotaContext(
        amount = Int.MaxValue,
        clientRegionId = RegSaratov,
        clientCityId = None,
        clientMarks = None,
        tariff = None
      )
      val interval = wholeDay(DateTime.parse("2019-09-01T00:00:00+03:00"))
      val quotaRequest = AutoRuQuotaRequest(PlacementCarsNew, context, interval)
      policy.estimate(quotaRequest).get.points.head.product.goods.head.price / 100 shouldBe 330
    }
  }
}
