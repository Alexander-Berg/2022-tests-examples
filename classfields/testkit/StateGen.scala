package ru.yandex.vertis.general.gost.model.testkit

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.google.protobuf.timestamp.Timestamp
import ru.yandex.vertis.general.gost.model.SellingAddress.{apply => _}
import ru.yandex.vertis.general.gost.model.sheduler.State
import zio.test.Gen
import zio.test.magnolia.DeriveGen
import zio.test.magnolia.DeriveGen.instance

object StateGen {

  implicit val genInstant: DeriveGen[Instant] = {
    val to = Instant.now()
    val from = to.minus(1, ChronoUnit.DAYS)
    instance(Gen.instant(from, to))
  }

  val genInstantMs: DeriveGen[Instant] = instance(genInstant.derive.map(_.truncatedTo(ChronoUnit.MILLIS)))

  implicit val protobufAnyGen: DeriveGen[com.google.protobuf.any.Any] = instance {
    for {
      long <- Gen.long(0, Long.MaxValue)
      int <- Gen.int(0, Int.MaxValue)
      timestamp = Timestamp.of(long, int)
      any = com.google.protobuf.any.Any.pack(timestamp)
    } yield any
  }

  val any = DeriveGen[State]
}
