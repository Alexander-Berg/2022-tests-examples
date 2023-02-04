package ru.yandex.vertis.moderation.rule.view

import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers.check
import play.api.libs.json.{Format, Json}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.rule.Generators._
import ru.yandex.vertis.moderation.rule.ModerationAction
import ru.yandex.vertis.moderation.searcher.core.saas.search.SearchClauseBuilder
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.rule.view.JsonConversions.moderationActionFormat

/**
  * Spec for [[JsonConversions]]
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class JsonConversionsSpec extends SpecBase with SearchClauseBuilder {

  "JsonConversions" should {

    "provide correctly conversions for ModerationAction" in {
      implicit val format: Format[ModerationAction] = moderationActionFormat(ServiceGen.next)

      check(forAll(ModerationActionGen) { action =>
        Json.toJson(action).as[ModerationAction] == action
      })
    }

    "provide correctly conversions for legacy format of ModerationAction" in {
      implicit val format: Format[ModerationAction] = moderationActionFormat(Service.USERS_AUTORU)
      val json =
        """
          |{
          |  "type": "ban",
          |  "reason": "DUPLICATE",
          |  "domains": [{"type": "users_autoru", "value": "LCV"}]
          |}
        """.stripMargin
      val actualResult = Json.parse(json).as[ModerationAction]
      val expectedResult =
        ModerationAction.Ban(
          domains = Set(Domain.UsersAutoru(Model.Domain.UsersAutoru.LCV)),
          detailedReason = DetailedReason.Duplicate(None, Set.empty, None, Set.empty),
          comment = None
        )
      actualResult shouldBe expectedResult
    }

    "provide correctly conversions for ModerationAction.Hobo without check type (use default value)" in {
      val service = ServiceGen.next
      implicit val format: Format[ModerationAction] = moderationActionFormat(service)
      val json =
        """
          |{
          |  "type": "hobo"
          |}
        """.stripMargin
      val actualResult = Json.parse(json).as[ModerationAction]
      val expectedResult =
        ModerationAction.Hobo(
          comment = None,
          taskPriority = None,
          checkType = HoboCheckType.MODERATION_RULES
        )
      actualResult shouldBe expectedResult
    }
  }
}
