package ru.auto.tests.publicapi.operations.feeds.settings

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.GetSettingsFeedOper
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait GetFeedsSettingsOps {

  def api: ApiClient

  @Step("Получаем настройки фидов")
  def getSettings(): GetSettingsFeedOper = {
    api.feeds.getSettingsFeed
      .reqSpec(defaultSpec)
  }

}
