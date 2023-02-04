package ru.yandex.vertis.moderation.dao

import org.scalacheck.Gen
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.ModerationRuleDao.Sort.{ByCreateTime, ByName, ByUpdateTime}
import ru.yandex.vertis.moderation.dao.ModerationRuleDao.{Filter, Sort}
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.DomainGen
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.rule.Generators.{moderationRuleGen, HoboActionGen}
import ru.yandex.vertis.moderation.rule.{
  ModerationAction,
  ModerationRule,
  ModerationRuleImpl,
  RuleApplyingPolicy,
  State
}
import ru.yandex.vertis.moderation.util.DateTimeUtil.OrderedDateTime
import ru.yandex.vertis.moderation.util.ModerationRuleUtil.ruleToSource
import ru.yandex.vertis.moderation.util.{DateTimeUtil, OptInterval, Page}
import ru.yandex.vertis.moderation.rule.Generators._
import ru.yandex.vertis.moderation.rule.ModerationAction.{Ban, Hobo, Warn}
import ru.yandex.vertis.moderation.rule.ModerationRule.Tags

import scala.concurrent.{ExecutionContext, Future}

/**
  * Spec for [[ru.yandex.vertis.moderation.dao.ModerationRuleDao]]
  *
  * @author potseluev
  */
trait ModerationRuleDaoSpecBase extends SpecBase {

  import ModerationRuleDaoSpecBase._

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def moderationRuleDao: ModerationRuleDao

  private lazy val ModerationRuleGen: Gen[ModerationRuleImpl] = moderationRuleGen(moderationRuleDao.service)

  private def checkFilterCorrectly(relatedRulesGen: Gen[ModerationRuleImpl],
                                   otherRulesGen: Gen[ModerationRuleImpl],
                                   filter: ModerationRuleDao.Filter
                                  ): Unit = {
    moderationRuleDao.deleteAll().futureValue
    val relatedRules = relatedRulesGen.next(3)
    val otherRules = otherRulesGen.next(5)
    Future.traverse(relatedRules ++ otherRules)(moderationRuleDao.add(_)).futureValue
    val found = moderationRuleDao.getByFilter(filter, Page(0, 10)).futureValue
    found.values.size shouldBe relatedRules.size
  }

  private def checkSortCorrectly(rulesGen: Gen[ModerationRuleImpl],
                                 sort: Sort,
                                 lt: (ModerationRule, ModerationRule) => Boolean
                                ): Unit = {
    val rules = ModerationRuleGen.next(10)
    Future.traverse(rules)(moderationRuleDao.add(_)).futureValue
    val slice = Page(0, 100)
    val found =
      moderationRuleDao
        .getByFilter(
          ModerationRuleDao.Filter(),
          slice,
          sort = sort
        )
        .futureValue
        .values
    found shouldBe found.sortWith(lt)
  }

  val commentTestCases: Seq[CommentTestCase] =
    Seq(
      CommentTestCase(comment = "%soMe Text%"),
      CommentTestCase(comment = "%soMe Text"),
      CommentTestCase(comment = "soMe Text%"),
      CommentTestCase(comment = "soMe Text")
    )

  val filterVinTestCases: Seq[Filter] =
    Seq(
      "kmj*",
      "KmJ*",
      "KMJ*"
    ).map(v => Filter(filterVin = Some(v)))

  val filterPhonesTestCases: Seq[String] =
    Seq(
      "7923*",
      "7999*",
      "79*"
    )

