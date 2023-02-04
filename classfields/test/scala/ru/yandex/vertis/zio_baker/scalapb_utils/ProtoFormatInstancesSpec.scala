package ru.yandex.vertis.zio_baker.scalapb_utils

import io.circe.syntax._
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoFormatInstances._
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoJson._
import zio.Tag
import zio.test._
import zio.test.diff.Diff
import zio.test.environment.TestEnvironment
import zio.test.internal.OptionalImplicit

import java.time.Instant
import scala.concurrent.duration._

object ProtoFormatInstancesSpec extends DefaultRunnableSpec {

  private val genInstant = Gen.instant(
    // ScalaPB throws exceptions when encoding timestamps outside this range.
    min = Instant.ofEpochSecond(scalapb_json.Timestamps.TIMESTAMP_SECONDS_MIN),
    max = Instant.ofEpochSecond(scalapb_json.Timestamps.TIMESTAMP_SECONDS_MAX)
  )
  private val genFiniteDuration = Gen.anyLong.map(_.nanos)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("codecs")(
      checkMessage(genInstant),
      checkMessage(genFiniteDuration)
    )

  def checkMessage[T, M <: ProtoMessage[M]](
      gen: Gen[TestEnvironment, T]
    )(implicit format: ProtoMessageFormat[T, M],
      tag: Tag[T],
      diff: OptionalImplicit[Diff[T]]) =
    suite(s"convert proto msg ${tag.tag}")(
      testM("protobuf round-trip")(
        check(gen) { t: T =>
          val result = format.fromProto(format.toProto(t))
          assertTrue(result.toEither.right.get == t)
        }
      ),
      testM("JSON round-trip")(
        check(gen) { t: T =>
          val result = io.circe.parser.decode[T](t.asJson.toString)
          assertTrue(result.right.get == t)
        }
      )
    )
}
