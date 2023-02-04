package ru.auto.api.model.bunker.telepony

import java.io.ByteArrayInputStream

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.model.AutoruDealer

class CallbackWithExtendedInfoTest extends AnyFunSuite {

  test("parse CallbackWithExtendedInfoDealerIds from test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/telepony_callback_with_extended_info",
        |"content": {
        |  "client_ids": [123, 456]
        |}
        |}]""".stripMargin.getBytes("UTF-8"))
    val callback: CallbackWithExtendedInfo = CallbackWithExtendedInfo.parse(in)
    assert(callback.clientIds == Set(AutoruDealer(123), AutoruDealer(456)))
  }

  test("parse CallbackWithExtendedInfoDealerIds from empty test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/telepony_callback_with_extended_info",
        |"content": {
        |  "client_ids": []
        |}
        |}]""".stripMargin.getBytes("UTF-8"))
    val callback: CallbackWithExtendedInfo = CallbackWithExtendedInfo.parse(in)
    assert(callback.clientIds == Set())
  }

  test("parse CallbackWithExtendedInfoDealerIds from unexpected test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/telepony_callback_with_extended_info",
        |"content": {"title":"test"}
        |}]""".stripMargin.getBytes("UTF-8"))
    val callback: CallbackWithExtendedInfo = CallbackWithExtendedInfo.parse(in)
    assert(callback.clientIds == Set())
  }

  test("parse CallbackWithExtendedInfoDealerIds from no test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/other_node",
        |"content": {"title":"test"}
        |}]""".stripMargin.getBytes("UTF-8"))
    val callback: CallbackWithExtendedInfo = CallbackWithExtendedInfo.parse(in)
    assert(callback.clientIds == Set())
  }

}
