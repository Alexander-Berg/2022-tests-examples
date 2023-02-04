package ru.yandex.vertis.general.wizard.meta.rules

import ru.yandex.vertis.general.wizard.core.service.CategoryTagsService
import ru.yandex.vertis.general.wizard.meta.parser.{PartialParsedQuery, QueryParser, Variants}
import ru.yandex.vertis.general.wizard.meta.rules.impl.CommercialRequirement
import ru.yandex.vertis.general.wizard.model.Pragmatic.MetaPragmatic
import ru.yandex.vertis.general.wizard.model._
import ru.yandex.vertis.mockito.MockitoSupport
import zio.Task
import zio.test.Assertion._
import zio.test._

object CommercialRequirementSpec extends DefaultRunnableSpec with MockitoSupport {

  private def addCommercialIntent(parseState: ParseState): ParseState =
    parseState.copy(
      intentionMatches = Set(RequestMatch.Intention.userInputIndices(Set.empty[Int], IntentionType.Commercial))
    )

  private def testParseStateInfo(parseStateInfo: ParseStateInfo, shouldBeValid: Boolean): TestResult =
    assert(parseStateInfo == ParseStateInfo.Valid)(equalTo(shouldBeValid))

  private val metaPragmatics = Set(
    MetaPragmatic(MetaPragmaticType.CommercialRequire, "CommercialRequire", Set("карты"))
  )

  private val metaPragmaticsParser = new QueryParser[Seq[RequestMatch.MetaPragmatic]] {

    override def parse(in: PartialParsedQuery): Task[Variants[Seq[RequestMatch.MetaPragmatic]]] =
      Task.succeed(
        Seq(
          metaPragmatics
            .filter(metaPragmatic =>
              metaPragmatic.aliases.exists(alias => in.metaWizardRequest.userRequest.contains(alias))
            )
            .map(metaPragmatic => RequestMatch.MetaPragmatic.userInputIndices(Set.empty[Int], metaPragmatic.`type`))
            .toSeq
        )
      )
  }

  private val commercialRequireCategoryId = "commercial-require"

  private val categoryTagsService = new CategoryTagsService.Service {

    override def hasTag(categoryId: CategoryId, categoryTag: CategoryTag): Task[Boolean] =
      Task.succeed(
        if (categoryId == commercialRequireCategoryId && categoryTag == CategoryTag.CommercialRequire)
          true
        else
          false
      )
  }

  private val commercialRequirementNode =
    CommercialRequirement(categoryTagsService, metaPragmaticsParser)

  private val commercialRequireCategoryState = ParseState(
    MetaWizardRequest.empty("doesn't matter"),
    Some(RequestMatch.Category.userInputIndices(Set.empty[Int], commercialRequireCategoryId, 0)),
    None,
    Set.empty,
    Set.empty,
    Seq.empty,
    ParseStateInfo.Valid
  )

  private val commercialRequireCategoryFulfilledState =
    addCommercialIntent(commercialRequireCategoryState)

  private val commercialRequireIntentState = ParseState(
    MetaWizardRequest.empty("doesn't matter"),
    None,
    None,
    Set.empty,
    Set(RequestMatch.Intention.userInputIndices(Set.empty[Int], IntentionType.CommercialRequire)),
    Seq.empty,
    ParseStateInfo.Valid
  )

  private val commercialRequireIntentFulfilledState =
    addCommercialIntent(commercialRequireIntentState)

  private val commercialRequireWordState = ParseState(
    MetaWizardRequest.empty("карты"),
    None,
    None,
    Set.empty,
    Set.empty,
    Seq.empty,
    ParseStateInfo.Valid
  )

  private val commercialRequireWordFulfilledState =
    addCommercialIntent(commercialRequireWordState)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CommercialRequirement RuleNode")(
      testM("not validate commercial require category without commercial intent")(
        commercialRequirementNode
          .validate(commercialRequireCategoryState)
          .map(testParseStateInfo(_, shouldBeValid = false))
      ),
      testM("validate commercial require category if commercial intent is present")(
        commercialRequirementNode
          .validate(commercialRequireCategoryFulfilledState)
          .map(testParseStateInfo(_, shouldBeValid = true))
      ),
      testM("not validate commercial require intent without commercial intent")(
        commercialRequirementNode
          .validate(commercialRequireIntentState)
          .map(testParseStateInfo(_, shouldBeValid = false))
      ),
      testM("validate commercial require intent if commercial intent is present")(
        commercialRequirementNode
          .validate(commercialRequireIntentFulfilledState)
          .map(testParseStateInfo(_, shouldBeValid = true))
      ),
      testM("not validate commercial require word without commercial intent")(
        commercialRequirementNode
          .validate(commercialRequireWordState)
          .map(testParseStateInfo(_, shouldBeValid = false))
      ),
      testM("validate commercial require word if commercial intent is present")(
        commercialRequirementNode
          .validate(commercialRequireWordFulfilledState)
          .map(testParseStateInfo(_, shouldBeValid = true))
      )
    )
}
