package ru.yandex.vertis.billing.service.checking

import org.scalatest.Ignore
import ru.yandex.vertis.billing.model_core.{CostPerIndexing, Custom, Product}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.model_core.FundsConversions.FundsLong
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice

@Ignore
class RealtyCommercialParallelInputCheckerSpec extends ServiceParallelInputCheckerSpec with MockitoSupport {

  override def defaultUserInputCheckerProvider = new RealtyCommercialInputChecker(_)

  override def productMaker(id: Int): Product =
    Product(Custom(RealtyCommercialInputCheckerSpec.FeedPromotionGoodId, CostPerIndexing(id.rubles)))

}
