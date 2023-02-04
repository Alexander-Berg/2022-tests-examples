package ru.yandex.auto.garage.scheduler.stage

import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.auto.vin.decoder.scheduler.stage.Stage

trait GarageCardStageSupport[B <: Stage[Long, GarageCard]] extends StageSupport[Long, GarageCard, B] {
  implicit override val stateS = ru.yandex.auto.garage.garageCardState
}
