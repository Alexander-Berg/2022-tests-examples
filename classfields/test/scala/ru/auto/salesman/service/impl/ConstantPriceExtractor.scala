package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.salesman.model.{Funds, ProductId}
import ru.auto.salesman.service.PriceExtractor
import zio.{Task, ZIO}

object ConstantPriceExtractor extends PriceExtractor {

  def price(product: ProductId, date: DateTime): Task[Funds] =
    ZIO.succeed(100)

  def productInfo(
      productId: ProductId,
      dateTime: DateTime
  ): Task[PriceExtractor.ProductInfo] =
    ???
}
