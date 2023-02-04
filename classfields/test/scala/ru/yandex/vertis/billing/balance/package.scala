package ru.yandex.vertis.billing

import ru.yandex.vertis.billing.balance.model.{OperatorId, ProductId, ServiceId}

package object balance {

  implicit val operatorUid: OperatorId = "123"

  val serviceId: ServiceId = 97
  val productId: ProductId = 503794L

  // samehome
  val uid = "37161071"
  val login = "samehome"
}