  "ModerationRuleDao" should {
    "get by rule_ids correctly" in {
      val rules = ModerationRuleGen.next(5)
      moderationRuleDao.deleteAll().futureValue
      Future.traverse(rules)(moderationRuleDao.add(_)).futureValue
      val all = moderationRuleDao.getByFilter(Filter(), Page(0, 10)).futureValue
      val size = 2
      val ids = all.map(_.id).take(size).mkString(",")
      val filter = Filter(ruleIds = Some(ids))
      val found = moderationRuleDao.getByFilter(filter, Page(0, 10)).futureValue
      found.values.size shouldBe size
    }

    "add correctly" in {
      val rule = ModerationRuleGen.next
      val actual = moderationRuleDao.add(rule).futureValue
      val expected = rule.copy(id = actual.id, createTime = actual.createTime, updateTime = actual.updateTime)
      actual shouldBe expected
    }

    "delete correctly" in {
      val rule = ModerationRuleGen.next
      val id = moderationRuleDao.add(rule).futureValue.id
      moderationRuleDao.deleteById(id).futureValue
      val updatedRule = moderationRuleDao.getById(id).futureValue.get
      updatedRule.isDeleted shouldBe true
      updatedRule.state shouldBe State.Ignore
    }

    "fail on delete by not existed id" in {
      moderationRuleDao.deleteById(Int.MaxValue).shouldCompleteWithException[NoSuchElementException]
    }

    "fail on update by not existed id" in {
      moderationRuleDao.update(Int.MaxValue, ModerationRuleGen.next).shouldCompleteWithException[NoSuchElementException]
    }

    "not return deleted rules on get by filter" in {
      val notDeletedRules = ModerationRuleGen.next(3)
      val deletedRules = ModerationRuleGen.next(5)
      moderationRuleDao.deleteAll().futureValue
      (for {
        _   <- Future.traverse(notDeletedRules)(moderationRuleDao.add(_))
        ids <- Future.traverse(deletedRules)(moderationRuleDao.add(_).map(_.id))
        _   <- Future.traverse(ids)(moderationRuleDao.deleteById)
      } yield ()).futureValue
      val allFound = moderationRuleDao.getByFilter(Filter(), Page(0, 10)).futureValue
      allFound.values.size shouldBe (notDeletedRules ++ deletedRules).size
      val notDeletedFound = moderationRuleDao.getByFilter(Filter(isDeleted = Some(false)), Page(0, 10)).futureValue
      notDeletedFound.values.size shouldBe notDeletedRules.size
    }

    "get by filter.state correctly" in {
      val state = State(RuleApplyingPolicy.Do, RuleApplyingPolicy.Undo)
      val relatedRules = ModerationRuleGen.map(_.copy(state = state))
      val otherRules = ModerationRuleGen.suchThat(_.state != state)
      checkFilterCorrectly(relatedRules, otherRules, ModerationRuleDao.Filter(state = Some(state)))
    }

    "get by filter.user correctly" in {
      val activeRules = ModerationRuleGen.map(_.copy(userId = "user"))
      val otherRules = ModerationRuleGen
      checkFilterCorrectly(activeRules, otherRules, ModerationRuleDao.Filter(userId = Some("user")))
    }

    "get by filter.sql correctly" in {
      val relatedRules =
        ModerationRuleGen
          .withSearchAttribute("object_id", "0123,1234,2345")
          .withSearchAttribute("max_create_time", "100")
      checkFilterCorrectly(
        relatedRules,
        ModerationRuleGen,
        ModerationRuleDao.Filter(
          sqlWhere =
            Some(
              "AND CAST(filter->'$.max_create_time' AS UNSIGNED) < 200 AND filter->'$.object_id' LIKE '%1234%'"
            )
        )
      )
    }

    "get by filter.actionType correctly" in {
      val relatedRules = ModerationRuleGen.withAction(HoboActionGen.next)
      val otherRules =
        ModerationRuleGen.suchThat(_.action match {
          case _: ModerationAction.Hobo => false
          case _                        => true
        })
      checkFilterCorrectly(
        relatedRules,
        otherRules,
        ModerationRuleDao.Filter(
          actionType = Some("hobo")
        )
      )
    }

    "get by filter.reason correctly" in {
      val relatedRules =
        ModerationRuleGen
          .withAction(ModerationAction.Warn(Set(DomainGen.next), DetailedReason.WrongPrice, None))
      val otherRules =
        ModerationRuleGen.suchThat(_.action match {
          case ModerationAction(DetailedReason.WrongPrice) => false
          case _                                           => true
        })
      checkFilterCorrectly(
        relatedRules,
        otherRules,
        ModerationRuleDao.Filter(
          reason = Some("WRONG_PRICE")
        )
      )
    }

    "get by filter.comment correctly" in {
      val actionWithComment = ModerationActionGen.next.withComment(Some("Some text comment"))
      val relatedRules = ModerationRuleGen.withAction(actionWithComment)
      checkFilterCorrectly(
        relatedRules,
        ModerationRuleGen,
        ModerationRuleDao.Filter(
          comment = Some("%text%")
        )
      )
    }

    "get by filter.tag correctly" in {
      val relatedRules =
        ModerationRuleGen.withTags(
          Map(
            "tag1" -> "Resolution",
            "tag2" -> "Resolution2"
          )
        )
      checkFilterCorrectly(
        relatedRules,
        ModerationRuleGen,
        ModerationRuleDao.Filter(
          filterTag = Some("esoluti")
        )
      )
    }

    "get by filter.createTime correctly" in {
      moderationRuleDao.deleteAll().futureValue
      val relatedRules = ModerationRuleGen.next(3)
      val createTime = DateTimeUtil.now().minusMinutes(1)
      Future
        .traverse(relatedRules)(rule =>
          for {
            id <- moderationRuleDao.add(rule).map(_.id)
            _  <- moderationRuleDao.setCreateTime(id, createTime)
          } yield ()
        )
        .futureValue
      val otherRules = ModerationRuleGen.next(5)
      Future.traverse(otherRules)(moderationRuleDao.add(_)).futureValue
      val found =
        moderationRuleDao
          .getByFilter(
            ModerationRuleDao.Filter(
              createTime =
                OptInterval(
                  min = Some(createTime.minusSeconds(5)),
                  max = Some(createTime.plusSeconds(5))
                )
            ),
            Page(0, 10)
          )
          .futureValue
      found.values.size shouldBe relatedRules.size
    }

    commentTestCases.foreach { case CommentTestCase(str) =>
      val description = s"get by filter.comment case insensitive for string like: $str"
      description in {
        val action = ModerationActionGen.next.withComment(Some("one more Some texT commeNt"))
        val relatedRules = ModerationRuleGen.withAction(action)
        checkFilterCorrectly(
          relatedRules,
          ModerationRuleGen,
          ModerationRuleDao.Filter(
            comment = Some(str)
          )
        )
      }
    }

    "do pagination correctly" in {
      moderationRuleDao.deleteAll().futureValue
      val rules = ModerationRuleGen.next(10)
      Future.traverse(rules)(moderationRuleDao.add(_)).futureValue
      val slice = Page(1, 3)
      val found = moderationRuleDao.getByFilter(ModerationRuleDao.Filter(), slice).futureValue
      found.values.size shouldBe slice.length
      found.total shouldBe rules.size
      found.slice shouldBe slice
    }

    "update correctly" in {
      val rule = ModerationRuleGen.next
      val id = moderationRuleDao.add(rule).futureValue.id
      val newRule = ModerationRuleGen.next
      moderationRuleDao.update(id, newRule).futureValue.id shouldBe id
      val actual = moderationRuleDao.getById(id).futureValue.get
      val expected = newRule.copy(id = id, createTime = actual.createTime, updateTime = actual.updateTime)
      actual shouldBe expected
    }

    "sort by create time correctly" in {
      checkSortCorrectly(ModerationRuleGen, sort = ByCreateTime(), lt = by(_.createTime))
    }

    "sort by name correctly (case insensitive)" in {
      checkSortCorrectly(
        ModerationRuleGen,
        sort = ByName(asc = false),
        lt = by(_.action.comment.map(_.toLowerCase), asc = false)
      )
    }

    "sort by update time correctly" in {
      checkSortCorrectly(ModerationRuleGen, sort = ByUpdateTime(), lt = by(_.updateTime))
    }

    "change state correctly" in {
      val ruleSource = ModerationRuleGen.next
      val rule = moderationRuleDao.add(ruleSource).futureValue
      val newState = ModerationRuleStateGen.next
      moderationRuleDao.setState(rule.id, state = newState).futureValue
      moderationRuleDao.getById(rule.id).futureValue.get.state shouldBe newState
    }

    "get by filter.checkType correctly" in {
      val relatedRules =
        ModerationRuleGen
          .withAction(HoboActionGen.next.copy(checkType = HoboCheckType.AUTOMATIC_QUALITY_CHECK_FRAUD))
      val otherRules =
        ModerationRuleGen.suchThat(_.action match {
          case _: ModerationAction.Hobo => false
          case _                        => true
        })
      checkFilterCorrectly(
        relatedRules,
        otherRules,
        ModerationRuleDao.Filter(
          actionCheckType = Some("AUTOMATIC_QUALITY_CHECK_FRAUD")
        )
      )
    }

    "get by filter.mark correctly" in {
      val relatedRules =
        ModerationRuleGen
          .withSearchAttribute("mark", "VAZ")
          .withService(Service.AUTORU)
      checkFilterCorrectly(
        relatedRules,
        ModerationRuleGen,
        ModerationRuleDao.Filter(
          filterMark = Some("VAZ")
        )
      )
    }

    "get by filter.model correctly" in {
      val relatedRules =
        ModerationRuleGen
          .withSearchAttribute("model", "2110")
          .withService(Service.AUTORU)
      checkFilterCorrectly(
        relatedRules,
        ModerationRuleGen,
        ModerationRuleDao.Filter(
          filterModel = Some("2110")
        )
      )
    }

    "get by filter.section correctly" in {
      val relatedRules =
        ModerationRuleGen
          .withSearchAttribute("section", "USED")
          .withService(Service.AUTORU)
      checkFilterCorrectly(
        relatedRules,
        ModerationRuleGen,
        ModerationRuleDao.Filter(
          filterSection = Some("USED")
        )
      )
    }

    "get by filter.category correctly" in {
      val relatedRules =
        ModerationRuleGen
          .withSearchAttribute("category", "CARS")
          .withService(Service.AUTORU)
      checkFilterCorrectly(
        relatedRules,
        ModerationRuleGen,
        ModerationRuleDao.Filter(
          filterCategory = Some("CARS")
        )
      )
    }
  }

