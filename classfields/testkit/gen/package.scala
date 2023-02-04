package auto.dealers.booking.testkit

import java.time.{Instant, OffsetDateTime}

import com.google.protobuf.struct.Value
import common.scalapb.ScalaProtobuf
import zio.random.Random
import zio.test.{Gen, Sized}
import com.google.protobuf.timestamp.Timestamp
import common.zio.clock.MoscowClock

package object gen {

  type RGen[A] = Gen[Random with Sized, A]

  implicit def value2gen[A](a: A): RGen[A] = Gen.const(a)

  def string2Value(value: String): Value =
    Value(kind = com.google.protobuf.struct.Value.Kind.StringValue(value))

  val instantGen: RGen[Instant] = Gen.long(-10000, 10000).map(i => Instant.now().minusSeconds(i))
  val timestampGen: RGen[Timestamp] = instantGen.map(ScalaProtobuf.instantToTimestamp)
  val offsetDateTimeGen: RGen[OffsetDateTime] = instantGen.map(t => OffsetDateTime.ofInstant(t, MoscowClock.timeZone))

}
