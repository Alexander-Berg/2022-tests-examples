package ru.auto.tests.publicapi.operations.feeds.tasks

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.GetHistoryFeedOper
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait GetFeedsHistoryOps {

  def api: ApiClient

  @Step("Получаем историю загрузок фидов")
  def getHistory(): GetHistoryFeedOper = {
    api.feeds.getHistoryFeed
      .reqSpec(defaultSpec)
  }

}