  "get by filter.vin correctly (case insensitive)" in {
    val relatedRules =
      ModerationRuleGen
        .withSearchAttribute("vin", "kmj*")
        .withService(Service.AUTORU)
    filterVinTestCases.foreach(checkFilterCorrectly(relatedRules, ModerationRuleGen, _))
  }

  "get by filter.phones correctly" in {
    filterPhonesTestCases
      .foreach { filterPhones =>
        val relatedRules = ModerationRuleGen.withSearchAttribute("phones", filterPhones)
        val filter = Filter(filterPhones = Some(filterPhones))
        checkFilterCorrectly(relatedRules, ModerationRuleGen, filter)
      }
  }
}

object ModerationRuleDaoSpecBase {

  implicit private class RichRuleGen(val gen: Gen[ModerationRuleImpl]) extends AnyVal {
    def withSearchAttribute(key: String, value: String): Gen[ModerationRuleImpl] =
      gen.map { rule =>
        rule.copy(searchAttributes =
          rule.searchAttributes
            .updated(key, value)
        )
      }

    def withService(service: Service): Gen[ModerationRuleImpl] = gen.map(_.copy(service = service))

    def withAction(action: ModerationAction): Gen[ModerationRuleImpl] = gen.map(_.copy(action = action))

    def withTags(tags: Tags): Gen[ModerationRuleImpl] = gen.map(_.copy(tags = tags))
  }

  implicit private class RichAction(val action: ModerationAction) extends AnyVal {
    def withComment(comment: Option[String]): ModerationAction =
      action match {
        case action: Hobo => action.copy(comment = comment)
        case action: Warn => action.copy(comment = comment)
        case action: Ban  => action.copy(comment = comment)
      }
  }

  private def by[S, T](f: S => T, asc: Boolean = true)(implicit ord: Ordering[T]): (S, S) => Boolean = { (s1, s2) =>
    if (asc) ord.lt(f(s1), f(s2))
    else ord.lt(f(s2), f(s1))
  }

  case class CommentTestCase(comment: String)

}
