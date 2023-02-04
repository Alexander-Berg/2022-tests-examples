package ru.yandex.vertis.moderation.dao.impl.mysql

import org.joda.time.DateTime
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.ModerationRuleArchiveDao.Archive
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.rule.Generators.moderationRuleGen
import ru.yandex.vertis.moderation.util.{DateTimeUtil, MySqlSpecBase}

import scala.concurrent.ExecutionContext

class MySqlModerationRuleArchiveDaoSpec extends SpecBase with MySqlSpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val moderationRuleDao: MySqlModerationRuleArchiveDao =
    new MySqlModerationRuleArchiveDao(Service.REALTY, database)

  "ModerationRuleArchiveDao" should {
    "append moderation rule" in {
      val time = DateTimeUtil.now()
      val time1 = DateTimeUtil.now().plusSeconds(1)
      val rule = moderationRuleGen(moderationRuleDao.service).next.copy(id = 0, updateTime = time)
      val rule1 = moderationRuleGen(moderationRuleDao.service).next.copy(id = 0, updateTime = time1)
      moderationRuleDao.getArchive(rule.id).futureValue shouldBe Archive(Seq.empty)
      moderationRuleDao.appendRule(rule).futureValue
      moderationRuleDao.getArchive(rule.id).futureValue shouldBe Archive(Seq(rule))
      moderationRuleDao.appendRule(rule1).futureValue
      moderationRuleDao.getArchive(rule.id).futureValue shouldBe Archive(Seq(rule1, rule))
    }
  }
}
