package ru.yandex.vertis.telepony.vox

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.AppCallToken
import ru.yandex.vertis.telepony.vox.AppBackCallEvent._

import scala.concurrent.duration._

class AppBackCustomDataJsonConversionSpec extends SpecBase {

  private val time: DateTime = DateTime.now()

  "AppBackCustomDataJsonConversion" should {
    "do isomorphic transformations" in {
      val events = Seq(StartScenario(time), OutCallConnected(time), InCallDisconnected(time), ScenarioTerminated(time))
      val expectedCustomData = AppBackCallCustomData(
        events = events,
        sourceVoxUsername = "source_username",
        targetVoxUsername = "target_username",
        talkDuration = 10.minutes,
        callToken = AppCallToken("token"),
        recordUrl = Some("http://example.com/something"),
        uuid = Some("Some123"),
        payloadJson = Some("{description: 'Some JSON-like String'}")
      )

      val view = expectedCustomData.toView
      val actualCustomData = AppBackCallCustomData.fromView(view).get
      actualCustomData shouldBe expectedCustomData
    }
  }
}
