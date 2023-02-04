package ru.yandex.realty.util.protobuf

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.generator.TimeGenerators

/**
  * User: abulychev
  * Date: 14.06.2018
  */
@RunWith(classOf[JUnitRunner])
class TimeProtoFormatSpec extends ProtoFormatSpecBase {

  testEnumFormat(ProtobufFormats.TimeUnitProtoFormat, TimeGenerators.TimeUnitGen)

  testFormat(ProtobufFormats.DurationProtoFormat, TimeGenerators.DurationGen)

  testFormat(ProtobufFormats.TimeRangeProtoFormat, TimeGenerators.TimeRangeGen)

}
