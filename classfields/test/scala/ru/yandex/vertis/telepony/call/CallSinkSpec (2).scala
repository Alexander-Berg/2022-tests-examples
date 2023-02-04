package ru.yandex.vertis.telepony.call

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, verifyNoInteractions}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.generator.TeleponyCallGenerator.{TeleponyNonRedirectBlockedCallGen, TeleponyNormalRedirectCallGen, TeleponyRedirectBlockedCallGen}
import ru.yandex.vertis.telepony.model.TeleponyCall

import scala.concurrent.Future

class CallSinkSpec extends SpecBase with ScalaCheckPropertyChecks with MockitoSupport {

  trait TestEnvironment {
    val blockedCallsSinkMock = mock[BlockedCallsSink]
    when(blockedCallsSinkMock.write(?)(?)).thenReturn(Future.unit)

    val normalCallsSinkMock = mock[NormalCallsSink]
    when(normalCallsSinkMock.write(?)(?)).thenReturn(Future.unit)

    val callSink = new CallsSink(blockedCallsSinkMock, normalCallsSinkMock)
  }

  "CallsSink.write" should {
    "only write to sink for normal calls" when {
      "TeleponyCall.callType is not RedirectCall" in {
        val call: TeleponyCall = TeleponyNonRedirectBlockedCallGen.next
        new TestEnvironment {
          callSink.write(call).futureValue

          verify(normalCallsSinkMock).write(ArgumentMatchers.eq(call))(?)
          verifyNoInteractions(blockedCallsSinkMock)
        }
      }
    }

    "TeleponyCall.callType is RedirectCall" when {
      "its callResult is other than Blocked" in {
        val call: TeleponyCall = TeleponyNormalRedirectCallGen.next
        new TestEnvironment {
          callSink.write(call).futureValue

          verify(normalCallsSinkMock).write(ArgumentMatchers.eq(call))(?)
          verifyNoInteractions(blockedCallsSinkMock)
        }
      }
    }

    "only write to sink for blocked calls" when {
      "call is blocked" in {
        val call: TeleponyCall = TeleponyRedirectBlockedCallGen.next
        new TestEnvironment {
          callSink.write(call).futureValue

          verify(blockedCallsSinkMock).write(ArgumentMatchers.eq(call))(?)
          verifyNoInteractions(normalCallsSinkMock)
        }
      }
    }
  }
}
