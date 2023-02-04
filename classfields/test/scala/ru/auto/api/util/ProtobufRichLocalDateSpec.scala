package ru.auto.api.util

import com.google.protobuf.Timestamp
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec

import java.time.{LocalDate, LocalTime}

class ProtobufRichLocalDateSpec extends BaseSpec with ScalaCheckPropertyChecks {

  import Protobuf.RichLocalDate

  "Protobuf.RichLocalDate" should {
    "convert java.time.LocalDate to protobuf.Timestamp with default local time" in {
      forAll(
        Table(
          ("date", "timestamp"),
          (LocalDate.of(2021, 5, 13), timestamp(1620853200))
        )
      ) { (date, timestamp) =>
        date.toProtobufTimestamp() shouldBe timestamp
      }
    }
    "convert java.time.LocalDate to protobuf.Timestamp with custom local time" in {
      forAll(
        Table(
          ("date", "timestamp"),
          (LocalDate.of(2021, 5, 13), timestamp(1620939599, 999999999))
        )
      ) { (date, timestamp) =>
        date.toProtobufTimestamp(time = LocalTime.MAX) shouldBe timestamp
      }
    }
  }

  private def timestamp(seconds: Long, nanos: Int = 0) =
    Timestamp
      .newBuilder()
      .setSeconds(seconds)
      .setNanos(nanos)
      .build()
}
