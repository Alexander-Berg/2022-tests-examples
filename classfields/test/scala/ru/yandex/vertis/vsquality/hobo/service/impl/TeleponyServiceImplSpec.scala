package ru.yandex.vertis.vsquality.hobo.service.impl

import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions, when}
import org.scalatest.BeforeAndAfter

import ru.yandex.vertis.vsquality.hobo.concurrent.Threads
import ru.yandex.vertis.vsquality.hobo.model.TeleponyDomain
import ru.yandex.vertis.vsquality.hobo.telepony.AnyDomainTeleponyClient
import ru.yandex.vertis.vsquality.hobo.telepony.StubAnyDomainTeleponyClient.StubCall
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

import scala.concurrent.{ExecutionContext, Future}

class TeleponyServiceImplSpec extends SpecBase with BeforeAndAfter {
  implicit private val ec: ExecutionContext = Threads.SameThreadEc

  private val telepony = mock[AnyDomainTeleponyClient].defaultReturn(Future.unit)
  private val service = new TeleponyServiceImpl(telepony)

  before {
    reset(telepony)
  }

  "TeleponyServiceImpl" should {
    "go through happy path addToBlacklistByCallId" in {
      when(telepony.getCallInfo(any(), any()))
        .thenReturn(Future.successful(Some(StubCall)))

      service
        .addToBlacklistByCallId(
          domain = TeleponyDomain.AutoruDef,
          callId = "call-id",
          reason = "reason",
          comment = Some("comment")
        )
        .futureValue

      verify(telepony).getCallInfo(any(), any())
      verify(telepony).addToBlacklist(any())
    }

    "go through happy path addToBlacklistByPhone" in {
      service
        .addToBlacklistByPhone(
          domain = TeleponyDomain.AutoruDef,
          phone = "1112233",
          reason = "reason",
          comment = Some("comment")
        )
        .futureValue

      verify(telepony).addToBlacklist(any())
    }

    "do not blacklist if call not found" in {
      when(telepony.getCallInfo(any(), any()))
        .thenReturn(Future.successful(None))

      service
        .addToBlacklistByCallId(
          domain = TeleponyDomain.AutoruDef,
          callId = "call-id",
          reason = "reason",
          comment = Some("comment")
        )
        .shouldCompleteWithException[IllegalArgumentException]

      verify(telepony).getCallInfo(any(), any())
      verifyNoMoreInteractions(telepony)
    }
  }

}
