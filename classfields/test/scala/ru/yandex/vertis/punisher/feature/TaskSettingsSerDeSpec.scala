package ru.yandex.vertis.punisher.feature

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.feature.UserPunisherFeatureTypes._
import ru.yandex.vertis.punisher.tasks.settings.TaskSettings
import spray.json.DefaultJsonProtocol.jsonFormat4
import spray.json._

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class TaskSettingsSerDeSpec extends BaseSpec {

  "TaskSettings serialization/deserialization" should {

    implicit val taskSettingsFormat = jsonFormat4(TaskSettings.apply)

    "work correctly for FiniteDuration" in {
      val fd1 = 15.seconds
      val fd2 = FiniteDuration(20, "seconds")

      fd1.toJson.convertTo[FiniteDuration] shouldBe fd1
      fd2.toJson.convertTo[FiniteDuration] shouldBe fd2
    }

    "work correctly for TaskSettings" in {
      val ts1 = TaskSettings(stepMin = 1.seconds, stepMax = 2.minutes, stepBack = 3.hours, maxGap = 7.days)

      ts1.toJson.convertTo[TaskSettings] shouldBe ts1
    }

    "work correctly for string with escaped quotes" in {
      val s =
        "{\"stepMin\":{\"length\":30,\"unit\":\"MINUTES\"},\"stepMax\":{\"length\":30,\"unit\":\"MINUTES\"},\"stepBack\":{\"length\":30,\"unit\":\"MINUTES\"},\"maxGap\":{\"length\":7,\"unit\":\"DAYS\"}}"
      val ts =
        TaskSettings(
          stepMin = 30.minutes,
          stepMax = 30.minutes,
          stepBack = 30.minutes,
          maxGap = 7.days
        )
      s.parseJson.convertTo[TaskSettings] shouldBe ts
    }
  }
}
