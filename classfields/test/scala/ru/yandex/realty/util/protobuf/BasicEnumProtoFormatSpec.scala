package ru.yandex.realty.util.protobuf

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.generator.EnumGenerators
import ru.yandex.realty.util.protobuf.BasicEnumProtoFormats.{
  CallTypeProtoFormat,
  CategoryTypeProtoFormat,
  OfferTypeProtoFormat
}

/**
  * User: abulychev
  * Date: 14.06.2018
  */
@RunWith(classOf[JUnitRunner])
class BasicEnumProtoFormatSpec extends ProtoFormatSpecBase {

  testEnumFormat(OfferTypeProtoFormat, EnumGenerators.OfferTypeGen)

  testEnumFormat(CategoryTypeProtoFormat, EnumGenerators.CategoryTypeGen)

  testEnumFormat(CallTypeProtoFormat, EnumGenerators.CallTypeGen)

}
