package ru.auto.comeback.model.testkit

import ru.auto.comeback.model.{ComebackUpdateEvent, ComebackUpdateEventT}
import ru.auto.comeback.model.ComebackUpdateEvent.{ComebackUpdateEvent, NewComebackUpdateEvent}
import zio.random.Random
import zio.test.{Gen, Sized}

object ComebackUpdateEventGen {

  val anyEvent: Gen[Random with Sized, ComebackUpdateEvent] = for {
    id <- Gen.int(0, Int.MaxValue)
    clientId <- Gen.int(0, Int.MaxValue)
    date <- CommonGen.anyInstant
    prevState <- Gen.option(ComebackGen.anyComeback)
    currentState <- ComebackGen.anyComeback
  } yield ComebackUpdateEvent(id, clientId, date, prevState, currentState)

  val anyNewEvent: Gen[Random with Sized, NewComebackUpdateEvent] = anyEvent.map(_.copy(id = None))
}
