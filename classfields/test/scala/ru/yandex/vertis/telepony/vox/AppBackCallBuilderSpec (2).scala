package ru.yandex.vertis.telepony.vox

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.CallResults._
import ru.yandex.vertis.telepony.vox.AppBackCallEvent._

class AppBackCallBuilderSpec extends SpecBase {

  private val time: DateTime = DateTime.now()

  private val outCallConnected = OutCallConnected(time)
  private val outCallFailed = OutCallFailed(time, Some(486))
  private val inCallDisconnected = InCallDisconnected(time)
  private val outCallDisconnected = OutCallDisconnected(time)
  private val noRedirectCall = NoRedirectCall(time)

  val callResultMap = Map(
    Seq(noRedirectCall) -> NoRedirect,
    Seq(inCallDisconnected) -> StopCaller,
    Seq(outCallFailed) -> BusyCallee,
    Seq(outCallConnected, inCallDisconnected) -> Success,
    Seq(outCallConnected, outCallDisconnected) -> Success
  )

  val incorrectEventSeq = Set(
    Seq(outCallConnected, outCallFailed),
    Seq()
  )

  "AppBackCallBuilder" should {
    "get call result by event sequence" in {
      callResultMap.foreach {
        case (events, callResult) =>
          VoxAppBackCallBuilder.getCallResult(events.toSet) shouldBe callResult
      }
    }

    "get unknown call result for incorrect events sequence" in {
      incorrectEventSeq.foreach { events =>
        VoxAppBackCallBuilder.getCallResult(events.toSet) shouldBe Unknown
      }
    }
  }
}
