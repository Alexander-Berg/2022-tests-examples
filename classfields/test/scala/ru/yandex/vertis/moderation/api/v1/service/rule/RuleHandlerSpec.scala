package ru.yandex.vertis.moderation.api.v1.service.rule

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import ArgumentMatchers.{eq => meq}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.backend.Environment
import ru.yandex.vertis.moderation.dao.{ModerationRuleArchiveDao, ModerationRuleDao}
import ru.yandex.vertis.moderation.dao.ModerationRuleDao.{Filter, Sort}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.rule.RuleId
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.rule.Generators._
import ru.yandex.vertis.moderation.rule.{ModerationRule, ModerationRuleImpl}
import ru.yandex.vertis.moderation.service.ModerationRuleStatisticsService
import ru.yandex.vertis.moderation.util.ModerationRuleUtil.ruleToSource
import ru.yandex.vertis.moderation.util.{HandlerSpecBase, Interval, Page, Slice, SlicedResult}
import ru.yandex.vertis.moderation.view.ViewCompanion.MarshallingContext

import scala.concurrent.Future

/**
  * Spec for [[RuleHandler]]
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class RuleHandlerSpec extends SpecBase {

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}

  private val service = Service.REALTY

  trait TestContext extends HandlerSpecBase {
    val environment: Environment = environmentRegistry(service)
    val moderationRuleDao: ModerationRuleDao = environment.moderationRuleDao
    val moderationRuleArchiveDao: ModerationRuleArchiveDao = environment.moderationRuleArchiveDao
    val ruleStatisticsService: ModerationRuleStatisticsService = environment.moderationRuleStatisticsService

    doReturn(Future.unit).when(moderationRuleArchiveDao).appendRule(any())

    override def basePath = s"/api/1.x/$service/rule"

    implicit override lazy val marshallingContext = MarshallingContext(service)
  }

  "add rule" should {

    "return 200 on correct request" in {
      new TestContext {
        val moderationRule = moderationRuleGen(service).next
        val moderationRuleSource = ruleToSource(moderationRule)
        doReturn(Future.successful(moderationRule)).when(moderationRuleDao).add(moderationRule)
        doReturn(Future.successful(moderationRule)).when(moderationRuleDao).getById(moderationRule.id)
        Post(url("/"), moderationRuleSource) ~> route ~> check {
          status shouldBe OK
          there.was(one(moderationRuleDao).add(moderationRuleSource))
          responseAs[ModerationRule] shouldBe moderationRule
        }
      }
    }
  }

  "get rule by id" should {

    "return 200 when requested rule exists" in {
      new TestContext {
        val moderationRule = moderationRuleGen(service).next
        doReturn(Future.successful(Some(moderationRule))).when(moderationRuleDao).getById(moderationRule.id)
        Get(url(s"/${moderationRule.id}")) ~> route ~> check {
          status shouldBe OK
          there.was(one(moderationRuleDao).getById(moderationRule.id))
          responseAs[ModerationRule] shouldBe moderationRule
        }
      }
    }

    "return 404 when requested rule doesn't exist" in {
      new TestContext {
        doReturn(Future.successful(None)).when(moderationRuleDao).getById(any[RuleId])
        Get(url(s"/0")) ~> route ~> check {
          status shouldBe NotFound
          there.was(one(moderationRuleDao).getById(0))
        }
      }
    }
  }

  "get rules by filter" should {

    "return 400 when some mandatory parameters aren't specified" in {
      new TestContext {
        Get(url("/")) ~> route ~> check {
          status shouldBe BadRequest
        }
      }
    }

    "return 200 and invoke correct method with filter by service" in {
      new TestContext {
        val page = Page(0, 100)
        val rules = moderationRuleGen(service).next(10).toSeq
        val result = SlicedResult(rules, rules.size, page)
        doReturn(Future.successful(result))
          .when(moderationRuleDao)
          .getByFilter(any[Filter], any[Slice], any[Sort])
        Get(url(s"/?page_number=${page.number}&page_size=${page.size}&sort=create_time")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(moderationRuleDao).getByFilter(
              Filter(isDeleted = Some(false)),
              page,
              Sort.ByCreateTime(asc = false)
            )
          )
          responseAs[SlicedResult[ModerationRule]] shouldBe result
        }
      }
    }
  }

  "update rule by id" should {

    "return 200 and invoke correct method" in {
      new TestContext {
        val rule = moderationRuleGen(service).next
        doReturn(Future.successful(rule)).when(moderationRuleDao).update(rule.id, rule)
        doReturn(Future.successful(rule)).when(moderationRuleDao).getById(rule.id)
        Put(url(s"/${rule.id}"), ruleToSource(rule)) ~> route ~> check {
          status shouldBe OK
          there.was(one(moderationRuleDao).update(rule.id, rule))
          responseAs[ModerationRule] shouldBe rule
        }
      }
    }

    "return 404 when update nonexistent rule" in {
      new TestContext {
        val rule = moderationRuleGen(service).next
        doReturn(Future.failed(new NoSuchElementException)).when(moderationRuleDao).update(rule.id, rule)
        Put(url(s"/${rule.id}"), ruleToSource(rule)) ~> route ~> check {
          status shouldBe NotFound
          there.was(one(moderationRuleDao).update(rule.id, rule))
        }
      }
    }
  }

  "delete rule by id" should {

    "return 200 and invoke correct method" in {
      new TestContext {
        val id = 0
        val rule = moderationRuleGen(service).next.copy(id = id)
        doReturn(Future.successful(rule)).when(moderationRuleDao).deleteById(id)
        Delete(url(s"/$id")) ~> route ~> check {
          status shouldBe OK
          there.was(one(moderationRuleDao).deleteById(id))
        }
      }
    }

    "return 404 when delete nonexistent rule" in {
      new TestContext {
        val id = 0
        doReturn(Future.failed(new NoSuchElementException)).when(moderationRuleDao).deleteById(id)
        Delete(url(s"/$id")) ~> route ~> check {
          status shouldBe NotFound
          there.was(one(moderationRuleDao).deleteById(id))
        }
      }
    }

  }

  "get rule statistics" should {

    "return 200 and invoke correct method" in {
      new TestContext {
        val id = 0
        val interval = TimeIntervalGen.next
        val statistics = RuleStatisticsGen.next
        doReturn(Future.successful(statistics)).when(ruleStatisticsService).getStatistics(id, interval)
        Get(url(s"/$id/statistics?from=${interval.from.getMillis}&to=${interval.to.getMillis}")) ~>
          route ~> check {

            status shouldBe OK
            there.was(one(ruleStatisticsService).getStatistics(id, interval))
          }
      }
    }

    "work without specified time interval" in {
      new TestContext {
        val id = 0
        val statistics = RuleStatisticsGen.next
        doReturn(Future.successful(statistics))
          .when(ruleStatisticsService)
          .getStatistics(meq(id), any[Interval[DateTime]])
        Get(url(s"/$id/statistics")) ~>
          route ~> check {

            status shouldBe OK
            there.was(one(ruleStatisticsService).getStatistics(meq(id), any[Interval[DateTime]]))
          }
      }
    }

  }

}
