package ru.auto.cabinet

import ru.auto.cabinet.model.moisha._

package object service {

  def moishaPoint(product: Product, total: Long) =
    Point(ProductView(product, total))
}
