package ru.yandex.vertis.general.wisp.clients.fanout_api

import java.nio.{ByteBuffer, ByteOrder}

import general.wisp.clients.model.{TClientMessage, TInMessage, TPlain}
import zio.test.Assertion._
import zio.test._

object FanoutApiClientLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FanoutApiClientLive")(
      test("Correct check sum calculating") {
        testData.foldLeft(assertCompletes) { case (assertion, testEntry) =>
          val message = TInMessage(
            guid = testEntry.senderGuid,
            userIp = "127.0.0.1",
            clientMessage = Some(
              TClientMessage(
                body = TClientMessage.Body.Plain(
                  TPlain(
                    chatId = testEntry.chatId,
                    timestamp = testEntry.messageId,
                    moderationVerdicts = testEntry.verdicts
                  )
                )
              )
            )
          )
          val bodyWithHeader = FanoutApiClientLive.prepareBody(message)
          val dataWithHeader = ByteBuffer.wrap(bodyWithHeader).order(ByteOrder.LITTLE_ENDIAN)
          val checkSum = dataWithHeader.getLong(4)

          assertion && assert(checkSum)(equalTo(testEntry.checkSum))
        }
      }
    )

  case class TestDataEntry(senderGuid: String, chatId: String, messageId: Long, verdicts: Seq[String], checkSum: Long)

  private val testData = Seq(
    TestDataEntry(
      "19522f0b-3ed9-9d92-7850-7bb8736eabf1",
      "0/18/d97bcb32-23e0-4e7e-80ed-1e3cd7ac6190",
      177749054L,
      Seq("verdict"),
      559294136L
    ),
    TestDataEntry(
      "cf10aa01-8f18-9639-4c27-e25ff3963b4a",
      "0/18/178c8ac3-064e-47b4-9bc0-7e8f69fdef95",
      178749760L,
      Seq("verdict"),
      1642873860L
    ),
    TestDataEntry(
      "5922f60c-7e57-93bc-7828-fb5503112f2f",
      "0/18/d97bcb32-23e0-4e7e-80ed-1e3cd7ac6190",
      178945348L,
      Seq("verdict"),
      3752857824L
    ),
    TestDataEntry(
      "cf10aa01-8f18-9639-4c27-e25ff3963b4a",
      "0/18/7f4ef8ce-540b-4f54-9cea-bc44b18c0d28",
      179450367L,
      Seq("verdict"),
      516071246L
    ),
    TestDataEntry(
      "b1b1050b-ef5b-92bc-700e-8798231d6fa6",
      "0/18/d97bcb32-23e0-4e7e-80ed-1e3cd7ac6190",
      178324054L,
      Seq("verdict"),
      3831492542L
    ),
    TestDataEntry(
      "cf10aa01-8f18-9639-4c27-e25ff3963b4a",
      "0/18/7954f6a8-1439-44f7-9ac8-4aa606d2e148",
      179395032L,
      Seq("verdict"),
      3319381747L
    )
  )
}
