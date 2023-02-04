package ru.yandex.auto.vin.decoder.scheduler.stage

import ru.yandex.auto.vin.decoder.model.scheduler._
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.Order

trait OrdersStageSupport[B <: Stage[String, Order]] extends StageSupport[String, Order, B] {
  implicit override val stateS = os
}
