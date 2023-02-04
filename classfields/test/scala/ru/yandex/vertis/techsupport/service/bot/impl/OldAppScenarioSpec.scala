package ru.yandex.vertis.vsquality.techsupport.service.bot.impl

import cats.Id
import com.softwaremill.tagging._
import org.scalatest.Assertion
import org.scalatest.matchers.Matcher
import ru.yandex.vertis.vsquality.techsupport.model.{BotStateId, Domain, Platform, Tags, Version}
import ru.yandex.vertis.vsquality.techsupport.service.bot.OldAppScenario.OldAppContext
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.scenario.OldAppScenarioImpl
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

/**
  * @author devreggs
  */
class OldAppScenarioSpec extends SpecBase {
  def toVersion(s: String): String @@ Tags.Version = s.taggedWith[Tags.Version]
  private val scenario = new OldAppScenarioImpl[Id](Domain.Autoru)

  "OldAppScenario" should {
    "switch to correct state" in {
      import ExternalScenarios.ChoiceScenarioStates._
      import OldAppScenarioImpl.{AutoruMinAndroidVersion, AutoruMinIosVersion}
      import Platform._
      def test(os1: Option[Platform], ver: Option[Version], stateId: BotStateId): Assertion = {
        val ctx = new OldAppContext { def os: Option[Platform] = os1; def appVersion: Option[Version] = ver }
        scenario.process(ctx) shouldBe scenario.switch(stateId)
      }

      val OldIosVersion = toVersion("9.21")
      val OldAndroidVersion = toVersion("6.8.6")

      test(Some(Ios), Some(AutoruMinIosVersion), Start)
      test(Some(Ios), Some(OldIosVersion), OldestIOS)
      test(Some(Android), Some(AutoruMinAndroidVersion), Start)
      test(Some(Android), Some(OldAndroidVersion), OldestAndroid)

      test(None, Some(AutoruMinIosVersion), Start)
      test(None, Some(OldIosVersion), Start)
      test(None, Some(AutoruMinAndroidVersion), Start)
      test(None, Some(OldIosVersion), Start)
      test(None, None, Start)
    }

    "correct compare string versions" in {
      import ru.yandex.vertis.vsquality.techsupport.util.VersionUtil.VersionOrdering
      val `<`: Version => Matcher[Version] = be.< _
      val `>`: Version => Matcher[Version] = be.> _
      val `==`: Version => Matcher[Version] = x => (be >= x) and (be <= x)
      def test(v1: String, m: Version => Matcher[Version], v2: String): Assertion =
        toVersion(v1) should m(toVersion(v2)).asInstanceOf[Matcher[String]]

      test("9.23", >, "9.22")
      test(".23", <, "9.22")
      test("0.0.23", <, "1")
      test("9.23", >, "")
      test("9.23", <, "9.23.541")
      test("9.23", >, "9.1")
      test("9.23", >, "9")
      test("9.23", >, "8.500")
      test("6.8.7", >, "6.8.3_dev")
      test("0.0.0.1", ==, ".0.0.1")
    }
  }

}
