package ru.yandex.auto.vin.decoder.scheduler.stage

import ru.yandex.auto.vin.decoder.model.scheduler._
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState

trait CompoundStageSupport[A, B <: Stage[A, CompoundState]] extends StageSupport[A, CompoundState, B] {
  implicit override val stateS = cs
}
