package auto.dealers.amoyak.storage

import common.zio.clock.MoscowClock
import auto.dealers.amoyak.model.{Tariff, Vas}
import zio.test.Gen

package object testkit {
  implicit val zonedDateTimeGen = Gen.anyOffsetDateTime.map(MoscowClock.asMoscowTime)

  implicit val tariffGen = Gen.fromIterable(Tariff.values)
  implicit val vasGen = Gen.fromIterable(Vas.values)
}
