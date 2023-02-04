package ru.yandex.vertis.moderation.dao.impl.mysql

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.ModerationRuleDaoSpecBase
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.MySqlSpecBase

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class MySqlModerationRuleDaoSpec extends ModerationRuleDaoSpecBase with MySqlSpecBase {

  override protected def schemaScripts: Seq[String] =
    Seq(
      "/mysql/patch/05.sql",
      "/mysql/patch/06.sql",
      "/mysql/patch/07.sql",
      "/mysql/patch/14.sql",
      "/mysql/patch/15.sql",
      "/mysql/patch/17.sql"
    )

  override lazy val moderationRuleDao: MySqlModerationRuleDao = new MySqlModerationRuleDao(Service.REALTY, database)
}
