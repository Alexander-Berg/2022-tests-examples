package ru.yandex.vertis.moderation.dao

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.ModerationRuleDao.Sort
import ru.yandex.vertis.moderation.util.{Page, SlicedResult}
import org.mockito.Mockito._
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.rule.ModerationRuleImpl

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class ValidateFullModerationRuleDaoSpec extends SpecBase {

  case class TestCase(name: String, input: String, expectedResult: Boolean)

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        name = "works for one word",
        input = "\"bmw\"",
        expectedResult = true
      ),
      TestCase(
        name = "works for two words",
        input = "\"продам bmw\"",
        expectedResult = true
      ),
      TestCase(
        name = "works with numbers",
        input = "\"продам bmw i8\"",
        expectedResult = true
      ),
      TestCase(
        name = "working with capital english letters",
        input = "\"продам BMW i8\"",
        expectedResult = true
      ),
      TestCase(
        name = "not working with capital russian letters",
        input = "\"Продам bmw i8\"",
        expectedResult = true
      ),
      TestCase(
        name = "not working with non-russian and non-english letters",
        input = "\"прöдам bmw i8\"",
        expectedResult = false
      ),
      TestCase(
        name = "not working with not alphanumeric or space character",
        input = "\"продам bmw i8!\"",
        expectedResult = false
      ),
      TestCase(
        name = "not working with whitespace letter (except space)",
        input = "\"продам bmw\ni8\"",
        expectedResult = false
      ),
      TestCase(
        name = "not working when first symbol is space",
        input = "\" продам bmw i8\"",
        expectedResult = false
      ),
      TestCase(
        name = "not working when last symbol is space",
        input = "\"продам bmw i8 \"",
        expectedResult = false
      ),
      TestCase(
        name = "not working with two spaces in a row",
        input = "\"продам  bmw i8\"",
        expectedResult = false
      )
    )

  "ZoneFieldRegex" should {

    testCases.foreach { case TestCase(name, input, expectedResult) =>
      name in {
        val actualResult = ValidateUpdateModerationRuleDao.ZoneFieldRegex.pattern.matcher(input).matches
        actualResult shouldBe expectedResult
      }
    }
  }

  "ValidateModerationRuleDao" should {
    "validate search attributes for getByFilter" in {
      val dao = mock[ModerationRuleDao]
      val ruleDao =
        new DelegateModerationRuleDao(dao) with ValidateFullModerationRuleDao {
          override def service: Model.Service = Model.Service.USERS_AUTORU
          implicit override protected def ec: ExecutionContext = scala.concurrent.ExecutionContext.global
          override protected def searchInstanceDao: SearchInstanceDao = mock[SearchInstanceDao]
          override protected def featureRegistry: FeatureRegistry = mock[FeatureRegistry]
        }

      val rule = mock[ModerationRuleImpl]
      when(rule.searchAttributes).thenReturn(
        Map(
          "unknown_flag" -> "unknown_flag",
          "min_meta_signals_WARN_AUTOCODE_NEW_REGISTRATION_count" -> "123",
          "meta_signals_maybe_inaccurate" -> "false"
        )
      )

      when(
        dao.getByFilter(ModerationRuleDao.Filter(), Page(0, 1), Sort.ByCreateTime())
      ).thenReturn(Future.successful(SlicedResult(Seq(rule), 1, Page(0, 1))))

      ruleDao
        .getByFilter(ModerationRuleDao.Filter(), Page(0, 1), Sort.ByCreateTime())
        .failed
        .futureValue shouldBe a[IllegalArgumentException]

      when(rule.searchAttributes).thenReturn(
        Map(
          "min_meta_signals_WARN_AUTOCODE_NEW_REGISTRATION_count" -> "123",
          "meta_signals_maybe_inaccurate" -> "false"
        )
      )

      ruleDao
        .getByFilter(ModerationRuleDao.Filter(), Page(0, 1), Sort.ByCreateTime())
        .futureValue shouldBe SlicedResult(Seq(rule), 1, Page(0, 1))
    }
  }
}
