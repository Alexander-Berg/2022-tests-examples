package ru.auto.comeback.model.testkit

import ru.auto.comeback.model.sender.SenderLogRecord
import zio.random.Random
import zio.test.{Gen, Sized}

object SenderLogGen {

  val anyExistingSenderLogRecord: Gen[Random with Sized, SenderLogRecord] = for {
    id <- Gen.long(1, Long.MaxValue)
    comebackId <- Gen.long(1, Long.MaxValue)
    mail <- CommonGen.anyYandexEmail
    date <- CommonGen.anyInstant
  } yield SenderLogRecord(id, comebackId, mail, date)

  val anyNewSenderLogRecord: Gen[Random with Sized, SenderLogRecord] = anyExistingSenderLogRecord.map(_.copy(id = 0))
}
