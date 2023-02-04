package ru.vertistraf.notification_center.decoder

import ru.vertistraf.notification_center.model.ExpFlag
import zio.test.Assertion._
import zio.test._

import java.nio.charset.StandardCharsets
import java.util.Base64

object UaasFlagsHeaderDecoderSpec extends DefaultRunnableSpec {

  private def testId(handlerName: String, flags: Seq[ExpFlag]): ExpFlag = {
    val json =
      s"""
        |[
        | {
        |  "HANDLER": "$handlerName",
        |  "CONTEXT": {
        |    "MAIN": {
        |      "AUTORU_APP": {
        |       "flags": [${flags.map(flag => "\"" + flag + "\"").mkString(", ")}]
        |      }
        |    }
        |  }
        | }
        |]
        |""".stripMargin
    encode(json)
  }

  private val NCHandler = "AUTORU_APP"

  private def encode(input: String): String =
    Base64.getEncoder.encodeToString(input.getBytes(StandardCharsets.UTF_8))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("UaasFlagsHeaderDecoder") {
      testM("Parse NC handler and ignore others") {
        val ncFlags = Seq("flag1", "flag2")
        val otherFlags = Seq("flag3", "flag4")
        val ncTestId = testId(NCHandler, ncFlags)
        val otherTestId = testId("OTHER_HANDLER", otherFlags)
        val header = s"$ncTestId,$otherTestId"
        for {
          parsedFlags <- UaasFlagsHeaderDecoder.decode(header)
        } yield assert(parsedFlags)(equalTo(ncFlags))
      }
    }
}
