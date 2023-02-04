package auto.carfax.common.utils.misc

import auto.carfax.common.utils.misc.DateTimeUtils._
import auto.carfax.common.utils.protobuf.ProtobufConverterOps._
import com.google.protobuf.util.Timestamps
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class DateTimeUtilsTest extends AnyFunSuite {

  test("convert local date time to timestamps") {
    val dates = List(
      LocalDate.of(2010, 6, 1),
      LocalDate.of(2020, 6, 1)
    )
    val timestamps = dates.map(date => Timestamps.fromMillis(date.getMillis))
    val convertedDates = timestamps.map(timestamp => timestamp.toLocalDateTime().toLocalDate)

    assert(dates === convertedDates)
  }

}
