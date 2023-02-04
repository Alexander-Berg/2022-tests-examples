package ru.auto.salesman.dao

import ru.auto.salesman.model.ProductId
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.test.BaseSpec

trait OffersWithPaidProductsArchiveSalesDaoSpec extends BaseSpec {
  protected def dao: OffersWithPaidProductsArchiveSalesDao

  "OffersWithPaidProductsArchiveSalesDao" should {
    "check find by client id and more id" in {

      val res = dao
        .getRecords(
          fromSaleServiceId = 1112226,
          limit = 100
        )
        .success
        .value

      res.size shouldBe 2

      res.head.offerId shouldBe AutoruOfferId(5555, "6c99")
      res.head.clientId shouldBe 20102L
      res.head.product shouldBe ProductId.Placement

    }

  }
}
