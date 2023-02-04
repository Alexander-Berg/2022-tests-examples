package ru.auto.api.services.settings

import org.scalatest.Ignore
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.HttpClientSuite

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 22.02.18
  */
@Ignore
class DefaultSettingsClientIntSpec extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("http", "personal-api-01-sas.test.vertis.yandex.net", 36900)

  val settingsClient = new DefaultSettingsClient(http)

  test("upsert settings") {
    val ref = PersonalUserRefGen.next
    settingsClient.clearSettings(SettingsClient.SettingsDomain, ref).futureValue

    settingsClient.updateSettings(SettingsClient.SettingsDomain, ref, Map("a" -> "1")).futureValue
    settingsClient.updateSettings(SettingsClient.SettingsDomain, ref, Map("b" -> "2")).futureValue

    settingsClient.getSettings(SettingsClient.SettingsDomain, ref).futureValue shouldBe Map(
      "a" -> "1",
      "b" -> "2"
    )
  }
}
