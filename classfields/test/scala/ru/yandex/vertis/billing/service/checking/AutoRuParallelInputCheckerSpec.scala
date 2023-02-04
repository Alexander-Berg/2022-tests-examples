package ru.yandex.vertis.billing.service.checking

import org.scalatest.Ignore
import ru.yandex.vertis.billing.model_core.{CostPerIndexing, Custom, DynamicPrice, Product}
import ru.yandex.vertis.billing.service.OrderService
import ru.yandex.vertis.mockito.MockitoSupport

@Ignore
class AutoRuParallelInputCheckerSpec extends ServiceParallelInputCheckerSpec with MockitoSupport {

  private val ordersMock = mock[OrderService]

  override def defaultUserInputCheckerProvider = new AutoRuInputChecker(ordersMock, _)

  override def productMaker(id: Int): Product = {
    Product(
      Custom(
        s"test$id",
        CostPerIndexing(DynamicPrice(None, DynamicPrice.NoConstraints)),
        None,
        None
      )
    )
  }

}
