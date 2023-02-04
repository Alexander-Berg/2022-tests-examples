package common.scalapb.test

import com.google.protobuf.timestamp.Timestamp
import common.scalapb.ScalaProtobuf
import zio.test._
import zio.test.Assertion._

import java.time.OffsetDateTime

object ScalaProtobufSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[environment.TestEnvironment, Any] =
    suite("""ScalaProtobuf""") {
      testM("""should convert OffsetDateTime to protobuf.Timestamp""") {
        val testCases = Seq(
          OffsetDateTime.parse("2021-05-17T12:37:14.125Z") -> Timestamp.of(1621255034, 125000000)
        )

        checkAll(Gen.fromIterable(testCases)) { case (offsetDateTime, expected) =>
          assert(ScalaProtobuf.toTimestamp(offsetDateTime))(equalTo(expected))
        }
      }
    }
}
