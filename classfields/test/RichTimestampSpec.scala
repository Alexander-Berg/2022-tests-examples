package common.scalapb.test

import com.google.protobuf.timestamp.Timestamp
import common.scalapb.ScalaProtobuf
import common.scalapb.ScalaProtobuf.RichTimestamp
import common.zio.clock.MoscowClock
import zio.ZIO
import zio.test.Assertion._
import zio.test._

import java.time.{DateTimeException, OffsetDateTime}

object RichTimestampSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[environment.TestEnvironment, Any] = suite(classOf[RichTimestamp].getName)(
    test("""should convert protobuf.Timestamp to OffsetDateTime""") {
      val nowOffsetDateTime = OffsetDateTime.now(MoscowClock.timeZone)
      val nowTimestamp = ScalaProtobuf.toTimestamp(nowOffsetDateTime)
      assert(nowTimestamp.toOffsetDateTime)(equalTo(nowOffsetDateTime))
    },
    testM("""should fail when protobuf.Timestamp is ouf of range for OffsetDateTime""") {
      val MinSecond = -31557014167219200L
      val MaxSecond = 31556889864403199L

      checkAll(Gen.fromIterable(Seq(MinSecond - 1, MaxSecond + 1))) { outOfRangeSeconds =>
        val outOfRangeTimestamp = Timestamp.of(outOfRangeSeconds, 0)

        assert(outOfRangeTimestamp.toOffsetDateTime)(
          throws(isSubtype[DateTimeException](anything))
        )
      }
    },
    test("""should fail when DateTimeException on invalid Timestamp""") {
      val longOverflowCausingTimestamp = Timestamp.of(Long.MinValue, Int.MinValue)

      assert(longOverflowCausingTimestamp.toOffsetDateTime)(
        throws(isSubtype[DateTimeException](anything))
      )
    }
  )
}
