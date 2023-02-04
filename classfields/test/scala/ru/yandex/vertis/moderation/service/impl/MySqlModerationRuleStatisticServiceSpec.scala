package ru.yandex.vertis.moderation.service.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.{ModerationRuleStatisticsService, ModerationRuleStatisticsServiceSpecBase}
import ru.yandex.vertis.moderation.util.MySqlSpecBase

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class MySqlModerationRuleStatisticServiceSpec extends ModerationRuleStatisticsServiceSpecBase with MySqlSpecBase {

  override protected lazy val ruleStatisticsService: ModerationRuleStatisticsService =
    new MySqlModerationRuleStatisticsService(Service.REALTY, database)
}
